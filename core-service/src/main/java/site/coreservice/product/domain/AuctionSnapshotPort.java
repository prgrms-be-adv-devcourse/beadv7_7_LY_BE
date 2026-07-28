package site.coreservice.product.domain;

import java.util.Optional;

/**
 * 경매 정보 조회 창구 (도메인 인터페이스). 반환형이 우리 VO인 이유는 ClosedAuction 주석 참고.
 * 구현체는 infrastructure/client의 AuctionSnapshotHttpClient.
 */
public interface AuctionSnapshotPort {

    Optional<ClosedAuction> findClosedAuction(Long auctionId);
}
