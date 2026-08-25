package site.pointwalletservice.wallet.deadletter.domain;

public enum DeadLetterStatus {
    /** 아직 사람이 확인/재처리 안 한 상태 - 관리자 목록의 기본 조회 대상. */
    OPEN,
    /** 관리자가 수동 재처리(또는 확인)까지 마친 상태. */
    RESOLVED
}