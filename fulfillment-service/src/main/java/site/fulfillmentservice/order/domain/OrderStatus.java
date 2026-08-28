package site.fulfillmentservice.order.domain;

public enum OrderStatus {
    PENDING, // 생성
    ORDERED, // 주문
    CANCELLED, // 주문취소
    COMPLETED, // 거래확정
    REFUND_REQUESTED, // 환불 신청
    REFUND, // 환불 완료
    REFUND_REJECTED // 환불 반려
}
