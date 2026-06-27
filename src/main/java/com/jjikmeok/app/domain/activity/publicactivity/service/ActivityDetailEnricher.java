package com.jjikmeok.app.domain.activity.publicactivity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjikmeok.app.domain.activity.publicactivity.dto.NormalizedActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityDetailEnricher {

    private static final int INVALID_FUTURE_YEAR = 2099;
    private static final String FALLBACK_DESCRIPTION = "상세 내용은 원문 링크를 확인하세요.";
    private static final String FALLBACK_ADDRESS = "장소 정보는 원문 링크를 확인하세요.";
    private static final String FALLBACK_ORGANIZER = "운영기관 정보는 원문 링크를 확인하세요.";
    private static final String FALLBACK_CONTACT_INFO = "문의/선정 안내는 원문 링크를 확인하세요.";
    private static final String FALLBACK_TARGET = "참여 대상은 원문 링크를 확인하세요.";

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ActivitySyncUtils utils;

    public NormalizedActivity enrichIfMissing(NormalizedActivity activity) {
        if (activity == null || utils.isBlank(activity.sourceUrl())) return activity;
        if (!hasMissingField(activity)) return activity;

        try {
            // 🌟 [봇 방어벽 전면 우회]: 실제 데스크톱 크롬 브라우저 헤더를 탑재하여 403 Forbidden 우회 방어
            String html = restClient.get()
                    .uri(URI.create(activity.sourceUrl()))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                    .retrieve()
                    .body(String.class);

            if (utils.isBlank(html)) return withFallback(activity);

            String text = strip(html);
            Extracted extracted = extract(activity.sourceUrl(), html, text);

            // 🌟 [0순위 무결성 교정]: 천수의 NormalizedActivity DTO 스펙에 정확하게 아규먼트 매칭 순서 정렬 완료!
            return new NormalizedActivity(
                    resolveTitle(activity.title(), extracted.title()),
                    fallback(utils.firstText(activity.description(), extracted.description()), FALLBACK_DESCRIPTION),
                    utils.firstText(activity.thumbnailUrl(), extracted.thumbnailUrl()),
                    activity.sourceUrl(),
                    fallback(utils.firstText(cleanAddress(activity.address()), extracted.address()), FALLBACK_ADDRESS),
                    fallback(utils.firstText(cleanOrganizer(activity.organizer()), extracted.organizer()), FALLBACK_ORGANIZER),
                    fallback(utils.contactOnly(utils.firstText(activity.contactInfo(), extracted.contactInfo())), FALLBACK_CONTACT_INFO),
                    fallback(utils.firstText(activity.target(), extracted.target()), FALLBACK_TARGET),
                    validDate(activity.startAt()) ? activity.startAt() : extracted.startAt(),
                    validDate(activity.endAt()) ? activity.endAt() : extracted.endAt(),
                    validDate(activity.recruitStartAt()) ? activity.recruitStartAt() : extracted.recruitStartAt(),
                    validDate(activity.recruitEndAt()) ? activity.recruitEndAt() : extracted.recruitEndAt(),
                    resolvePrice(activity.price(), extracted.price(), text),
                    activity.activityType(),
                    activity.category(),
                    activity.sourceType(),
                    activity.externalId(),
                    activity.approvalStatus(),
                    activity.active()
            );
        } catch (Exception e) {
            log.warn("⚠️ 활동 상세 정보 추가 보완 실패. 원문URL={}, 에러내용={}", activity.sourceUrl(), e.getMessage());
            return withFallback(activity);
        }
    }

    public NormalizedActivity withFallback(NormalizedActivity activity) {
        // 🌟 [폴백 생성자 아규먼트 순서 불일치 전면 교정 완료]
        return new NormalizedActivity(
                activity.title(),
                fallback(activity.description(), FALLBACK_DESCRIPTION),
                activity.thumbnailUrl(),
                activity.sourceUrl(),
                fallback(cleanAddress(activity.address()), FALLBACK_ADDRESS),
                fallback(cleanOrganizer(activity.organizer()), FALLBACK_ORGANIZER),
                fallback(utils.contactOnly(activity.contactInfo()), FALLBACK_CONTACT_INFO),
                fallback(activity.target(), FALLBACK_TARGET),
                validDate(activity.startAt()) ? activity.startAt() : null,
                validDate(activity.endAt()) ? activity.endAt() : null,
                validDate(activity.recruitStartAt()) ? activity.recruitStartAt() : null,
                validDate(activity.recruitEndAt()) ? activity.recruitEndAt() : null,
                activity.price(),
                activity.activityType(),
                activity.category(),
                activity.sourceType(),
                activity.externalId(),
                activity.approvalStatus(),
                activity.active()
        );
    }

    private boolean hasMissingField(NormalizedActivity activity) {
        return utils.isBlank(activity.description()) || utils.isBlank(activity.thumbnailUrl())
                || utils.isBlank(activity.address()) || utils.isBlank(activity.organizer())
                || utils.isBlank(activity.contactInfo()) || utils.isBlank(activity.target())
                || !validDate(activity.startAt()) || !validDate(activity.endAt())
                || !validDate(activity.recruitStartAt()) || !validDate(activity.recruitEndAt())
                || activity.price() == null;
    }

    private Extracted extract(String sourceUrl, String html, String text) {
        JsonNode jsonLd = jsonLd(html);
        String flattenedText = text.replaceAll("\\n+", " ");

        DateRange recruitRange = firstRange(
                findDateRange(text, "모집기간|신청기간|접수기간|신청 기간|접수 기간|모집 기간|예약기간|예약 기간"),
                findLooseDateRange(text, "모집|신청|접수|예약")
        );
        LocalDateTime recruitDeadline = findDeadlineDate(text);
        DateRange activityRange = firstRange(
                findDateRange(text, "교육기간|행사기간|운영기간|활동기간|이용기간|이용 기간|일정|교육 기간|행사 기간|운영 기간|활동 기간"),
                findLooseDateRange(text, "교육|행사|운영|활동|이용|일정")
        );

        return new Extracted(
                extractTitle(html, flattenedText, jsonLd), extractDescription(html, jsonLd, flattenedText),
                extractThumbnail(sourceUrl, html, jsonLd), extractAddress(flattenedText, jsonLd),
                extractOrganizer(flattenedText, jsonLd), extractContactInfo(flattenedText), extractTarget(flattenedText),
                activityRange.start(), activityRange.end(), recruitRange.start(), utils.firstDateTime(recruitRange.end(), recruitDeadline),
                extractPrice(flattenedText, jsonLd)
        );
    }

    private String extractTitle(String html, String text, JsonNode jsonLd) {
        return firstValidTitle(headingTitle(html), labeledText(text, "전시 제목|제목|강좌명|행사명|프로그램명"),
                meta(html, "og:title"), meta(html, "twitter:title"), jsonText(jsonLd, "name"), htmlTitle(html));
    }

    private String extractDescription(String html, JsonNode jsonLd, String text) {
        return validLongText(utils.firstText(meta(html, "og:description"), meta(html, "twitter:description"),
                meta(html, "description"), jsonText(jsonLd, "description"),
                labeledText(text, "설명|소개|내용|강의소개|교육소개|프로그램소개|행사소개")));
    }

    private String extractThumbnail(String sourceUrl, String html, JsonNode jsonLd) {
        return absoluteUrl(sourceUrl, utils.firstText(meta(html, "og:image"), meta(html, "twitter:image"), jsonImage(jsonLd), firstImage(html)));
    }

    private String extractAddress(String text, JsonNode jsonLd) {
        return cleanAddress(utils.firstText(jsonLocation(jsonLd), labeledText(text, "교육장소|행사장소|운영장소|활동장소|이용장소|장소|주소|위치")));
    }

    private String extractOrganizer(String text, JsonNode jsonLd) {
        return cleanOrganizer(utils.firstText(jsonNamedValue(jsonLd, "organizer"), jsonNamedValue(jsonLd, "provider"),
                labeledText(text, "주최|주관|운영기관|교육기관|기관명")));
    }

    private String extractContactInfo(String text) {
        String labeled = labeledText(text, "문의|문의처|전화|연락처|대표번호|담당자|결과발표|선정발표|발표");
        if (!utils.isBlank(labeled)) {
            String contact = utils.contactOnly(validText(labeled));
            if (!utils.isBlank(contact)) return contact;
        }
        return utils.contactOnly(text);
    }

    private String extractTarget(String text) {
        String value = validText(labeledText(text, "신청대상|교육대상|참여대상|모집대상|대상"));
        if (utils.isBlank(value) || value.length() > 180) return null;
        if (value.matches(".*(유아|영유아|아동|어린이|초등|중등|고등|청소년|가족|부모|보호자|자녀|키즈|학생).*")) return null;
        if (value.matches(".*(개별연락|개별 연락|별도 연락|추후 연락|문자|이메일|전화|발표|선정).*")) return null;
        if (value.matches(".*(문의|연락처|전화번호|대표번호).*")) return null;
        return value;
    }

    private Integer extractPrice(String text, JsonNode jsonLd) {
        String jsonPrice = jsonPrice(jsonLd);
        if (!utils.isBlank(jsonPrice)) return firstInt(jsonPrice);

        String labeled = labeledText(text, "가격|금액|참가비|수강료|교육비|이용료|비용|Fee|fee");
        if (!utils.isBlank(labeled)) {
            Integer parsed = firstInt(labeled);
            if (parsed != null) return parsed;
            if (labeled.contains("무료") || labeled.matches("(?is).*free.*")) return 0;
        }
        return null;
    }

    private Integer resolvePrice(Integer originalPrice, Integer extractedPrice, String text) {
        if (originalPrice != null && originalPrice > 0) return originalPrice;
        if (extractedPrice != null) return extractedPrice;
        if (originalPrice != null && originalPrice == 0 && text != null && text.matches("(?is).*(무료|Free|free).*")) return 0;
        return null;
    }

    private String resolveTitle(String originalTitle, String extractedTitle) {
        String original = utils.cleanText(originalTitle);
        String extracted = firstValidTitle(extractedTitle);
        return isPlaceholderTitle(original) ? utils.firstText(extracted, original) : utils.firstText(original, extracted);
    }

    private boolean isPlaceholderTitle(String value) {
        if (utils.isBlank(value)) return true;
        String compact = value.replaceAll("[\\s_\\-]+", "").toLowerCase(Locale.ROOT);
        return compact.equals("test") || compact.equals("sample") || compact.equals("untitled") || compact.equals("title")
                || compact.equals("제목") || compact.equals("테스트") || compact.endsWith("activity");
    }

    private JsonNode jsonLd(String html) {
        Matcher matcher = Pattern.compile("<script[^>]+type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        while (matcher.find()) {
            try {
                JsonNode node = objectMapper.readTree(matcher.group(1).trim());
                JsonNode picked = pickJsonLd(node);
                if (picked != null) return picked;
            } catch (Exception ignored) {}
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
            return null;
        }
        JsonNode graph = node.path("@graph");
        if (graph.isArray()) return pickJsonLd(graph);
        String type = jsonType(node);
        if (type != null && (type.contains("Event") || type.contains("Course") || type.contains("Product"))) return node;
        return node.isObject() ? node : null;
    }

    private String jsonType(JsonNode node) {
        JsonNode type = child(node, "@type");
        if (type == null || type.isNull()) return null;
        if (type.isTextual()) return type.asText();
        if (type.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode val : type) {
                if (val.isTextual()) {
                    if (!sb.isEmpty()) sb.append(",");
                    sb.append(val.asText());
                }
            }
            return sb.toString();
        }
        return null;
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

    private String jsonText(JsonNode node, String name) {
        JsonNode child = child(node, name);
        return child != null && child.isValueNode() && !child.asText().isBlank() ? utils.cleanText(child.asText()) : null;
    }

    private String jsonImage(JsonNode node) {
        JsonNode image = child(node, "image");
        if (image == null || image.isNull()) return null;
        if (image.isTextual()) return utils.cleanText(image.asText());
        if (image.isArray()) {
            for (JsonNode item : image) {
                String val = item.isTextual() ? item.asText() : utils.firstText(jsonText(item, "url"), jsonText(item, "contentUrl"));
                if (!utils.isBlank(val)) return utils.cleanText(val);
            }
            return null;
        }
        return utils.firstText(jsonText(image, "url"), jsonText(image, "contentUrl"));
    }

    private String jsonLocation(JsonNode node) {
        JsonNode location = child(node, "location");
        if (location == null || location.isNull()) return null;
        return utils.firstText(jsonText(location, "name"), addressParts(child(location, "address")), jsonText(location, "address"));
    }

    private String addressParts(JsonNode address) {
        if (address == null || address.isNull()) return null;
        if (address.isTextual()) return utils.cleanText(address.asText());
        return utils.firstText(join(jsonText(address, "addressRegion"), jsonText(address, "addressLocality"), jsonText(address, "streetAddress"), jsonText(address, "postalCode")), jsonText(address, "name"));
    }

    private String jsonNamedValue(JsonNode node, String name) {
        JsonNode value = child(node, name);
        if (value == null || value.isNull()) return null;
        if (value.isArray() && !value.isEmpty()) value = value.get(0);
        if (value.isTextual()) return utils.cleanText(value.asText());
        return utils.firstText(jsonText(value, "name"), jsonText(value, "legalName"), jsonText(value, "url"));
    }

    private String jsonPrice(JsonNode node) {
        JsonNode offers = child(node, "offers");
        if (offers == null || offers.isNull()) return null;
        if (offers.isArray() && !offers.isEmpty()) offers = offers.get(0);
        return utils.firstText(jsonText(offers, "price"), jsonText(offers, "lowPrice"));
    }

    private String meta(String html, String key) {
        Matcher matcher = Pattern.compile("<meta\\b[^>]*>", Pattern.CASE_INSENSITIVE).matcher(html);
        while (matcher.find()) {
            String tag = matcher.group();
            if (key.equalsIgnoreCase(attr(tag, "property")) || key.equalsIgnoreCase(attr(tag, "name"))) return attr(tag, "content");
        }
        return null;
    }

    private String attr(String tag, String name) {
        Matcher matcher = Pattern.compile(name + "\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE).matcher(tag);
        return matcher.find() ? utils.cleanText(matcher.group(1)) : null;
    }

    private String htmlTitle(String html) {
        Matcher matcher = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        return matcher.find() ? utils.cleanText(matcher.group(1)) : null;
    }

    private String headingTitle(String html) {
        Matcher matcher = Pattern.compile("<h[1-4]\\b[^>]*>(.*?)</h[1-4]>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        while (matcher.find()) {
            String title = validTitle(stripTags(matcher.group(1)));
            if (!utils.isBlank(title)) return title;
        }
        return null;
    }

    private String stripTags(String value) {
        if (value == null) return null;
        return utils.cleanText(value.replaceAll("(?is)<script.*?</script>|<style.*?</style>", " ").replaceAll("<[^>]+>", " "));
    }

    private String firstImage(String html) {
        Matcher matcher = Pattern.compile("<img\\b[^>]*>", Pattern.CASE_INSENSITIVE).matcher(html);
        while (matcher.find()) {
            String tag = matcher.group();
            String src = utils.firstText(attr(tag, "src"), attr(tag, "data-src"), attr(tag, "data-original"), attr(tag, "data-lazy"));
            if (!utils.isBlank(src) && !src.startsWith("data:") && !src.matches("(?i).*(logo|icon|btn|arrow|loading|blank|spacer|sns|facebook|instagram|kakao).*")) return src;
        }
        return null;
    }

    private String labeledText(String text, String labelRegex) {
        Matcher matcher = Pattern.compile("(?:" + labelRegex + ")\\s*[:：]?\\s*([^|\\n]{2,160})", Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? validText(matcher.group(1)) : null;
    }

    private DateRange findDateRange(String text, String labelRegex) {
        Matcher matcher = Pattern.compile("(?:" + labelRegex + ").{0,80}?(\\d{4}\\s*[.\\-/년]\\s*\\d{1,2}\\s*[.\\-/월]\\s*\\d{1,2}\\s*[.]?\\s*(?:일)?)(?:\\s*\\([^)]*\\))?\\s*(?:~|-|–|—|부터|\\s+to\\s+)\\s*(\\d{4}\\s*[.\\-/년]\\s*\\d{1,2}\\s*[.\\-/월]\\s*\\d{1,2}\\s*[.]?\\s*(?:일)?)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(text);
        return matcher.find() ? new DateRange(parseDate(matcher.group(1)), parseDate(matcher.group(2))) : DateRange.empty();
    }

    private DateRange findLooseDateRange(String text, String nearKeywordRegex) {
        Matcher matcher = Pattern.compile("(?:" + nearKeywordRegex + ").{0,60}?(\\d{4}[.\\-/]\\s*\\d{1,2}[.\\-/]\\s*\\d{1,2})\\s*(?:~|-|–|—|부터|\\s+to\\s+)\\s*(\\d{4}[.\\-/]\\s*\\d{1,2}[.\\-/]\\s*\\d{1,2})", Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? new DateRange(parseDate(matcher.group(1)), parseDate(matcher.group(2))) : DateRange.empty();
    }

    private LocalDateTime findDeadlineDate(String text) {
        Matcher matcher = Pattern.compile("(?:모집\\s*마감|신청\\s*마감|접수\\s*마감|예약\\s*마감|마감일|접수마감일|신청마감일).{0,60}?(\\d{4}\\s*[.\\-/년]\\s*\\d{1,2}\\s*[.\\-/월]\\s*\\d{1,2}\\s*[.]?\\s*(?:일)?)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(text);
        return matcher.find() ? parseDate(matcher.group(1)) : null;
    }

    private DateRange firstRange(DateRange first, DateRange second) {
        return (first != null && (first.start() != null || first.end() != null)) ? first : (second != null ? second : DateRange.empty());
    }

    private LocalDateTime parseDate(String value) {
        if (utils.isBlank(value)) return null;
        try {
            String normalized = value.replaceAll("\\s+", "").replace("년", "-").replace("월", "-").replace("일", "").replace(".", "-").replace("/", "-").replaceAll("-+", "-").replaceAll("-$", "");
            if (normalized.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
                String[] parts = normalized.split("-");
                normalized = "%s-%02d-%02d".formatted(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            }
            return java.time.LocalDate.parse(normalized).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean validDate(LocalDateTime value) {
        return value != null && value.getYear() < INVALID_FUTURE_YEAR;
    }

    private Integer firstInt(String... values) {
        for (String value : values) {
            if (utils.isBlank(value)) continue;
            Integer money = utils.extractPriceFromText(value, true);
            if (money != null) return money;
            if (value.contains("무료") || value.matches("(?is).*\\bfree\\b.*")) return 0;
            money = utils.extractPriceFromText(value, false);
            if (money != null) return money;
        }
        return null;
    }

    private String absoluteUrl(String baseUrl, String value) {
        if (utils.isBlank(value)) return null;
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("data:image/")) return value;
        try {
            return URI.create(baseUrl).resolve(value).toString();
        } catch (Exception e) {
            return value;
        }
    }

    private String strip(String html) {
        return html.replaceAll("(?is)<script.*?</script>|<style.*?</style>", " ").replaceAll("(?i)<br\\s*/?>", "\n").replaceAll("(?i)</p>|</div>|</li>|</tr>|</section>|</article>|</dd>|</dt>", "\n").replaceAll("<[^>]+>", " ").replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replaceAll("[\\t\\x0B\\f\\r ]+", " ").replaceAll("\\n+", "\n").trim();
    }

    private String validText(String value) {
        String cleaned = utils.cleanText(value);
        if (cleaned == null || cleaned.length() < 2) return null;
        return cleaned.matches(".*(찾아오시는길|Event Location|내역|온라인 접수|개별연락|개별 연락|별도 안내|추후 안내|입장권 운영안내|운영안내|예약하기|통합예약|원문 링크|장소 정보|URL복사|SNS공유|마이페이지).*") ? null : cleaned;
    }

    private String firstValidTitle(String... values) {
        for (String value : values) {
            String title = validTitle(value);
            if (!utils.isBlank(title)) return title;
        }
        return null;
    }

    private String validTitle(String value) {
        String cleaned = utils.cleanText(value);
        if (cleaned == null || cleaned.length() < 2) return null;
        if (cleaned.matches(".*(전체메뉴보기|이용안내|마이페이지|회원서비스|SNS공유|URL복사|Language|닫기|목록|이전글|다음글).*")) return null;
        if (cleaned.matches(".*(로그인|회원가입|공유|상세보기).*") && !cleaned.contains(" - ")) return null;
        return cleaned;
    }

    private String validLongText(String value) {
        String cleaned = utils.cleanText(value);
        if (cleaned == null || cleaned.length() < 5) return null;
        if (cleaned.matches(".*(로그인|회원가입|공유하기|목록보기).*")) return null;
        return cleaned.length() > 1000 ? cleaned.substring(0, 1000) : cleaned;
    }

    private String cleanAddress(String value) {
        String cleaned = validText(value);
        return (utils.isBlank(cleaned) || isInvalidAddress(cleaned) || cleaned.length() > 150) ? null : cleaned;
    }

    private boolean isInvalidAddress(String value) {
        String compact = value.replaceAll("\\s+", "");
        if (compact.equals(".") || compact.equals("-") || compact.equals("통합예약") || compact.matches("(서울|과천|청주|김해|제주|광주|부산|대구|대전|인천|울산|세종)")) return true;
        return value.matches(".*(찾아오시는길|Event Location|내역|온라인 접수|개별연락|개별 연락|별도 안내|추후 안내|입장권 운영안내|운영안내|예약하기|통합예약|원문 링크|장소 정보|URL복사|SNS공유|마이페이지).*");
    }

    private String cleanOrganizer(String value) {
        String cleaned = validText(value);
        return (utils.isBlank(cleaned) || isInvalidOrganizer(cleaned) || cleaned.length() > 80) ? null : cleaned;
    }

    private boolean isInvalidOrganizer(String value) {
        String compact = value.replaceAll("\\s+", "");
        return compact.equals(".") || compact.equals("-") || compact.equals("통합예약") || value.matches(".*(규정|허가조건|준수|시간\\s*[:：]|운영시간|관람 시간|입장권 운영안내|운영안내|통합예약|예약하기|원문 링크|장소 정보|URL복사|SNS공유|로그인|회원가입|찾아오시는길|신청하기|접수하기|목록|공유).*");
    }

    private String fallback(String value, String message) {
        return !utils.isBlank(value) ? value : message;
    }

    private String join(String... values) {
        StringBuilder sb = new StringBuilder();
        for (String val : values) {
            if (utils.isBlank(val)) continue;
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(val);
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private record DateRange(LocalDateTime start, LocalDateTime end) {
        static DateRange empty() { return new DateRange(null, null); }
    }
    private record Extracted(String title, String description, String thumbnailUrl, String address, String organizer, String contactInfo, String target, LocalDateTime startAt, LocalDateTime endAt, LocalDateTime recruitStartAt, LocalDateTime recruitEndAt, Integer price) {}
}
