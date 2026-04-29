package com.san.api.global.exception.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 인증/인가 도메인 에러 코드 (A 계열)
 *
 * 클라이언트에 노출되는 에러 코드는 최소한의 정보만 담습니다.
 * 구체적인 실패 원인(어떤 필드가 틀렸는지 등)은 로그에만 기록합니다.
 */
@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "A001", "이미 사용 중인 아이디입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A002", "아이디 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "A003", "로그인 시도 초과로 계정이 잠겼습니다. 잠시 후 다시 시도해주세요."),
    ACCOUNT_WITHDRAWN(HttpStatus.FORBIDDEN, "A004", "탈퇴한 계정입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A005", "유효하지 않은 리프레시 토큰입니다."),
    TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, "A006", "이미 로그아웃된 토큰입니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "A007", "유효하지 않은 액세스 토큰입니다."),
    GITHUB_OAUTH_FAILED(HttpStatus.UNAUTHORIZED, "A008", "GitHub OAuth 인증에 실패했습니다."),
    GITHUB_ACCOUNT_NOT_LINKED(HttpStatus.BAD_REQUEST, "A009", "GitHub 계정이 연결되어 있지 않습니다."),
    GITHUB_REPOSITORY_NOT_FOUND(HttpStatus.NOT_FOUND, "A010", "GitHub 저장소를 찾을 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
