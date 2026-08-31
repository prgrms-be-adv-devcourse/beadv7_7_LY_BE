package site.productservice.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * 캐시 기능을 켠다. 이 설정이 없으면 @Cacheable을 붙여도 아무 일이 일어나지 않고 매번 원래 메서드가 실행된다.
 * <p>
 * 애플리케이션 클래스가 아니라 별도 설정 클래스에 두는 이유는, 애플리케이션 클래스에 붙이면 데이터베이스 조각만
 * 띄우는 테스트(@DataJpaTest)에도 함께 켜지기 때문이다. 그런 테스트에는 캐시 저장소가 만들어지지 않아
 * 컨텍스트가 뜨지 않는다. 설정 클래스로 빼면 그 테스트들이 이 클래스를 읽지 않으므로 영향을 받지 않는다.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
