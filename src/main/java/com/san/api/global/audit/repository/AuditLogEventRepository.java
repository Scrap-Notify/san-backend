package com.san.api.global.audit.repository;

import com.san.api.global.audit.entity.AuditLogEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * 감사 로그 이벤트 저장소.
 * 사용자 행위, 외부 연동 성공/실패, 비동기 처리 흐름에서 발생한 감사 이벤트를 저장하고 조회
 */
public interface AuditLogEventRepository extends JpaRepository<AuditLogEvent, UUID> {
}
