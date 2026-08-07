package site.pointwalletservice.ledger.domain;

import java.util.List;

/** 거래내역 검색 결과 한 페이지. product 모듈의 ProductSearchPage와 동일한 패턴 — Pageable/Page는 infra 안에서만. */
public record PointTransactionSearchPage(List<PointTransaction> content, long totalElements) {
}