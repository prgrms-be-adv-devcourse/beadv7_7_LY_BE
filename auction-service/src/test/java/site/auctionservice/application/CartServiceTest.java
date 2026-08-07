package site.auctionservice.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.auctionservice.application.dto.CartItemGroupResult;
import site.auctionservice.application.dto.InternalAuctionSnapshotResult;
import site.auctionservice.domain.AuctionStatus;
import site.auctionservice.domain.CartItem;
import site.auctionservice.domain.CartItemRepository;
import site.auctionservice.exception.AuctionErrorCode;
import site.auctionservice.exception.AuctionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final BigDecimal PRICE = BigDecimal.valueOf(10_000);
    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final long MAX_WATCHED_AUCTIONS = 30L;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private InternalAuctionService internalAuctionService;

    private static InternalAuctionSnapshotResult summary(Long auctionId, AuctionStatus status,
        LocalDateTime startAt,
        LocalDateTime endAt) {
        return new InternalAuctionSnapshotResult(auctionId, status, PRICE, startAt, endAt);
    }

    @Test
    void add는_이미_존재하면_저장하지_않는다() {
        when(cartItemRepository.existsByMemberIdAndAuctionId(1L, 100L)).thenReturn(true);

        final CartService service = new CartService(cartItemRepository, internalAuctionService);
        service.addItem(1L, 100L);

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItem은_존재하지_않으면_저장한다() {
        when(cartItemRepository.existsByMemberIdAndAuctionId(1L, 100L)).thenReturn(false);
        when(internalAuctionService.getSnapshot(100L))
            .thenReturn(summary(100L, AuctionStatus.RUNNING, NOW.minusHours(1), NOW.plusHours(1)));
        when(cartItemRepository.countByMemberId(1L)).thenReturn(0L);

        final CartService service = new CartService(cartItemRepository, internalAuctionService);
        service.addItem(1L, 100L);

        final ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo(1L);
        assertThat(captor.getValue().getAuctionId()).isEqualTo(100L);
    }

    @Test
    void addItem은_존재하지_않는_경매면_예외를_던지고_저장하지_않는다() {
        when(cartItemRepository.existsByMemberIdAndAuctionId(1L, 100L)).thenReturn(false);
        when(internalAuctionService.getSnapshot(100L))
            .thenThrow(new AuctionException(AuctionErrorCode.AUCTION_NOT_FOUND));

        final CartService service = new CartService(cartItemRepository, internalAuctionService);

        assertThatThrownBy(() -> service.addItem(1L, 100L))
            .isInstanceOf(AuctionException.class)
            .extracting(ex -> ((AuctionException) ex).getErrorCode())
            .isEqualTo(AuctionErrorCode.AUCTION_NOT_FOUND);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItem은_SCHEDULED_경매면_저장한다() {
        when(cartItemRepository.existsByMemberIdAndAuctionId(1L, 100L)).thenReturn(false);
        when(internalAuctionService.getSnapshot(100L))
            .thenReturn(summary(100L, AuctionStatus.SCHEDULED, NOW.plusHours(1), NOW.plusHours(2)));
        when(cartItemRepository.countByMemberId(1L)).thenReturn(0L);

        final CartService service = new CartService(cartItemRepository, internalAuctionService);
        service.addItem(1L, 100L);

        verify(cartItemRepository).save(any());
    }

    @Test
    void addItem은_종료되었거나_취소된_경매면_예외를_던지고_저장하지_않는다() {
        when(cartItemRepository.existsByMemberIdAndAuctionId(1L, 100L)).thenReturn(false);
        when(internalAuctionService.getSnapshot(100L))
            .thenReturn(
                summary(100L, AuctionStatus.ENDED_WON, NOW.minusHours(2), NOW.minusHours(1)));

        final CartService service = new CartService(cartItemRepository, internalAuctionService);

        assertThatThrownBy(() -> service.addItem(1L, 100L))
            .isInstanceOf(AuctionException.class)
            .extracting(ex -> ((AuctionException) ex).getErrorCode())
            .isEqualTo(AuctionErrorCode.WATCHED_AUCTION_STATUS_INVALID);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItem은_최대_개수에_도달하면_예외를_던진다() {
        when(cartItemRepository.existsByMemberIdAndAuctionId(1L, 100L)).thenReturn(false);
        when(internalAuctionService.getSnapshot(100L))
            .thenReturn(summary(100L, AuctionStatus.RUNNING, NOW.minusHours(1), NOW.plusHours(1)));
        when(cartItemRepository.countByMemberId(1L)).thenReturn(MAX_WATCHED_AUCTIONS);

        final CartService service = new CartService(cartItemRepository, internalAuctionService);

        assertThatThrownBy(() -> service.addItem(1L, 100L))
            .isInstanceOf(AuctionException.class)
            .extracting(ex -> ((AuctionException) ex).getErrorCode())
            .isEqualTo(AuctionErrorCode.WATCHED_AUCTION_LIMIT_EXCEEDED);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void removeItem은_리포지토리에_삭제를_위임한다() {
        final CartService service = new CartService(cartItemRepository, internalAuctionService);
        service.removeItem(1L, 100L);

        verify(cartItemRepository).deleteByMemberIdAndAuctionId(1L, 100L);
    }

    @Test
    void findAll은_각_아이템에_해당_경매_정보를_채워_반환한다() {
        final CartItem cartItem = CartItem.of(1L, 100L);
        final LocalDateTime startAt = NOW.minusHours(1);
        final LocalDateTime endAt = NOW.plusHours(2);
        when(cartItemRepository.findAllByMemberId(1L)).thenReturn(List.of(cartItem));
        when(internalAuctionService.getSnapshots(List.of(100L)))
            .thenReturn(List.of(summary(100L, AuctionStatus.RUNNING, startAt, endAt)));

        final CartService service = new CartService(cartItemRepository, internalAuctionService);
        final List<CartItemGroupResult> result = service.findAll(1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().status()).isEqualTo(AuctionStatus.RUNNING);
        assertThat(result.getFirst().items()).hasSize(1);
        assertThat(result.getFirst().items().getFirst().auctionId()).isEqualTo(100L);
        assertThat(result.getFirst().items().getFirst().currentPrice()).isEqualByComparingTo(PRICE);
        assertThat(result.getFirst().items().getFirst().startAt()).isEqualTo(startAt);
        assertThat(result.getFirst().items().getFirst().endAt()).isEqualTo(endAt);
    }

    @Test
    void findAll은_취소된_경매도_상태_그대로_반환한다() {
        final CartItem cartItem = CartItem.of(1L, 100L);
        when(cartItemRepository.findAllByMemberId(1L)).thenReturn(List.of(cartItem));
        when(internalAuctionService.getSnapshots(List.of(100L)))
            .thenReturn(List.of(
                summary(100L, AuctionStatus.CANCELED, NOW.minusHours(3), NOW.minusHours(1))));

        final CartService service = new CartService(cartItemRepository, internalAuctionService);
        final List<CartItemGroupResult> result = service.findAll(1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().status()).isEqualTo(AuctionStatus.CANCELED);
    }

    @Test
    void findAll은_참조하는_경매가_없으면_건너뛰고_에러를_발생시키지_않는다() {
        final CartItem cartItem = CartItem.of(1L, 999L);
        when(cartItemRepository.findAllByMemberId(1L)).thenReturn(List.of(cartItem));
        when(internalAuctionService.getSnapshots(List.of(999L))).thenReturn(List.of());

        final CartService service = new CartService(cartItemRepository, internalAuctionService);
        final List<CartItemGroupResult> result = service.findAll(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void findAll은_상태_우선순위_순으로_그룹을_나눈다() {
        // 입력 순서를 일부러 섞어서 그룹 순서가 실제로 정렬 로직에서 나오는지 검증한다.
        final List<CartItem> cartItems = List.of(
            CartItem.of(1L, 5L), CartItem.of(1L, 3L), CartItem.of(1L, 1L),
            CartItem.of(1L, 4L), CartItem.of(1L, 2L)
        );
        when(cartItemRepository.findAllByMemberId(1L)).thenReturn(cartItems);
        when(internalAuctionService.getSnapshots(any())).thenReturn(List.of(
            summary(1L, AuctionStatus.RUNNING, NOW.minusHours(1), NOW.plusHours(1)),
            summary(2L, AuctionStatus.SCHEDULED, NOW.plusHours(1), NOW.plusHours(5)),
            summary(3L, AuctionStatus.ENDED_WON, NOW.minusHours(5), NOW.minusHours(1)),
            summary(4L, AuctionStatus.ENDED_FAILED, NOW.minusHours(5), NOW.minusHours(1)),
            summary(5L, AuctionStatus.CANCELED, NOW.minusHours(5), NOW.minusHours(1))
        ));

        final CartService service = new CartService(cartItemRepository, internalAuctionService);
        final List<CartItemGroupResult> result = service.findAll(1L);

        assertThat(result).extracting(CartItemGroupResult::status)
            .containsExactly(AuctionStatus.RUNNING, AuctionStatus.SCHEDULED,
                AuctionStatus.ENDED_WON,
                AuctionStatus.ENDED_FAILED, AuctionStatus.CANCELED);
        assertThat(result).flatExtracting(CartItemGroupResult::items)
            .extracting("auctionId")
            .containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    void findAll은_진행중인_경매를_같은_그룹_안에서_마감_임박순으로_정렬한다() {
        final List<CartItem> cartItems = List.of(CartItem.of(1L, 1L), CartItem.of(1L, 2L));
        when(cartItemRepository.findAllByMemberId(1L)).thenReturn(cartItems);
        when(internalAuctionService.getSnapshots(any())).thenReturn(List.of(
            summary(1L, AuctionStatus.RUNNING, NOW.minusHours(1), NOW.plusHours(3)),
            summary(2L, AuctionStatus.RUNNING, NOW.minusHours(1), NOW.plusHours(1))
        ));

        final CartService service = new CartService(cartItemRepository, internalAuctionService);
        final List<CartItemGroupResult> result = service.findAll(1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().items()).extracting("auctionId").containsExactly(2L, 1L);
    }

    @Test
    void findAll은_예정된_경매를_같은_그룹_안에서_시작_임박순으로_정렬한다() {
        final List<CartItem> cartItems = List.of(CartItem.of(1L, 1L), CartItem.of(1L, 2L));
        when(cartItemRepository.findAllByMemberId(1L)).thenReturn(cartItems);
        when(internalAuctionService.getSnapshots(any())).thenReturn(List.of(
            summary(1L, AuctionStatus.SCHEDULED, NOW.plusHours(5), NOW.plusHours(10)),
            summary(2L, AuctionStatus.SCHEDULED, NOW.plusHours(2), NOW.plusHours(10))
        ));

        final CartService service = new CartService(cartItemRepository, internalAuctionService);
        final List<CartItemGroupResult> result = service.findAll(1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().items()).extracting("auctionId").containsExactly(2L, 1L);
    }

    @Test
    void findAll은_종료된_경매를_같은_그룹_안에서_최근_종료순으로_정렬한다() {
        final List<CartItem> cartItems = List.of(CartItem.of(1L, 1L), CartItem.of(1L, 2L));
        when(cartItemRepository.findAllByMemberId(1L)).thenReturn(cartItems);
        when(internalAuctionService.getSnapshots(any())).thenReturn(List.of(
            summary(1L, AuctionStatus.ENDED_WON, NOW.minusHours(5), NOW.minusHours(3)),
            summary(2L, AuctionStatus.ENDED_WON, NOW.minusHours(5), NOW.minusHours(1))
        ));

        final CartService service = new CartService(cartItemRepository, internalAuctionService);
        final List<CartItemGroupResult> result = service.findAll(1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().items()).extracting("auctionId").containsExactly(2L, 1L);
    }
}
