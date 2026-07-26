package site.coreservice.pointwallet.ledger.domain;

public enum PointTransactionType {
    DEPOSIT,
    HOLD,
    RELEASE,
    DEPOSIT_CANCEL,
    WITHDRAW,      // 인출 시 사용자 지갑에서 차감 (전액)
    FEE_INCOME     // 수수료가 플랫폼 계정으로 적립 (인출/정산 공용)
}