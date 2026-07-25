package site.coreservice.pointwallet.ledger.application.dto;

import java.util.List;
import site.coreservice.pointwallet.ledger.domain.PointTransactionSearchPage;

public record PointTransactionSearchResult(List<PointTransactionResult> content, int page, int size,
                                           long totalElements, int totalPages) {

    public static PointTransactionSearchResult of(PointTransactionSearchPage searchPage, int page, int size) {
        List<PointTransactionResult> content = searchPage.content().stream()
                .map(PointTransactionResult::from)
                .toList();
        int totalPages = (int) Math.ceil((double) searchPage.totalElements() / size);
        return new PointTransactionSearchResult(content, page, size, searchPage.totalElements(), totalPages);
    }

    public static PointTransactionSearchResult empty(int page, int size) {
        return new PointTransactionSearchResult(List.of(), page, size, 0L, 0);
    }
}