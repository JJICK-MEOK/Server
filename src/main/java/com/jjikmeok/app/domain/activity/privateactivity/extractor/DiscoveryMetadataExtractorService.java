package com.jjikmeok.app.domain.activity.privateactivity.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjikmeok.app.domain.activity.privateactivity.dto.DiscoveryCandidateDto;
import com.jjikmeok.app.domain.activity.privateactivity.dto.SearchResultDto;
import com.jjikmeok.app.domain.activity.privateactivity.enums.ExtractionMode;
import com.jjikmeok.app.domain.activity.privateactivity.support.DiscoveryUrlNormalizer;
import com.jjikmeok.app.domain.activity.publicactivity.service.ActivitySyncUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscoveryMetadataExtractorService {

    private final ActivitySyncUtils utils;
    private final DiscoveryUrlNormalizer urlNormalizer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();

    public DiscoveryCandidateDto extract(SearchResultDto searchResult) {
        return extract(searchResult, ExtractionMode.FULL_CONTENT, 20d);
    }

    public DiscoveryCandidateDto extract(SearchResultDto searchResult, ExtractionMode extractionMode, double confidenceScore) {
        String sourceUrl = urlNormalizer.normalize(searchResult.url());
        if (utils.isBlank(sourceUrl)) {
            sourceUrl = searchResult.url();
        }

        if (extractionMode == ExtractionMode.URL_ONLY) {
            return urlOnly(searchResult, sourceUrl, confidenceScore);
        }

        String html = fetchHtml(sourceUrl);
        if (utils.isBlank(html)) {
            return fallback(searchResult, sourceUrl, null, extractionMode, confidenceScore);
        }

        String visibleText = strip(html);
        JsonNode jsonLd = jsonLd(html);
        String thumbnailUrl = first(
                absoluteUrl(sourceUrl, jsonImage(jsonLd)),
                absoluteUrl(sourceUrl, meta(html, "og:image")),
                absoluteUrl(sourceUrl, meta(html, "twitter:image")),
                firstImage(html)
        );
        thumbnailUrl = first(thumbnailUrl, faviconUrl(html, sourceUrl));

        String title = utils.cleanTitle(first(
                jsonText(jsonLd, "name"),
                meta(html, "og:title"),
                meta(html, "twitter:title"),
                headingTitle(html),
                htmlTitle(html),
                searchResult.title()
        ));

        String description = utils.cleanDescriptionBody(first(
                jsonText(jsonLd, "description"),
                meta(html, "og:description"),
                meta(html, "twitter:description"),
                meta(html, "description"),
                searchResult.snippet()
        ));

        if (extractionMode == ExtractionMode.METADATA_ONLY) {
            return new DiscoveryCandidateDto(
                    searchResult.keyword(),
                    searchResult,
                    title,
                    sourceUrl,
                    thumbnailUrl,
                    description,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    extractionMode,
                    confidenceScore,
                    compact(first(visibleText, searchResult.snippet(), description, title))
            );
        }

        String address = first(
                utils.cleanAddressStrict(jsonLocation(jsonLd)),
                utils.cleanAddressStrict(labeledValue(visibleText, "??", "???", "??", "LOCATION")),
                utils.cleanAddressStrict(searchResult.snippet())
        );

        String organizer = cleanOrganizer(first(
                jsonNamedValue(jsonLd, "organizer"),
                jsonNamedValue(jsonLd, "provider"),
                labeledValue(visibleText, "??", "??", "??", "ORGANIZER", "PROVIDER")
        ));

        String contactInfo = utils.contactOnly(first(
                labeledValue(visibleText, "??", "???", "??", "CONTACT"),
                searchResult.snippet(),
                visibleText
        ));

        String target = first(
                labeledValue(visibleText, "??", "????", "????", "????", "????", "????"),
                utils.extractTargetFromMixedText(visibleText)
        );

        ActivitySyncUtils.DateRange recruitRange = utils.extractDateRangeFromMixedText(first(
                labeledValue(visibleText, "????", "????", "????", "?? ??", "?? ??"),
                searchResult.snippet()
        ));

        ActivitySyncUtils.DateRange activityRange = utils.extractDateRangeFromMixedText(first(
                labeledValue(visibleText, "????", "????", "????", "????", "??????", "??"),
                searchResult.snippet(),
                visibleText
        ));

        LocalDateTime startAt = first(
                jsonDate(jsonLd, "startDate"),
                activityRange.start(),
                utils.extractSingleDateTime(visibleText)
        );

        LocalDateTime endAt = first(
                jsonDate(jsonLd, "endDate"),
                activityRange.end(),
                startAt
        );

        LocalDateTime recruitStartAt = first(
                jsonDate(jsonLd, "registrationStartDate"),
                jsonDate(jsonLd, "validFrom"),
                recruitRange.start()
        );

        LocalDateTime recruitEndAt = first(
                jsonDate(jsonLd, "registrationEndDate"),
                jsonDate(jsonLd, "validThrough"),
                recruitRange.end()
        );

        Integer price = firstPrice(
                jsonPrice(jsonLd),
                utils.extractPriceFromText(description, true),
                utils.extractPriceFromText(visibleText, true),
                utils.extractPriceFromText(searchResult.snippet(), true)
        );

        String pageText = compact(first(visibleText, searchResult.snippet(), description, title));
        return new DiscoveryCandidateDto(
                searchResult.keyword(),
                searchResult,
                title,
                sourceUrl,
                thumbnailUrl,
                description,
                organizer,
                contactInfo,
                target,
                address,
                startAt,
                endAt,
                recruitStartAt,
                recruitEndAt,
                price,
                extractionMode,
                confidenceScore,
                pageText
        );
    }

    private DiscoveryCandidateDto fallback(SearchResultDto searchResult, String sourceUrl, String html, ExtractionMode extractionMode, double confidenceScore) {
        String visibleText = html == null ? searchResult.snippet() : strip(html);
        String thumbnailUrl = first(null, faviconUrl(html, sourceUrl));
        return new DiscoveryCandidateDto(
                searchResult.keyword(),
                searchResult,
                utils.cleanTitle(searchResult.title()),
                sourceUrl,
                thumbnailUrl,
                utils.cleanDescriptionBody(searchResult.snippet()),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                extractionMode,
                confidenceScore,
                compact(visibleText)
        );
    }

    private DiscoveryCandidateDto urlOnly(SearchResultDto searchResult, String sourceUrl, double confidenceScore) {
        String thumbnailUrl = faviconUrl(null, sourceUrl);
        return new DiscoveryCandidateDto(
                searchResult.keyword(),
                searchResult,
                utils.cleanTitle(first(searchResult.title(), sourceUrl)),
                sourceUrl,
                thumbnailUrl,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                ExtractionMode.URL_ONLY,
                confidenceScore,
                compact(first(searchResult.title(), searchResult.snippet(), sourceUrl))
        );
    }

    private String fetchHtml(String sourceUrl) {

        if (utils.isBlank(sourceUrl)) {
            return null;
        }

        try {
            return restClient.get()
                    .uri(URI.create(sourceUrl))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.debug("[발견] 메타데이터 조회에 실패했습니다. URL={}", sourceUrl, e);
            return null;
        }
    }

    private JsonNode jsonLd(String html) {
        Matcher matcher = Pattern.compile("<script[^>]+type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        while (matcher.find()) {
            try {
                JsonNode node = objectMapper.readTree(matcher.group(1).trim());
                JsonNode picked = pickJsonLd(node);
                if (picked != null) {
                    return picked;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private JsonNode pickJsonLd(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode picked = pickJsonLd(child);
                if (picked != null) {
                    return picked;
                }
            }
            return null;
        }
        JsonNode graph = node.path("@graph");
        if (graph.isArray()) {
            return pickJsonLd(graph);
        }
        return node.isObject() ? node : null;
    }

    private String jsonText(JsonNode node, String name) {
        JsonNode child = child(node, name);
        return child != null && child.isValueNode() && !child.asText().isBlank() ? utils.cleanText(child.asText()) : null;
    }

    private String jsonImage(JsonNode node) {
        JsonNode image = child(node, "image");
        if (image == null || image.isNull()) {
            return null;
        }
        if (image.isTextual()) {
            return utils.cleanText(image.asText());
        }
        if (image.isArray()) {
            for (JsonNode item : image) {
                String value = item.isTextual() ? item.asText() : first(jsonText(item, "url"), jsonText(item, "contentUrl"));
                if (!utils.isBlank(value)) {
                    return value;
                }
            }
            return null;
        }
        return first(jsonText(image, "url"), jsonText(image, "contentUrl"));
    }

    private String jsonLocation(JsonNode node) {
        JsonNode location = child(node, "location");
        if (location == null || location.isNull()) {
            return null;
        }
        return first(jsonText(location, "name"), jsonText(child(location, "address"), "streetAddress"), jsonText(location, "address"));
    }

    private String jsonNamedValue(JsonNode node, String name) {
        JsonNode value = child(node, name);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isArray() && !value.isEmpty()) {
            value = value.get(0);
        }
        if (value.isTextual()) {
            return utils.cleanText(value.asText());
        }
        return first(jsonText(value, "name"), jsonText(value, "legalName"), jsonText(value, "url"));
    }

    private String jsonPrice(JsonNode node) {
        JsonNode offers = child(node, "offers");
        if (offers == null || offers.isNull()) {
            return null;
        }
        if (offers.isArray() && !offers.isEmpty()) {
            offers = offers.get(0);
        }
        return first(jsonText(offers, "price"), jsonText(offers, "lowPrice"));
    }

    private LocalDateTime jsonDate(JsonNode node, String name) {
        String value = jsonText(node, name);
        if (utils.isBlank(value)) {
            return null;
        }

        try {
            return LocalDateTime.parse(value);
        } catch (RuntimeException ignored) {
        }

        try {
            return java.time.OffsetDateTime.parse(value).toLocalDateTime();
        } catch (RuntimeException ignored) {
        }

        String normalized = value.replace('/', '-').replace('.', '-').trim();
        if (normalized.matches("\\d{8}")) {
            return LocalDate.parse(normalized, DateTimeFormatter.BASIC_ISO_DATE).atStartOfDay();
        }
        if (normalized.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
            String[] parts = normalized.split("-");
            return LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])).atStartOfDay();
        }
        if (normalized.matches("\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}(:\\d{2})?")) {
            String[] parts = normalized.split(" ", 2);
            String time = parts[1];
            if (time.length() == 5) {
                time = time + ":00";
            }
            String[] dateParts = parts[0].split("-");
            return LocalDate.of(Integer.parseInt(dateParts[0]), Integer.parseInt(dateParts[1]), Integer.parseInt(dateParts[2]))
                    .atTime(Integer.parseInt(time.substring(0, 2)), Integer.parseInt(time.substring(3, 5)), Integer.parseInt(time.substring(6, 8)));
        }
        return utils.extractSingleDateTime(value);
    }

    private JsonNode child(JsonNode node, String name) {
        if (node == null || !node.isObject()) {
            return null;
        }
        JsonNode direct = node.path(name);
        if (!direct.isMissingNode()) {
            return direct;
        }
        for (var iterator = node.fields(); iterator.hasNext(); ) {
            var field = iterator.next();
            if (field.getKey().equalsIgnoreCase(name)) {
                return field.getValue();
            }
        }
        return null;
    }

    private String faviconUrl(String html, String sourceUrl) {
        if (html != null) {
            String icon = first(
                    iconHref(html, "icon"),
                    iconHref(html, "shortcut icon"),
                    iconHref(html, "apple-touch-icon")
            );
            if (!utils.isBlank(icon)) {
                return absoluteUrl(sourceUrl, icon);
            }
        }
        return fallbackFavicon(sourceUrl);
    }

    private String fallbackFavicon(String sourceUrl) {
        if (utils.isBlank(sourceUrl)) {
            return null;
        }
        try {
            URI uri = URI.create(sourceUrl);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            return uri.getScheme() + "://" + uri.getHost() + "/favicon.ico";
        } catch (Exception e) {
            return null;
        }
    }

    private String iconHref(String html, String relValue) {
        Matcher matcher = Pattern.compile("<link\b[^>]*>", Pattern.CASE_INSENSITIVE).matcher(html);
        while (matcher.find()) {
            String tag = matcher.group();
            String rel = attr(tag, "rel");
            if (rel == null || !rel.toLowerCase().contains(relValue.toLowerCase())) {
                continue;
            }
            String href = attr(tag, "href");
            if (!utils.isBlank(href)) {
                return href;
            }
        }
        return null;
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
        return matcher.find() ? utils.cleanText(matcher.group(1)) : null;
    }

    private String htmlTitle(String html) {
        Matcher matcher = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        return matcher.find() ? utils.cleanText(matcher.group(1)) : null;
    }

    private String headingTitle(String html) {
        Matcher matcher = Pattern.compile("<h[1-4]\\b[^>]*>(.*?)</h[1-4]>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        while (matcher.find()) {
            String title = utils.cleanTitle(stripTags(matcher.group(1)));
            if (!utils.isBlank(title)) {
                return title;
            }
        }
        return null;
    }

    private String firstImage(String html) {
        Matcher matcher = Pattern.compile("<img\\b[^>]*>", Pattern.CASE_INSENSITIVE).matcher(html);
        while (matcher.find()) {
            String tag = matcher.group();
            String src = first(attr(tag, "src"), attr(tag, "data-src"), attr(tag, "data-original"), attr(tag, "data-lazy"));
            if (!utils.isBlank(src) && !src.startsWith("data:")) {
                return src;
            }
        }
        return null;
    }

    private String stripTags(String value) {
        if (value == null) {
            return null;
        }
        return utils.cleanText(value.replaceAll("(?is)<script.*?</script>|<style.*?</style>", " ").replaceAll("<[^>]+>", " "));
    }

    private String labeledValue(String text, String... labels) {
        if (utils.isBlank(text)) {
            return null;
        }

        for (String label : labels) {
            Matcher matcher = Pattern.compile(Pattern.quote(label) + "\\s*[:：]?\\s*([^\\n|]{2,160})", Pattern.CASE_INSENSITIVE).matcher(text);
            if (matcher.find()) {
                return utils.cleanText(matcher.group(1));
            }
        }
        return null;
    }

    private String strip(String html) {
        return HtmlUtils.htmlUnescape(html
                .replaceAll("(?is)<script.*?</script>|<style.*?</style>", " ")
                .replaceAll("(?i)<br\\s*/?>|</p>|</div>|</li>|</tr>|</section>|</article>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replace('\u00A0', ' ')
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll("\\n+", "\n")
                .trim());
    }

    private String absoluteUrl(String baseUrl, String value) {
        if (utils.isBlank(value)) {
            return null;
        }
        if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("data:image/")) {
            return value;
        }
        try {
            return URI.create(baseUrl).resolve(value).toString();
        } catch (Exception e) {
            return value;
        }
    }

    private String cleanOrganizer(String value) {
        String cleaned = utils.cleanText(value);
        if (cleaned == null || cleaned.length() > 80) {
            return null;
        }
        if (cleaned.matches(".*(문의|연락처|전화번호|영업시간|로그인|회원가입|공지|URL복사|약관|계좌).*")) {
            return null;
        }
        return cleaned;
    }

    private String compact(String value) {
        if (value == null) {
            return null;
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() > 6_000 ? compact.substring(0, 6_000) : compact;
    }

    private Integer firstPrice(Object... values) {
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof Integer integer) {
                return integer;
            }
            if (value instanceof Number number) {
                return number.intValue();
            }

            String text = value.toString();
            if (utils.isBlank(text)) {
                continue;
            }

            Integer price = utils.extractPriceFromText(text, true);
            if (price != null) {
                return price;
            }
            if (text.contains("무료") || text.matches("(?is).*\\bfree\\b.*")) {
                return 0;
            }
            price = utils.extractPriceFromText(text, false);
            if (price != null) {
                return price;
            }
        }
        return null;
    }

    @SafeVarargs
    private final <T> T first(T... values) {
        for (T value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof String text) {
                if (!text.isBlank()) {
                    return value;
                }
            } else {
                return value;
            }
        }
        return null;
    }
}
