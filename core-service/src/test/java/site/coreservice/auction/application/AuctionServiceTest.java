package site.coreservice.auction.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.coreservice.auction.application.dto.AuctionResult;
import site.coreservice.auction.application.dto.CreateAuctionCommand;
import site.coreservice.auction.application.dto.ModifyAuctionCommand;
import site.coreservice.auction.application.port.AuctionSearchViewRepository;
import site.coreservice.auction.application.port.MemberPort;
import site.coreservice.auction.application.port.ProductPort;
import site.coreservice.auction.application.port.dto.ProductSnapshot;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionRepository;
import site.coreservice.auction.domain.AuctionStatus;
import site.coreservice.auction.domain.ItemCondition;
import site.coreservice.auction.domain.ItemInfo;
import site.coreservice.auction.domain.Money;
import site.coreservice.auction.domain.Period;
import site.coreservice.auction.domain.Pricing;
import site.coreservice.auction.domain.AuctionSchedule;
import site.coreservice.auction.exception.AuctionErrorCode;
import site.coreservice.auction.exception.AuctionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private MemberPort memberPort;

    @Mock
    private ProductPort productPort;

    @Mock
    private AuctionSearchViewRepository searchViewRepository;

    @InjectMocks
    private AuctionService auctionService;

    private static final String DESCRIPTION = "충분히 긴 상품 설명입니다.";
    private static final LocalDateTime PAST_START = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime PAST_END = PAST_START.plusHours(2);
    private static final LocalDateTime FUTURE_START = LocalDateTime.of(2999, 1, 1, 0, 0);
    private static final LocalDateTime FUTURE_END = FUTURE_START.plusHours(2);

    private final ProductSnapshot productSnapshot =
            new ProductSnapshot(100L, "Abbey Road", "The Beatles", 1969, "Rock", "ORIGINAL", true);
    private final ItemInfo itemInfo = ItemInfo.of(ItemCondition.MINT, DESCRIPTION, null);
    private final Pricing pricing = Pricing.of(Money.of(10_000L), Money.of(500L), Money.of(3_000L));

    private CreateAuctionCommand validCommand(String itemCondition) {
        return new CreateAuctionCommand(
                100L,
                itemCondition,
                DESCRIPTION,
                List.of("1.png"),
                BigDecimal.valueOf(10_000),
                BigDecimal.valueOf(3_000),
                BigDecimal.valueOf(500),
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 7, 2, 0, 0),
                false,
                null
        );
    }

    private Auction auctionWith(AuctionStatus status, LocalDateTime startAt, LocalDateTime endAt) {
        AuctionSchedule schedule = AuctionSchedule.of(Period.of(startAt, endAt), false, null);
        return Auction.of(1L, 100L, itemInfo, pricing, schedule, status, null);
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
    @DisplayName("SCHEDULED 상태면 시작 시각이 지났어도 경매를 수정한다")
    void testModifyAuction_scheduledStatus_evenAfterStartTime_succeeds() {
        // given
        Auction auction = auctionWith(AuctionStatus.SCHEDULED, PAST_START, PAST_END);
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));

        // when
        AuctionResult result = auctionService.modifyAuction(modifyCommand(1L, 100L, PAST_START, PAST_END), 1L);

        // then
        assertThat(result.status()).isEqualTo(AuctionStatus.SCHEDULED.name());
        assertThat(auction.getItemInfo().getDescription()).isEqualTo("수정된 상품 설명입니다.");
        verify(productPort, never()).getProduct(any());
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
}
