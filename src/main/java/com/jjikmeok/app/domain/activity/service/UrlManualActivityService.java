package com.jjikmeok.app.domain.activity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjikmeok.app.domain.activity.converter.ActivityConverter;
import com.jjikmeok.app.domain.activity.dto.response.ActivityDetailResponse;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.enums.ActivityCategory;
import com.jjikmeok.app.domain.activity.enums.ActivityType;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.PreferenceTag;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.activity.publicactivity.service.CategoryClassifier;
import com.jjikmeok.app.domain.region.entity.Region;
import com.jjikmeok.app.domain.region.repository.RegionRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UrlManualActivityService {

    private static final String DEFAULT_DESCRIPTION = "상세 설명은 원문에서 확인하세요.";
    private static final Set<String> TRACKING_QUERY_PARAMS = Set.of(
            "fbclid", "gclid", "igshid", "igsh", "mc_cid", "mc_eid", "si"
    );

    private final ActivityRepository activityRepository;
    private final RegionRepository regionRepository;
    private final CategoryClassifier classifier;
    private final ObjectMapper objectMapper;
    private final ActivityTagSuggestionService activityTagSuggestionService;
    private final ActivityTagAutoAttachService activityTagAutoAttachService;
    private final RestClient restClient = RestClient.create();

    @Value("${app.activity-sync.default-region-id:1}")
    private Long defaultRegionId;

    public Preview preview(String sourceUrl) {
        URI uri = validateUri(sourceUrl);
        if (!robotsAllowed(uri)) {
            throw new CustomException(ErrorCode.ACTIVITY_INVALID_URI);
        }
        String html = restClient.get().uri(uri).retrieve().body(String.class);
        return previewFromHtml(normalizeUri(uri), html == null ? "" : html);
    }

    Preview previewFromHtml(String sourceUrl, String html) {
        String normalizedSourceUrl = normalizeUri(validateUri(sourceUrl));
        JsonNode jsonLd = jsonLd(html);
        String visible = strip(html);
        String title = previewTitle(html, visible, jsonLd);
        String description = first(
                meta(html, "og:description"),
                meta(html, "twitter:description"),
                meta(html, "description"),
                jsonText(jsonLd, "description"),
                regex(visible, "(?:설명|소개|내용|description)[:：]\\s*([^\\n]+)")
        );
        String thumbnailUrl = first(meta(html, "og:image"), meta(html, "twitter:image"), jsonImage(jsonLd));
        String address = cleanAddress(first(jsonLocation(jsonLd), regex(visible, "(?:장소|주소|위치|place|location)[:：]\\s*([^\\n]+)")));
        String organizer = cleanOrganizer(first(jsonNamedValue(jsonLd, "organizer"), jsonNamedValue(jsonLd, "provider"),
                regex(visible, "(?:주최|주관|운영기관|교육기관|기관명|organizer)[:：]\\s*([^\\n]+)")));
        String contactInfo = regex(visible, "(?:문의|전화|연락처|contact)[:：]\\s*([^\\n]+)");
        String target = regex(visible, "(?:대상|target)[:：]\\s*([^\\n]+)");
        LocalDateTime startAt = first(date(jsonText(jsonLd, "startDate")), date(meta(html, "event:start_time")), regexDate(visible, 0));
        LocalDateTime endAt = first(date(jsonText(jsonLd, "endDate")), date(meta(html, "event:end_time")), regexDate(visible, 1));
        Integer price = price(jsonPrice(jsonLd), regex(visible, "(?:가격|금액|참가비|수강료|price|fee)[:：]?\\s*([^\\n]{1,160})"));
        if (title == null) title = normalizedSourceUrl;
        if (description == null) description = DEFAULT_DESCRIPTION;
        String text = (title + " " + description + " " + visible).substring(0, Math.min(2000, (title + " " + description + " " + visible).length()));
        return new Preview(
                title,
                description,
                thumbnailUrl,
                normalizedSourceUrl,
                address,
                organizer,
                contactInfo,
                target,
                startAt,
                endAt,
                null,
                endAt,
                price,
                classifier.classifyCategory(SourceType.URL_MANUAL, text),
                classifier.classifyType(SourceType.URL_MANUAL, text, startAt, endAt),
                activityTagSuggestionService.suggest(
                        text,
                        classifier.classifyCategory(SourceType.URL_MANUAL, text),
                        price,
                        startAt,
                        endAt
                )
        );
    }

    @Transactional
    public ActivityDetailResponse saveManual(ManualCommand command) {
        String sourceUrl = normalizeUri(validateUri(command.sourceUrl()));
        Region region = regionRepository.findById(command.regionId() != null ? command.regionId() : defaultRegionId)
                .orElseThrow(() -> new CustomException(ErrorCode.REGION_NOT_FOUND));
        String title = command.title() == null || command.title().isBlank() ? sourceUrl : command.title().trim();
        String description = command.description() == null || command.description().isBlank() ? DEFAULT_DESCRIPTION : command.description();
        String text = title + " " + description + " " + safe(command.address());
        ActivityCategory category = command.category() != null ? command.category() : classifier.classifyCategory(SourceType.URL_MANUAL, text);
        ActivityType activityType = command.activityType() != null ? command.activityType() : classifier.classifyType(SourceType.URL_MANUAL, text, command.startAt(), command.endAt());
        String externalId = hash(sourceUrl);
        LocalDateTime recruitEndAt = command.recruitEndAt() != null ? command.recruitEndAt() : command.endAt();
        Activity existing = activityRepository.findDuplicate(SourceType.URL_MANUAL, externalId, sourceUrl, title, command.startAt(), command.address()).orElse(null);
        if (existing == null) {
            existing = activityRepository.save(Activity.builder()
                    .region(region)
                    .title(title)
                    .description(description)
                    .thumbnailUrl(command.thumbnailUrl())
                    .sourceUrl(sourceUrl)
                    .address(command.address())
                    .organizer(command.organizer())
                    .contactInfo(command.contactInfo())
                    .target(command.target())
                    .startAt(command.startAt())
                    .endAt(command.endAt())
                    .recruitStartAt(command.recruitStartAt())
                    .recruitEndAt(recruitEndAt != null ? recruitEndAt : LocalDateTime.now().plusMonths(1))
                    .price(command.price())
                    .activityType(activityType)
                    .category(category)
                    .sourceType(SourceType.URL_MANUAL)
                    .externalId(externalId)
                    .approvalStatus(ApprovalStatus.PENDING)
                    .isActive(false)
                    .build());
        } else {
            existing.update(region, title, description, command.thumbnailUrl(), sourceUrl, command.address(),
                    command.organizer(), command.contactInfo(), command.target(),
                    command.startAt(), command.endAt(), command.recruitStartAt(),
                    recruitEndAt != null ? recruitEndAt : LocalDateTime.now().plusMonths(1), command.price(), activityType,
                    category, SourceType.URL_MANUAL, externalId, ApprovalStatus.PENDING, false);
            existing.updateExtra(command.organizer(), command.contactInfo(), command.target());
        }
        activityTagAutoAttachService.refresh(existing);
        return ActivityConverter.toDetailResponse(existing);
    }

    @Transactional
    public ActivityDetailResponse approve(Long id) {
        Activity activity = find(id);
        activity.approve();
        return ActivityConverter.toDetailResponse(activity);
    }

    @Transactional
    public ActivityDetailResponse reject(Long id) {
        Activity activity = find(id);
        activity.reject();
        return ActivityConverter.toDetailResponse(activity);
    }

    @Transactional
    public ActivityDetailResponse deactivate(Long id) {
        Activity activity = find(id);
        activity.deactivate();
        return ActivityConverter.toDetailResponse(activity);
    }

    private Activity find(Long id) {
        return activityRepository.findByIdWithRegion(id)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
    }

    private URI validateUri(String value) {
        try {
            URI uri = new URI(value == null ? "" : value.trim());
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new CustomException(ErrorCode.ACTIVITY_INVALID_URI);
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new CustomException(ErrorCode.ACTIVITY_INVALID_URI);
            }
            return uri;
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.ACTIVITY_INVALID_URI);
        }
    }

    private String normalizeUri(URI uri) {
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        int port = uri.getPort();
        if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
            port = -1;
        }

        String path = uri.getRawPath();
        if (path == null || "/".equals(path)) {
            path = "";
        } else if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        try {
            return new URI(scheme, null, host, port, path, normalizeQuery(uri.getRawQuery()), null).toString();
        } catch (URISyntaxException e) {
            throw new CustomException(ErrorCode.ACTIVITY_INVALID_URI);
        }
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        String normalized = Arrays.stream(query.split("&"))
                .filter(value -> !value.isBlank())
                .filter(value -> {
                    String name = value.split("=", 2)[0].toLowerCase(Locale.ROOT);
                    return !name.startsWith("utm_") && !TRACKING_QUERY_PARAMS.contains(name);
                })
                .sorted()
                .collect(Collectors.joining("&"));

        return normalized.isBlank() ? null : normalized;
    }

    private boolean robotsAllowed(URI uri) {
        try {
            URI robots = new URI(uri.getScheme(), uri.getAuthority(), "/robots.txt", null, null);
            String body = restClient.get().uri(robots).retrieve().body(String.class);
            return body == null || !disallowed(body, uri.getPath());
        } catch (Exception e) {
            return true;
        }
    }

    private boolean disallowed(String robots, String path) {
        boolean target = false;
        for (String raw : robots.split("\\R")) {
            String line = raw.split("#", 2)[0].trim();
            if (line.toLowerCase(Locale.ROOT).startsWith("user-agent:")) {
                target = line.substring(11).trim().equals("*");
            } else if (target && line.toLowerCase(Locale.ROOT).startsWith("disallow:")) {
                String rule = line.substring(9).trim();
                if (!rule.isBlank() && path.startsWith(rule)) return true;
            }
        }
        return false;
    }

    private JsonNode jsonLd(String html) {
        Matcher matcher = Pattern.compile("<script[^>]+type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        while (matcher.find()) {
            try {
                JsonNode node = objectMapper.readTree(matcher.group(1).trim());
                JsonNode picked = pickJsonLd(node);
                if (picked != null) return picked;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private JsonNode pickJsonLd(JsonNode node) {
        if (node == null) return null;
        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode picked = pickJsonLd(child);
                if (picked != null) return picked;
            }
        }
        JsonNode graph = node.path("@graph");
        if (graph.isArray()) return pickJsonLd(graph);
        String type = jsonType(node);
        return type != null && (type.contains("Event") || type.contains("Course") || type.contains("Product")) ? node : null;
    }

    private String meta(String html, String key) {
        Matcher matcher = Pattern.compile("<meta\\b[^>]*>", Pattern.CASE_INSENSITIVE).matcher(html);
        while (matcher.find()) {
            String tag = matcher.group();
            if (key.equalsIgnoreCase(attr(tag, "property")) || key.equalsIgnoreCase(attr(tag, "name"))) {
                return attr(tag, "content");
            }
        }
        return null;
    }

    private String attr(String tag, String name) {
        Matcher matcher = Pattern.compile(name + "\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE).matcher(tag);
        return matcher.find() ? clean(matcher.group(1)) : null;
    }

    private String jsonText(JsonNode node, String name) {
        JsonNode child = child(node, name);
        return child != null && child.isValueNode() && !child.asText().isBlank() ? clean(child.asText()) : null;
    }

    private String jsonImage(JsonNode node) {
        JsonNode image = child(node, "image");
        if (image == null || image.isNull()) return null;
        if (image.isTextual()) return image.asText();
        if (image.isArray()) {
            for (JsonNode item : image) {
                String value = item.isTextual() ? item.asText() : first(jsonText(item, "url"), jsonText(item, "contentUrl"));
                if (value != null && !value.isBlank()) return value;
            }
            return null;
        }
        return first(jsonText(image, "url"), jsonText(image, "contentUrl"));
    }

    private String jsonLocation(JsonNode node) {
        JsonNode location = child(node, "location");
        if (location == null) return null;
        JsonNode address = child(location, "address");
        return first(
                jsonText(location, "name"),
                addressParts(address),
                jsonText(location, "address")
        );
    }

    private String jsonNamedValue(JsonNode node, String name) {
        JsonNode value = child(node, name);
        if (value == null || value.isNull()) return null;
        if (value.isArray() && !value.isEmpty()) value = value.get(0);
        if (value.isTextual()) return clean(value.asText());
        return first(jsonText(value, "name"), jsonText(value, "legalName"), jsonText(value, "url"));
    }

    private String jsonPrice(JsonNode node) {
        JsonNode offers = child(node, "offers");
        if (offers == null) return null;
        if (offers.isArray() && !offers.isEmpty()) offers = offers.get(0);
        return jsonText(offers, "price");
    }

    private JsonNode child(JsonNode node, String name) {
        if (node == null || !node.isObject()) return null;
        JsonNode direct = node.path(name);
        if (!direct.isMissingNode()) return direct;
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getKey().equalsIgnoreCase(name)) return field.getValue();
        }
        return null;
    }

    private String jsonType(JsonNode node) {
        JsonNode type = child(node, "@type");
        if (type == null || type.isNull()) return null;
        if (type.isTextual()) return type.asText();
        if (type.isArray()) {
            List<String> values = new ArrayList<>();
            type.forEach(value -> {
                if (value.isTextual()) values.add(value.asText());
            });
            return String.join(",", values);
        }
        return null;
    }

    private String addressParts(JsonNode address) {
        if (address == null || address.isNull()) return null;
        if (address.isTextual()) return clean(address.asText());

        String joined = List.of(
                        jsonText(address, "addressRegion"),
                        jsonText(address, "addressLocality"),
                        jsonText(address, "streetAddress"),
                        jsonText(address, "postalCode")
                ).stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));

        return joined.isBlank() ? null : joined;
    }

    private String regex(String text, String pattern) {
        Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? clean(matcher.group(1)) : null;
    }

    private LocalDateTime regexDate(String text, int index) {
        Matcher matcher = Pattern.compile("\\d{4}[./-]\\s*\\d{1,2}[./-]\\s*\\d{1,2}(?:\\D{0,8}\\d{1,2}:\\d{2})?|\\d{8}").matcher(text);
        for (int i = 0; matcher.find(); i++) {
            if (i == index) return date(matcher.group());
        }
        return null;
    }

    private LocalDateTime date(String raw) {
        if (raw == null) return null;
        String value = clean(raw);
        try {
            try {
                return LocalDateTime.parse(value);
            } catch (RuntimeException ignored) {
            }
            try {
                return OffsetDateTime.parse(value).toLocalDateTime();
            } catch (RuntimeException ignored) {
            }
            String normalized = value.replace('/', '-').replace('.', '-');
            if (normalized.matches("\\d{8}")) return LocalDate.parse(normalized, DateTimeFormatter.BASIC_ISO_DATE).atStartOfDay();
            Matcher matcher = Pattern.compile("(\\d{4})-\\s*(\\d{1,2})-\\s*(\\d{1,2})(?:\\D{0,8}(\\d{1,2}):(\\d{2}))?").matcher(normalized);
            if (matcher.find()) {
                LocalDate date = LocalDate.of(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3))
                );
                if (matcher.group(4) != null) {
                    return date.atTime(Integer.parseInt(matcher.group(4)), Integer.parseInt(matcher.group(5)));
                }
                return date.atStartOfDay();
            }
        } catch (RuntimeException e) {
            return null;
        }
        return null;
    }

    private String previewTitle(String html, String visible, JsonNode jsonLd) {
        String contentTitle = firstValidTitle(
                headingTitle(html),
                regex(visible, "(?:전시 제목|제목|강좌명|행사명|프로그램명|title|name)[:：]\\s*([^\\n]+)")
        );
        String metadataTitle = firstValidTitle(
                meta(html, "og:title"),
                meta(html, "twitter:title"),
                jsonText(jsonLd, "name"),
                htmlTitle(html)
        );

        return first(contentTitle, metadataTitle);
    }

    private Integer price(String... values) {
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            Integer money = firstMoney(value, true);
            if (money != null) return money;
            if (value.contains("무료")) return 0;
            money = firstMoney(value, false);
            if (money != null) return money;
        }
        return null;
    }

    private Integer firstMoney(String value, boolean requirePriceSignal) {
        Pattern pattern = requirePriceSignal
                ? Pattern.compile("(\\d{1,3}(?:,\\d{3})+|\\d+)\\s*(?:원|KRW|₩)")
                : Pattern.compile("\\b(\\d+)\\b");
        Matcher matcher = pattern.matcher(value);
        while (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1).replace(",", ""));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private String strip(String html) {
        return clean(html.replaceAll("(?is)<script.*?</script>|<style.*?</style>", " ")
                .replaceAll("<[^>]+>", "\n"));
    }

    private String htmlTitle(String html) {
        Matcher matcher = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        return matcher.find() ? clean(matcher.group(1)) : null;
    }

    private String headingTitle(String html) {
        Matcher matcher = Pattern.compile("<h[1-4]\\b[^>]*>(.*?)</h[1-4]>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        while (matcher.find()) {
            String title = validTitle(stripTags(matcher.group(1)));
            if (title != null) return title;
        }
        return null;
    }

    private String stripTags(String value) {
        if (value == null) return null;
        return clean(value
                .replaceAll("(?is)<script.*?</script>|<style.*?</style>", " ")
                .replaceAll("<[^>]+>", " "));
    }

    private String clean(String value) {
        if (value == null) return null;
        return repairMojibake(HtmlUtils.htmlUnescape(value))
                .replace('\u00A0', ' ')
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ").trim();
    }

    private String cleanAddress(String value) {
        String cleaned = clean(value);
        if (cleaned == null || isInvalidAddress(cleaned)) {
            return null;
        }
        return cleaned.length() > 150 ? null : cleaned;
    }

    private boolean isInvalidAddress(String value) {
        String compact = value.replaceAll("\\s+", "");
        if (compact.equals(".") || compact.equals("-") || compact.equals("통합예약")) {
            return true;
        }
        if (compact.matches("(서울|과천|청주|김해|제주|광주|부산|대구|대전|인천|울산|세종)")) {
            return true;
        }
        return value.matches(".*(입장권 운영안내|운영안내|예약하기|통합예약|원문 링크|장소 정보|URL복사|SNS공유|마이페이지).*");
    }

    private String cleanOrganizer(String value) {
        String cleaned = clean(value);
        if (cleaned == null || isInvalidOrganizer(cleaned)) {
            return null;
        }
        return cleaned.length() > 80 ? null : cleaned;
    }

    private boolean isInvalidOrganizer(String value) {
        String compact = value.replaceAll("\\s+", "");
        if (compact.equals(".") || compact.equals("-") || compact.equals("통합예약")) {
            return true;
        }
        return value.matches(".*(규정|허가조건|준수|시간\\s*[:：]|운영시간|관람 시간|입장권 운영안내|운영안내|통합예약|예약하기|원문 링크|장소 정보|URL복사|SNS공유|로그인|회원가입).*");
    }

    private String repairMojibake(String value) {
        if (value == null || !looksMojibake(value)) {
            return value;
        }

        CharsetEncoder encoder = StandardCharsets.ISO_8859_1.newEncoder();
        if (!encoder.canEncode(value)) {
            return value;
        }

        String repaired = new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        return hangulCount(repaired) > hangulCount(value) ? repaired : value;
    }

    private boolean looksMojibake(String value) {
        return value.matches(".*[ÃÂìíëê][\\u0080-\\u00ff]?.*");
    }

    private int hangulCount(String value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= '\uAC00' && ch <= '\uD7A3') {
                count++;
            }
        }
        return count;
    }

    private String firstValidTitle(String... values) {
        for (String value : values) {
            String title = validTitle(value);
            if (title != null) return title;
        }
        return null;
    }

    private String validTitle(String value) {
        String cleaned = clean(value);
        if (cleaned == null || cleaned.length() < 2) return null;
        String compact = cleaned.replaceAll("[\\s_\\-]+", "").toLowerCase(Locale.ROOT);
        if (compact.equals("test") || compact.equals("sample") || compact.equals("untitled")
                || compact.equals("title") || compact.equals("제목") || compact.equals("테스트")) {
            return null;
        }
        if (cleaned.matches(".*(전체메뉴보기|이용안내|마이페이지|회원서비스|SNS공유|URL복사|Language|닫기|목록|이전글|다음글).*")) {
            return null;
        }
        if (cleaned.matches(".*(로그인|회원가입|공유|상세보기).*") && !cleaned.contains(" - ")) {
            return null;
        }
        return cleaned;
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 12; i++) builder.append(String.format("%02x", hash[i]));
            return builder.toString();
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return null;
    }

    private LocalDateTime first(LocalDateTime... values) {
        for (LocalDateTime value : values) if (value != null) return value;
        return null;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private List<PreferenceTag> suggestTags(String text, Integer price, LocalDateTime startAt, LocalDateTime endAt) {
        List<PreferenceTag> tags = new ArrayList<>();
        if (contains(text, "차분", "독서", "명상")) add(tags, PreferenceTag.CALM);
        if (contains(text, "활기", "러닝", "운동", "댄스")) add(tags, PreferenceTag.LIVELY);
        if (contains(text, "힐링", "휴식")) add(tags, PreferenceTag.HEALING);
        if (contains(text, "입문", "초보", "처음")) add(tags, PreferenceTag.BEGINNER);
        if (contains(text, "도전", "심화")) add(tags, PreferenceTag.CHALLENGE);
        if (contains(text, "취미", "원데이", "클래스")) add(tags, PreferenceTag.HOBBY);
        if (contains(text, "배움", "강좌", "교육", "강연")) add(tags, PreferenceTag.LEARNING);
        if (contains(text, "모임", "클럽", "크루", "네트워킹")) add(tags, PreferenceTag.SOCIAL);
        if (contains(text, "체험", "경험")) add(tags, PreferenceTag.EXPERIENCE);
        add(tags, durationTag(text, startAt, endAt));
        return tags;
    }

    private PreferenceTag durationTag(String text, LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null) {
            long days = ChronoUnit.DAYS.between(startAt.toLocalDate(), endAt.toLocalDate()) + 1;
            if (days <= 1) return PreferenceTag.ONE_DAY;
            if (days <= 3) return PreferenceTag.THREE_DAYS;
            if (days <= 7) return PreferenceTag.ONE_WEEK;
            if (days <= 31) return PreferenceTag.ONE_MONTH;
            if (days <= 93) return PreferenceTag.THREE_MONTHS;
            if (days <= 366) return PreferenceTag.OVER_SIX_MONTHS;
            return PreferenceTag.OVER_ONE_YEAR;
        }
        if (contains(text, "하루", "원데이")) return PreferenceTag.ONE_DAY;
        if (contains(text, "한달", "1개월")) return PreferenceTag.ONE_MONTH;
        return null;
    }

    private boolean contains(String text, String... keywords) {
        String value = text == null ? "" : text;
        for (String keyword : keywords) if (value.contains(keyword)) return true;
        return false;
    }

    private void add(List<PreferenceTag> tags, PreferenceTag tag) {
        if (tag != null && !tags.contains(tag)) tags.add(tag);
    }

    public record ManualCommand(
            Long regionId,
            String title,
            String description,
            String thumbnailUrl,
            String sourceUrl,
            String address,
            String organizer,
            String contactInfo,
            String target,
            LocalDateTime startAt,
            LocalDateTime endAt,
            LocalDateTime recruitStartAt,
            LocalDateTime recruitEndAt,
            Integer price,
            ActivityCategory category,
            ActivityType activityType
    ) {
    }

    public record Preview(
            String title,
            String description,
            String thumbnailUrl,
            String sourceUrl,
            String address,
            String organizer,
            String contactInfo,
            String target,
            LocalDateTime startAt,
            LocalDateTime endAt,
            LocalDateTime recruitStartAt,
            LocalDateTime recruitEndAt,
            Integer price,
            ActivityCategory suggestedCategory,
            ActivityType suggestedActivityType,
            List<PreferenceTag> suggestedPreferenceTags
    ) {
    }
}
