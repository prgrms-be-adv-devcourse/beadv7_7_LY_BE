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
import site.coreservice.auction.application.port.AuctionSearchViewRepository;
import site.coreservice.auction.application.port.MemberPort;
import site.coreservice.auction.application.port.ProductPort;
import site.coreservice.auction.application.port.dto.ProductSnapshot;
import site.coreservice.auction.domain.Auction;
import site.coreservice.auction.domain.AuctionRepository;
import site.coreservice.auction.domain.AuctionStatus;
import site.coreservice.auction.exception.InvalidValueException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    private final ProductSnapshot productSnapshot =
            new ProductSnapshot(100L, "Abbey Road", "The Beatles", 1969, "Rock", "ORIGINAL", true);

    private CreateAuctionCommand validCommand(String itemCondition) {
        return new CreateAuctionCommand(
                100L,
                itemCondition,
                "충분히 긴 상품 설명입니다.",
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
        assertThatThrownBy(() -> auctionService.createAuction(validCommand("NOT_A_CONDITION"), 1L)).isInstanceOf(InvalidValueException.class);
        verify(auctionRepository, never()).save(any());
        verify(searchViewRepository, never()).save(any(), any(), any());
    }
}
