package site.productservice.infrastructure.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import site.productservice.infrastructure.ProductJpaRepository;

/**
 * 판매 중인 상품이 몇 건인지 세어 두고 다시 쓴다.
 * <p>
 * 이 값은 목록 응답의 전체 건수로 나가는데, 세려면 상품 테이블 전체를 훑어야 해서 목록 응답 시간의 대부분을
 * 차지한다. 상품 목록은 한 번 적재한 뒤로 바뀌지 않으므로 요청마다 다시 세지 않는다.
 * <p>
 * 리포지토리에 바로 캐시를 붙이지 않고 빈을 따로 둔 이유는, 캐시가 스프링이 씌운 대리 객체를 거쳐야
 * 동작하기 때문이다. 같은 클래스 안에서 자기 메서드를 부르면 그 대리 객체를 지나지 않아 캐시가 걸리지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ActiveProductCounter {

    public static final String CACHE_NAME = "activeProductCount";

    private final ProductJpaRepository productJpaRepository;

    @Cacheable(CACHE_NAME)
    public long countActive() {
        return productJpaRepository.countActive();
    }
}
