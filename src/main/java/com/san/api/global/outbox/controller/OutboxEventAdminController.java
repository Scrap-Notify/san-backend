package com.san.api.global.outbox.controller;

import com.san.api.global.outbox.dto.request.OutboxEventSearchRequest;
import com.san.api.global.outbox.dto.response.OutboxEventPageResponse;
import com.san.api.global.outbox.dto.response.OutboxEventResponse;
import com.san.api.global.outbox.service.OutboxEventQueryService;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Outbox Admin", description = "Outbox 이벤트 운영 API")
@Validated
@RestController
@RequestMapping("/admin/outbox-events")
@RequiredArgsConstructor
public class OutboxEventAdminController {

    private final OutboxEventQueryService outboxEventQueryService;

    /**
     * Outbox 이벤트를 조건별로 조회합니다.
     *
     * @param request 조회 조건
     * @param page    페이지 번호
     * @param size    페이지 크기
     * @return Outbox 이벤트 목록
     */
    @Operation(summary = "Outbox 이벤트 운영 조회", description = "Outbox 이벤트 상태와 실패 사유를 조건별로 조회합니다.")
    @GetMapping
    public ApiResponse<OutboxEventPageResponse> search(
            @ModelAttribute OutboxEventSearchRequest request,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(outboxEventQueryService.search(request, page, size));
    }

    /**
     * Outbox 이벤트를 단건 조회합니다.
     *
     * @param outboxEventId 조회할 Outbox 이벤트 ID
     * @return Outbox 이벤트 상세
     */
    @Operation(summary = "Outbox 이벤트 상세 조회", description = "Outbox 이벤트 payload와 처리 상태를 단건 조회합니다.")
    @GetMapping("/{outboxEventId}")
    public ApiResponse<OutboxEventResponse> get(@PathVariable UUID outboxEventId) {
        return ApiResponse.success(outboxEventQueryService.get(outboxEventId));
    }
}
