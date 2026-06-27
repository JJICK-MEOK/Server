package com.jjikmeok.app.domain.activity.privateactivity.deduplication;

import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.enums.SourceType;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.ai.dto.DiscoveryAnalysisDto;
import com.jjikmeok.app.domain.activity.privateactivity.support.DiscoveryUrlNormalizer;
import com.jjikmeok.app.domain.activity.publicactivity.service.ActivitySyncUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DiscoveryDeduplicationService {

    private final ActivityRepository activityRepository;
    private final DiscoveryUrlNormalizer urlNormalizer;
    private final ActivitySyncUtils utils;

    public boolean isDuplicate(DiscoveryAnalysisDto analysis) {
        return findDuplicateReason(
                analysis.sourceUrl(),
                analysis.title(),
                analysis.organizer()
        ).isPresent();
    }

    public Optional<String> findDuplicateReason(String sourceUrl, String title, String organizer) {
        String normalizedUrl = urlNormalizer.normalize(sourceUrl);
        if (!utils.isBlank(normalizedUrl) && activityRepository.existsBySourceUrl(normalizedUrl)) {
            return Optional.of("sourceUrl 중복");
        }

        String normalizedTitle = normalizeText(title);
        String normalizedOrganizer = normalizeText(organizer);
        if (!utils.isBlank(normalizedTitle) && !utils.isBlank(normalizedOrganizer)) {
            Optional<Activity> exactMatch = activityRepository.findFirstByTitleIgnoreCaseAndOrganizerIgnoreCase(
                    normalizedTitle,
                    normalizedOrganizer
            );
            if (exactMatch.isPresent()) {
                return Optional.of("title + organizer 중복");
            }

            String titleToken = trimmedToken(normalizedTitle);
            String organizerToken = trimmedToken(normalizedOrganizer);
            if (!utils.isBlank(titleToken) && !utils.isBlank(organizerToken)) {
                List<Activity> candidates = activityRepository.findTop50ByTitleContainingIgnoreCaseOrOrganizerContainingIgnoreCaseOrderByCreatedAtDesc(
                        titleToken,
                        organizerToken
                );
                for (Activity candidate : candidates) {
                    double titleScore = similarity(normalizedTitle, normalizeText(candidate.getTitle()));
                    double organizerScore = similarity(normalizedOrganizer, normalizeText(candidate.getOrganizer()));
                    if (titleScore >= 0.86 && organizerScore >= 0.86) {
                        return Optional.of("title + organizer 유사 중복");
                    }
                }
            }
        }

        String externalId = hash(normalizedUrl != null ? normalizedUrl : (sourceUrl == null ? "" : sourceUrl) + "|" + title);
        return activityRepository.findDuplicate(
                SourceType.DISCOVERY,
                externalId,
                normalizedUrl,
                title,
                null,
                null
        ).map(activity -> "기존 데이터 중복: " + activity.getId());
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase()
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String trimmedToken(String value) {
        if (utils.isBlank(value)) {
            return "";
        }
        return value.length() > 20 ? value.substring(0, 20) : value;
    }

    private double similarity(String left, String right) {
        if (utils.isBlank(left) || utils.isBlank(right)) {
            return 0.0;
        }
        if (left.equals(right)) {
            return 1.0;
        }
        int distance = levenshtein(left, right);
        int max = Math.max(left.length(), right.length());
        return max == 0 ? 1.0 : 1.0 - ((double) distance / max);
    }

    private int levenshtein(String left, String right) {
        int[] prev = new int[right.length() + 1];
        int[] curr = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            prev[j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(
                        Math.min(curr[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost
                );
            }
            int[] swap = prev;
            prev = curr;
            curr = swap;
        }
        return prev[right.length()];
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
}
