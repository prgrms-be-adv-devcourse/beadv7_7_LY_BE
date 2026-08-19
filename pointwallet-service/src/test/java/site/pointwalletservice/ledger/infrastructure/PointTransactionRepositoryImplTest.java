package site.pointwalletservice.ledger.infrastructure;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import site.pointwalletservice.ledger.domain.PointTransaction;
import site.pointwalletservice.ledger.domain.PointTransactionRepository;
import site.pointwalletservice.ledger.domain.PointTransactionSearchPage;
import site.pointwalletservice.ledger.domain.PointTransactionType;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.support.RepositoryTest;


@RepositoryTest
@Import(PointTransactionRepositoryImpl.class)
class PointTransactionRepositoryImplTest {

    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    @Autowired
    private PointTransactionJpaRepository pointTransactionJpaRepository;

    private static final Long WALLET_ID = 100L;

    // occurredAt은 updatable=false라 저장 후엔 못 바꾸지만, 최초 save(=insert) 전에 필드를 세팅해두면
    // 그대로 반영된다 — 시간 관련 테스트를 결정론적으로 만들기 위한 용도.
    //
    // relatedId는 이 테스트들의 검증 대상(walletId/type/기간/페이징 필터링)과 무관하지만,
    // point_transaction(related_id, type) 유니크 제약 때문에 같은 type으로 여러 건 저장할 때는
    // 서로 다른 값을 넘겨야 한다 — 실제 운영에서도 같은 relatedId+type 조합은 한 번만 존재하므로,
    // 호출부가 매번 구분되는 값을 넘기는 게 더 현실적인 픽스처이기도 하다.
    private PointTransaction saveTransaction(Long walletId, PointTransactionType type, long amount,
                                             LocalDateTime occurredAt, Long relatedId) {
        PointTransaction transaction = PointTransaction.record(walletId, type, Money.of(amount), Money.of(amount), relatedId);
        ReflectionTestUtils.setField(transaction, "occurredAt", occurredAt);
        return pointTransactionJpaRepository.save(transaction);
    }

    @Test
    @DisplayName("walletId로 조회하면 다른 지갑의 거래는 제외하고, 최신순(occurredAt desc)으로 반환한다")
    void search_walletId만() {
        // given
        LocalDateTime now = LocalDateTime.now();
        saveTransaction(WALLET_ID, PointTransactionType.DEPOSIT, 10_000, now.minusMinutes(1), 1L);
        saveTransaction(WALLET_ID, PointTransactionType.HOLD, 3_000, now, 2L);
        saveTransaction(999L, PointTransactionType.DEPOSIT, 5_000, now, 3L); // 다른 지갑 - 제외돼야 함

        // when
        PointTransactionSearchPage result = pointTransactionRepository.search(WALLET_ID, null, null, null, 0, 20);

        // then
        assertThat(result.totalElements()).isEqualTo(2L);
        assertThat(result.content()).extracting(PointTransaction::getType)
                .containsExactly(PointTransactionType.HOLD, PointTransactionType.DEPOSIT);
    }

