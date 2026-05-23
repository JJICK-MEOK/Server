package com.jjikmeok.app.domain.user.dto.request;

import com.jjikmeok.app.domain.user.entity.ProfileGender;
import com.jjikmeok.app.domain.user.entity.ProfileStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "사용자 프로필 생성 요청")
public record UserProfileCreateReq(

        @Schema(
                description = "사용자 닉네임",
                example = "찍먹러"
        )
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
        String nickname,

        @Schema(
                description = "사용자 생년월일",
                example = "2001-03-15"
        )
        @NotNull(message = "생년월일은 필수입니다.")
        LocalDate birthDate,

        @Schema(
                description = "성별. MALE, FEMALE, NONE",
                example = "MALE"
        )
        @NotNull(message = "성별은 필수입니다.")
        ProfileGender gender,

        @Schema(
                description = "현재 상태. STUDENT, WORKER, JOB_SEEKER, FREELANCER, ETC",
                example = "STUDENT"
        )
        @NotNull(message = "상태는 필수입니다.")
        ProfileStatus status,

        @Schema(
                description = "서비스 이용 약관 동의 여부",
                example = "true"
        )
        @NotNull(message = "서비스 이용 약관 동의 여부는 필수입니다.")
        Boolean serviceTermsAgreed,

        @Schema(
                description = "개인정보 처리방침 동의 여부",
                example = "true"
        )
        @NotNull(message = "개인정보 처리방침 동의 여부는 필수입니다.")
        Boolean privacyPolicyAgreed,

        @Schema(
                description = "마케팅 정보 수신 동의 여부",
                example = "false"
        )
        @NotNull(message = "마케팅 동의 여부는 필수입니다.")
        Boolean marketingAgreed
) {
}