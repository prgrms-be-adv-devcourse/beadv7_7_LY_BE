package site.coreservice.order.domain;

public enum CancelReason {

    BUYER_DECLINED, // 구매자가 직접 취소
    CONFIRMATION_TIMEOUT // 타임아웃 자동 취소

}
