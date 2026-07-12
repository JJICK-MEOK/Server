package com.jjikmeok.app.domain.personalization.entity;

public final class PreferenceVectorConstants {

    /**
     * 벡터 차원 순서
     *
     * 0  차분
     * 1  힐링
     * 2  편안
     * 3  활기
     * 4  입문
     * 5  가볍게
     * 6  몰입
     * 7  도전
     * 8  휴식
     * 9  취미
     * 10 배울
     * 11 사교
     * 12 성장
     * 13 경험
     */
    public static final int DIMENSION = 14;

    /**
     * 나중에 벡터 구성이나 차원 순서가 변경되었는지 구분하기 위한 버전
     */
    public static final int CURRENT_VERSION = 1;

    private PreferenceVectorConstants() {
    }
}