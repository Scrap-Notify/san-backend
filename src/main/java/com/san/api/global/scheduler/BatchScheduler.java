package com.san.api.global.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 배치 스케줄러 진입점. 비즈니스 로직 없이 각 서비스의 배치 메서드를 호출한다. */
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final GhostJobRecoveryService ghostJobRecoveryService;
    private final OrphanScrapRecoveryService orphanScrapRecoveryService;
    private final TilAutoGenerationScheduleService tilAutoGenerationScheduleService;

    /**
     * 유령 잡·고아 스크랩 복구 배치. 2시간마다 실행.
     */
    @Scheduled(cron = "0 0 */2 * * *")
    public void runRecovery() {
        ghostJobRecoveryService.recover();
        orphanScrapRecoveryService.recover();
    }

    /**
     * TIL 자동 생성 배치. 매일 03:00 실행.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void runTilAutoGeneration() {
        tilAutoGenerationScheduleService.generate();
    }
}
