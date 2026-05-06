package com.san.api.domain.til.entity;

/** TIL GitHub 커밋 처리 상태 */
public enum TilGithubCommitStatus {
    /** GitHub 커밋 요청이 등록된 상태 */
    PENDING,
    /** GitHub 커밋 요청을 처리 중인 상태 */
    PROCESSING,
    /** GitHub 커밋이 완료된 상태 */
    COMPLETED,
    /** GitHub 커밋이 실패한 상태 */
    FAILED
}
