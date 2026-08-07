package site.fulfillmentservice.order.domain;

public enum OrderStatus {
    PENDING, // 생성
    ORDERED, // 주문
    CANCELLED, // 주문취소
    COMPLETED // 거래확정
}
