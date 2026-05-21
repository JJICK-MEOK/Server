package com.jjikmeok.app.global.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 오류가 발생했습니다. 관리자에게 문의해 주세요."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_405", "지원하지 않는 HTTP 메서드입니다."),
    RESOURCE_CONFLICT(HttpStatus.CONFLICT, "COMMON_409", "이미 존재하는 데이터입니다."),

    // Auth
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401", "인증이 필요합니다."),
    AUTH_INVALID_CODE(HttpStatus.UNAUTHORIZED, "AUTH_401_CODE", "유효하지 않은 인증 코드입니다."),
    AUTH_INVALID_SOCIAL_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_SOCIAL_ACCESS", "유효하지 않은 소셜 액세스 토큰입니다."),
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_401_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."),
    AUTH_ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_401_ACCESS", "Access Token이 만료되었습니다."),
    AUTH_REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_401_REFRESH", "Refresh Token이 만료되었습니다. 다시 로그인해 주세요."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_403", "해당 API에 접근할 권한이 없습니다."),
    SIGNUP_FAILED(HttpStatus.CONFLICT, "AUTH_409_SIGNUP", "회원가입 요청을 처리할 수 없습니다."),
    MAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_500_MAIL", "인증 메일 발송에 실패했습니다."),
    MAIL_VERIFICATION_CODE_INVALID(HttpStatus.BAD_REQUEST, "AUTH_400_MAIL_CODE", "인증번호가 올바르지 않습니다."),
    MAIL_VERIFICATION_CODE_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_401_MAIL_EXPIRED", "인증번호가 만료되었습니다."),

    // JWT
    JWT_INVALID_SECRET(HttpStatus.INTERNAL_SERVER_ERROR, "JWT_500_SECRET", "JWT 서명 키 설정이 올바르지 않습니다."),
    JWT_EMPTY_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_401_EMPTY", "JWT 토큰이 비어 있습니다."),
    JWT_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_401_EXPIRED", "JWT 토큰이 만료되었습니다."),
    JWT_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_401_INVALID", "유효하지 않은 JWT 토큰입니다."),
    JWT_INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "JWT_401_SIGNATURE", "JWT 서명이 유효하지 않습니다."),
    JWT_MALFORMED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_401_MALFORMED", "형식이 올바르지 않은 JWT 토큰입니다."),
    JWT_UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT_401_UNSUPPORTED", "지원하지 않는 JWT 토큰입니다."),
    JWT_INVALID_SUBJECT(HttpStatus.UNAUTHORIZED, "JWT_401_SUBJECT", "JWT subject 값이 올바르지 않습니다."),
    JWT_MISSING_ROLE_CLAIM(HttpStatus.UNAUTHORIZED, "JWT_401_ROLE", "JWT role claim이 존재하지 않습니다."),

    // OAuth
    INVALID_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH_400_PROVIDER", "지원하지 않는 소셜 로그인 provider입니다."),

    // Activity
    ACTIVITY_NOT_FOUND(HttpStatus.NOT_FOUND, "ACTIVITY_404", "해당 활동 정보를 찾을 수 없습니다."),
    ACTIVITY_INVALID_RECRUIT_PERIOD(HttpStatus.BAD_REQUEST, "ACTIVITY_400_RECRUIT_PERIOD", "모집 시작일은 모집 마감일보다 늦을 수 없습니다."),
    ACTIVITY_INVALID_ACTIVITY_PERIOD(HttpStatus.BAD_REQUEST, "ACTIVITY_400_ACTIVITY_PERIOD", "활동 시작일은 활동 종료일보다 늦을 수 없습니다."),
    ACTIVITY_INVALID_SCHEDULE_ORDER(HttpStatus.BAD_REQUEST, "ACTIVITY_400_SCHEDULE_ORDER", "모집 마감일은 활동 시작일보다 늦을 수 없습니다."),
    ACTIVITY_INVALID_URI(HttpStatus.BAD_REQUEST, "ACTIVITY_400_URI", "활동 URI 형식이 올바르지 않습니다."),

    // Advertisement
    ADVERTISEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ADVERTISEMENT_404", "해당 광고 정보를 찾을 수 없습니다."),
    ADVERTISEMENT_INVALID_PERIOD(HttpStatus.BAD_REQUEST, "ADVERTISEMENT_400_PERIOD", "광고 시작일은 광고 종료일보다 늦을 수 없습니다."),
    ADVERTISEMENT_INVALID_URL(HttpStatus.BAD_REQUEST, "ADVERTISEMENT_400_URL", "광고 URL 형식이 올바르지 않습니다."),

    // Region
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "REGION_404", "해당 지역 정보를 찾을 수 없습니다."),
    REGION_IN_USE(HttpStatus.CONFLICT, "REGION_409_IN_USE", "사용 중인 지역이어서 삭제할 수 없습니다."),
    REGION_HAS_CHILDREN(HttpStatus.CONFLICT, "REGION_409_HAS_CHILDREN", "하위 지역이 존재하여 삭제하거나 상위 지역으로 변경할 수 없습니다."),
    REGION_PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "REGION_404_PARENT", "상위 지역 정보를 찾을 수 없습니다."),
    REGION_PARENT_REQUIRED(HttpStatus.BAD_REQUEST, "REGION_400_PARENT_REQUIRED", "하위 지역은 상위 지역이 필요합니다."),
    REGION_PARENT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "REGION_400_PARENT_NOT_ALLOWED", "상위 지역은 parent를 가질 수 없습니다."),
    REGION_INVALID_PARENT_DEPTH(HttpStatus.BAD_REQUEST, "REGION_400_PARENT_DEPTH", "하위 지역의 상위는 PROVINCE만 가능합니다."),
    REGION_SELF_PARENT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "REGION_400_SELF_PARENT", "자기 자신을 상위 지역으로 지정할 수 없습니다."),
    REGION_DUPLICATE_NAME(HttpStatus.CONFLICT, "REGION_409_DUPLICATE_NAME", "같은 상위 지역 아래에 동일한 지역명이 이미 존재합니다."),

    // User Profile
    PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT, "PROFILE_409_EXISTS", "이미 사용자의 프로필이 존재합니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "PROFILE_409_NICKNAME", "이미 사용 중인 닉네임입니다."),
    REQUIRED_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "PROFILE_400_TERMS", "필수 약관에 동의해야 합니다."),
    INVALID_PROFILE_GENDER(HttpStatus.BAD_REQUEST, "PROFILE_400_GENDER", "올바르지 않은 gender 값입니다."),
    INVALID_PROFILE_STATUS(HttpStatus.BAD_REQUEST, "PROFILE_400_STATUS", "올바르지 않은 status 값입니다."),

    // Onboarding
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "TAG_404", "해당 태그 정보를 찾을 수 없습니다."),
    ONBOARDING_INVALID_TOPIC_TAG_TYPE(HttpStatus.BAD_REQUEST, "ONBOARDING_400_TOPIC_TAG_TYPE", "topicTagIds에는 TOPIC_CATEGORY 타입의 태그만 포함할 수 있습니다."),
    ONBOARDING_INVALID_PREFERENCE_TAG_TYPE(HttpStatus.BAD_REQUEST, "ONBOARDING_400_PREFERENCE_TAG_TYPE", "preferenceTagIds에는 PREFERENCE_TAG 타입의 태그만 포함할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
