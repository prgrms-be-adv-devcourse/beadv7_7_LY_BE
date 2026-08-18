package site.pointwalletservice.deposit.reconciliation.domain;

/**
 * DepositApplicationService에서 "PG는 이미 처리됐는데, 우리 쪽 보정(재취소/재충전/DB반영)까지
 * 실패한" 이중 실패가 어느 지점에서 났는지 구분한다. 세 곳 모두 성격이 달라서(사용자 지갑 상태가
 * 다름) 관리자가 어떻게 대응해야 할지가 이 값에 따라 달라진다.
 */
public enum ReconciliationFailureType {
    /** PG 승인은 성공, DB 반영 실패, 보정 취소(cancel)도 실패 - 사용자 지갑엔 아직 반영 안 됨. */
    CONFIRM_COMPENSATION_FAILED,
    /** 지갑 차감까지는 끝났는데 PG 취소 실패, 보정(재충전)도 실패 - 사용자 지갑이 부당하게 차감된 상태. */
    CANCEL_COMPENSATION_FAILED,
    /** PG 취소는 성공(실제 환불됨)했는데 DB 반영만 실패 - 되돌릴 건 없고 우리 기록만 안 맞음. */
    CANCEL_DB_SAVE_FAILED
}