package com.jjikmeok.app.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignupRes {

    private Long userId;
    private String email;
}
