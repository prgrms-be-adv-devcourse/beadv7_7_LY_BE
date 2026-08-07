package site.auctionservice.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import site.auctionservice.application.dto.InternalAuctionCountResult;
import site.auctionservice.application.dto.InternalAuctionSnapshotResult;
import site.auctionservice.application.dto.InternalAuctionSummaryResult;
import site.auctionservice.domain.*;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InternalAuctionServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BidRepository bidRepository;

    @InjectMocks
    private InternalAuctionService internalAuctionService;

    private static final String DESCRIPTION = "충분히 긴 상품 설명입니다.";
    private static final LocalDateTime PAST_START = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime PAST_END = PAST_START.plusHours(2);
    private static final LocalDateTime FUTURE_START = LocalDateTime.of(2999, 1, 1, 0, 0);
    private static final LocalDateTime FUTURE_END = FUTURE_START.plusHours(2);

    private final ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, DESCRIPTION, null);
    private final Pricing pricing = Pricing.of(Money.of(10_000L), Money.of(500L), Money.of(3_000L));

    private Auction auctionWith(AuctionStatus status, LocalDateTime startAt, LocalDateTime endAt) {
        return auctionWith(status, startAt, endAt, null);
    }

    private Auction auctionWith(AuctionStatus status, LocalDateTime startAt, LocalDateTime endAt,
        HighestBid highestBid) {
        AuctionSchedule schedule = AuctionSchedule.of(Period.of(startAt, endAt), false, null);
        return Auction.of(1L, 100L, itemInfo, pricing, schedule, status, highestBid);
    }

    @Test
    @DisplayName("입찰이 있는 경매의 내부 요약 조회 시 낙찰가를 포함한다")
    void testGetInternalSummary_withBid_includesFinalPrice() {
        // given
        HighestBid highestBid = HighestBid.of(Money.of(12_000L), 2L, 10L);
        Auction auction = auctionWith(AuctionStatus.ENDED_WON, PAST_START, PAST_END, highestBid);
        ReflectionTestUtils.setField(auction, "id", 1L);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(bidRepository.countByAuctionId(1L)).willReturn(2L);

        // when
        InternalAuctionSummaryResult result = internalAuctionService.getInternalSummary(1L);

        // then
        assertThat(result.auctionId()).isEqualTo(1L);
        assertThat(result.productId()).isEqualTo(100L);
        assertThat(result.itemCondition()).isEqualTo(ItemCondition.MINT.name());
        assertThat(result.bidCount()).isEqualTo(2L);
        assertThat(result.finalPrice()).isEqualByComparingTo(BigDecimal.valueOf(12_000));
        assertThat(result.endAt()).isEqualTo(PAST_END);
        assertThat(result.status()).isEqualTo("ENDED_WON");
    }

    @Test
    @DisplayName("입찰이 없는 경매의 내부 요약 조회 시 낙찰가는 null이다")
    void testGetInternalSummary_noBid_finalPriceIsNull() {
        // given
        Auction auction = auctionWith(AuctionStatus.SCHEDULED, FUTURE_START, FUTURE_END);
        ReflectionTestUtils.setField(auction, "id", 1L);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(bidRepository.countByAuctionId(1L)).willReturn(0L);

        // when
        InternalAuctionSummaryResult result = internalAuctionService.getInternalSummary(1L);

        // then
        assertThat(result.finalPrice()).isNull();
        assertThat(result.bidCount()).isZero();
        assertThat(result.status()).isEqualTo("SCHEDULED");
    }

    @Test
    @DisplayName("존재하지 않는 경매의 내부 요약을 조회하면 예외를 던진다")
    void testGetInternalSummary_auctionNotFound_throws() {
        // given
        given(auctionRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> internalAuctionService.getInternalSummary(1L))
            .isInstanceOf(AuctionException.class)
            .extracting(e -> ((AuctionException) e).getErrorCode())
            .isEqualTo(AuctionErrorCode.AUCTION_NOT_FOUND);
    }

    @Test
    @DisplayName("CANCELED 상태의 경매는 내부 요약 조회 시 조회할 수 없다")
    void testGetInternalSummary_canceledStatus_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.CANCELED, FUTURE_START, FUTURE_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        // when & then
        assertThatThrownBy(() -> internalAuctionService.getInternalSummary(1L))
            .isInstanceOf(AuctionException.class)
            .extracting(e -> ((AuctionException) e).getErrorCode())
            .isEqualTo(AuctionErrorCode.AUCTION_NOT_FOUND);
        verify(bidRepository, never()).countByAuctionId(any());
    }

    @Test
    @DisplayName("getSnapshots()는 취소된 경매도 그대로 포함한다")
    void testGetSnapshots_includesCanceledAuctions() {
        // given
        Auction running = auctionWith(AuctionStatus.RUNNING, PAST_START, FUTURE_END);
        ReflectionTestUtils.setField(running, "id", 1L);
        Auction canceled = auctionWith(AuctionStatus.CANCELED, PAST_START, FUTURE_END);
        ReflectionTestUtils.setField(canceled, "id", 2L);
        given(auctionRepository.findAllByIds(List.of(1L, 2L))).willReturn(
            List.of(running, canceled));

        // when
        List<InternalAuctionSnapshotResult> result = internalAuctionService.getSnapshots(
            List.of(1L, 2L));

        // then
        assertThat(result).extracting(InternalAuctionSnapshotResult::id)
            .containsExactlyInAnyOrder(1L, 2L);
        assertThat(result).extracting(InternalAuctionSnapshotResult::status)
            .containsExactlyInAnyOrder(AuctionStatus.RUNNING, AuctionStatus.CANCELED);
    }

    @Test
    @DisplayName("경매가 존재하면 getSnapshot은 CANCELED여도 그대로 반환한다")
    void testGetSnapshot_auctionPresent_returnsSnapshotEvenIfCanceled() {
        // given
        Auction auction = auctionWith(AuctionStatus.CANCELED, PAST_START, FUTURE_END);
        ReflectionTestUtils.setField(auction, "id", 1L);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        // when
        InternalAuctionSnapshotResult result = internalAuctionService.getSnapshot(1L);

        // then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(AuctionStatus.CANCELED);
    }

    @Test
    @DisplayName("getSnapshot은 persisted status가 아니라 실시간 보정된 상태를 반환한다")
    void testGetSnapshot_returnsEffectiveStatus_notPersistedStatus() {
        // given: 시작 스케줄러가 조기에 RUNNING으로 바꿔놨지만 실제 시작 시각은 아직 안 지났다
        Auction auction = auctionWith(AuctionStatus.RUNNING, FUTURE_START, FUTURE_END);
        ReflectionTestUtils.setField(auction, "id", 1L);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        // when
        InternalAuctionSnapshotResult result = internalAuctionService.getSnapshot(1L);

        // then
        assertThat(result.status()).isEqualTo(AuctionStatus.SCHEDULED);
    }

    @Test
    @DisplayName("경매가 없으면 getSnapshot은 예외를 던진다")
    void testGetSnapshot_auctionAbsent_throws() {
        // given
        given(auctionRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> internalAuctionService.getSnapshot(1L))
            .isInstanceOf(AuctionException.class)
            .extracting(e -> ((AuctionException) e).getErrorCode())
            .isEqualTo(AuctionErrorCode.AUCTION_NOT_FOUND);
    }

    @Test
    @DisplayName("진행 중인 경매가 없는 productId는 0건으로 채워진다")
    void testGetOpenAuctionCounts_fillsZeroForProductsWithoutRunningAuction() {
        // given
        given(auctionRepository.countRunningByProductIds(List.of(55L, 56L))).willReturn(
            Map.of(55L, 3L));

        // when
        List<InternalAuctionCountResult> result = internalAuctionService.getOpenAuctionCounts(
            List.of(55L, 56L));

        // then
        assertThat(result).containsExactly(
            new InternalAuctionCountResult(55L, 3L),
            new InternalAuctionCountResult(56L, 0L)
        );
    }

    @Test
    @DisplayName("productIds에 중복이 있으면 한 번만 조회하고 결과에도 한 번만 담는다")
    void testGetOpenAuctionCounts_duplicateProductIds_deduplicated() {
        // given
        given(auctionRepository.countRunningByProductIds(List.of(55L))).willReturn(Map.of(55L, 2L));

        // when
        List<InternalAuctionCountResult> result = internalAuctionService.getOpenAuctionCounts(
            List.of(55L, 55L));

        // then
        assertThat(result).containsExactly(new InternalAuctionCountResult(55L, 2L));
        verify(auctionRepository).countRunningByProductIds(List.of(55L));
    }

    @Test
    @DisplayName("productIds가 비어있으면 저장소를 조회하지 않고 빈 결과를 반환한다")
    void testGetOpenAuctionCounts_emptyProductIds_returnsEmpty() {
        // given
        given(auctionRepository.countRunningByProductIds(List.of())).willReturn(Map.of());

        // when
        List<InternalAuctionCountResult> result = internalAuctionService.getOpenAuctionCounts(
            List.of());

        // then
        assertThat(result).isEmpty();
    }
}
