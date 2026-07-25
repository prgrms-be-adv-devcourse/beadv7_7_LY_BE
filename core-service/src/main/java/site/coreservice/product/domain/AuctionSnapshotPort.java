package site.coreservice.product.domain;

import java.util.Optional;

/**
 * 경매 정보 조회 창구 (도메인 인터페이스). 반환형이 우리 VO인 이유는 ClosedAuction 주석 참고.
 * 세미 구현체는 infrastructure의 StubAuctionSnapshotAdapter, 실제 연동 시 구현체만 교체한다.
 */
public interface AuctionSnapshotPort {

    Optional<ClosedAuction> findClosedAuction(Long auctionId);
}