    @Test
    @DisplayName("type으로 필터링한다")
    void search_type_필터() {
        // given
        LocalDateTime now = LocalDateTime.now();
        saveTransaction(WALLET_ID, PointTransactionType.DEPOSIT, 10_000, now, 1L);
        saveTransaction(WALLET_ID, PointTransactionType.HOLD, 3_000, now, 2L);

        // when
        PointTransactionSearchPage result =
                pointTransactionRepository.search(WALLET_ID, PointTransactionType.HOLD, null, null, 0, 20);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).getType()).isEqualTo(PointTransactionType.HOLD);
    }

    @Test
    @DisplayName("from~to 범위를 벗어난 거래는 결과에서 제외한다")
    void search_기간_필터() {
        // given
        LocalDateTime now = LocalDateTime.now();
        saveTransaction(WALLET_ID, PointTransactionType.DEPOSIT, 10_000, now, 1L);
        LocalDateTime past = now.minusDays(1);
        LocalDateTime future = now.plusDays(1);

        // when
        PointTransactionSearchPage inRange = pointTransactionRepository.search(WALLET_ID, null, past, future, 0, 20);
        PointTransactionSearchPage tooLate = pointTransactionRepository.search(WALLET_ID, null, future, null, 0, 20);

        // then
        assertThat(inRange.content()).hasSize(1);
        assertThat(tooLate.content()).isEmpty();
    }

    @Test
    @DisplayName("페이징이 되고 totalElements는 조건에 맞는 전체 개수와 일치한다")
    void search_페이징() {
        // given
        LocalDateTime now = LocalDateTime.now();
        saveTransaction(WALLET_ID, PointTransactionType.DEPOSIT, 1_000, now.minusMinutes(2), 1L);
        saveTransaction(WALLET_ID, PointTransactionType.DEPOSIT, 2_000, now.minusMinutes(1), 2L);
        saveTransaction(WALLET_ID, PointTransactionType.DEPOSIT, 3_000, now, 3L);

        // when
        PointTransactionSearchPage firstPage = pointTransactionRepository.search(WALLET_ID, null, null, null, 0, 2);
        PointTransactionSearchPage secondPage = pointTransactionRepository.search(WALLET_ID, null, null, null, 1, 2);

        // then
        assertThat(firstPage.content()).hasSize(2);
        assertThat(secondPage.content()).hasSize(1);
        assertThat(firstPage.totalElements()).isEqualTo(3L);
    }

    @Test
    @DisplayName("existsByRelatedIdAndType는 related_id+type 조합이 이미 있으면 true, 없으면 false를 반환한다")
    void existsByRelatedIdAndType_존재여부() {
        // given
        saveTransaction(WALLET_ID, PointTransactionType.FEE_INCOME, 2_000, LocalDateTime.now(), 777L);

        // when & then
        assertThat(pointTransactionRepository.existsByRelatedIdAndType(777L, PointTransactionType.FEE_INCOME)).isTrue();
        // 같은 relatedId라도 type이 다르면 별개 취급
        assertThat(pointTransactionRepository.existsByRelatedIdAndType(777L, PointTransactionType.WITHDRAW)).isFalse();
        // 존재하지 않는 relatedId
        assertThat(pointTransactionRepository.existsByRelatedIdAndType(999_999L, PointTransactionType.FEE_INCOME)).isFalse();
    }

    @Test
    @DisplayName("findLatestByAuctionIdAndType는 같은 경매에 재입찰로 HOLD 원장이 여러 건이어도 가장 최근 것 하나만 반환한다")
    void findLatestByAuctionIdAndType_재입찰시_최신건만_반환() {
        // given: 같은 auctionId에 재입찰로 HOLD가 두 번 쌓인 상황 (관행상 이전 입찰은 RELEASE 처리되고
        // 다음 HOLD가 새로 생김 - 여기선 조회 대상인 auctionId+HOLD 필터링/정렬만 검증)
        Long auctionId = 5001L;
        LocalDateTime now = LocalDateTime.now();
        PointTransaction firstBid = PointTransaction.recordForAuction(
                WALLET_ID, PointTransactionType.HOLD, Money.of(10_000), Money.of(90_000), 1L, auctionId);
        ReflectionTestUtils.setField(firstBid, "occurredAt", now.minusMinutes(5));
        pointTransactionJpaRepository.save(firstBid);

        PointTransaction winningBid = PointTransaction.recordForAuction(
                WALLET_ID, PointTransactionType.HOLD, Money.of(15_000), Money.of(85_000), 2L, auctionId);
        ReflectionTestUtils.setField(winningBid, "occurredAt", now);
        pointTransactionJpaRepository.save(winningBid);

        // when
        var result = pointTransactionRepository.findLatestByAuctionIdAndType(auctionId, PointTransactionType.HOLD);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAmount()).isEqualTo(Money.of(15_000));
        assertThat(result.get().getRelatedId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("findLatestByAuctionIdAndType는 다른 경매나 다른 타입의 원장은 무시한다")
    void findLatestByAuctionIdAndType_다른경매나_다른타입은_제외() {
        // given
        Long auctionId = 5001L;
        LocalDateTime now = LocalDateTime.now();
        PointTransaction otherAuction = PointTransaction.recordForAuction(
                WALLET_ID, PointTransactionType.HOLD, Money.of(20_000), Money.of(80_000), 3L, 9999L);
        pointTransactionJpaRepository.save(otherAuction);
        PointTransaction releaseOfSameAuction = PointTransaction.recordForAuction(
                WALLET_ID, PointTransactionType.RELEASE, Money.of(10_000), Money.of(90_000), 4L, auctionId);
        pointTransactionJpaRepository.save(releaseOfSameAuction);

        // when
        var result = pointTransactionRepository.findLatestByAuctionIdAndType(auctionId, PointTransactionType.HOLD);

        // then
        assertThat(result).isEmpty();
    }
}