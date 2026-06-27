package com.jjikmeok.app.domain.sync.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jjikmeok.app.domain.activity.enums.ApprovalStatus;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.sync.dto.NormalizedActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityNormalizer {

    private static final String DEFAULT_DESCRIPTION = "상세 설명은 원문에서 확인하세요.";
    private final ObjectMapper objectMapper;
    private final CategoryClassifier classifier;
    private final ActivitySyncUtils utils;

    public List<NormalizedActivity> normalize(SourceType sourceType, String requestUrl, String contentType, String payload) {
        try {
            List<NormalizedActivity> result = new ArrayList<>();

            if ("XML".equals(contentType)) {
                for (JsonNode item : xmlItems(sourceType, payload)) {
                    try {
                        NormalizedActivity activity = fromNode(sourceType, requestUrl, item);
                        if (activity != null) result.add(activity);
                    } catch (Exception e) {
                        log.warn("⚠️ [{}] XML row 정규화 실패. row skip. reason={}", sourceType, e.getMessage());
                    }
                }
                return result;
            }

            JsonNode root = objectMapper.readTree(payload);
            for (JsonNode item : items(root)) {
                try {
                    NormalizedActivity activity = fromNode(sourceType, requestUrl, item);
                    if (activity != null) result.add(activity);
                } catch (Exception e) {
                    log.warn("⚠️ [{}] JSON row 정규화 실패. row skip. reason={}", sourceType, e.getMessage());
                }
            }

            return result;
        } catch (Exception e) {
            log.warn("⚠️ [{}] payload 전체 파싱 실패. single fallback 저장 금지. reason={}", sourceType, e.getMessage());
            return List.of();
        }
    }

    private NormalizedActivity fromNode(SourceType sourceType, String requestUrl, JsonNode item) {
        String rawTitle = text(item, "title", "prfnm", "TITLE", "progrmSj", "강좌명", "SVCNM", "name", "eventNm", "pstTtl");
        String title = utils.cleanTitle(rawTitle);

        String rawDescription = text(item,
                "description", "DESCRIPTION", "progrmCn", "DTLCONT", "강좌내용",
                "desc", "content", "overview", "intro", "summary",
                "pstWholCn", "ETC_DESC", "PROGRAM", "SUB_DESCRIPTION", "sty"
        );

        String description = utils.cleanDescriptionBody(cleanDescription(rawDescription));

        String sourceUrl = text(item,
                "sourceUrl", "URL", "HMPG_ADDR", "ORG_LINK", "홈페이지주소",
                "SVCURL", "homepage", "eventUrl", "detailUrl", "href", "link", "url", "pstUrlAddr"
        );

        if (sourceUrl == null && sourceType == SourceType.KOPIS) sourceUrl = kopisDetailUrl(item);
        sourceUrl = utils.normalizeUrl(sourceUrl);

        String externalId = text(item, "externalId", "contentid", "mt20id", "LOCAL_ID", "progrmRegistNo", "SVCID", "id", "seq", "eventId", "crseId", "pstSn");

        String rawAddress = text(item,
                "address", "addr1", "addr2", "fcltynm", "EVENT_SITE",
                "actPlace", "progrmPlace", "PLACE", "교육장소",
                "PLACENM", "place", "venue", "location"
        );

        String mixedText = String.join(" ",
                safe(rawTitle),
                safe(rawDescription),
                safe(rawAddress),
                safe(text(item, "SUB_DESCRIPTION", "ETC_DESC", "PROGRAM", "pstWholCn", "PERIOD", "DATE", "EVENT_PERIOD"))
        );

        String address = utils.cleanAddressStrict(rawAddress);


        if (sourceType == SourceType.KOPIS) {
            address = utils.firstText(
                    utils.extractAddressByLabel(mixedText, "공연장소", "주소"),
                    address
            );
        }

        if (sourceType == SourceType.EXHIBITION) {
            address = utils.firstText(
                    utils.extractAddressByLabel(mixedText, "도로명주소", "전시장소", "장소"),
                    address
            );
        }

        if (address == null) {
            address = utils.cleanAddressStrict(mixedText);
        }

        String thumbnailUrl = thumbnailUrl(text(item,
                "thumbnailUrl", "firstimage", "firstimage2", "poster",
                "IMAGE_OBJECT", "MAIN_IMG", "IMGURL", "image", "imgUrl", "thumbnail", "atchFile"
        ));

        String organizer = cleanOrganizer(text(item,
                "organizer", "ORG_NAME", "CNTC_INSTT_NM", "nanmmbyNm",
                "기관명", "주최", "주관", "운영기관", "insttNm", "CONTRIBUTOR", "entrpsnm"
        ));

        String contactInfo = utils.contactOnly(text(item,
                "contactInfo", "tel", "TEL", "telno", "TELNO",
                "phone", "INQUIRY", "CONTACT_POINT", "문의", "CONTACT"
        ));

        if (contactInfo == null) {
            contactInfo = utils.contactOnly(mixedText);
        }

        String target = cleanTarget(text(item,
                "target", "대상", "USE_TRGT", "USETGTINFO",
                "useTgtInfo", "AUDIENCE", "봉사대상", "prfage"
        ));

        if ("A".equalsIgnoreCase(target)) {
            target = "전체 관람가";
        }

        if (target == null) {
            target = utils.extractTargetFromMixedText(mixedText);
        }

        if ("고객센터".equals(target) || utils.isFallbackText(target)) {
            target = null;
        }

        String categoryHint = text(item, "category", "genrenm", "GENRE", "분류", "realmName", "CODENAME", "codename", "pstSeNm", "MAXCLASSNM", "MINCLASSNM");

        ActivitySyncUtils.DateRange mixedRange = utils.extractDateRangeFromMixedText(mixedText);

        Period period = period(String.join(" ",
                safe(rawTitle),
                safe(rawDescription),
                safe(text(item, "PERIOD", "DATE", "EVENT_PERIOD"))
        ));

        LocalDateTime startAt = utils.first(
                firstDateTime(item, "startAt", "startDate", "eventstartdate", "prfpdfrom", "progrmBgnde", "교육시작일자", "SVCOPNBGNDT", "STRTDATE"),
                period.startAt()
        );

        LocalDateTime endAt = utils.first(
                firstDateTime(item, "endAt", "endDate", "eventenddate", "prfpdto", "progrmEndde", "교육종료일자", "SVCOPNENDDT", "END_DATE"),
                period.endAt()
        );

        LocalDateTime recruitStartAt = firstDateTime(item, "recruitStartAt", "RCPTBGNDT", "noticeBgnde", "reqstBeginDe", "recruitStartDate", "rceptStartDate");
        LocalDateTime recruitEndAt = firstDateTime(item, "recruitEndAt", "RCPTENDDT", "noticeEndde", "reqstEndDe", "recruitEndDate", "rceptEndDate");


        if (sourceType == SourceType.EXHIBITION) {
            startAt = utils.firstDateTime(startAt, period.startAt(), mixedRange.start());
            endAt = utils.firstDateTime(endAt, period.endAt(), mixedRange.end());
        }

        startAt = utils.validOrNull(startAt);
        endAt = utils.validOrNull(endAt);
        recruitStartAt = utils.validOrNull(recruitStartAt);
        recruitEndAt = utils.validOrNull(recruitEndAt);

        String feeText = text(item, "price", "CHARGE", "USE_FEE", "SVCCHARGENM", "PAYATNM", "수강료", "fee", "entrfee", "tuition", "pcseguidance", "티켓가격", "요금정보", "참가비용");

        Integer price = null;

        if (sourceType == SourceType.EXHIBITION && feeText != null && feeText.matches("^0[1-9]$")) {
            price = null;
        } else {
            price = utils.extractPriceFromText(feeText, true);
        }

        if (price == null && feeText != null && feeText.matches(".*(무료|전액\\s*무료|참가비\\s*없음|0원).*")) {
            price = 0;
        }
        if (sourceUrl == null) sourceUrl = requestUrl;
        if (title == null) title = sourceType.name() + " activity";
        if (description == null) description = DEFAULT_DESCRIPTION;
        if (externalId == null) externalId = hash(sourceType.name() + sourceUrl + title);

        String status = text(item,
                "status",
                "STATUS",
                "SVCSTATNM",
                "STATE",
                "recruitStatus",
                "접수상태",
                "진행상태"
        );

        String all = String.join(" ", safe(title), safe(description), safe(address), safe(categoryHint));

        return new NormalizedActivity(
                title,
                description,
                thumbnailUrl,
                sourceUrl,
                address,
                organizer,
                contactInfo,
                target,
                startAt,
                endAt,
                recruitStartAt,
                recruitEndAt,
                price,
                classifier.classifyType(sourceType, all, startAt, endAt),
                classifier.classifyCategory(sourceType, all),
                sourceType,
                externalId,
                sourceType == SourceType.URL_MANUAL ? ApprovalStatus.PENDING : ApprovalStatus.APPROVED,
                active(status)
        );
    }

    private List<JsonNode> items(JsonNode root) {
        JsonNode candidate = firstNode(root, "response.body.items.item", "response.body.items", "body.items.item", "body.items", "items.item", "items", "data", "result.youthPolicyList", "culturalEventInfo.row", "ListPublicReservationCulture.row");
        if (candidate == null) candidate = firstArray(root);
        if (candidate == null) candidate = root;
        if (candidate.isArray()) {
            List<JsonNode> result = new ArrayList<>();
            candidate.forEach(result::add);
            return result;
        }
        return List.of(candidate);
    }

    private JsonNode firstArray(JsonNode node) {
        if (node == null || !node.isObject()) return null;
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getValue().isArray()) return field.getValue();
            JsonNode nested = firstArray(field.getValue());
            if (nested != null) return nested;
        }
        return null;
    }

    private JsonNode firstNode(JsonNode node, String... paths) {
        for (String path : paths) {
            JsonNode current = node;
            for (String part : path.split("\\.")) current = current == null ? null : child(current, part);
            if (current != null && !current.isMissingNode() && !current.isNull()) return current;
        }
        return null;
    }

    private List<JsonNode> xmlItems(SourceType sourceType, String payload) throws Exception {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setExpandEntityReferences(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(payload)));
            NodeList nodes = sourceType == SourceType.KOPIS ? document.getElementsByTagName("db") : document.getElementsByTagName("item");
            if (nodes.getLength() == 0) nodes = sourceType == SourceType.KOPIS ? document.getElementsByTagName("item") : document.getElementsByTagName("db");
            List<JsonNode> result = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) result.add(xmlToJson((Element) nodes.item(i)));
            return result;
        } catch (Exception e) {
            List<JsonNode> fallback = xmlItemsByPattern(sourceType, payload);
            if (!fallback.isEmpty()) return fallback;
            throw e;
        }
    }

    private ObjectNode xmlToJson(Element element) {
        ObjectNode node = objectMapper.createObjectNode();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element e) node.put(e.getTagName(), e.getTextContent().trim());
        }
        return node;
    }

    private List<JsonNode> xmlItemsByPattern(SourceType sourceType, String payload) {
        if (payload == null || payload.isBlank()) return List.of();
        String element = sourceType == SourceType.KOPIS ? "(?:db|item)" : "item";
        Matcher matcher = Pattern.compile("(?is)<" + element + ">(.*?)</" + element + ">").matcher(payload);
        List<JsonNode> result = new ArrayList<>();
        while (matcher.find()) result.add(xmlFragmentToJson(matcher.group(1)));
        return result;
    }

    private ObjectNode xmlFragmentToJson(String fragment) {
        ObjectNode node = objectMapper.createObjectNode();
        Matcher matcher = Pattern.compile("(?is)<([A-Za-z_][A-Za-z0-9_.-]*)>(.*?)</\\1>").matcher(fragment);
        while (matcher.find()) {
            String value = matcher.group(2).replaceAll("(?is)<[^>]+>", " ").replaceAll("\\s+", " ").trim();
            node.put(matcher.group(1), HtmlUtils.htmlUnescape(value));
        }
        return node;
    }

    private String text(JsonNode item, String... names) {
        for (String name : names) {
            JsonNode node = child(item, name);
            if (node != null && !node.isMissingNode() && !node.isNull() && !node.asText().isBlank()) {
                return utils.cleanText(node.asText());
            }
        }
        return null;
    }

    private JsonNode child(JsonNode item, String name) {
        if (item == null) return null;
        JsonNode node = item.path(name);
        if (!node.isMissingNode()) return node;
        if (!item.isObject()) return null;
        String target = name.replaceAll("[_\\-\\s]", "").toLowerCase(Locale.ROOT);
        Iterator<Map.Entry<String, JsonNode>> fields = item.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getKey().replaceAll("[_\\-\\s]", "").toLowerCase(Locale.ROOT).equals(target)) return field.getValue();
        }
        return null;
    }

    private LocalDateTime firstDateTime(JsonNode item, String... names) {
        String value = text(item, names);
        return value == null ? null : dateTime(value);
    }

    private Period period(String value) {
        if (value == null) return new Period(null, null);
        Matcher matcher = Pattern.compile("\\d{4}[./-]\\d{1,2}[./-]\\d{1,2}|\\d{8}").matcher(value);
        LocalDateTime start = matcher.find() ? dateTime(matcher.group()) : null;
        LocalDateTime end = matcher.find() ? dateTime(matcher.group()) : start;
        return new Period(start, end);
    }

    private LocalDateTime dateTime(String raw) {
        String value = raw.trim().replace('/', '-');
        value = value.contains(" ") ? value.split(" ", 2)[0].replace('.', '-') + " " + value.split(" ", 2)[1] : value.replace('.', '-');
        try {
            if (value.matches("\\d{8}")) return LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE).atStartOfDay();
            if (value.matches("\\d{14}")) return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            if (value.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) return LocalDate.parse(padDate(value)).atStartOfDay();
            if (value.matches("\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}")) return LocalDateTime.parse(padDateTime(value), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            if (value.matches("\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}:\\d{2}\\.\\d+")) value = value.replaceFirst("\\.\\d+$", "");
            if (value.matches("\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}:\\d{2}")) return LocalDateTime.parse(padDateTime(value), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            if (value.matches(".*[+-]\\d{2}:\\d{2}$")) return OffsetDateTime.parse(value).toLocalDateTime();
            return LocalDateTime.parse(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String padDate(String value) {
        String[] p = value.split("-");
        return "%s-%02d-%02d".formatted(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]));
    }

    private String padDateTime(String value) {
        String[] p = value.split(" ", 2);
        return padDate(p[0]) + " " + p[1];
    }

    private String cleanDescription(String value) {
        if (value == null) return null;
        String compact = HtmlUtils.htmlUnescape(value.replaceAll("(?is)<script.*?</script>", " ").replaceAll("(?is)<style.*?</style>", " ").replaceAll("(?is)<[^>]+>", " ")).replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
        return (compact.matches("[0-9,]+") || compact.matches("[\\p{Punct}\\s0-9]+") || compact.isBlank()) ? null : compact;
    }

    private String cleanDescriptionOnly(String value) {
        if (value == null) return null;
        String lines = HtmlUtils.htmlUnescape(value
                .replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("(?i)<br\\s*/?>|</p>|</div>|</li>|</tr>|</section>|</article>", "\n")
                .replaceAll("(?is)<[^>]+>", " "))
                .replace('\u00A0', ' ');
        StringBuilder result = new StringBuilder();
        for (String line : lines.split("\\R+")) {
            String cleaned = utils.cleanText(line);
            if (cleaned == null || cleaned.length() < 2) continue;
            if (cleaned.matches(".*(모집|신청|접수|교육|행사|운영|활동)?\\s*(기간|일시|날짜|장소|기관|주최|주관|문의|연락처|대상|비용|정원|신청방법|홈페이지|링크)\\s*[:：].*")) continue;
            if (cleaned.matches(".*(https?://|www\\.|@|\\d{2,4}-\\d{3,4}-\\d{4}).*")) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(cleaned);
            if (result.length() >= 1000) break;
        }
        return cleanDescription(result.isEmpty() ? value : result.toString());
    }

    private String cleanOrganizer(String value) {
        String cleaned = utils.cleanText(value);
        return (cleaned == null || isInvalidOrganizer(cleaned) || cleaned.length() > 80) ? null : cleaned;
    }

    private boolean isInvalidOrganizer(String value) {
        String compact = value.replaceAll("\\s+", "");
        return compact.equals(".") || compact.equals("-") || compact.equals("통합예약") || value.matches(".*(규정|허가조건|준수|시간\\s*[:：]|운영시간|관람 시간|입장권 운영안내|운영안내|통합예약|예약하기|원문 링크|장소 정보|URL복사|SNS공유|로그인|회원가입).*");
    }

    private String cleanTarget(String value) {
        return (value == null || value.matches(".*(접수종료|예약마감|접수중|안내중|마감|종료|취소|완료).*")) ? null : value;
    }

    private String thumbnailUrl(String value) {
        if (value == null || value.isBlank()) return null;
        return (value.startsWith("data:image/") || value.length() <= 500) ? value : null;
    }

    private String extractHref(String value) {
        if (value == null) return null;
        Matcher m = Pattern.compile("(?i)href\\s*=\\s*['\"]([^'\"]+)['\"]").matcher(value);
        return m.find() ? HtmlUtils.htmlUnescape(m.group(1).trim()) : null;
    }

    private String youthContentId(JsonNode item) {
        String bbsSn = text(item, "bbsSn");
        String pstSn = text(item, "pstSn");
        return (bbsSn == null || pstSn == null) ? null : bbsSn + ":" + pstSn;
    }

    private String youthContentDetailUrl(JsonNode item) {
        String bbsSn = text(item, "bbsSn");
        String pstSn = text(item, "pstSn");
        if (bbsSn == null || pstSn == null) return null;
        String pstSeSn = text(item, "pstSeSn");
        return "https://www.youthcenter.go.kr/bbs01View/" + bbsSn + "/" + pstSn + (pstSeSn == null ? "" : "/" + pstSeSn);
    }

    private String kopisDetailUrl(JsonNode item) {
        String id = text(item, "mt20id");
        return id == null ? null : "https://www.kopis.or.kr/por/db/pblprfr/pblprfrView.do?mt20Id=" + id;
    }

    private String volunteerDetailUrl(JsonNode item) {
        String id = text(item, "progrmRegistNo");
        return id == null ? null : "https://www.1365.go.kr/vols/P9210/partcptn/timeCptn.do?type=show&progrmRegistNo=" + id;
    }

    private Boolean active(String value) {
        return value == null ? null : !contains(value, "접수종료", "예약마감", "마감", "종료", "취소", "완료");
    }

    private boolean contains(String value, String... keywords) {
        if (value == null) return false;
        for (String k : keywords) {
            if (value.contains(k)) return true;
        }
        return false;
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] h = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < 12; i++) b.append(String.format("%02x", h[i]));
            return b.toString();
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private String safe(String value) { return value == null ? "" : value; }
    private record Period(LocalDateTime startAt, LocalDateTime endAt) {}
}
