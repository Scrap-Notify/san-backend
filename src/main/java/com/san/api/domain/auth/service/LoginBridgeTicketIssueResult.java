package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.response.LoginBridgeTicketResponse;
import com.san.api.domain.auth.entity.ClientType;

record LoginBridgeTicketIssueResult(
        String userId,
        ClientType sourceClientType,
        LoginBridgeTicketResponse response
) {
}
