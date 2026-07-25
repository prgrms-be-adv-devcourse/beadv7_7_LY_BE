package site.coreservice.product.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.coreservice.product.domain.ClosedAuction;
import site.coreservice.product.domain.MediaCondition;
import site.coreservice.product.domain.ProductRepository;

// 상품 목록 스텁은 @BeforeEach가 아니라 각 테스트 안에 둔다 — 상품 없음 테스트가 빈 목록으로
// 다시 스텁해야 하는데, 덮어쓰여 쓰이지 않은 스텁을 Mockito 기본 엄격 모드가 오류로 처리하기 때문.
@ExtendWith(MockitoExtension.class)
class StubAuctionSnapshotAdapterTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private StubAuctionSnapshotAdapter adapter;

    @Test
    @DisplayName("같은 경매 id로 두 번 조회하면 같은 응답을 돌려준다")
    void findClosedAuction_같은_id_같은_응답() {
        // given
        given(productRepository.findAllActiveIds()).willReturn(List.of(11L, 22L, 33L));

        // when
        ClosedAuction first = adapter.findClosedAuction(7L).orElseThrow();
        ClosedAuction second = adapter.findClosedAuction(7L).orElseThrow();

        // then
        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("상품 id는 실제 존재하는 상품 목록 안에서 매핑되고 필드는 유효 범위를 지킨다")
    void findClosedAuction_상품목록_매핑과_유효_필드() {
        // given
        given(productRepository.findAllActiveIds()).willReturn(List.of(11L, 22L, 33L));

        // when-then: 여러 id를 훑어 전 필드 검증
        for (long auctionId = 1; auctionId <= 50; auctionId++) {
            ClosedAuction auction = adapter.findClosedAuction(auctionId).orElseThrow();
            assertThat(auction.productId()).isIn(11L, 22L, 33L);
            assertThat(auction.finalPrice()).isPositive();
            assertThat(auction.bidCount()).isPositive();
            assertThat(auction.isClosed()).isTrue();
            assertThat(auction.closedAt()).isBefore(LocalDateTime.now());
            assertThat(auction.closedAt()).isAfter(LocalDateTime.now().minusDays(366));
        }
    }

    @Test
    @DisplayName("같은 상품에 배정된 경매들도 컨디션 등급은 여러 가지가 나온다")
    void findClosedAuction_같은_상품_컨디션_다양() {
        // given
        given(productRepository.findAllActiveIds()).willReturn(List.of(11L, 22L, 33L));

        // when: 상품 하나(id 11)로 매핑되는 경매만 골라 컨디션을 모은다
        Set<MediaCondition> conditions = new HashSet<>();
        for (long auctionId = 1; auctionId <= 54; auctionId++) {
            ClosedAuction auction = adapter.findClosedAuction(auctionId).orElseThrow();
            if (auction.productId() == 11L) {
                conditions.add(auction.mediaCondition());
            }
        }

        // then: 등급 6종이 전부 등장한다 — 상품 배정과 컨디션을 같은 식에서 뽑으면 한 가지로 고정된다
        assertThat(conditions).containsExactlyInAnyOrder(MediaCondition.values());
    }

    @Test
    @DisplayName("실패 예약 대역은 빈 결과 또는 미마감 경매를 돌려준다")
    void findClosedAuction_실패_대역() {
        // given: "경매 없음" 대역은 상품 조회 없이 끝나므로 미마감 대역 응답 생성에만 쓰인다
        given(productRepository.findAllActiveIds()).willReturn(List.of(11L, 22L, 33L));

        // when-then
        assertThat(adapter.findClosedAuction(90_000L)).isEmpty();
        Optional<ClosedAuction> open = adapter.findClosedAuction(90_500L);
        assertThat(open).isPresent();
        assertThat(open.orElseThrow().isClosed()).isFalse();
    }

    @Test
    @DisplayName("상품이 하나도 없으면 명확한 예외를 던진다")
    void findClosedAuction_상품_없음_예외() {
        // given
        given(productRepository.findAllActiveIds()).willReturn(List.of());

        // when-then
        assertThatThrownBy(() -> adapter.findClosedAuction(1L))
                .isInstanceOf(IllegalStateException.class);
    }
}
