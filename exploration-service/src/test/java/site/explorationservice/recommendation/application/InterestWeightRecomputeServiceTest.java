package site.explorationservice.recommendation.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.explorationservice.productindex.domain.AxisWeights;
import site.explorationservice.recommendation.application.dto.InterestWeightResult;
import site.explorationservice.recommendation.application.port.WishlistPort;
import site.explorationservice.recommendation.application.port.dto.WishlistProduct;
import site.explorationservice.recommendation.domain.DirtyMemberTracker;
import site.explorationservice.recommendation.domain.DueMember;
import site.explorationservice.recommendation.domain.InterestWeightCacheRepository;

/**
 * 성공/빈 위시리스트/실패 세 갈래에서 complete·release가 정확히 갈리는지 검증한다 — 여기서 잘못되면 dirty 신호가 영구히 사라지거나 재클레임이 영원히
 * 막히는(inFlight 누수) 실제 장애로 이어진다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("관심사 가중치 비동기 재계산")
class InterestWeightRecomputeServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Instant DIRTY_SINCE = Instant.parse("2026-01-01T00:00:00Z");
    private static final DueMember DUE = new DueMember(MEMBER_ID, DIRTY_SINCE);

    @Mock
    private WishlistPort wishlistPort;

    @Mock
    private InterestWeightService interestWeightService;

    @Mock
    private InterestWeightCacheRepository interestWeightCacheRepository;

    @Mock
    private DirtyMemberTracker dirtyMemberTracker;

    @InjectMocks
    private InterestWeightRecomputeService sut;

    @Test
    @DisplayName("성공하면 캐시에 저장하고 complete를 호출한다")
    void 성공() {
        final List<WishlistProduct> wishlist = List.of(wishlistProduct(10L));
        given(wishlistPort.findRecentProducts(MEMBER_ID, 50)).willReturn(wishlist);
        final AxisWeights weights = new AxisWeights(0.5, 0.3, 0.2);
        given(interestWeightService.analyzeWeights(wishlist))
            .willReturn(new InterestWeightResult(0.5, 0.3, 0.2, "근거"));

        sut.recompute(DUE);

        then(interestWeightCacheRepository).should().save(MEMBER_ID, weights, DIRTY_SINCE);
        then(dirtyMemberTracker).should().complete(MEMBER_ID, DIRTY_SINCE);
        then(dirtyMemberTracker).should(never()).release(any());
    }

    @Test
    @DisplayName("위시리스트가 비어 있으면 LLM도 캐시 저장도 안 하고 complete만 호출한다")
    void 빈_위시리스트() {
        given(wishlistPort.findRecentProducts(MEMBER_ID, 50)).willReturn(List.of());

        sut.recompute(DUE);

        then(interestWeightService).should(never()).analyzeWeights(anyList());
        then(interestWeightCacheRepository).should(never()).save(any(), any(), any());
        then(dirtyMemberTracker).should().complete(MEMBER_ID, DIRTY_SINCE);
    }

    @Test
    @DisplayName("LLM 호출이 실패하면 캐시 저장·complete 없이 release만 호출한다 — 다음 스윕에서 재시도된다")
    void LLM_실패() {
        given(wishlistPort.findRecentProducts(MEMBER_ID, 50))
            .willReturn(List.of(wishlistProduct(10L)));
        given(interestWeightService.analyzeWeights(anyList()))
            .willThrow(new RuntimeException("OpenAI 호출 실패"));

        sut.recompute(DUE);

        then(interestWeightCacheRepository).should(never()).save(any(), any(), any());
        then(dirtyMemberTracker).should(never()).complete(any(), any());
        then(dirtyMemberTracker).should().release(MEMBER_ID);
    }

    @Test
    @DisplayName("위시리스트 조회 자체가 실패해도 release로 안전하게 빠진다")
    void 위시리스트_조회_실패() {
        given(wishlistPort.findRecentProducts(MEMBER_ID, 50))
            .willThrow(new RuntimeException("product-service 호출 실패"));

        sut.recompute(DUE);

        then(dirtyMemberTracker).should().release(MEMBER_ID);
        then(dirtyMemberTracker).should(never()).complete(any(), any());
    }

    private WishlistProduct wishlistProduct(final Long productId) {
        return new WishlistProduct(productId, "제목", "아티스트", "Jazz", "Columbia", 1959, "미국",
            "ORIGINAL");
    }
}
