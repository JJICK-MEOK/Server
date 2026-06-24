package com.jjikmeok.app.domain.personalization.service;

import com.jjikmeok.app.domain.personalization.dto.PersonalizationResponse;
import com.jjikmeok.app.domain.personalization.repository.PersonalizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

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
                .collect(java.util.stream.Collectors.toSet());

        if (userTagSet.isEmpty()) {
            return new PersonalizationResponse("분류 불가", pickRandomDisplayTags(userTags));
        }

        Map<String, List<String>> typeTags = new LinkedHashMap<>();

        typeTags.put("우선 한입만 먹어보는 형",
                List.of("입문", "가볍게", "단기", "취미"));

        typeTags.put("부담 없는 것부터 고르는 형",
                List.of("편안한", "힐링", "휴식", "가볍게", "단기"));

        typeTags.put("천천히 음미하는 형",
                List.of("편안한", "감성적", "취미", "배움", "한달"));

        typeTags.put("제대로 맛보고 싶은 형",
                List.of("몰입", "도전", "배움", "성장", "한달"));

        typeTags.put("여러 가지 취향껏 골라먹는 형",
                List.of("활기찬", "트렌디", "취미", "입문", "가볍게", "단기"));

        typeTags.put("같이 먹어야 더 맛있는 형",
                List.of("활기찬", "취미", "소규모", "대규모"));

        typeTags.put("꽂히면 계속 먹는 형",
                List.of("몰입", "취미", "성장", "한달", "6개월"));

        typeTags.put("새로운 맛에 끌리는 형",
                List.of("활기찬", "트렌디", "도전", "입문", "가볍게", "단기"));

        typeTags.put("스테디한 맛을 좋아하는 형",
                List.of("편안한", "휴식", "취미", "가볍게", "6개월"));

        typeTags.put("끝까지 맛보는 형",
                List.of("몰입", "도전", "배움", "성장", "6개월", "1년이상"));

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
}
