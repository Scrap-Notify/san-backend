package com.san.api.global.async.enums;

public enum JobTypeEnum {
    /** 스크랩 원문 기반 지식 카드 AI 분석 */
    CARD_ANALYSIS,
    /** 지식 카드들을 바탕으로 TIL 문서 생성 */
    TIL_GENERATION,
    /** TIL 임베딩 기반 연관 리콜 카드 추천 생성 */
    RECALL_GENERATION
}
