package com.jjikmeok.app.domain.activity.privateactivity.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.jjikmeok.app.domain.activity.privateactivity.dto.SearchResultDto;
import com.jjikmeok.app.domain.activity.privateactivity.support.DiscoveryUrlNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SerperSearchServiceImpl implements SearchService {

    private final RestClient restClient = RestClient.create();
    private final DiscoveryUrlNormalizer urlNormalizer;

    @Value("${app.discovery.search.provider:serper}")
    private String provider;

    @Value("${app.discovery.search.results-per-keyword:${app.discovery.search.result-limit:10}}")
    private int defaultLimit;

    @Value("${app.discovery.search.serper.base-url:https://google.serper.dev/search}")
    private String baseUrl;

    @Value("${app.discovery.search.serper.api-key:}")
    private String apiKey;

    @Value("${app.discovery.search.serper.gl:kr}")
    private String gl;

    @Value("${app.discovery.search.serper.hl:ko}")
    private String hl;

    public SerperSearchServiceImpl(DiscoveryUrlNormalizer urlNormalizer) {
        this.urlNormalizer = urlNormalizer;
    }

    @Override
    public List<SearchResultDto> search(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        if (!"serper".equalsIgnoreCase(provider)) {
            return List.of();
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            log.warn("[Discovery] Serper 기본 URL이 없습니다. 검색어={}", keyword);
            return List.of();
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[Discovery] Serper API 키가 없습니다. 검색어={}", keyword);
            return List.of();
        }

        int effectiveLimit = Math.max(1, limit > 0 ? limit : defaultLimit);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("q", keyword);
        body.put("num", effectiveLimit);
        if (gl != null && !gl.isBlank()) {
            body.put("gl", gl);
        }
        if (hl != null && !hl.isBlank()) {
            body.put("hl", hl);
        }

        try {
            JsonNode root = restClient.post()
                    .uri(baseUrl)
                    .header("X-API-KEY", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null || !root.has("organic") || !root.get("organic").isArray()) {
                return List.of();
            }

            List<SearchResultDto> results = new ArrayList<>();
            for (JsonNode item : root.get("organic")) {
                String title = text(item, "title");
                String url = urlNormalizer.normalize(text(item, "link"));
                String snippet = text(item, "snippet");
                Integer position = item.hasNonNull("position") ? item.get("position").asInt() : null;
                if (title == null || url == null) {
                    continue;
                }
                results.add(new SearchResultDto(keyword, title, url, snippet, position, "serper", null));
            }
            return results;
        } catch (Exception e) {
            log.warn("[Discovery] Serper 검색에 실패했습니다. 검색어={}, 사유={}", keyword, e.getMessage());
            return List.of();
        }
    }

    private String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        String value = node.get(field).asText();
        return value == null || value.isBlank() ? null : value.trim();
    }
}
