package com.jjikmeok.app.global.security.handler;

import com.jjikmeok.app.global.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        log.warn(
                "Authentication failed. requestURI={}, method={}, exception={}",
                request.getRequestURI(),
                request.getMethod(),
                authException.getClass().getSimpleName()
        );

        securityErrorResponseWriter.write(response, ErrorCode.AUTH_UNAUTHORIZED);
    }
}
