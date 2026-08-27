package site.productservice.infrastructure.catalog;

import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.productservice.domain.catalog.CatalogItem;
import site.productservice.domain.catalog.CatalogPage;
import site.productservice.domain.catalog.CatalogRepository;
import site.productservice.infrastructure.ProductJpaRepository;

/**
 * 목록 조회 구현. Pageable을 쓰지 않고 EntityManager로 직접 질의한다 — PageRequest는 offset을
 * 페이지 크기에서 계산해서, 다음 페이지 확인용으로 한 건 더 읽으려 하면 시작 위치까지 같이 밀린다.
 */
@Repository
@RequiredArgsConstructor
public class CatalogRepositoryImpl implements CatalogRepository {

    private static final String ACTIVE_PAGE_JPQL = """
            select new site.productservice.domain.catalog.CatalogItem(
                    p.id, p.title, a.name, p.coverImage, p.releaseYear, p.pressType, p.releaseCountry)
            from Product p join Artist a on a.id = p.artistId
            where p.active = true
            order by p.id desc
            """;

    private final EntityManager entityManager;
    private final ProductJpaRepository productJpaRepository;

    /** 다음 페이지가 있는지 알아내려고 size보다 한 건 더 조회한다. 그 한 건은 판단에만 쓰고 버린다. */
    @Override
    public CatalogPage findActivePage(int page, int size) {
        // 시작 위치는 int로만 줄 수 있다. 곱이 그 범위를 넘으면 어차피 상품이 없는 자리이므로
        // 예외 대신 빈 페이지로 답한다 — 범위 밖 페이지를 다루는 방식이 아래와 같아야 한다
        long offset = (long) page * size;
        if (offset > Integer.MAX_VALUE) {
            return new CatalogPage(List.of(), productJpaRepository.countActive(), false);
        }

        List<CatalogItem> fetched = entityManager.createQuery(ACTIVE_PAGE_JPQL, CatalogItem.class)
                .setFirstResult((int) offset)
                .setMaxResults(size + 1)
                .getResultList();

        boolean hasNext = fetched.size() > size;
        List<CatalogItem> items = hasNext ? List.copyOf(fetched.subList(0, size)) : fetched;
        return new CatalogPage(items, productJpaRepository.countActive(), hasNext);
    }
}
