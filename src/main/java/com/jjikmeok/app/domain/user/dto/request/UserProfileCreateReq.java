package com.jjikmeok.app.domain.user.dto.request;

import com.jjikmeok.app.domain.user.entity.ProfileGender;
import com.jjikmeok.app.domain.user.entity.ProfileStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserProfileCreateReq(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
        String nickname,

        @NotNull(message = "생년월일은 필수입니다.")
        LocalDate birthDate,

        @NotNull(message = "성별은 필수입니다.")
        ProfileGender gender,

        @NotNull(message = "상태는 필수입니다.")
        ProfileStatus status,

        @NotNull(message = "서비스 이용 약관 동의 여부는 필수입니다.")
        Boolean serviceTermsAgreed,

        @NotNull(message = "개인정보 처리방침 동의 여부는 필수입니다.")
        Boolean privacyPolicyAgreed,

        @NotNull(message = "마케팅 동의 여부는 필수입니다.")
        Boolean marketingAgreed
) {
}
