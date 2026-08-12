package site.auctionservice.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import site.auctionservice.domain.Auction;
import site.auctionservice.domain.AuctionRepository;
import site.auctionservice.domain.AuctionStatus;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AuctionRepositoryImpl implements AuctionRepository {

    /* InnoDB 락 대기 상한선(초). MySQL 기본값(50초)은 인기 경매에 입찰이 몰릴 때 뒤에 대기하는 요청들의 스레드/커넥션을 너무 오래 묶어두므로, 그보다 훨씬 짧게 끊는다. */
    private static final int LOCK_WAIT_TIMEOUT_SECONDS = 3;

    private final AuctionJpaRepository jpaRepository;

    @Override
    public Auction save(Auction auction) {
        return jpaRepository.save(auction);
    }

    @Override
    public Optional<Auction> findById(Long id) {
        return jpaRepository.findById(id);
    }

    /**
     * jakarta.persistence.lock.timeout JPA 힌트는 Hibernate MySQL 다이얼렉트에서 0(NOWAIT) 아니면 무시되는 이분법이라 "3초만 대기" 같은 임의 값을 표현할 수 없다
     * 그래서 MySQL이 실제로 지원하는 세션 변수를 직접 설정한 뒤 조회하고, 그래도 락을 못 잡으면 PessimisticLockingFailureException을 AuctionException으로 번역한다.
     */
    @Override
    public Optional<Auction> findByIdForUpdate(Long id) {
        try {
            jpaRepository.setLockWaitTimeout(LOCK_WAIT_TIMEOUT_SECONDS);
            return jpaRepository.findByIdForUpdate(id);
        } catch (PessimisticLockingFailureException e) {
            throw new AuctionException(AuctionErrorCode.LOCK_ACQUISITION_FAILED);
        }
    }

    @Override
    public List<Auction> findAllScheduledToStart(LocalDateTime threshold) {
        return jpaRepository.findAllByStatusAndStartAtLessThanEqual(AuctionStatus.SCHEDULED,
            threshold);
    }

    @Override
    public Map<Long, Long> countRunningByProductIds(List<Long> productIds) {
        return jpaRepository.countByProductIdsAndStatus(productIds, AuctionStatus.RUNNING).stream()
            .collect(Collectors.toMap(
                AuctionJpaRepository.ProductAuctionCountRow::getProductId,
                AuctionJpaRepository.ProductAuctionCountRow::getCount
            ));
    }

    @Override
    public List<Auction> findAllRunningToEnd(LocalDateTime threshold) {
        return jpaRepository.findAllByStatusAndEndAtLessThanEqual(AuctionStatus.RUNNING, threshold);
    }

    @Override
    public List<Auction> findAllByIds(List<Long> auctionIds) {
        return jpaRepository.findAllById(auctionIds);
    }

    @Override
    public Page<Auction> findBySellerId(Long sellerId, Pageable pageable) {
        List<Auction> content = jpaRepository.findBySellerId(sellerId, pageable);
        long total = jpaRepository.countBySellerId(sellerId);
        return new PageImpl<>(content, pageable, total);
    }
}
