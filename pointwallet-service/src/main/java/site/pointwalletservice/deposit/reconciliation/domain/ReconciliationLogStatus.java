package site.pointwalletservice.deposit.reconciliation.domain;

public enum ReconciliationLogStatus {
    /** 아직 사람이 확인/처리 안 한 상태 - 관리자 목록의 기본 조회 대상. */
    OPEN,
    /** 관리자가 수동으로 확인하고 조치를 마친 상태. */
    RESOLVED
}