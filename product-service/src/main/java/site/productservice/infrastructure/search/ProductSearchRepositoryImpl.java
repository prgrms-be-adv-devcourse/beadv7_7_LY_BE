package site.productservice.infrastructure.search;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import site.productservice.domain.PressType;
import site.productservice.domain.search.ProductSearchHit;
import site.productservice.domain.search.ProductSearchPage;
import site.productservice.domain.search.ProductSearchRepository;
import site.productservice.domain.search.SearchKeyword;
import site.productservice.infrastructure.ProductJpaRepository;

/**
 * LIKE 기반 검색 구현. 검색 SQL을 직접 조립한다 — 토큰 수만큼 조건이 늘어나는 동적 쿼리라
 * 정적 JPQL로는 표현할 수 없기 때문. 목록(findActivePage)은 기존 JPQL 경로를 그대로 쓴다.
 */
@Repository
@RequiredArgsConstructor
public class ProductSearchRepositoryImpl implements ProductSearchRepository {

    private final EntityManager entityManager;
    // 검색은 네이티브 SQL로 옮겼지만 findActivePage(카탈로그 목록)는 여전히 이 리포지토리를 쓴다
    private final ProductJpaRepository productJpaRepository;

    @Override
    public ProductSearchPage searchActiveByKeyword(SearchKeyword keyword, int page, int size) {
        Query query = entityManager.createNativeQuery(buildSearchSql(keyword.getTokens().size(), keyword.hasDigit()));
        bindKeywordParams(query, keyword);
        query.setParameter("limit", size);
        query.setParameter("offset", (long) page * size);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        // 행이 있으면 전체 건수가 각 행에 같이 실려 온다. 없으면(범위 밖 페이지) 건수만 따로 센다
        long total = rows.isEmpty() ? countByKeyword(keyword) : ((Number) rows.getFirst()[7]).longValue();
        List<ProductSearchHit> hits = rows.stream().map(this::toHit).toList();
        return new ProductSearchPage(hits, total);
    }

    @Override
    public ProductSearchPage findActivePage(int page, int size) {
        List<ProductSearchHit> hits = productJpaRepository.findActiveHits(PageRequest.of(page, size));
        long totalElements = productJpaRepository.countActive();
        return new ProductSearchPage(hits, totalElements);
    }

    /**
     * 두 갈래를 UNION ALL로 합친다.
     * 갈래 1(카탈로그): 번호 앞부분 일치 — 인덱스를 타는 유일한 경로라 다른 조건과 섞지 않는다.
     *   검색어에 숫자가 있을 때만 켠다. 번호에는 거의 항상 숫자가 있어서, 숫자 없는 검색어(jazz 등)가
     *   우연히 번호와 겹쳐 최상위를 차지하는 오염을 막는다 (실데이터에서 jazz로 시작하는 번호 51건 확인).
     * 갈래 2(텍스트): 토큰마다 (제목/아티스트/별칭 중 어디든) 걸리면 통과 — 전부 만족해야 함(AND).
     *   갈래 1에서 이미 잡힌 행은 제외해 중복을 막는다. 이 제외는 갈래 1이 켜져 있을 때만 넣는다 —
     *   갈래 1 없이 제외만 남으면 번호와 제목이 둘 다 걸리는 상품이 양쪽에서 다 빠져 결과에서 사라진다.
     * 바깥에서 순위 구간(rank_bucket)순 정렬 + 전체 건수를 한 번에 계산(COUNT OVER) —
     * 목록과 건수를 쿼리 두 번으로 나누면 같은 스캔을 두 번 하기 때문.
     */
    private String buildSearchSql(int tokenCount, boolean withCatalogArm) {
        return """
                SELECT t.id, t.title, t.artist_name, t.cover_image, t.release_year, t.press_type, t.release_country,
                       COUNT(*) OVER () AS total_count
                FROM (
                %s
                ) t
                ORDER BY t.rank_bucket, t.id
                LIMIT :limit OFFSET :offset
                """.formatted(buildMatchArms(tokenCount, withCatalogArm));
    }

    private String buildCountSql(int tokenCount, boolean withCatalogArm) {
        return """
                SELECT COUNT(*)
                FROM (
                %s
                ) t
                """.formatted(buildMatchArms(tokenCount, withCatalogArm));
    }

    // 갈래 1의 SELECT와 갈래 2의 제외 조건은 반드시 같은 withCatalogArm 값에서 함께 켜지고 꺼져야 한다
    private String buildMatchArms(int tokenCount, boolean withCatalogArm) {
        StringBuilder tokenWhere = new StringBuilder();
        for (int i = 0; i < tokenCount; i++) {
            tokenWhere.append("""
                      AND (p.normalized_title LIKE :tokContain%d
                           OR a.normalized_name LIKE :tokContain%d
                           OR EXISTS (SELECT 1 FROM product_alias pa
                                      WHERE pa.product_id = p.id AND pa.normalized_name LIKE :tokContain%d)
                           OR EXISTS (SELECT 1 FROM artist_alias aa
                                      WHERE aa.artist_id = p.artist_id AND aa.normalized_name LIKE :tokContain%d))
                    """.formatted(i, i, i, i));
        }
        String catalogExclusion = withCatalogArm ? """
                      AND (p.normalized_catalog_number IS NULL
                           OR p.normalized_catalog_number NOT LIKE :wholePrefix)
                """ : "";
        String textArm = """
                    SELECT p.id, p.title, a.name AS artist_name, p.cover_image,
                           p.release_year, p.press_type, p.release_country,
                           CASE WHEN p.normalized_title = :whole THEN 1
                                WHEN p.normalized_title LIKE :wholePrefix THEN 2
                                WHEN p.normalized_title LIKE :wholeContain THEN 3
                                ELSE 4 END AS rank_bucket
                    FROM product p JOIN artist a ON a.id = p.artist_id
                    WHERE p.active = true
                %s%s""".formatted(catalogExclusion, tokenWhere);
        if (!withCatalogArm) {
            return textArm;
        }
        return """
                    SELECT p.id, p.title, a.name AS artist_name, p.cover_image,
                           p.release_year, p.press_type, p.release_country, 0 AS rank_bucket
                    FROM product p JOIN artist a ON a.id = p.artist_id
                    WHERE p.active = true AND p.normalized_catalog_number LIKE :wholePrefix
                    UNION ALL
                %s""".formatted(textArm);
    }

    private long countByKeyword(SearchKeyword keyword) {
        Query query = entityManager.createNativeQuery(buildCountSql(keyword.getTokens().size(), keyword.hasDigit()));
        bindKeywordParams(query, keyword);
        return ((Number) query.getSingleResult()).longValue();
    }

    private void bindKeywordParams(Query query, SearchKeyword keyword) {
        query.setParameter("whole", keyword.getWhole());
        query.setParameter("wholePrefix", keyword.getWhole() + "%");
        query.setParameter("wholeContain", "%" + keyword.getWhole() + "%");
        for (int i = 0; i < keyword.getTokens().size(); i++) {
            query.setParameter("tokContain" + i, "%" + keyword.getTokens().get(i) + "%");
        }
    }

    private ProductSearchHit toHit(Object[] row) {
        return new ProductSearchHit(((Number) row[0]).longValue(), (String) row[1], (String) row[2],
                (String) row[3], ((Number) row[4]).intValue(), PressType.valueOf((String) row[5]),
                (String) row[6]);
    }
}
