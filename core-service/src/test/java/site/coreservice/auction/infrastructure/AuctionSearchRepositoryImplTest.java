package site.coreservice.auction.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import site.coreservice.auction.application.dto.AuctionListQuery;
import site.coreservice.auction.application.port.AuctionSearchViewRepository;
import site.coreservice.auction.application.port.dto.AuctionListSummary;
import site.coreservice.auction.application.port.dto.ProductSnapshot;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionSchedule;
import site.coreservice.auction.domain.AuctionStatus;
import site.coreservice.auction.domain.ItemCondition;
import site.coreservice.auction.domain.ItemInfo;
import site.coreservice.auction.domain.Money;
import site.coreservice.auction.domain.Period;
import site.coreservice.auction.domain.Pricing;
import site.coreservice.auction.exception.AuctionErrorCode;
import site.coreservice.auction.exception.AuctionException;
import site.coreservice.support.RepositoryTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@RepositoryTest
@Import(AuctionSearchRepositoryImpl.class)
class AuctionSearchRepositoryImplTest {

    @Autowired
    private AuctionSearchViewRepository auctionSearchViewRepository;

    @Autowired
    private AuctionSearchViewJpaRepository searchViewJpaRepository;

    private static final LocalDateTime FUTURE_START = LocalDateTime.of(2999, 1, 1, 0, 0);
    private static final LocalDateTime PAST_END = LocalDateTime.of(2000, 1, 1, 0, 0);

    private void saveSearchView(Long auctionId, String genre, String pressType,
        long price, int bidCount, LocalDateTime startAt, LocalDateTime endAt) {
        ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, "충분히 긴 상품 설명입니다.", List.of("1.png"));
        Pricing pricing = Pricing.of(Money.of(price), Money.of(500L), Money.of(0L));
        AuctionSchedule schedule = AuctionSchedule.of(Period.of(startAt, endAt), false, null);
        Auction auction = Auction.register(1L, 100L, itemInfo, pricing, schedule,
            startAt.minusHours(1));
        ReflectionTestUtils.setField(auction, "id", auctionId);
        ProductSnapshot productSnapshot = new ProductSnapshot(100L, "Title" + auctionId,
            "Artist" + auctionId, 1969, genre, pressType, true);
        auctionSearchViewRepository.save(auction, productSnapshot, "seller" + auctionId);

