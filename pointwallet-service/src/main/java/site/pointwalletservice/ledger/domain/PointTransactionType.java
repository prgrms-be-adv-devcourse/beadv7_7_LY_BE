package site.pointwalletservice.ledger.domain;

public enum PointTransactionType {
    DEPOSIT,
    HOLD,
    RELEASE,
    DEPOSIT_CANCEL,
    WITHDRAW,          // 인출 시 사용자 지갑에서 차감 (전액)
    FEE_INCOME,        // 수수료가 플랫폼 계정으로 적립 (인출/정산 공용)
    SETTLEMENT_PAYOUT, // 정산 확정 시 판매자 지갑에 정산금이 입금
    REFUND              // 주문 환불 승인 시 구매자 지갑에 낙찰금이 환불
}