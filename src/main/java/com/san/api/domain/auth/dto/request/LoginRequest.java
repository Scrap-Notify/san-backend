package com.san.api.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/** 로그인 요청 */
public record LoginRequest(

                @NotBlank(message = "아이디를 입력해주세요.") String username,

                @NotBlank(message = "비밀번호를 입력해주세요.") String password) {
}
