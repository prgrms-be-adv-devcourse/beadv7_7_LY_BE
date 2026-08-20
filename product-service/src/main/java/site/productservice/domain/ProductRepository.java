package site.productservice.domain;

import java.util.List;
import java.util.Optional;

/**
 * 상품 저장소 (도메인 인터페이스).
 * 구현체는 infrastructure의 ProductRepositoryImpl.
 */
public interface ProductRepository {

    Product save(Product product);

    /**
     * 중복 확인 기준(정규화 카탈로그번호 + 포맷 + 발매국가)으로 조회한다.
     * 시드를 여러 번 실행해도 같은 상품이 두 번 저장되지 않게 하는 데 쓴다.
     */
    Optional<Product> findByNaturalKey(String normalizedCatalogNumber, String format, String releaseCountry);

    /**
     * 카탈로그번호가 없는 상품의 중복 확인 기준(제목 + 아티스트 + 발매연도 + 발매국가 + 포맷 + 프레스구분)으로 조회한다.
     * 카탈로그번호가 있는 행은 비교 대상에서 제외한다 — 번호 있는 정품과 번호 없는 부틀렉은 제목이 같아도 다른 상품이기 때문.
     */
    Optional<Product> findByFallbackNaturalKey(String normalizedTitle, Long artistId, int releaseYear,
            String releaseCountry, String format, PressType pressType);

    Optional<Product> findById(Long id);

    List<Product> findAllByIds(List<Long> ids);

    /**
     * id 오름차순으로 순회한다(cursor가 null이면 처음부터). 상품 백필처럼 전체를 빠짐없이 훑을 때 쓴다 — 위시리스트
     * 커서 페이징과 달리 최신순이 아니라 처음부터 끝까지 도는 게 목적이라 오름차순이다.
     */
    List<Product> findAllOrderByIdAfter(Long cursor, int limit);
}
