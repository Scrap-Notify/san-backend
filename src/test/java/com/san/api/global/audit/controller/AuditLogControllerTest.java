package com.san.api.global.audit.controller;

import com.san.api.global.audit.dto.request.AuditLogSearchRequest;
import com.san.api.global.audit.dto.response.AuditLogPageResponse;
import com.san.api.global.audit.service.AuditLogQueryService;
import com.san.api.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogControllerTest {

    @Mock
    private AuditLogQueryService auditLogQueryService;

    @Test
    void searchScopesActorUserIdToCurrentUser() {
        AuditLogController controller = new AuditLogController(auditLogQueryService);
        UUID currentUserId = UUID.randomUUID();
        UUID requestedActorUserId = UUID.randomUUID();
        AuditLogSearchRequest request = new AuditLogSearchRequest(
                requestedActorUserId,
                "trace-137",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        AuditLogPageResponse response = new AuditLogPageResponse(List.of(), 0, 20, 0, 0);
        when(auditLogQueryService.search(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(20)))
                .thenReturn(response);

        controller.search(new TestingAuthenticationToken(currentUserId.toString(), null), request, 0, 20);

        ArgumentCaptor<AuditLogSearchRequest> captor = ArgumentCaptor.forClass(AuditLogSearchRequest.class);
        verify(auditLogQueryService).search(captor.capture(), org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(20));
        assertThat(captor.getValue().actorUserId()).isEqualTo(currentUserId);
        assertThat(captor.getValue().actorUserId()).isNotEqualTo(requestedActorUserId);
        assertThat(captor.getValue().traceId()).isEqualTo("trace-137");
    }

    @Test
    void searchRejectsMissingAuthentication() {
        AuditLogController controller = new AuditLogController(auditLogQueryService);
        AuditLogSearchRequest request = new AuditLogSearchRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> controller.search(null, request, 0, 20))
                .isInstanceOf(BusinessException.class);
    }
}
