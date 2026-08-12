package site.auctionservice.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AuctionRepository {

    Auction save(Auction auction);

    Optional<Auction> findById(Long id);

    /**
     * 비관적 쓰기 락(SELECT ... FOR UPDATE)으로 조회. Auction/Bid를 변경하는 5개 경로(입찰/수정/취소/시작·마감 스케줄러)에서 사용.
     * 락 대기 상한선을 짧게 걸어두고, 그 시간 안에 못 잡으면 AuctionException(LOCK_ACQUISITION_FAILED) 던진다
     */
    Optional<Auction> findByIdForUpdate(Long id);

    List<Auction> findAllByIds(List<Long> ids);

    List<Auction> findAllScheduledToStart(LocalDateTime threshold);

    Map<Long, Long> countRunningByProductIds(List<Long> productIds);

    List<Auction> findAllRunningToEnd(LocalDateTime threshold);

    Page<Auction> findBySellerId(Long sellerId, Pageable pageable);
}
