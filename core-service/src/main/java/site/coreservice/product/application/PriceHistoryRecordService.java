package site.coreservice.product.application;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import site.coreservice.product.domain.AuctionSnapshotPort;
import site.coreservice.product.domain.ClosedAuction;
import site.coreservice.product.domain.PriceHistory;
import site.coreservice.product.domain.PriceHistoryRepository;
import site.coreservice.product.exception.AuctionContractViolationException;
import site.coreservice.product.exception.PriceHistoryAuctionNotClosedException;
import site.coreservice.product.exception.PriceHistoryAuctionNotFoundException;

/**
 * 거래확정 이벤트를 받아 시세 기록을 저장한다. 같은 이벤트가 몇 번 오더라도 결과는 한 번 처리한 것과 같다.
 * <p>
 * 이 메서드에는 @Transactional이 없고, DB를 만지는 지점마다 새 트랜잭션을 짧게 열었다 닫는다. 이유 둘:
 * <ul>
 * <li>커밋 직후 콜백에서 실행되므로, 기본 전파로 DB에 접근하면 이미 끝난 발행자 트랜잭션에 얹혀
 *     변경이 저장되지 않는다 (Spring 공식 경고). 그래서 매번 새 트랜잭션이 필수다.</li>
 * <li>저장이 유니크 제약에 걸렸을 때의 뒷수습(아래 catch)은 트랜잭션 밖에서 해야 한다. 제약 위반은
 *     그 트랜잭션을 롤백 전용으로 만들고, MySQL은 트랜잭션 안에서 다른 커넥션이 커밋한 행을 보여주지
 *     않기 때문 — 같은 트랜잭션 안에서 잡고 재확인하면 둘 다 어긋난다.</li>
 * </ul>
 * 주의: TransactionTemplate 기본 전파는 REQUIRED다. 반드시 이 클래스처럼 자기 소유 인스턴스에
 * REQUIRES_NEW를 명시해야 하며, 공유 빈을 주입받아 설정을 바꾸면 다른 사용처까지 오염된다.
 * 람다 안에는 리포지토리 호출 한 줄만 둔다 — 람다 안에서 예외를 잡으면 오류인데 커밋되는 사고가 난다.
 */
@Slf4j
@Service
public class PriceHistoryRecordService {

    private final AuctionSnapshotPort auctionSnapshotPort;
    private final PriceHistoryRepository priceHistoryRepository;
    private final TransactionTemplate txTemplate;

    public PriceHistoryRecordService(AuctionSnapshotPort auctionSnapshotPort,
            PriceHistoryRepository priceHistoryRepository, PlatformTransactionManager transactionManager) {
        this.auctionSnapshotPort = auctionSnapshotPort;
        this.priceHistoryRepository = priceHistoryRepository;
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void recordConfirmedTrade(Long auctionId, LocalDateTime confirmedAt) {
        PriceHistory existing = txTemplate.execute(status ->
                priceHistoryRepository.findByAuctionId(auctionId).orElse(null));
        if (existing != null) {
            if (!existing.getConfirmedAt().equals(confirmedAt)) {
                log.warn("같은 경매의 거래확정이 다른 확정시각으로 재도착 — auctionId: {}, 기존: {}, 수신: {}",
                        auctionId, existing.getConfirmedAt(), confirmedAt);
            }
            log.info("이미 기록된 거래라 건너뜀 — auctionId: {}", auctionId);
            return;
        }

        ClosedAuction auction = auctionSnapshotPort.findClosedAuction(auctionId)
                .orElseThrow(() -> new PriceHistoryAuctionNotFoundException(auctionId));
        if (!auction.isClosed()) {
            throw new PriceHistoryAuctionNotClosedException(auctionId, auction.status());
        }

        PriceHistory priceHistory = toPriceHistory(auction, confirmedAt);
        try {
            txTemplate.executeWithoutResult(status -> priceHistoryRepository.save(priceHistory));
        } catch (DataIntegrityViolationException e) {
            // 저장 거부의 원인이 "다른 스레드가 먼저 같은 경매를 기록"인지 새 트랜잭션에서 확인한다.
            // 행이 생겨 있으면 목표 상태는 이미 달성된 것이므로 정상 종료, 아니면 다른 문제이니 그대로 올린다.
            Boolean already = txTemplate.execute(status -> priceHistoryRepository.existsByAuctionId(auctionId));
            if (Boolean.TRUE.equals(already)) {
                log.info("동시 처리 경합으로 다른 쪽이 먼저 기록 — auctionId: {}", auctionId);
                return;
            }
            throw e;
        }
        log.info("시세 기록 저장 — auctionId: {}, productId: {}", auctionId, auction.productId());
    }

    /**
     * 엔티티 검증 실패를 계약 위반으로 번역한다. PriceHistory.of가 걸러내는 것들(빠진 필수값, 0 이하 낙찰가
     * 등)은 전부 "경매가 준 값이 우리 계약과 다르다"는 뜻인데, 평범한 IllegalArgumentException으로 나가면
     * 수신 측에서 "다시 넣으면 되는 실패"로 분류돼 사람이 헛되이 재적재를 반복한다.
     * <p>
     * try-catch로 감싸는 이유: 검증이 응답 변환(AuctionSummaryPayload)과 엔티티 생성(PriceHistory) 두
     * 계층에 나뉘어 있고, 앞쪽만 계약 위반을 알고 있다. 한쪽으로 합치는 게 더 깔끔하지만 엔티티 불변식은
     * 경매와 무관하게 지켜야 하는 것이라 그대로 두는 편이 맞고, 그 판단에 시간을 더 쓰기 어려워
     * **지금은 경계에서 번역만 한다.** 검증 위치를 정리할 때 이 메서드는 사라질 수 있다.
     * <p>
     * 호출 한 줄만 감싸는 이유: 범위를 넓히면 이 경로의 무관한 IllegalArgumentException까지
     * 계약 위반으로 오분류된다.
     */
    private PriceHistory toPriceHistory(ClosedAuction auction, LocalDateTime confirmedAt) {
        try {
            return PriceHistory.of(auction, confirmedAt);
        } catch (IllegalArgumentException e) {
            throw new AuctionContractViolationException(
                    "경매 응답 값으로 시세를 만들 수 없습니다 — auctionId: " + auction.auctionId()
                            + ", " + e.getMessage());
        }
    }
}
