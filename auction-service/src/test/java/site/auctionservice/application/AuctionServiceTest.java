package site.auctionservice.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import site.auctionservice.application.dto.*;
import site.auctionservice.application.port.AuctionSearchViewRepository;
import site.auctionservice.application.port.LockPort;
import site.auctionservice.application.port.MemberPort;
import site.auctionservice.application.port.ProductPort;
import site.auctionservice.application.port.WalletPort;
import site.auctionservice.application.port.dto.*;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private MemberPort memberPort;

    @Mock
    private ProductPort productPort;

    @Mock
    private AuctionSearchViewRepository searchViewRepository;

    @Mock
    private WalletPort walletPort;

    @Mock
    private AuctionEventPublisher auctionEventPublisher;

    @Mock
    private ImageUrlValidator imageUrlValidator;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private LockPort lockPort;

    @InjectMocks
    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        // TransactionTemplate.execute()가 실제 트랜잭션 없이 콜백을 즉시 실행하도록 스텁 —
        // pointwallet-service의 WithdrawApplicationServiceTest 등과 동일한 패턴.
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction((TransactionStatus) null);
        }).when(transactionTemplate).execute(any());

        // LockPort.executeWithLockOnAuction()도 실제 락 없이 액션을 바로 실행하도록 스텁
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            java.util.function.Supplier<?> action = invocation.getArgument(4);
            return action.get();
        }).when(lockPort).executeWithLockOnAuction(any(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(), any(), any());
    }

    private static final String DESCRIPTION = "충분히 긴 상품 설명입니다.";
    private static final LocalDateTime PAST_START = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime PAST_END = PAST_START.plusHours(2);
    private static final LocalDateTime FUTURE_START = LocalDateTime.of(2999, 1, 1, 0, 0);
    private static final LocalDateTime FUTURE_END = FUTURE_START.plusHours(2);

    private final ProductSnapshot productSnapshot =
            new ProductSnapshot(100L, "Abbey Road", "The Beatles", 1969, "Rock", "ORIGINAL", true);
    private final ProductDetail productDetail =
            new ProductDetail(100L, "Abbey Road", "The Beatles", "https://cdn.example.com/cover.jpg", 1969, "Rock", "ORIGINAL", true);
    private final ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, DESCRIPTION, null);
    private final Pricing pricing = Pricing.of(Money.of(10_000L), Money.of(500L), Money.of(3_000L));

    private Auction auctionWith(AuctionStatus status, LocalDateTime startAt, LocalDateTime endAt) {
        return auctionWith(status, startAt, endAt, null);
    }

    private Auction auctionWith(AuctionStatus status, LocalDateTime startAt, LocalDateTime endAt, HighestBid highestBid) {
        AuctionSchedule schedule = AuctionSchedule.of(Period.of(startAt, endAt), false, null);
        return Auction.of(1L, 100L, itemInfo, pricing, schedule, status, highestBid);
    }

    private CreateAuctionCommand validCommand(String itemCondition) {
        return new CreateAuctionCommand(
                100L,
                itemCondition,
                DESCRIPTION,
                List.of("1.png"),
                BigDecimal.valueOf(10_000),
                BigDecimal.valueOf(3_000),
                BigDecimal.valueOf(500),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                false,
                null
        );
    }

    private ModifyAuctionCommand modifyCommand(Long auctionId, Long productId, LocalDateTime startAt, LocalDateTime endAt) {
        return new ModifyAuctionCommand(
                auctionId,
                productId,
                "MINT",
                "수정된 상품 설명입니다.",
                List.of("2.png"),
                BigDecimal.valueOf(20_000),
                BigDecimal.valueOf(3_500),
                BigDecimal.valueOf(1_000),
                startAt,
                endAt,
                false,
                null
        );
    }

    @Test
    @DisplayName("경매를 생성하면 저장하고 서치 뷰에도 반영한다")
    void testCreateAuction_savesAuctionAndIndexesSearchView() {
        // given
        given(memberPort.getNickname(1L)).willReturn("vinyl_king");
        given(productPort.getProduct(100L)).willReturn(productSnapshot);
        given(auctionRepository.save(any(Auction.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        AuctionResult result = auctionService.createAuction(validCommand("MINT"), 1L);

        // then
        assertThat(result.status()).isEqualTo(AuctionStatus.SCHEDULED.name());

        ArgumentCaptor<Auction> auctionCaptor = ArgumentCaptor.forClass(Auction.class);
        verify(auctionRepository).save(auctionCaptor.capture());
        assertThat(auctionCaptor.getValue().getSellerId()).isEqualTo(1L);
        assertThat(auctionCaptor.getValue().getProductId()).isEqualTo(100L);

        verify(searchViewRepository).save(auctionCaptor.getValue(), productSnapshot, "vinyl_king");
    }

    @Test
    @DisplayName("유효하지 않은 상품 상태면 예외를 던지고 경매를 저장하지 않는다")
    void testCreateAuction_invalidItemCondition_throws() {
        // given
        given(memberPort.getNickname(1L)).willReturn("vinyl_king");
        given(productPort.getProduct(100L)).willReturn(productSnapshot);

        // when & then
        assertThatThrownBy(() -> auctionService.createAuction(validCommand("NOT_A_CONDITION"), 1L))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.ITEM_CONDITION_INVALID);
        verify(auctionRepository, never()).save(any());
        verify(searchViewRepository, never()).save(any(), any(), any());
    }

    @Test
    @DisplayName("판매 중단된 상품이면 예외를 던지고 경매를 저장하지 않는다")
    void testCreateAuction_productNotActive_throws() {
        // given
        ProductSnapshot inactiveProduct =
                new ProductSnapshot(100L, "Abbey Road", "The Beatles", 1969, "Rock", "ORIGINAL", false);
        given(memberPort.getNickname(1L)).willReturn("vinyl_king");
        given(productPort.getProduct(100L)).willReturn(inactiveProduct);

        // when & then
        assertThatThrownBy(() -> auctionService.createAuction(validCommand("MINT"), 1L))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.PRODUCT_NOT_ACTIVE);
        verify(auctionRepository, never()).save(any());
        verify(searchViewRepository, never()).save(any(), any(), any());
    }

    @Test
    @DisplayName("SCHEDULED 상태여도 시작 시각이 지났으면 실제로는 RUNNING이라 경매를 수정할 수 없다")
    void testModifyAuction_scheduledStatus_afterStartTime_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.SCHEDULED, PAST_START, PAST_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        // when & then
        assertThatThrownBy(() -> auctionService.modifyAuction(modifyCommand(1L, 100L, PAST_START, PAST_END), 1L))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_EDITABLE);
        verify(searchViewRepository, never()).updateFromAuction(any(), any());
    }

    @Test
    @DisplayName("RUNNING 상태이고 시작 시각 이전이면 경매를 수정한다")
    void testModifyAuction_runningStatus_beforeStartTime_succeeds() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING, FUTURE_START, FUTURE_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        // when
        AuctionResult result = auctionService.modifyAuction(modifyCommand(1L, 100L, FUTURE_START, FUTURE_END), 1L);

        // then
        assertThat(result.status()).isEqualTo(AuctionStatus.RUNNING.name());
    }

    @Test
    @DisplayName("RUNNING 상태이고 시작 시각이 지났으면 수정할 수 없다")
    void testModifyAuction_runningStatus_afterStartTime_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING, PAST_START, PAST_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        // when & then
        assertThatThrownBy(() -> auctionService.modifyAuction(modifyCommand(1L, 100L, PAST_START, PAST_END), 1L))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_EDITABLE);
        verify(searchViewRepository, never()).updateFromAuction(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 경매를 수정하려 하면 예외를 던진다")
    void testModifyAuction_auctionNotFound_throws() {
        // given
        given(auctionRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> auctionService.modifyAuction(modifyCommand(1L, 100L, FUTURE_START, FUTURE_END), 1L))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_FOUND);
    }

    @Test
    @DisplayName("판매자 본인이 아니면 경매를 수정할 수 없다")
    void testModifyAuction_notOwner_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.SCHEDULED, FUTURE_START, FUTURE_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        // when & then
        assertThatThrownBy(() -> auctionService.modifyAuction(modifyCommand(1L, 100L, FUTURE_START, FUTURE_END), 2L))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_ACCESS_DENIED);
        verify(searchViewRepository, never()).updateFromAuction(any(), any());
    }

    @Test
    @DisplayName("상품 ID가 변경되면 상품 정보를 다시 조회해 서치 뷰를 갱신한다")
    void testModifyAuction_productIdChanged_refetchesProductAndUpdatesSearchView() {
        // given
        Auction auction = auctionWith(AuctionStatus.SCHEDULED, FUTURE_START, FUTURE_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(productPort.getProduct(200L)).willReturn(productSnapshot);

        // when
        auctionService.modifyAuction(modifyCommand(1L, 200L, FUTURE_START, FUTURE_END), 1L);

        // then
        verify(productPort).getProduct(200L);
        verify(searchViewRepository).updateFromAuction(auction, productSnapshot);
    }

    @Test
    @DisplayName("변경된 상품이 판매 중단 상태면 예외를 던지고 서치 뷰를 갱신하지 않는다")
    void testModifyAuction_productIdChanged_productNotActive_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.SCHEDULED, FUTURE_START, FUTURE_END);
        ProductSnapshot inactiveProduct =
                new ProductSnapshot(200L, "Abbey Road", "The Beatles", 1969, "Rock", "ORIGINAL", false);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(productPort.getProduct(200L)).willReturn(inactiveProduct);

        // when & then
        assertThatThrownBy(() -> auctionService.modifyAuction(modifyCommand(1L, 200L, FUTURE_START, FUTURE_END), 1L))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.PRODUCT_NOT_ACTIVE);
        verify(searchViewRepository, never()).updateFromAuction(any(), any());
    }

    @Test
    @DisplayName("경매를 취소하면 상태를 변경하고 서치 뷰를 삭제한다")
    void testDeleteAuction_cancelsAuctionAndDeletesSearchView() {
        // given
        Auction auction = auctionWith(AuctionStatus.SCHEDULED, FUTURE_START, FUTURE_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        // when
        auctionService.deleteAuction(1L, 1L);

        // then
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CANCELED);
        verify(searchViewRepository).deleteById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 경매를 취소하려 하면 예외를 던진다")
    void testDeleteAuction_auctionNotFound_throws() {
        // given
        given(auctionRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> auctionService.deleteAuction(1L, 1L))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_FOUND);
        verify(searchViewRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("판매자 본인이 아니면 경매를 취소할 수 없다")
    void testDeleteAuction_notOwner_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.SCHEDULED, FUTURE_START, FUTURE_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        // when & then
        assertThatThrownBy(() -> auctionService.deleteAuction(1L, 2L))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_ACCESS_DENIED);
        verify(searchViewRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("RUNNING 상태이고 시작 시각이 지났으면 취소할 수 없다")
    void testDeleteAuction_notEditable_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING, PAST_START, PAST_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        // when & then
        assertThatThrownBy(() -> auctionService.deleteAuction(1L, 1L))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_EDITABLE);
        verify(searchViewRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("관리자가 SCHEDULED 상태의 경매를 강제 취소하면 상태를 변경하고 서치 뷰를 삭제한다")
    void testForceCancelAuction_scheduledStatus_forceCancelsAndDeletesSearchView() {
        // given
        Auction auction = auctionWith(AuctionStatus.SCHEDULED, FUTURE_START, FUTURE_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        // when
        auctionService.forceCancelAuction(1L);

        // then
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.FORCE_CANCELED);
        verify(searchViewRepository).deleteById(1L);
        verify(auctionEventPublisher, never()).publishForceCanceled(any(), any());
    }

    @Test
    @DisplayName("관리자는 RUNNING 상태이고 시작 시각이 지난 경매도 강제 취소할 수 있다")
    void testForceCancelAuction_runningStatus_afterStartTime_succeeds() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING, PAST_START, FUTURE_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        // when
        auctionService.forceCancelAuction(1L);

        // then
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.FORCE_CANCELED);
        verify(searchViewRepository).deleteById(1L);
        verify(auctionEventPublisher, never()).publishForceCanceled(any(), any());
    }

    @Test
    @DisplayName("활성 입찰이 있는 경매를 강제 취소하면 해당 입찰도 CANCELED로 전이하고 예치금 홀드 해제 이벤트를 발행한다")
    void testForceCancelAuction_withActiveBid_cancelsActiveBid() {
        // given
        HighestBid highestBid = HighestBid.of(Money.of(15_000L), 5L, 10L);
        Auction auction = auctionWith(AuctionStatus.RUNNING, PAST_START, FUTURE_END, highestBid);
        Bid activeBid = Bid.place(1L, 5L, Money.of(15_000L), PAST_START.plusMinutes(1));
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(bidRepository.findActiveBid(1L)).willReturn(Optional.of(activeBid));

        // when
        auctionService.forceCancelAuction(1L);

        // then
        assertThat(activeBid.getOutcome()).isEqualTo(BidOutcome.CANCELED);
        verify(auctionEventPublisher).publishForceCanceled(1L, 5L);
    }

    @Test
    @DisplayName("이벤트 발행이 실패해도 강제 취소 자체는 예외 없이 커밋된다")
    void testForceCancelAuction_eventPublishFails_stillCommits() {
        // given
        HighestBid highestBid = HighestBid.of(Money.of(15_000L), 5L, 10L);
        Auction auction = auctionWith(AuctionStatus.RUNNING, PAST_START, FUTURE_END, highestBid);
        Bid activeBid = Bid.place(1L, 5L, Money.of(15_000L), PAST_START.plusMinutes(1));
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(bidRepository.findActiveBid(1L)).willReturn(Optional.of(activeBid));
        willThrow(new RuntimeException("kafka down"))
                .given(auctionEventPublisher).publishForceCanceled(1L, 5L);

        // when & then
        auctionService.forceCancelAuction(1L);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.FORCE_CANCELED);
        assertThat(activeBid.getOutcome()).isEqualTo(BidOutcome.CANCELED);
        verify(searchViewRepository).deleteById(1L);
    }

    @Test
    @DisplayName("활성 입찰이 없는 경매를 강제 취소해도 예외 없이 처리된다")
    void testForceCancelAuction_withoutActiveBid_succeeds() {
        // given
        Auction auction = auctionWith(AuctionStatus.SCHEDULED, FUTURE_START, FUTURE_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        // when & then
        auctionService.forceCancelAuction(1L);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.FORCE_CANCELED);
        verify(bidRepository, never()).findActiveBid(any());
        verify(auctionEventPublisher, never()).publishForceCanceled(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 경매를 강제 취소하려 하면 예외를 던진다")
    void testForceCancelAuction_auctionNotFound_throws() {
        // given
        given(auctionRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> auctionService.forceCancelAuction(1L))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_FOUND);
        verify(searchViewRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("이미 종료된 경매는 강제 취소할 수 없다")
    void testForceCancelAuction_alreadyEnded_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.ENDED_WON, PAST_START, PAST_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        // when & then
        assertThatThrownBy(() -> auctionService.forceCancelAuction(1L))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_FORCE_CANCELABLE);
        verify(searchViewRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("강제 취소된 경매는 상세 조회 시 찾을 수 없다는 예외를 던진다")
    void testGetAuctionDetail_forceCanceledStatus_throwsNotFound() {
        // given: status는 FORCE_CANCELED이지만 시작~종료 시각 사이라, 실효 상태 계산에 버그가 있으면 RUNNING으로 잘못 조회된다
        Auction auction = auctionWith(AuctionStatus.FORCE_CANCELED, PAST_START, FUTURE_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        // when & then
        assertThatThrownBy(() -> auctionService.getAuctionDetail(1L, 1L))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_FOUND);
    }

    @Test
    @DisplayName("SCHEDULED 상태의 경매 상세를 조회하면 ScheduledDetail을 반환한다")
    void testGetAuctionDetail_scheduledStatus_returnsScheduledDetail() {
        // given
        Auction auction = auctionWith(AuctionStatus.SCHEDULED, FUTURE_START, FUTURE_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(productPort.getProductDetail(100L)).willReturn(productDetail);
        given(memberPort.getNickname(1L)).willReturn("vinyl_king");

        // when
        AuctionDetailResult result = auctionService.getAuctionDetail(1L, null);

        // then
        assertThat(result.sellerNickname()).isEqualTo("vinyl_king");
        assertThat(result.itemCondition()).isEqualTo(ItemCondition.MINT.name());
        assertThat(result.startBidAmount()).isEqualByComparingTo(BigDecimal.valueOf(13_000));
        assertThat(result.detail()).isInstanceOf(AuctionStatusDetail.ScheduledDetail.class);
    }

    @Test
    @DisplayName("판매자 닉네임 조회에 실패해도 fallback 닉네임으로 대체하고 경매 상세 조회는 계속된다")
    void testGetAuctionDetail_sellerNicknameLookupFails_fallsBackToUnknown() {
        // given
        Auction auction = auctionWith(AuctionStatus.SCHEDULED, FUTURE_START, FUTURE_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(productPort.getProductDetail(100L)).willReturn(productDetail);
        given(memberPort.getNickname(1L)).willThrow(new RuntimeException("member-service down"));

        // when
        AuctionDetailResult result = auctionService.getAuctionDetail(1L, null);

        // then
        assertThat(result.sellerNickname()).isEqualTo("알 수 없음");
    }

    @Test
    @DisplayName("RUNNING 상태여도 시작 시각 전이면 ScheduledDetail로 노출된다 (시작 스케줄러가 미리 상태를 바꿔둔 구간)")
    void testGetAuctionDetail_runningStatus_beforeStartTime_returnsScheduledDetail() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING, FUTURE_START, FUTURE_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(productPort.getProductDetail(100L)).willReturn(productDetail);
        given(memberPort.getNickname(1L)).willReturn("vinyl_king");

        // when
        AuctionDetailResult result = auctionService.getAuctionDetail(1L, null);

        // then
        assertThat(result.detail()).isInstanceOf(AuctionStatusDetail.ScheduledDetail.class);
        verify(bidRepository, never()).findRecentByAuctionId(any(), anyInt());
        verify(bidRepository, never()).countByAuctionId(any());
    }

    @Test
    @DisplayName("입찰이 없는 RUNNING 경매는 다음 최소 입찰가가 시작가와 같다")
    void testGetAuctionDetail_runningStatus_noBid_nextMinBidEqualsStartPrice() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING, PAST_START, FUTURE_END);
        ReflectionTestUtils.setField(auction, "id", 1L);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(productPort.getProductDetail(100L)).willReturn(productDetail);
        given(memberPort.getNickname(1L)).willReturn("vinyl_king");
        given(bidRepository.countByAuctionId(1L)).willReturn(0L);
        given(bidRepository.findRecentByAuctionId(1L, 5)).willReturn(List.of());

        // when
        AuctionDetailResult result = auctionService.getAuctionDetail(1L, 2L);

        // then
        AuctionStatusDetail.RunningDetail running = (AuctionStatusDetail.RunningDetail) result.detail();
        assertThat(running.highestBidAmount()).isNull();
        assertThat(running.nextMinBidAmount()).isEqualByComparingTo(BigDecimal.valueOf(13_000));
        assertThat(running.bidCount()).isZero();
        assertThat(running.myHighest()).isFalse();
        assertThat(running.bidDetails()).isEmpty();
    }

    @Test
    @DisplayName("입찰이 있는 RUNNING 경매는 최고입찰자에게 myHighest가 true다")
    void testGetAuctionDetail_runningStatus_withBid_viewerIsHighestBidder() {
        // given
        HighestBid highestBid = HighestBid.of(Money.of(12_000L), 2L, 10L);
        Auction auction = auctionWith(AuctionStatus.RUNNING, PAST_START, FUTURE_END, highestBid);
        ReflectionTestUtils.setField(auction, "id", 1L);
        Bid bid = Bid.place(1L, 2L, Money.of(12_000L), PAST_START.plusMinutes(1));
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(productPort.getProductDetail(100L)).willReturn(productDetail);
        given(memberPort.getNickname(1L)).willReturn("vinyl_king");
        given(memberPort.getNickname(2L)).willReturn("bidder_2");
        given(bidRepository.countByAuctionId(1L)).willReturn(1L);
        given(bidRepository.findRecentByAuctionId(1L, 5)).willReturn(List.of(bid));

        // when
        AuctionDetailResult result = auctionService.getAuctionDetail(1L, 2L);

        // then
        AuctionStatusDetail.RunningDetail running = (AuctionStatusDetail.RunningDetail) result.detail();
        assertThat(running.highestBidAmount()).isEqualByComparingTo(BigDecimal.valueOf(12_000));
        assertThat(running.nextMinBidAmount()).isEqualByComparingTo(BigDecimal.valueOf(12_500));
        assertThat(running.bidCount()).isEqualTo(1);
        assertThat(running.myHighest()).isTrue();
        assertThat(running.bidDetails()).hasSize(1);
        assertThat(running.bidDetails().getFirst().bidderNickname()).isEqualTo("bidder_2");
    }

    @Test
    @DisplayName("RUNNING 상태이고 종료 시각이 지났으면 낙찰자가 있어도 ClosingDetail로 노출되고 입찰 정보를 감춘다")
    void testGetAuctionDetail_runningStatus_afterEndTime_withBid_returnsClosingDetail() {
        // given
        HighestBid highestBid = HighestBid.of(Money.of(15_000L), 3L, 20L);
        Auction auction = auctionWith(AuctionStatus.RUNNING, PAST_START, PAST_END, highestBid);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(productPort.getProductDetail(100L)).willReturn(productDetail);
        given(memberPort.getNickname(1L)).willReturn("vinyl_king");

        // when
        AuctionDetailResult result = auctionService.getAuctionDetail(1L, null);

        // then
        assertThat(result.detail()).isInstanceOf(AuctionStatusDetail.ClosingDetail.class);
        verify(bidRepository, never()).findById(any());
        verify(memberPort, never()).getNickname(3L);
    }

    @Test
    @DisplayName("RUNNING 상태이고 종료 시각이 지났고 입찰이 없어도 ClosingDetail로 노출된다")
    void testGetAuctionDetail_runningStatus_afterEndTime_noBid_returnsClosingDetail() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING, PAST_START, PAST_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(productPort.getProductDetail(100L)).willReturn(productDetail);
        given(memberPort.getNickname(1L)).willReturn("vinyl_king");

        // when
        AuctionDetailResult result = auctionService.getAuctionDetail(1L, null);

        // then
        assertThat(result.detail()).isInstanceOf(AuctionStatusDetail.ClosingDetail.class);
    }

    @Test
    @DisplayName("ENDED_WON 상태의 경매 상세는 낙찰 정보를 포함한다")
    void testGetAuctionDetail_endedWonStatus_includesWinningInfo() {
        // given
        HighestBid highestBid = HighestBid.of(Money.of(15_000L), 3L, 20L);
        Auction auction = auctionWith(AuctionStatus.ENDED_WON, PAST_START, PAST_END, highestBid);
        Bid winningBid = Bid.place(1L, 3L, Money.of(15_000L), PAST_START.plusMinutes(1));
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(productPort.getProductDetail(100L)).willReturn(productDetail);
        given(memberPort.getNickname(1L)).willReturn("vinyl_king");
        given(memberPort.getNickname(3L)).willReturn("winner_3");
        given(bidRepository.findById(20L)).willReturn(Optional.of(winningBid));

        // when
        AuctionDetailResult result = auctionService.getAuctionDetail(1L, null);

        // then
        AuctionStatusDetail.EndedWonDetail endedWon = (AuctionStatusDetail.EndedWonDetail) result.detail();
        assertThat(endedWon.bidDetail().amount()).isEqualByComparingTo(BigDecimal.valueOf(15_000));
        assertThat(endedWon.bidDetail().bidderNickname()).isEqualTo("winner_3");
    }

    @Test
    @DisplayName("낙찰자 닉네임 조회에 실패해도 fallback 닉네임으로 대체하고 경매 상세 조회는 계속된다")
    void testGetAuctionDetail_endedWonStatus_winnerNicknameLookupFails_fallsBackToUnknown() {
        // given
        HighestBid highestBid = HighestBid.of(Money.of(15_000L), 3L, 20L);
        Auction auction = auctionWith(AuctionStatus.ENDED_WON, PAST_START, PAST_END, highestBid);
        Bid winningBid = Bid.place(1L, 3L, Money.of(15_000L), PAST_START.plusMinutes(1));
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(productPort.getProductDetail(100L)).willReturn(productDetail);
        given(memberPort.getNickname(1L)).willReturn("vinyl_king");
        given(memberPort.getNickname(3L)).willThrow(new RuntimeException("member-service down"));
        given(bidRepository.findById(20L)).willReturn(Optional.of(winningBid));

        // when
        AuctionDetailResult result = auctionService.getAuctionDetail(1L, null);

        // then
        AuctionStatusDetail.EndedWonDetail endedWon = (AuctionStatusDetail.EndedWonDetail) result.detail();
        assertThat(endedWon.bidDetail().bidderNickname()).isEqualTo("알 수 없음");
    }

    @Test
    @DisplayName("낙찰 입찰을 찾을 수 없으면 예외를 던진다")
    void testGetAuctionDetail_endedWonStatus_bidNotFound_throws() {
        // given
        HighestBid highestBid = HighestBid.of(Money.of(15_000L), 3L, 20L);
        Auction auction = auctionWith(AuctionStatus.ENDED_WON, PAST_START, PAST_END, highestBid);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(productPort.getProductDetail(100L)).willReturn(productDetail);
        given(memberPort.getNickname(1L)).willReturn("vinyl_king");
        given(bidRepository.findById(20L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> auctionService.getAuctionDetail(1L, null))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.BID_NOT_FOUND);
    }

    @Test
    @DisplayName("ENDED_FAILED 상태의 경매 상세는 EndedFailedDetail을 반환한다")
    void testGetAuctionDetail_endedFailedStatus_returnsEndedFailedDetail() {
        // given
        Auction auction = auctionWith(AuctionStatus.ENDED_FAILED, PAST_START, PAST_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(productPort.getProductDetail(100L)).willReturn(productDetail);
        given(memberPort.getNickname(1L)).willReturn("vinyl_king");

        // when
        AuctionDetailResult result = auctionService.getAuctionDetail(1L, null);

        // then
        assertThat(result.detail()).isInstanceOf(AuctionStatusDetail.EndedFailedDetail.class);
    }

    @Test
    @DisplayName("CANCELED 상태의 경매는 상세 조회 시 조회할 수 없다")
    void testGetAuctionDetail_canceledStatus_throws() {
        // given
        Auction auction = auctionWith(AuctionStatus.CANCELED, FUTURE_START, FUTURE_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        // when & then
        assertThatThrownBy(() -> auctionService.getAuctionDetail(1L, null))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_FOUND);
        verify(productPort, never()).getProductDetail(any());
    }

    @Test
    @DisplayName("존재하지 않는 경매의 상세를 조회하면 예외를 던진다")
    void testGetAuctionDetail_auctionNotFound_throws() {
        // given
        given(auctionRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> auctionService.getAuctionDetail(1L, null))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_FOUND);
    }

    // getInternalSummary / getOpenAuctionCounts는 InternalAuctionService로 분리되어 InternalAuctionServiceTest에 있습니다.

    @Test
    @DisplayName("판매 이력 조회 시 상품 요약, 입찰 정보, 실효 상태를 담아 반환한다")
    void testGetHostedAuctions_returnsSummaryAndBidInfoAndEffectiveStatus() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING, PAST_START, FUTURE_END,
                HighestBid.of(Money.of(5_000L), 2L, 10L));
        ReflectionTestUtils.setField(auction, "id", 1L);
        Pageable pageable = PageRequest.of(0, 20);
        given(auctionRepository.findBySellerId(1L, pageable))
                .willReturn(new PageImpl<>(List.of(auction), pageable, 1));
        given(bidRepository.countGroupedByAuctionIds(List.of(1L))).willReturn(Map.of(1L, 3L));
        given(searchViewRepository.findAllSummaryByIds(List.of(1L)))
                .willReturn(List.of(new AuctionProductSummary(1L, "Abbey Road", "The Beatles")));

        // when
        PageResult<HostedAuctionResult> result = auctionService.getHostedAuctions(1L, pageable);

        // then
        assertThat(result.items()).hasSize(1);
        HostedAuctionResult item = result.items().get(0);
        assertThat(item.title()).isEqualTo("Abbey Road");
        assertThat(item.artistName()).isEqualTo("The Beatles");
        assertThat(item.status()).isEqualTo(AuctionStatus.RUNNING);
        assertThat(item.highestBidAmount()).isEqualByComparingTo(BigDecimal.valueOf(5_000L));
        assertThat(item.bidCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("판매 이력 조회 시 취소된 경매는 결과에서 제외한다")
    void testGetHostedAuctions_excludesCanceledAuctions() {
        // given
        Auction canceledAuction = auctionWith(AuctionStatus.CANCELED, PAST_START, PAST_END);
        ReflectionTestUtils.setField(canceledAuction, "id", 2L);
        Pageable pageable = PageRequest.of(0, 20);
        given(auctionRepository.findBySellerId(1L, pageable))
                .willReturn(new PageImpl<>(List.of(canceledAuction), pageable, 1));
        given(bidRepository.countGroupedByAuctionIds(List.of(2L))).willReturn(Map.of());
        given(searchViewRepository.findAllSummaryByIds(List.of(2L))).willReturn(List.of());

        // when
        PageResult<HostedAuctionResult> result = auctionService.getHostedAuctions(1L, pageable);

        // then
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("판매 이력 조회 시 입찰이 없는 경매는 최고 입찰가가 null이다")
    void testGetHostedAuctions_noBid_highestBidAmountIsNull() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING, PAST_START, FUTURE_END);
        ReflectionTestUtils.setField(auction, "id", 3L);
        Pageable pageable = PageRequest.of(0, 20);
        given(auctionRepository.findBySellerId(1L, pageable))
                .willReturn(new PageImpl<>(List.of(auction), pageable, 1));
        given(bidRepository.countGroupedByAuctionIds(List.of(3L))).willReturn(Map.of());
        given(searchViewRepository.findAllSummaryByIds(List.of(3L)))
                .willReturn(List.of(new AuctionProductSummary(3L, "Abbey Road", "The Beatles")));

        // when
        PageResult<HostedAuctionResult> result = auctionService.getHostedAuctions(1L, pageable);

        // then
        HostedAuctionResult item = result.items().get(0);
        assertThat(item.highestBidAmount()).isNull();
        assertThat(item.bidCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("판매 이력 조회 시 종료 시각이 지났지만 배치가 아직 반영 전이면 실효 상태로 보정해 반환한다")
    void testGetHostedAuctions_reflectsEffectiveStatusOverStaleRawStatus() {
        // given: status는 여전히 RUNNING이지만 종료 시각은 이미 지난 경매 (종료 스케줄러 미반영 상태)
        Auction auction = auctionWith(AuctionStatus.RUNNING, PAST_START, PAST_END);
        ReflectionTestUtils.setField(auction, "id", 4L);
        Pageable pageable = PageRequest.of(0, 20);
        given(auctionRepository.findBySellerId(1L, pageable))
                .willReturn(new PageImpl<>(List.of(auction), pageable, 1));
        given(bidRepository.countGroupedByAuctionIds(List.of(4L))).willReturn(Map.of());
        given(searchViewRepository.findAllSummaryByIds(List.of(4L)))
                .willReturn(List.of(new AuctionProductSummary(4L, "Abbey Road", "The Beatles")));

        // when
        PageResult<HostedAuctionResult> result = auctionService.getHostedAuctions(1L, pageable);

        // then
        assertThat(result.items().get(0).status()).isEqualTo(AuctionStatus.ENDED_FAILED);
    }

    @Test
    @DisplayName("참여 이력 조회 시 상품 요약, 내 입찰 정보, 실효 상태를 담아 반환한다")
    void testGetParticipatedAuctions_returnsSummaryAndMyBidInfoAndEffectiveStatus() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING, PAST_START, FUTURE_END);
        ReflectionTestUtils.setField(auction, "id", 1L);
        Bid bid = Bid.place(1L, 5L, Money.of(6_000L), LocalDateTime.now());
        Pageable pageable = PageRequest.of(0, 20);
        given(bidRepository.findLatestBidsByBidder(5L, pageable))
                .willReturn(new PageImpl<>(List.of(bid), pageable, 1));
        given(auctionRepository.findAllByIds(List.of(1L))).willReturn(List.of(auction));
        given(searchViewRepository.findAllSummaryByIds(List.of(1L)))
                .willReturn(List.of(new AuctionProductSummary(1L, "Abbey Road", "The Beatles")));

        // when
        PageResult<ParticipatedAuctionResult> result = auctionService.getParticipatedAuctions(5L, pageable);

        // then
        assertThat(result.items()).hasSize(1);
        ParticipatedAuctionResult item = result.items().get(0);
        assertThat(item.title()).isEqualTo("Abbey Road");
        assertThat(item.artistName()).isEqualTo("The Beatles");
        assertThat(item.status()).isEqualTo(AuctionStatus.RUNNING);
        assertThat(item.myBidAmount()).isEqualTo(Money.of(6_000L));
        assertThat(item.myOutcome()).isEqualTo(BidOutcome.ACTIVE);
        assertThat(item.thumbnail()).isNull();
        assertThat(item.highestBidAmount()).isNull();
    }

    @Test
    @DisplayName("참여 이력 조회 시 썸네일과 현재 최고 입찰가를 함께 반환한다")
    void testGetParticipatedAuctions_returnsThumbnailAndHighestBidAmount() {
        // given
        AuctionSchedule schedule = AuctionSchedule.of(Period.of(PAST_START, FUTURE_END), false, null);
        ItemInfo itemInfoWithImage = ItemInfo.of(ItemCondition.MINT, DESCRIPTION, List.of("cover.png"));
        HighestBid highestBid = HighestBid.of(Money.of(8_000L), 9L, 10L);
        Auction auction = Auction.of(1L, 100L, itemInfoWithImage, pricing, schedule, AuctionStatus.RUNNING, highestBid);
        ReflectionTestUtils.setField(auction, "id", 1L);
        Bid bid = Bid.place(1L, 5L, Money.of(6_000L), LocalDateTime.now());
        Pageable pageable = PageRequest.of(0, 20);
        given(bidRepository.findLatestBidsByBidder(5L, pageable))
                .willReturn(new PageImpl<>(List.of(bid), pageable, 1));
        given(auctionRepository.findAllByIds(List.of(1L))).willReturn(List.of(auction));
        given(searchViewRepository.findAllSummaryByIds(List.of(1L)))
                .willReturn(List.of(new AuctionProductSummary(1L, "Abbey Road", "The Beatles")));

        // when
        PageResult<ParticipatedAuctionResult> result = auctionService.getParticipatedAuctions(5L, pageable);

        // then
        ParticipatedAuctionResult item = result.items().get(0);
        assertThat(item.thumbnail()).isEqualTo("cover.png");
        assertThat(item.highestBidAmount()).isEqualTo(Money.of(8_000L));
    }

    @Test
    @DisplayName("참여 이력 조회 시 취소된 경매에 대한 입찰은 결과에서 제외한다")
    void testGetParticipatedAuctions_excludesBidsOnCanceledAuctions() {
        // given
        Auction canceledAuction = auctionWith(AuctionStatus.CANCELED, PAST_START, PAST_END);
        ReflectionTestUtils.setField(canceledAuction, "id", 2L);
        Bid bid = Bid.place(2L, 5L, Money.of(6_000L), LocalDateTime.now());
        Pageable pageable = PageRequest.of(0, 20);
        given(bidRepository.findLatestBidsByBidder(5L, pageable))
                .willReturn(new PageImpl<>(List.of(bid), pageable, 1));
        given(auctionRepository.findAllByIds(List.of(2L))).willReturn(List.of(canceledAuction));
        given(searchViewRepository.findAllSummaryByIds(List.of(2L))).willReturn(List.of());

        // when
        PageResult<ParticipatedAuctionResult> result = auctionService.getParticipatedAuctions(5L, pageable);

        // then
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("경매 목록 조회 시 서치 뷰 조회 결과를 애플리케이션 타입으로 변환해 반환한다")
    void testGetAuctions_mapsSearchViewResultToApplicationType() {
        // given
        AuctionListQuery query = new AuctionListQuery(null, null, "Rock", "ORIGINAL", "RUNNING", "price_asc");
        Pageable pageable = PageRequest.of(0, 20);
        AuctionListSummary summary = new AuctionListSummary(
                1L, 100L, "Abbey Road", "The Beatles", 1969, "Rock", "ORIGINAL", "1.png",
                2L, "vinyl_king", AuctionStatus.RUNNING, ItemCondition.MINT.name(),
                BigDecimal.valueOf(10_000), BigDecimal.valueOf(8_000), 3L,
                PAST_START, FUTURE_END);
        given(searchViewRepository.search(query, pageable))
                .willReturn(new PageImpl<>(List.of(summary), pageable, 1));

        // when
        PageResult<AuctionListItemResult> result = auctionService.getAuctions(query, pageable);

        // then
        assertThat(result.items()).hasSize(1);
        AuctionListItemResult item = result.items().get(0);
        assertThat(item.title()).isEqualTo("Abbey Road");
        assertThat(item.artistName()).isEqualTo("The Beatles");
        assertThat(item.status()).isEqualTo(AuctionStatus.RUNNING);
        assertThat(item.finalPrice()).isEqualTo(Money.of(10_000L));
        assertThat(item.bidCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("입찰에 성공하면 예치금을 홀드하고 Bid를 저장하고 최고입찰을 갱신한다")
    void testPlaceBid_success_holdsWalletAndSavesBidAndUpdatesHighestBid() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING, PAST_START, FUTURE_END);
        ReflectionTestUtils.setField(auction, "id", 1L);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(walletPort.hold(1L, 2L, Money.of(13_000L)))
                .willReturn(new WalletHoldInfo(100L, null, BigDecimal.valueOf(87_000)));
        given(bidRepository.save(any(Bid.class))).willAnswer(invocation -> {
            Bid bid = invocation.getArgument(0);
            ReflectionTestUtils.setField(bid, "id", 50L);
            return bid;
        });
        given(bidRepository.findActiveBid(1L)).willReturn(Optional.empty());
        given(bidRepository.countByAuctionId(1L)).willReturn(1L);

        PlaceBidCommand command = new PlaceBidCommand(1L, 2L, BigDecimal.valueOf(13_000));

        // when
        PlaceBidResult result = auctionService.placeBid(command);

        // then
        assertThat(result.bidId()).isEqualTo(50L);
        assertThat(result.auctionId()).isEqualTo(1L);
        assertThat(result.bidAmount()).isEqualTo(Money.of(13_000L));
        assertThat(result.outcome()).isEqualTo(BidOutcome.ACTIVE);
        assertThat(result.extended()).isFalse();
        assertThat(auction.getHighestBid().isBidder(2L)).isTrue();

        verify(walletPort).hold(1L, 2L, Money.of(13_000L));
        verify(searchViewRepository).updateOnBid(1L, BigDecimal.valueOf(13_000), 1, auction.getEndAt());
        // placeBid()의 수동 락 호출도 @DistributedLock 애노테이션 경로(DistributedLockAspect)와
        // 똑같이 LockPort.executeWithLockOnAuction()에 auctionId를 그대로 넘긴다는 걸 검증한다
        // (키 조합은 LockPort 안에서 AuctionRedisKeys.lockKey()로 통일돼 있다).
        verify(lockPort).executeWithLockOnAuction(eq(1L), anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("기존 활성 입찰이 있으면 새 입찰 저장 후 기존 입찰을 OUTBID로 전환한다")
    void testPlaceBid_existingActiveBid_marksPreviousBidOutbid() {
        // given
        HighestBid highestBid = HighestBid.of(Money.of(13_000L), 5L, 10L);
        Auction auction = auctionWith(AuctionStatus.RUNNING, PAST_START, FUTURE_END, highestBid);
        ReflectionTestUtils.setField(auction, "id", 1L);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(walletPort.hold(any(), any(), any()))
                .willReturn(new WalletHoldInfo(100L, 10L, BigDecimal.valueOf(87_000)));

        Bid previousBid = Bid.place(1L, 5L, Money.of(13_000L), PAST_START.plusMinutes(1));
        ReflectionTestUtils.setField(previousBid, "id", 10L);
        given(bidRepository.findActiveBid(1L)).willReturn(Optional.of(previousBid));
        given(bidRepository.save(any(Bid.class))).willAnswer(invocation -> {
            Bid bid = invocation.getArgument(0);
            ReflectionTestUtils.setField(bid, "id", 20L);
            return bid;
        });
        given(bidRepository.countByAuctionId(1L)).willReturn(2L);

        PlaceBidCommand command = new PlaceBidCommand(1L, 2L, BigDecimal.valueOf(13_500));

        // when
        auctionService.placeBid(command);

        // then
        assertThat(previousBid.getOutcome()).isEqualTo(BidOutcome.OUTBID);
    }

    @Test
    @DisplayName("제재된 회원이 입찰하면 예외를 던지고 락/경매 조회/예치금 홀드를 호출하지 않는다")
    void testPlaceBid_restrictedMember_throwsWithoutTouchingAuctionOrWallet() {
        // given
        given(memberPort.isMemberRestricted(2L)).willReturn(true);
        PlaceBidCommand command = new PlaceBidCommand(1L, 2L, BigDecimal.valueOf(13_000));

        // when & then
        assertThatThrownBy(() -> auctionService.placeBid(command))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.BID_MEMBER_RESTRICTED);
        verify(auctionRepository, never()).findById(any());
        verify(walletPort, never()).hold(any(), any(), any());
        verify(lockPort, never()).executeWithLockOnAuction(any(), anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 경매에 입찰하면 예외를 던지고 예치금 홀드를 호출하지 않는다")
    void testPlaceBid_auctionNotFound_throws() {
        // given
        given(auctionRepository.findById(1L)).willReturn(Optional.empty());
        PlaceBidCommand command = new PlaceBidCommand(1L, 2L, BigDecimal.valueOf(13_000));

        // when & then
        assertThatThrownBy(() -> auctionService.placeBid(command))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_NOT_FOUND);
        verify(walletPort, never()).hold(any(), any(), any());
    }

    @Test
    @DisplayName("최소 입찰가 미만이면 예치금 홀드를 호출하지 않고 예외를 던진다")
    void testPlaceBid_belowMinimum_throwsWithoutHoldingWallet() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING, PAST_START, FUTURE_END);
        ReflectionTestUtils.setField(auction, "id", 1L);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        PlaceBidCommand command = new PlaceBidCommand(1L, 2L, BigDecimal.valueOf(1_000));

        // when & then
        assertThatThrownBy(() -> auctionService.placeBid(command))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.BID_AMOUNT_TOO_LOW);
        verify(walletPort, never()).hold(any(), any(), any());
        verify(bidRepository, never()).save(any());
    }

    @Test
    @DisplayName("예치금 홀드가 실패하면 입찰을 저장하지 않고 예외를 그대로 전파한다")
    void testPlaceBid_walletHoldFails_propagatesAndDoesNotSaveBid() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING, PAST_START, FUTURE_END);
        ReflectionTestUtils.setField(auction, "id", 1L);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(walletPort.hold(any(), any(), any()))
                .willThrow(new AuctionException(AuctionErrorCode.WALLET_HOLD_FAILED));

        PlaceBidCommand command = new PlaceBidCommand(1L, 2L, BigDecimal.valueOf(13_000));

        // when & then
        assertThatThrownBy(() -> auctionService.placeBid(command))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.WALLET_HOLD_FAILED);
        verify(bidRepository, never()).save(any());
        // hold() 자체가 실패하면 holdId가 존재하지 않으므로 보상(rollback) 호출도 없어야 한다
        verify(walletPort, never()).rollback(any(), any(), any(), any());
    }

    @Test
    @DisplayName("예치금 홀드 이후 단계에서 예외가 나면 삼키지 않고 그대로 전파하되, 홀드를 보상(rollback)한다")
    void testPlaceBid_failureAfterHold_propagatesExceptionAndRollsBackHold() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING, PAST_START, FUTURE_END);
        ReflectionTestUtils.setField(auction, "id", 1L);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(walletPort.hold(any(), any(), any()))
                .willReturn(new WalletHoldInfo(100L, null, BigDecimal.valueOf(87_000)));
        given(bidRepository.save(any(Bid.class))).willAnswer(invocation -> {
            Bid bid = invocation.getArgument(0);
            ReflectionTestUtils.setField(bid, "id", 50L);
            return bid;
        });
        given(bidRepository.findActiveBid(1L)).willReturn(Optional.empty());
        given(bidRepository.countByAuctionId(1L)).willReturn(1L);
        willThrow(new AuctionException(AuctionErrorCode.AUCTION_SEARCH_VIEW_NOT_FOUND))
                .given(searchViewRepository).updateOnBid(any(), any(), anyInt(), any());

        PlaceBidCommand command = new PlaceBidCommand(1L, 2L, BigDecimal.valueOf(13_000));

        // when & then
        assertThatThrownBy(() -> auctionService.placeBid(command))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_SEARCH_VIEW_NOT_FOUND);
        verify(walletPort).rollback(100L, 1L, 2L, Money.of(13_000L));
    }

    @Test
    @DisplayName("보상(rollback) 호출 자체가 실패해도 원래 입찰 실패 사유를 덮지 않고 그대로 전파한다")
    void testPlaceBid_rollbackItselfFails_stillPropagatesOriginalException() {
        // given
        Auction auction = auctionWith(AuctionStatus.RUNNING, PAST_START, FUTURE_END);
        ReflectionTestUtils.setField(auction, "id", 1L);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        given(walletPort.hold(any(), any(), any()))
                .willReturn(new WalletHoldInfo(100L, null, BigDecimal.valueOf(87_000)));
        given(bidRepository.save(any(Bid.class))).willAnswer(invocation -> {
            Bid bid = invocation.getArgument(0);
            ReflectionTestUtils.setField(bid, "id", 50L);
            return bid;
        });
        given(bidRepository.findActiveBid(1L)).willReturn(Optional.empty());
        given(bidRepository.countByAuctionId(1L)).willReturn(1L);
        willThrow(new AuctionException(AuctionErrorCode.AUCTION_SEARCH_VIEW_NOT_FOUND))
                .given(searchViewRepository).updateOnBid(any(), any(), anyInt(), any());
        willThrow(new AuctionException(AuctionErrorCode.WALLET_ROLLBACK_FAILED))
                .given(walletPort).rollback(any(), any(), any(), any());

        PlaceBidCommand command = new PlaceBidCommand(1L, 2L, BigDecimal.valueOf(13_000));

        // when & then: 보상 실패(WALLET_ROLLBACK_FAILED)가 아니라 원래 실패 사유가 그대로 전파돼야 한다
        assertThatThrownBy(() -> auctionService.placeBid(command))
                .isInstanceOf(AuctionException.class)
                .extracting(e -> ((AuctionException) e).getErrorCode())
                .isEqualTo(AuctionErrorCode.AUCTION_SEARCH_VIEW_NOT_FOUND);
    }
}
