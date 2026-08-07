package site.productservice.application.port;

import java.util.Optional;
import site.productservice.domain.price.ClosedAuction;

/**
 * 경매 정보 조회 창구 (아웃바운드 포트). 반환형이 우리 VO인 이유는 ClosedAuction 주석 참고.
 * 구현체는 infrastructure/client의 AuctionSnapshotHttpClient.
 */
public interface AuctionSnapshotPort {

    Optional<ClosedAuction> findClosedAuction(Long auctionId);
}
