package com.san.api.global.async.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobType {
    CARD_ANALYSIS("스크랩 원문 기반 지식 카드 AI 분석"),
    TIL_GENERATION("지식 카드들을 바탕으로 TIL 문서 생성"),
    TIL_GITHUB_COMMIT("TIL GitHub 커밋 생성");

    private final String description;
}
