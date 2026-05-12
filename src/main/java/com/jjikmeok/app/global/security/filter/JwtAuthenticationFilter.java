package com.jjikmeok.app.global.security.filter;

import com.jjikmeok.app.global.common.exception.ErrorCode;
import com.jjikmeok.app.global.security.exception.JwtTokenException;
import com.jjikmeok.app.global.security.handler.SecurityErrorResponseWriter;
import com.jjikmeok.app.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            authenticate(request, token);
            filterChain.doFilter(request, response);
        } catch (JwtTokenException e) {
            handleUnauthorized(response, e);
        }
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    private void authenticate(HttpServletRequest request, String token) {
        jwtTokenProvider.validateToken(token);

        Long userId = jwtTokenProvider.getUserId(token);

        List<GrantedAuthority> authorities = extractAuthorities(token);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private List<GrantedAuthority> extractAuthorities(String token) {
        String role = jwtTokenProvider.getRole(token);
        if (role == null || role.isBlank()) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private void handleUnauthorized(
            HttpServletResponse response,
            JwtTokenException exception
    ) throws IOException {
        SecurityContextHolder.clearContext();
        ErrorCode errorCode = exception.getErrorCode();
        securityErrorResponseWriter.write(response, errorCode);
    }
}
