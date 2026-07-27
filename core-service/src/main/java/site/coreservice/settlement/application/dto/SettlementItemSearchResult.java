package site.coreservice.settlement.application.dto;

import java.util.List;
import site.coreservice.settlement.domain.SettlementItemSearchPage;

public record SettlementItemSearchResult(List<SettlementItemResult> content, int page, int size,
                                          long totalElements, int totalPages, boolean last) {

    public static SettlementItemSearchResult of(SettlementItemSearchPage searchPage, int page, int size) {
        List<SettlementItemResult> content = searchPage.content().stream()
                .map(SettlementItemResult::from)
                .toList();
        int totalPages = (int) Math.ceil((double) searchPage.totalElements() / size);
        boolean last = (page + 1) >= totalPages;
        return new SettlementItemSearchResult(content, page, size, searchPage.totalElements(), totalPages, last);
    }
}
