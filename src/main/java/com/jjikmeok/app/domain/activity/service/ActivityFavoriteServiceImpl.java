package com.jjikmeok.app.domain.activity.service;

import com.jjikmeok.app.domain.activity.converter.ActivityFavoriteConverter;
import com.jjikmeok.app.domain.activity.dto.request.ActivityFavoriteRequest;
import com.jjikmeok.app.domain.activity.dto.response.ActivityFavoriteResponse;
import com.jjikmeok.app.domain.activity.entity.Activity;
import com.jjikmeok.app.domain.activity.entity.ActivityFavorite;
import com.jjikmeok.app.domain.activity.repository.ActivityFavoriteRepository;
import com.jjikmeok.app.domain.activity.repository.ActivityRepository;
import com.jjikmeok.app.domain.user.entity.User;
import com.jjikmeok.app.domain.user.repository.UserRepository;
import com.jjikmeok.app.global.common.exception.CustomException;
import com.jjikmeok.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityFavoriteServiceImpl implements ActivityFavoriteService {

    private final ActivityFavoriteRepository favoriteRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Override
    public List<ActivityFavoriteResponse> getFavorites(Long userId, String sort) {
        findUserOrThrow(userId);
        return findFavorites(userId, sort).stream()
                .map(ActivityFavoriteConverter::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ActivityFavoriteResponse createFavorite(Long userId, ActivityFavoriteRequest request) {
        User user = findUserOrThrow(userId);
        Activity activity = findActivityOrThrow(request.activityId());
        if (favoriteRepository.existsByUserIdAndActivityId(userId, request.activityId())) {
            throw new CustomException(ErrorCode.ACTIVITY_FAVORITE_DUPLICATE);
        }

        try {
            ActivityFavorite favorite = favoriteRepository.save(ActivityFavorite.create(user, activity));
            activity.increaseLikeCount();
            return ActivityFavoriteConverter.toResponse(favorite);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.ACTIVITY_FAVORITE_DUPLICATE);
        }
    }

    @Override
    @Transactional
    public void deleteFavorite(Long userId, Long activityId) {
        findUserOrThrow(userId);
        Activity activity = findActivityOrThrow(activityId);
        ActivityFavorite favorite = favoriteRepository.findByUserIdAndActivityId(userId, activityId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_FAVORITE_NOT_FOUND));
        favoriteRepository.delete(favorite);
        activity.decreaseLikeCount();
    }

    private List<ActivityFavorite> findFavorites(Long userId, String sort) {
        if ("deadline".equals(normalizeSort(sort))) { //만약 정렬 조건이 데드라인 인 경우, 데드라인이 빠른 순서로 정렬
            return favoriteRepository.findAllByUserIdOrderByRecruitEndAtAsc(userId);
        } //아니라면 많이 찜한 순서로 정렬
        return favoriteRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 직렬화 메서드
     * null 방어적 코드, 소문자 직렬화
     */
    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "saved";
        }
        return sort.trim().toLowerCase(Locale.ROOT);
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_UNAUTHORIZED));
    }

    private Activity findActivityOrThrow(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
    }
}
