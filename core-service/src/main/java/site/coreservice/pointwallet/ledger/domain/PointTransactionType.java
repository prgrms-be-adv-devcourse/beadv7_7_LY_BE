package site.coreservice.pointwallet.ledger.domain;

/** DEPOSIT, HOLD, RELEASE까지 구현. 인출/정산 붙을 때 WITHDRAW, SETTLE 등 추가 예정. */
public enum PointTransactionType {
    DEPOSIT,
    HOLD,
    RELEASE
}