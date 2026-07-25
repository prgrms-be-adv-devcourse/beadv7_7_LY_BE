package site.coreservice.pointwallet.ledger.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import site.coreservice.pointwallet.ledger.application.dto.PointTransactionSearchResult;

public record PointTransactionHistoryResponse(List<Item> content, int page, int totalPages) {

    public static PointTransactionHistoryResponse from(PointTransactionSearchResult result) {
        List<Item> items = result.content().stream()
                .map(r -> new Item(r.transactionId(), r.type(), r.amount(), r.relatedId(), r.occurredAt()))
                .toList();
        return new PointTransactionHistoryResponse(items, result.page(), result.totalPages());
    }

    // relatedAuctionId 필드명은 문서 계약 유지용 — 실제 값은 홀드/충전 등 근원 애그리거트 id (auctionId 전용 아님)
    public record Item(Long transactionId, String type, BigDecimal amount,
                       Long relatedAuctionId, LocalDateTime createdAt) {
    }
}