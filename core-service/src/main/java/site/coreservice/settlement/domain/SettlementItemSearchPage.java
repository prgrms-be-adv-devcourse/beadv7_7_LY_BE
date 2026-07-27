package site.coreservice.settlement.domain;

import java.util.List;

public record SettlementItemSearchPage(List<SettlementItem> content, long totalElements) {
}
