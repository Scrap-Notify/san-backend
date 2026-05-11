package com.san.api.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 브릿지 ticket 교환 요청")
public record LoginBridgeTokenRequest(
        @Schema(description = "Dashboard에서 발급받은 일회용 로그인 브릿지 ticket")
        @NotBlank(message = "로그인 브릿지 ticket을 입력해주세요.")
        String ticket
) {
}
