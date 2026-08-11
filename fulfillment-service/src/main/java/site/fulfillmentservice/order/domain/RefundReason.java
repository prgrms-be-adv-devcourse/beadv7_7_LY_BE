package site.fulfillmentservice.order.domain;

public enum RefundReason {

    DEFECTIVE, // 상품 파손/불량
    WRONG_ITEM, // 오배송(다른 상품 수령)
    NOT_AS_DESCRIBED, // 설명과 다른 상태/컨디션
    SUSPECTED_FAKE, // 가품 의심
    NOT_DELIVERED, // 미배송
    OTHER // 기타

}
