package com.jjikmeok.app.domain.activity.publicactivity.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.privateactivity.dto.DiscoveryCandidateDto;
import com.jjikmeok.app.domain.activity.privateactivity.dto.SearchResultDto;
import com.jjikmeok.app.domain.activity.publicactivity.entity.RawActivity;
import com.jjikmeok.app.domain.activity.publicactivity.repository.RawActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RawActivityArchiveService {

    private static final String DISCOVERY_CONTENT_TYPE = "DISCOVERY_JSON";

    private final RawActivityRepository rawActivityRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void archiveFetchedPayload(ExternalActivityGateway.FetchedPayload fetchedPayload) {
        if (fetchedPayload == null) {
            return;
        }

        rawActivityRepository.save(RawActivity.create(
                fetchedPayload.sourceType(),
                null,
                fetchedPayload.requestUrl(),
                fetchedPayload.contentType(),
                fetchedPayload.payload()
        ));
    }

    @Transactional
    public void archiveDiscoveryCandidate(SearchResultDto searchResult, DiscoveryCandidateDto candidate) {
        if (searchResult == null && candidate == null) {
            return;
        }

        rawActivityRepository.save(RawActivity.create(
                SourceType.DISCOVERY,
                discoveryExternalId(searchResult, candidate),
                firstText(
                        candidate == null ? null : candidate.sourceUrl(),
                        searchResult == null ? null : searchResult.url()
                ),
                DISCOVERY_CONTENT_TYPE,
                writeDiscoveryPayload(searchResult, candidate)
        ));
    }

    private String writeDiscoveryPayload(SearchResultDto searchResult, DiscoveryCandidateDto candidate) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("searchResult", searchResult);
        payload.put("candidate", candidate);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return payload.toString();
        }
    }

    private String discoveryExternalId(SearchResultDto searchResult, DiscoveryCandidateDto candidate) {
        return hash(firstText(
                candidate == null ? null : candidate.sourceUrl(),
                searchResult == null ? null : searchResult.url(),
                candidate == null ? null : candidate.title(),
                searchResult == null ? null : searchResult.title()
        ));
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 12 && i < hash.length; i++) {
                builder.append(String.format("%02x", hash[i]));
            }
            return builder.toString();
        } catch (Exception e) {
            return Integer.toHexString((value == null ? "" : value).hashCode());
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
