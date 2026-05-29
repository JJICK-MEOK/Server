package com.jjikmeok.app.global.common.exception;

import com.jjikmeok.app.global.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 우리가 만든 커스텀 예외 처리 (가장 많이 타게 될 로직)
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("CustomException: {}", errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getHttpStatus()) // HTTP 헤더 상태 코드 (예: 400, 401, 404)
                .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMessage())); // JSON 바디 (예: "C009", "서버 에러")
    }

    // 2. 잘못된 요청 본문
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            HttpMessageConversionException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequestBody(Exception e) {
        log.warn("Invalid Request Body: {}", e.getMessage());
        return fail(ErrorCode.INVALID_REQUEST_BODY);
    }

    // 3. 잘못된 요청 파라미터
    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingPathVariableException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequestParameter(Exception e) {
        log.warn("Invalid Request Parameter: {}", e.getMessage());
        return fail(ErrorCode.INVALID_REQUEST_PARAMETER);
    }

    // 4. DTO/Bean Validation 실패
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HandlerMethodValidationException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleValidationFailed(Exception e) {
        log.warn("Validation Failed: {}", e.getMessage());
        return fail(ErrorCode.VALIDATION_FAILED);
    }

    // 5. 애플리케이션에서 발생한 잘못된 인자
    @ExceptionHandler({
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
        log.warn("Bad Request: {}", e.getMessage());
        return fail(ErrorCode.BAD_REQUEST);
    }

    // 6. 존재하지 않는 API 경로
    @ExceptionHandler({
            NoHandlerFoundException.class,
            NoResourceFoundException.class
    })
    public ResponseEntity<ApiResponse<Void>> handlePathNotFound(Exception e) {
        log.warn("Path Not Found: {}", e.getMessage());
        return fail(ErrorCode.REQUEST_PATH_NOT_FOUND);
    }

    // 7. 지원하지 않는 HTTP 메서드
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        log.warn("Method Not Allowed: {}", e.getMessage());
        return fail(ErrorCode.METHOD_NOT_ALLOWED);
    }

    // 8. DB 제약 조건 위반
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Data Integrity Violation: {}", e.getMessage());
        return fail(ErrorCode.DATA_INTEGRITY_VIOLATION);
    }

    // 9. 권한 부족 (Spring Security 관련)
    @ExceptionHandler({
            AuthorizationDeniedException.class,
            AccessDeniedException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(Exception e) {
        log.warn("Access Denied (권한 없음): {}", e.getMessage());
        return fail(ErrorCode.AUTH_FORBIDDEN);
    }

    // 10. 위에서 못 잡은 모든 서버 내부 에러 (최후의 보루)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllException(Exception e) {
        log.error("Internal Server Error: ", e); // 500 에러는 원인 파악을 위해 꼭 error 레벨로 전체 스택 트레이스를 남깁니다.
        return fail(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiResponse<Void>> fail(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMessage()));
    }
}
