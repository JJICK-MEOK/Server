package com.jjikmeok.app.global.common.exception;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityRegionErrorCodeTest {

    @ParameterizedTest
    @MethodSource("activityRegionErrorCodes")
    void activityAndRegionErrorCodes_areMappedCorrectly(ErrorCode errorCode, HttpStatus httpStatus, String code) {
        assertThat(errorCode.getHttpStatus()).isEqualTo(httpStatus);
        assertThat(errorCode.getCode()).isEqualTo(code);
        assertThat(errorCode.getMessage()).isNotBlank();
    }

    private static Stream<Arguments> activityRegionErrorCodes() {
        return Stream.of(
                Arguments.of(ErrorCode.ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND, "ACTIVITY_404"),
                Arguments.of(ErrorCode.ACTIVITY_INVALID_RECRUIT_PERIOD, HttpStatus.BAD_REQUEST, "ACTIVITY_400_RECRUIT_PERIOD"),
                Arguments.of(ErrorCode.ACTIVITY_INVALID_ACTIVITY_PERIOD, HttpStatus.BAD_REQUEST, "ACTIVITY_400_ACTIVITY_PERIOD"),
                Arguments.of(ErrorCode.ACTIVITY_INVALID_SCHEDULE_ORDER, HttpStatus.BAD_REQUEST, "ACTIVITY_400_SCHEDULE_ORDER"),
                Arguments.of(ErrorCode.ACTIVITY_INVALID_URI, HttpStatus.BAD_REQUEST, "ACTIVITY_400_URI"),
                Arguments.of(ErrorCode.REGION_NOT_FOUND, HttpStatus.NOT_FOUND, "REGION_404"),
                Arguments.of(ErrorCode.REGION_IN_USE, HttpStatus.CONFLICT, "REGION_409_IN_USE"),
                Arguments.of(ErrorCode.REGION_PARENT_NOT_FOUND, HttpStatus.NOT_FOUND, "REGION_404_PARENT"),
                Arguments.of(ErrorCode.REGION_PARENT_REQUIRED, HttpStatus.BAD_REQUEST, "REGION_400_PARENT_REQUIRED"),
                Arguments.of(ErrorCode.REGION_PARENT_NOT_ALLOWED, HttpStatus.BAD_REQUEST, "REGION_400_PARENT_NOT_ALLOWED"),
                Arguments.of(ErrorCode.REGION_INVALID_PARENT_DEPTH, HttpStatus.BAD_REQUEST, "REGION_400_PARENT_DEPTH"),
                Arguments.of(ErrorCode.REGION_SELF_PARENT_NOT_ALLOWED, HttpStatus.BAD_REQUEST, "REGION_400_SELF_PARENT"),
                Arguments.of(ErrorCode.REGION_DUPLICATE_NAME, HttpStatus.CONFLICT, "REGION_409_DUPLICATE_NAME")
        );
    }
}
