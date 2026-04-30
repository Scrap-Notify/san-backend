package com.san.api.domain.scrap.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 스크랩 원본 데이터 유형 */
@Getter
@AllArgsConstructor
public enum SourceType {
    // 링크
    LINK,

    // 텍스트
    TEXT,

    // 이미지
    IMAGE
}
