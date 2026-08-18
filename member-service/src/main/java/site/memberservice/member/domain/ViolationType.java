package site.memberservice.member.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum ViolationType {
    ORDER_CANCELLED("낙찰된 경매 상품 주문 취소"),
    ;

    private final String description;
}
