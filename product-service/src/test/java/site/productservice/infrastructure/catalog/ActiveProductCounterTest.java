package site.productservice.infrastructure.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import site.productservice.infrastructure.ProductJpaRepository;

/**
 * 캐시는 스프링이 씌운 대리 객체를 거쳐야 걸리므로 빈으로 꺼내 와서 확인한다.
 * 객체를 직접 만들어 부르면 애너테이션이 무시되어 이 테스트가 아무것도 검증하지 못한다.
 */
class ActiveProductCounterTest {

    private AnnotationConfigApplicationContext context;
    private ActiveProductCounter counter;
    private ProductJpaRepository productJpaRepository;
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(CachingTestConfig.class);
        counter = context.getBean(ActiveProductCounter.class);
        productJpaRepository = context.getBean(ProductJpaRepository.class);
        cacheManager = context.getBean(CacheManager.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    @DisplayName("여러 번 물어봐도 상품 수는 한 번만 센다")
    void countActive_두_번_호출해도_한_번만_조회() {
        // given
        given(productJpaRepository.countActive()).willReturn(435_319L);

        // when
        long first = counter.countActive();
        long second = counter.countActive();

        // then
        assertThat(first).isEqualTo(435_319L);
        assertThat(second).isEqualTo(435_319L);
        then(productJpaRepository).should(times(1)).countActive();
    }

    @Test
    @DisplayName("담아 둔 값을 비우면 다시 센다")
    void countActive_캐시를_비우면_다시_조회() {
        // given
        given(productJpaRepository.countActive()).willReturn(435_319L, 435_320L);
        counter.countActive();

        // when
        cacheManager.getCache(ActiveProductCounter.CACHE_NAME).clear();
        long afterClear = counter.countActive();

        // then
        assertThat(afterClear).isEqualTo(435_320L);
        then(productJpaRepository).should(times(2)).countActive();
    }

    @Configuration
    @EnableCaching
    static class CachingTestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(ActiveProductCounter.CACHE_NAME);
        }

        @Bean
        ProductJpaRepository productJpaRepository() {
            return mock(ProductJpaRepository.class);
        }

        @Bean
        ActiveProductCounter activeProductCounter(ProductJpaRepository productJpaRepository) {
            return new ActiveProductCounter(productJpaRepository);
        }
    }
}
