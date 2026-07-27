package site.coreservice.product.infrastructure.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import site.coreservice.product.domain.ClosedAuction;
import site.coreservice.product.domain.MediaCondition;
import site.coreservice.product.exception.AuctionContractViolationException;

/**
 * 경매 내부 API의 응답 본문. 필드명과 타입을 경매 쪽 InternalAuctionSummaryResponse에 1:1로 맞춘다 —
 * 우리 ClosedAuction과 이름이 달라(itemCondition/endAt) 직접 역직렬화할 수 없어 한 겹 둔다.
 * <p>
 * 변환 원칙은 하나다: 값이 우리 계약과 어긋나면 조용히 넘기지 않고 예외로 올린다. 시세는 나중에
 * 집계에 쓰이는 원본이라, 여기서 한 번 잘못 들어간 값은 나중에 어디서 틀어졌는지 되짚기 어렵다.
 */
public record AuctionSummaryPayload(Long auctionId, Long productId, String itemCondition, long bidCount,
        BigDecimal finalPrice, LocalDateTime endAt, String status) {

    public ClosedAuction toClosedAuction() {
        return new ClosedAuction(auctionId, productId, toMediaCondition(), toFinalPrice(), toBidCount(),
                endAt, status);
    }

    private MediaCondition toMediaCondition() {
        try {
            return MediaCondition.from(itemCondition);
        } catch (IllegalArgumentException e) {
            throw new AuctionContractViolationException("컨디션 등급을 해석할 수 없습니다: " + itemCondition);
        }
    }

    /**
     * 금액은 원 단위 정수로만 다루기로 한 계약이다. longValue()는 소수부를 말없이 버리므로 쓰지 않는다.
     * 소수부가 0인 값(decimal(19,2) 저장값인 15000.00 등)은 예외 없이 통과한다.
     */
    private Long toFinalPrice() {
        if (finalPrice == null) {
            return null;
        }
        try {
            return finalPrice.longValueExact();
        } catch (ArithmeticException e) {
            throw new AuctionContractViolationException("낙찰가가 정수가 아닙니다: " + finalPrice);
        }
    }

    private Integer toBidCount() {
        try {
            return Math.toIntExact(bidCount);
        } catch (ArithmeticException e) {
            throw new AuctionContractViolationException("입찰 수가 정수 범위를 넘습니다: " + bidCount);
        }
    }
}
