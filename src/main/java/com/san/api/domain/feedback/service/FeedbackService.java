package com.san.api.domain.feedback.service;

import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.feedback.dto.request.FeedbackCreateRequest;
import com.san.api.domain.feedback.entity.Feedback;
import com.san.api.domain.feedback.repository.FeedbackRepository;
import com.san.api.domain.user.entity.User;
import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/** 서비스 피드백 저장과 알림 전송을 담당합니다. */
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final MattermostFeedbackNotifier mattermostFeedbackNotifier;
    private final EntityManager entityManager;

    /**
     * 사용자 피드백을 저장하고 요청 컨텍스트 메타데이터를 함께 기록합니다.
     * 저장 후 Mattermost 알림을 전송합니다.
     */
    @Transactional
    public UUID createFeedback(UUID userId, ClientType clientType, FeedbackCreateRequest request) {
        AuditRequestContext context = AuditRequestContextHolder.get().orElse(null);
        Feedback feedback = Feedback.builder()
                .user(entityManager.getReference(User.class, userId))
                .type(request.type())
                .content(request.content())
                .contact(blankToNull(request.contact()))
                .pageUrl(blankToNull(request.pageUrl()))
                .clientType(clientType)
                .traceId(context == null ? null : context.traceId())
                .ipAddress(context == null ? null : context.ipAddress())
                .userAgent(context == null ? null : context.userAgent())
                .build();

        Feedback saved = feedbackRepository.save(feedback);
        notifyAfterCommit(createNotificationPayload(saved, userId));
        return saved.getFeedbackId();
    }

    /** 트랜잭션 커밋 이후 Mattermost 알림을 전송합니다. */
    private void notifyAfterCommit(FeedbackNotificationPayload payload) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            mattermostFeedbackNotifier.notify(payload);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                mattermostFeedbackNotifier.notify(payload);
            }
        });
    }

    /** 영속성 컨텍스트 밖에서도 사용할 수 있도록 알림에 필요한 값만 복사합니다. */
    private FeedbackNotificationPayload createNotificationPayload(Feedback feedback, UUID userId) {
        return new FeedbackNotificationPayload(
                feedback.getFeedbackId(),
                feedback.getType(),
                userId,
                feedback.getClientType(),
                feedback.getPageUrl(),
                feedback.getTraceId(),
                feedback.getContact(),
                feedback.getContent()
        );
    }

    /** 빈 문자열은 DB에 null로 저장합니다. */
    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
