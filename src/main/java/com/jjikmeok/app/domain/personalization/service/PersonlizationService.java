package com.jjikmeok.app.domain.personalization.service;

import com.jjikmeok.app.domain.personalization.dto.ActivityRecommendationResponse;
import com.jjikmeok.app.domain.personalization.dto.PersonalizationResponse;
import com.jjikmeok.app.domain.personalization.repository.PersonalizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonlizationService {

    private static final int DISPLAY_TAG_LIMIT = 4;

    private final PersonalizationRepository personalizationRepository;

    public List<String> findTags(Long userId) {
        return personalizationRepository.findTagNamesByUserId(userId);
    }

    public PersonalizationResponse findBestType(Long userId) {
        List<String> userTags = findTags(userId);

        if (userTags == null || userTags.isEmpty()) {
            return new PersonalizationResponse("분류 불가", List.of());
        }

        Set<String> userTagSet = userTags.stream()
                .filter(Objects::nonNull)
                .map(tag -> tag.replace("#", "").trim())
                .filter(tag -> !tag.isBlank())
                .collect(Collectors.toSet());

        if (userTagSet.isEmpty()) {
            return new PersonalizationResponse("분류 불가", pickRandomDisplayTags(userTags));
        }

        Map<String, List<String>> typeTags = new LinkedHashMap<>();
        typeTags.put("편안하게 쉬는 타입", List.of("편안한", "힐링", "휴식", "가볍게", "단기"));
        typeTags.put("감성 충전 타입", List.of("감성적", "창의적", "취미", "배움", "한달"));
        typeTags.put("배움 성장 타입", List.of("입문", "몰입", "배움", "성장", "한달"));
        typeTags.put("활기 도전 타입", List.of("활기찬", "트렌디", "도전", "입문", "단기"));
        typeTags.put("소규모 몰입 타입", List.of("몰입", "가볍게", "소규모", "단기"));
        typeTags.put("대규모 트렌드 타입", List.of("활기찬", "트렌디", "대규모", "취미"));

        String bestType = "분류 불가";
        long bestMatchCount = 0;

        for (Map.Entry<String, List<String>> entry : typeTags.entrySet()) {
            long matchCount = entry.getValue().stream()
                    .filter(userTagSet::contains)
                    .count();

            if (matchCount > bestMatchCount) {
                bestMatchCount = matchCount;
                bestType = entry.getKey();
            }
        }

        return new PersonalizationResponse(bestType, pickRandomDisplayTags(userTags));
    }

    private List<String> pickRandomDisplayTags(List<String> userTags) {
        if (userTags.size() <= DISPLAY_TAG_LIMIT) {
            return userTags;
        }

        List<String> shuffledTags = new ArrayList<>(userTags);
        Collections.shuffle(shuffledTags);
        return shuffledTags.subList(0, DISPLAY_TAG_LIMIT);
    }

    @Transactional(readOnly = true)
    public List<ActivityRecommendationResponse> getRecommendedActivities(Long userId) {
        return personalizationRepository.findRecommendedActivitiesByUserId(userId)
                .stream()
                .map(ActivityRecommendationResponse::from)
                .toList();
    }
}
