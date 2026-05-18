package com.san.api.global.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.san.api.global.audit.entity.AuditLogEvent;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;

@Component
public class AuditLogIntegrityHasher {

    private final ObjectMapper canonicalObjectMapper;

    public AuditLogIntegrityHasher(ObjectMapper objectMapper) {
        this.canonicalObjectMapper = objectMapper.copy()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public String hash(AuditLogEvent event) {
        String canonicalPayload = String.join("|",
                value(event.getAuditLogEventId()),
                value(event.getActorUserId()),
                value(event.getTraceId()),
                value(event.getEventDomain()),
                value(event.getEventType()),
                value(event.getTargetType()),
                value(event.getTargetId()),
                value(event.getOutcome()),
                value(event.getFailureReasonCode()),
                value(event.getFailureMessage()),
                value(event.getIpAddress()),
                value(event.getUserAgent()),
                metadata(event.getMetadata()),
                value(event.getOccurredAt() == null ? null : event.getOccurredAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        );
        return sha256(canonicalPayload);
    }

    private String value(Object value) {
        if (value == null) {
            return "null:";
        }
        String stringValue = String.valueOf(value);
        return stringValue.length() + ":" + stringValue;
    }

    private String metadata(Map<String, Object> metadata) {
        try {
            String json = canonicalObjectMapper.writeValueAsString(metadata);
            return value(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Audit log metadata serialization failed.", e);
        }
    }

    private String sha256(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }
}
