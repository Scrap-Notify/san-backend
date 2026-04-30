package com.san.api.global.external.ai.client;

/**
 * AI 서버 임베딩 변환 클라이언트 인터페이스.
 *
 * <p>TODO: AI 파트 연동 시 구현체 작성 필요 (FastAPI POST /embed 호출, 장애 시 BusinessException 발생)</p>
 */
public interface AiEmbeddingClient {

    /**
     * 텍스트를 1536차원 임베딩 벡터로 변환.
     *
     * @param text 임베딩할 텍스트 (검색어 또는 문서)
     * @return 1536차원 float 배열
     * @throws com.san.api.global.exception.BusinessException AI 서버 장애 또는 타임아웃 발생 시
     */
    float[] embed(String text);
}
