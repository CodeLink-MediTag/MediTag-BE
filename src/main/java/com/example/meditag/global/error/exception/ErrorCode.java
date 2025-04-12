package com.example.meditag.global.error.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 로그인 / 회원가입
    SAMPLE_ERROR(HttpStatus.BAD_REQUEST, "Sample Error Message"),
    MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용중인 아이디(이메일)입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    USERNAME_OR_PASSWORD_MISSING(HttpStatus.BAD_REQUEST, "아이디 또는 비밀번호를 입력하세요."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "로그인 요청을 처리하는 동안 오류가 발생했습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."),

    // 약 등록
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "복용 시간대를 지정해야 합니다."),
    ALREADY_HAS_PRESCRIBED_MEDICINE(HttpStatus.BAD_REQUEST, "이미 처방약이 등록되어있습니다."),

    // 약 조회
    MEDICINE_NOT_FOUND_FOR_DATE(HttpStatus.BAD_REQUEST, "해당 날짜에 복약 정보가 없습니다."),
    MEDICINE_NOT_FOUND(HttpStatus.BAD_REQUEST, "약을 찾을 수 없습니다."),
    ALARM_NOT_FOUND(HttpStatus.BAD_REQUEST, "알림을 찾을 수 없습니다."),

    // 날짜
    CALENDAR_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 날짜를 찾을 수 없습니다."),

    // 인증 관련
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "토큰이 제공되지 않았습니다."),

    OPENAI_PARSE_ERROR(HttpStatus.BAD_REQUEST, "해당 날짜를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
