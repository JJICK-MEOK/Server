package com.jjikmeok.app.domain.activity.publicactivity.service;

import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class ExternalActivityGateway {

    private final RestClient restClient = RestClient.create();
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int MAX_REQUEST_ATTEMPTS = 3;

    public FetchedPayload fetch(SourceType sourceType, String baseUrl, String serviceKey) {
        return fetchPage(sourceType, baseUrl, serviceKey, LocalDate.now(), LocalDate.now().plusMonths(3), 1);
    }

    public FetchedPayload fetchPage(SourceType sourceType, String baseUrl, String serviceKey, LocalDate today, LocalDate endDate, int page) {
        return fetchPage(sourceType, baseUrl, serviceKey, today, endDate, page, null);
    }

    public FetchedPayload fetchKopisPage(String baseUrl, String serviceKey, LocalDate today, LocalDate endDate, int page, String prfstate) {
        return fetchPage(SourceType.KOPIS, baseUrl, serviceKey, today, endDate, page, prfstate);
    }

    private FetchedPayload fetchPage(SourceType sourceType, String baseUrl, String serviceKey, LocalDate today, LocalDate endDate, int page, String prfstate) {
        if (baseUrl == null || baseUrl.isBlank()) throw new CustomException(ErrorCode.ACTIVITY_SYNC_CONFIG_MISSING);
        try {
            return request(sourceType, buildUrl(sourceType, baseUrl, serviceKey, today, endDate, page, prfstate), true);
        } catch (IllegalArgumentException e) {
            log.warn("❌ 외부 API 요청 URL 설정 오류. 수집출처={}, 기준URL={}", sourceType, maskKey(baseUrl));
            throw new CustomException(ErrorCode.ACTIVITY_SYNC_INVALID_URL);
        }
    }

    private FetchedPayload request(SourceType sourceType, String requestUrl, boolean allowHttpsFallback) {
        ResponseEntity<byte[]> response;
        for (int attempt = 1; ; attempt++) {
            try {
                // 🌟 [강화된 디버깅 로그] 통신 직전 날아가는 인코딩 주소 스캔용
                log.info("📡 [오픈 API 통신 시도] 수집출처={}, 요청URL={}, 시도횟수={}", sourceType, maskKey(requestUrl), attempt);

                response = restClient.get().uri(URI.create(requestUrl)).retrieve().toEntity(byte[].class);
                log.info("✅ 외부 API 데이터 연동 결과 성공. 수집출처={}, 상태코드={}, 시도횟수={}", sourceType, response.getStatusCode().value(), attempt);
                break;
            } catch (RestClientResponseException e) {
                if (attempt < MAX_REQUEST_ATTEMPTS && retryable(e.getStatusCode())) {
                    log.warn("⚠️ 외부 API 연결 일시적 장애로 인한 재시도. 수집출처={}, 상태코드={}, 시도횟수={}", sourceType, e.getStatusCode().value(), attempt);
                    try { Thread.sleep(300L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                }
                // 🌟 [에러 로그 상세화] 무엇 때문에 400 에러가 났는지 원문 주소의 구조 파악이 용이하도록 리팩토링
                if (allowHttpsFallback && e.getStatusCode().value() == 502 && requestUrl.startsWith("http://")) {
                    String httpsUrl = "https://" + requestUrl.substring("http://".length());
                    log.warn("⚠️ [{}] HTTP 502 감지. HTTPS 엔드포인트로 한 번 더 우회 시도합니다.", sourceType);
                    return request(sourceType, httpsUrl, false);
                }
                log.error("💥 외부 API 응답 오류 발생. 수집출처={}, 상세요청URL={}, 상태코드={}, 에러메시지={}", sourceType, maskKey(requestUrl), e.getStatusCode().value(), e.getResponseBodyAsString());
                throw new CustomException(ErrorCode.ACTIVITY_SYNC_EXTERNAL_FAILED);
            } catch (RestClientException e) {
                log.error("❌ 외부 엔드포인트 네트워크 연결 실패. 수집출처={}, 원인={}", sourceType, e.getMessage());
                throw new CustomException(ErrorCode.ACTIVITY_SYNC_EXTERNAL_FAILED);
            } catch (IllegalArgumentException e) {
                log.error("❌ 외부 API 요청 URL 형식 오류. 수집출처={}, 잘못된URL={}", sourceType, maskKey(requestUrl));
                throw new CustomException(ErrorCode.ACTIVITY_SYNC_INVALID_URL);
            }
        }
        String payload = decodePayload(response.getBody(), response.getHeaders().getContentType());
        return new FetchedPayload(sourceType, requestUrl, contentType(payload), payload == null ? "" : payload);
    }

    private boolean retryable(HttpStatusCode statusCode) {
        int v = statusCode.value();
        return v == 429 || v == 502 || v == 503 || v == 504;
    }

    private String decodePayload(byte[] body, MediaType contentType) {
        if (body == null || body.length == 0) return "";
        String head = new String(body, 0, Math.min(body.length, 200), StandardCharsets.US_ASCII);
        Matcher m = Pattern.compile("encoding=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(head);
        Charset cs = null;
        try { if (m.find()) cs = Charset.forName(m.group(1)); } catch (Exception ignored) {}
        if (cs == null && contentType != null) cs = contentType.getCharset();
        if (cs == null) cs = StandardCharsets.UTF_8;

        String decoded = new String(body, cs);
        if (decoded.matches(".*[ÃÂìíëê][\\u0080-\\u00ff]?.*")) {
            try { return new String(decoded.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8); } catch (Exception ignored) {}
        }
        return decoded;
    }

    String buildUrl(SourceType sourceType, String baseUrl, String serviceKey, LocalDate today, LocalDate endDate, int page) {
        return buildUrl(sourceType, baseUrl, serviceKey, today, endDate, page, null);
    }

    String buildUrl(SourceType sourceType, String baseUrl, String serviceKey, LocalDate today, LocalDate endDate, int page, String prfstate) {
        if (sourceType == SourceType.SEOUL_CULTURE || sourceType == SourceType.SEOUL_RESERVATION) baseUrl = baseUrl.replaceFirst("/\\d+/\\d+/?$", "");

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
        String keyName = keyName(sourceType, baseUrl);
        boolean appendKey = serviceKey != null && !serviceKey.isBlank();

        String ymd = today.format(DateTimeFormatter.BASIC_ISO_DATE);
        String endYmd = (sourceType == SourceType.KOPIS && today.plusDays(31).isBefore(endDate) ? today.plusDays(31) : endDate).format(DateTimeFormatter.BASIC_ISO_DATE);

        switch (sourceType) {
            case KOPIS -> builder.replaceQueryParam("rows", 100).replaceQueryParam("stdate", ymd).replaceQueryParam("eddate", endYmd).replaceQueryParam("cpage", page).replaceQueryParam("prfstate", prfstate == null ? "02" : prfstate);
            case EXHIBITION -> builder.replaceQueryParam("numOfRows", DEFAULT_PAGE_SIZE).replaceQueryParam("pageNo", page);
            case SEOUL_CULTURE -> builder.pathSegment(String.valueOf((page - 1) * 100 + 1), String.valueOf(page * 100));
            case SEOUL_RESERVATION -> builder.pathSegment(String.valueOf((page - 1) * 100 + 1), String.valueOf(page * 100)).replaceQueryParam("sortStdr", 1);
        }

        // 🌟 [파라미터 중복 생성 버그 수정 블록]
        // 기존 문자열 강제 결합 대신 정석적인 UriComponentsBuilder 방식을 타도록 전면 교정 완료
        if (appendKey) {
            String encodedKey = serviceKey.matches(".*%[0-9a-fA-F]{2}.*")
                    ? serviceKey
                    : UriUtils.encodeQueryParam(serviceKey, StandardCharsets.UTF_8);
            builder.replaceQueryParam(keyName, encodedKey);
        }

        return builder.build(true).toUriString();
    }

    private String keyName(SourceType sourceType, String baseUrl) {
        if (sourceType == SourceType.KOPIS) return "service";
        return "serviceKey";
    }

    private String maskKey(String url) { return url.replaceAll("(?i)(serviceKey|apiKey|apiKeyNm|service)=([^&]+)", "$1=***"); }
    private String contentType(String payload) { if (payload == null) return "UNKNOWN"; String t = payload.stripLeading(); return t.startsWith("<") ? "XML" : (t.startsWith("{") || t.startsWith("[") ? "JSON" : "TEXT"); }
    public record FetchedPayload(SourceType sourceType, String requestUrl, String contentType, String payload) {}
}