        AuctionSearchView view = searchViewJpaRepository.findById(auctionId).orElseThrow();
        ReflectionTestUtils.setField(view, "bidCount", bidCount);
    }

    @Test
    @DisplayName("경매를 서치 뷰로 저장하면 상품·판매자 정보와 함께 조회된다")
    void testSave_persistsSearchView() {
        // given
        ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, "충분히 긴 상품 설명입니다.", List.of("1.png"));
        Pricing pricing = Pricing.of(Money.of(10_000L), Money.of(500L), Money.of(3_000L));
        AuctionSchedule schedule = AuctionSchedule.of(
            Period.of(LocalDateTime.of(2026, 7, 1, 0, 0), LocalDateTime.of(2026, 7, 2, 0, 0)),
            false, null
        );
        Auction auction = Auction.register(1L, 100L, itemInfo, pricing, schedule,
            schedule.getPeriod().getStartAt().minusHours(1));
        ReflectionTestUtils.setField(auction, "id", 1L);
        ProductSnapshot productSnapshot = new ProductSnapshot(100L, "Abbey Road", "The Beatles",
            1969, "Rock", "ORIGINAL", true);

        // when
        auctionSearchViewRepository.save(auction, productSnapshot, "vinyl_king");

        // then
        AuctionSearchView found = searchViewJpaRepository.findById(1L).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("Abbey Road");
        assertThat(found.getSellerNickname()).isEqualTo("vinyl_king");
        assertThat(found.getThumbnail()).isEqualTo("1.png");
    }

    @Test
    @DisplayName("status=RUNNING으로 필터링하면 startAt~endAt 사이인 경매만 반환한다")
    void testSearch_filtersByStatus_running() {
        // given
        saveSearchView(1L, "Rock", "ORIGINAL", 10_000L, 0,
            LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        saveSearchView(2L, "Rock", "ORIGINAL", 10_000L, 0,
            FUTURE_START, FUTURE_START.plusDays(1));

        // when
        Page<AuctionListSummary> result = auctionSearchViewRepository.search(
            new AuctionListQuery(null, null, "RUNNING", null), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(AuctionListSummary::auctionId)
            .containsExactly(1L);
        assertThat(result.getContent()).extracting(AuctionListSummary::status)
            .containsExactly(AuctionStatus.RUNNING);
    }

    @Test
    @DisplayName("status=SCHEDULED로 필터링하면 startAt이 아직 안 지난 경매만 반환한다")
    void testSearch_filtersByStatus_scheduled() {
        // given
        saveSearchView(1L, "Rock", "ORIGINAL", 10_000L, 0,
            LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        saveSearchView(2L, "Rock", "ORIGINAL", 10_000L, 0,
            FUTURE_START, FUTURE_START.plusDays(1));

        // when
        Page<AuctionListSummary> result = auctionSearchViewRepository.search(
            new AuctionListQuery(null, null, "SCHEDULED", null), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(AuctionListSummary::auctionId)
            .containsExactly(2L);
        assertThat(result.getContent()).extracting(AuctionListSummary::status)
            .containsExactly(AuctionStatus.SCHEDULED);
    }

    @Test
    @DisplayName("status=ENDED_WON으로 필터링하면 endAt이 지났고 입찰이 있는 경매만 반환한다")
    void testSearch_filtersByStatus_endedWon() {
        // given
        saveSearchView(1L, "Rock", "ORIGINAL", 10_000L, 3,
            PAST_END.minusHours(2), PAST_END);
        saveSearchView(2L, "Rock", "ORIGINAL", 10_000L, 0,
            PAST_END.minusHours(2), PAST_END);

        // when
        Page<AuctionListSummary> result = auctionSearchViewRepository.search(
            new AuctionListQuery(null, null, "ENDED_WON", null), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(AuctionListSummary::auctionId)
            .containsExactly(1L);
        assertThat(result.getContent()).extracting(AuctionListSummary::status)
            .containsExactly(AuctionStatus.ENDED_WON);
    }

    @Test
    @DisplayName("status=ENDED_FAILED로 필터링하면 endAt이 지났고 입찰이 없는 경매만 반환한다")
    void testSearch_filtersByStatus_endedFailed() {
        // given
        saveSearchView(1L, "Rock", "ORIGINAL", 10_000L, 3,
            PAST_END.minusHours(2), PAST_END);
        saveSearchView(2L, "Rock", "ORIGINAL", 10_000L, 0,
            PAST_END.minusHours(2), PAST_END);

        // when
        Page<AuctionListSummary> result = auctionSearchViewRepository.search(
            new AuctionListQuery(null, null, "ENDED_FAILED", null), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(AuctionListSummary::auctionId)
            .containsExactly(2L);
        assertThat(result.getContent()).extracting(AuctionListSummary::status)
            .containsExactly(AuctionStatus.ENDED_FAILED);
    }

    @Test
    @DisplayName("status=CANCELED로 필터링하면 항상 빈 결과를 반환한다 (취소된 경매는 서치 뷰에서 삭제됨)")
    void testSearch_filtersByStatus_canceled_alwaysEmpty() {
        // given
        saveSearchView(1L, "Rock", "ORIGINAL", 10_000L, 0,
            LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));

        // when
        Page<AuctionListSummary> result = auctionSearchViewRepository.search(
            new AuctionListQuery(null, null, "CANCELED", null), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("유효하지 않은 status 값으로 조회하면 예외를 던진다")
    void testSearch_invalidStatus_throws() {
        assertThatThrownBy(() -> auctionSearchViewRepository.search(
            new AuctionListQuery(null, null, "NOT_A_STATUS", null), PageRequest.of(0, 20)))
            .isInstanceOf(AuctionException.class)
            .extracting(e -> ((AuctionException) e).getErrorCode())
            .isEqualTo(AuctionErrorCode.AUCTION_STATUS_INVALID);
    }

    @Test
    @DisplayName("필터 없이 조회하면 시간/입찰수 기준으로 계산한 status와 함께 전체 목록을 반환한다")
    void testSearch_noFilter_returnsAllWithComputedStatus() {
        // given
        saveSearchView(1L, "Rock", "ORIGINAL", 10_000L, 0,
            LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1)); // RUNNING
        saveSearchView(2L, "Jazz", "REISSUE", 20_000L, 0, FUTURE_START,
            FUTURE_START.plusDays(1)); // SCHEDULED
        saveSearchView(3L, "Rock", "ORIGINAL", 10_000L, 2, PAST_END.minusHours(2),
            PAST_END); // ENDED_WON
        saveSearchView(4L, "Rock", "ORIGINAL", 10_000L, 0, PAST_END.minusHours(2),
            PAST_END); // ENDED_FAILED

        // when
        Page<AuctionListSummary> result = auctionSearchViewRepository.search(
            new AuctionListQuery(null, null, null, null), PageRequest.of(0, 20));

        // then
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent())
            .extracting(AuctionListSummary::auctionId, AuctionListSummary::status)
            .containsExactlyInAnyOrder(
                tuple(1L, AuctionStatus.RUNNING),
                tuple(2L, AuctionStatus.SCHEDULED),
                tuple(3L, AuctionStatus.ENDED_WON),
                tuple(4L, AuctionStatus.ENDED_FAILED)
            );
    }

    @Test
    @DisplayName("조회 결과의 각 필드는 서치 뷰의 값과 동일하게 매핑된다")
    void testSearch_mapsAllFieldsFromSearchView() {
        // given
        saveSearchView(1L, "Rock", "ORIGINAL", 10_000L, 3, FUTURE_START, FUTURE_START.plusDays(1));
        AuctionSearchView view = searchViewJpaRepository.findById(1L).orElseThrow();

        // when
        Page<AuctionListSummary> result = auctionSearchViewRepository.search(
            new AuctionListQuery(null, null, null, null), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).containsExactly(new AuctionListSummary(
            view.getAuctionId(), view.getProductId(), view.getTitle(), view.getArtistName(),
            view.getReleaseYear(), view.getGenre(), view.getPressType(), view.getThumbnail(),
            view.getSellerId(), view.getSellerNickname(), AuctionStatus.SCHEDULED,
            view.getHighestBidAmount(), view.getBidCount(), view.getStartAt(), view.getEndAt()
        ));
    }

    @Test
    @DisplayName("genre로 필터링해 경매 목록을 조회한다")
    void testSearch_filtersByGenre() {
        // given
        saveSearchView(1L, "Rock", "ORIGINAL", 10_000L, 0, FUTURE_START, FUTURE_START.plusDays(1));
        saveSearchView(2L, "Jazz", "ORIGINAL", 10_000L, 0, FUTURE_START, FUTURE_START.plusDays(1));

        // when
        Page<AuctionListSummary> result = auctionSearchViewRepository.search(
            new AuctionListQuery("Rock", null, null, null), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(AuctionListSummary::auctionId)
            .containsExactly(1L);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("pressType으로 필터링해 경매 목록을 조회한다")
    void testSearch_filtersByPressType() {
        // given
        saveSearchView(1L, "Rock", "ORIGINAL", 10_000L, 0, FUTURE_START, FUTURE_START.plusDays(1));
        saveSearchView(2L, "Rock", "REISSUE", 10_000L, 0, FUTURE_START, FUTURE_START.plusDays(1));

        // when
        Page<AuctionListSummary> result = auctionSearchViewRepository.search(
            new AuctionListQuery(null, "REISSUE", null, null), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(AuctionListSummary::auctionId)
            .containsExactly(2L);
    }

    @Test
    @DisplayName("price_asc 정렬 시 가격이 낮은 순으로 반환한다")
    void testSearch_sortsByPriceAscending() {
        // given
        saveSearchView(1L, "Rock", "ORIGINAL", 30_000L, 0, FUTURE_START, FUTURE_START.plusDays(1));
        saveSearchView(2L, "Rock", "ORIGINAL", 10_000L, 0, FUTURE_START, FUTURE_START.plusDays(1));

        // when
        Page<AuctionListSummary> result = auctionSearchViewRepository.search(
            new AuctionListQuery(null, null, null, "price_asc"), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(AuctionListSummary::auctionId)
            .containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("most_bids 정렬 시 입찰 수가 많은 순으로 반환한다")
    void testSearch_sortsByMostBids() {
        // given
        saveSearchView(1L, "Rock", "ORIGINAL", 10_000L, 1, FUTURE_START, FUTURE_START.plusDays(1));
        saveSearchView(2L, "Rock", "ORIGINAL", 10_000L, 5, FUTURE_START, FUTURE_START.plusDays(1));

        // when
        Page<AuctionListSummary> result = auctionSearchViewRepository.search(
            new AuctionListQuery(null, null, null, "most_bids"), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(AuctionListSummary::auctionId)
            .containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("유효하지 않은 sort 값으로 조회하면 예외를 던진다")
    void testSearch_invalidSort_throws() {
        assertThatThrownBy(() -> auctionSearchViewRepository.search(
            new AuctionListQuery(null, null, null, "not_a_sort"), PageRequest.of(0, 20)))
            .isInstanceOf(AuctionException.class)
            .extracting(e -> ((AuctionException) e).getErrorCode())
            .isEqualTo(AuctionErrorCode.AUCTION_SORT_INVALID);
    }

    @Test
    @DisplayName("정렬 조건이 없으면 마감 임박순(ending_soon)으로 반환한다")
    void testSearch_defaultSort_endingSoon() {
        // given
        saveSearchView(1L, "Rock", "ORIGINAL", 10_000L, 0, FUTURE_START, FUTURE_START.plusDays(10));
        saveSearchView(2L, "Rock", "ORIGINAL", 10_000L, 0, FUTURE_START, FUTURE_START.plusDays(1));

        // when
        Page<AuctionListSummary> result = auctionSearchViewRepository.search(
            new AuctionListQuery(null, null, null, null), PageRequest.of(0, 20));

        // then
        assertThat(result.getContent()).extracting(AuctionListSummary::auctionId)
            .containsExactly(2L, 1L);
    }
}
