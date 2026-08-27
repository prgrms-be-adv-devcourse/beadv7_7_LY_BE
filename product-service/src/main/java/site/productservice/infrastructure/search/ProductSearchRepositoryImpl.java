package site.productservice.infrastructure.search;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.productservice.domain.PressType;
import site.productservice.domain.search.ProductSearchHit;
import site.productservice.domain.search.ProductSearchPage;
import site.productservice.domain.search.ProductSearchRepository;
import site.productservice.domain.search.SearchKeyword;

/**
 * LIKE 기반 검색 구현. 검색 SQL을 직접 조립한다 — 토큰 수만큼 조건이 늘어나는 동적 쿼리라
 * 정적 JPQL로는 표현할 수 없기 때문.
 */
@Repository
@RequiredArgsConstructor
public class ProductSearchRepositoryImpl implements ProductSearchRepository {

    private final EntityManager entityManager;

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

    /**
     * 두 갈래를 UNION ALL로 합친다.
     * 갈래 1(카탈로그): 번호 앞부분 일치 — 인덱스를 타는 유일한 경로라 다른 조건과 섞지 않는다.
     *   검색어에 숫자가 있을 때만 켠다. 번호에는 거의 항상 숫자가 있어서, 숫자 없는 검색어(jazz 등)가
     *   우연히 번호와 겹쳐 최상위를 차지하는 오염을 막는다 (실데이터에서 jazz로 시작하는 번호 51건 확인).
     * 갈래 2(텍스트): 토큰마다 (제목/아티스트/별칭 중 어디든) 걸리면 통과 — 전부 만족해야 함(AND).
     *   순위 구간: 제목·제목별칭 정확(1) > 아티스트 이름·별칭(2) > 제목·제목별칭 앞부분(3) >
     *   제목·제목별칭 부분(4) > 그 외(5 — 여러 단어가 서로 다른 필드에 나뉘어 걸린 경우가 여기 온다).
     *   제목 별칭을 제목과 같은 구간으로 두는 이유: 별칭은 같은 앨범을 부르는 다른 이름이라
     *   별칭을 그대로 친 검색은 제목을 친 것과 같은 지목이다. 각 구간의 별칭 확인은 컬럼 비교가
     *   거짓일 때만 실행되도록 OR 뒤에 둔다 (별칭 조회 단가는 행당 마이크로초 수준 실측).
     *   아티스트를 제목 앞부분보다도 위에 둔 이유는 인터페이스 주석 참조 (실데이터에서 coltrane·beatles
     *   검색 시 본인 앨범이 제목 우연 일치 앨범 수십~수백 건에 밀리는 문제 확인, 골든 Q03·Q23 —
     *   앞부분 일치 앨범만으로 top10이 차는 경우가 실재해 "부분보다 위"로는 부족했다).
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
                           CASE WHEN p.normalized_title = :whole
                                  OR EXISTS (SELECT 1 FROM product_alias re
                                             WHERE re.product_id = p.id
                                               AND re.normalized_name = :whole) THEN 1
                                WHEN (a.normalized_name LIKE :wholeContain
                                      OR EXISTS (SELECT 1 FROM artist_alias ra
                                                 WHERE ra.artist_id = p.artist_id
                                                   AND ra.normalized_name LIKE :wholeContain)) THEN 2
                                WHEN p.normalized_title LIKE :wholePrefix
                                  OR EXISTS (SELECT 1 FROM product_alias rp
                                             WHERE rp.product_id = p.id
                                               AND rp.normalized_name LIKE :wholePrefix) THEN 3
                                WHEN p.normalized_title LIKE :wholeContain
                                  OR EXISTS (SELECT 1 FROM product_alias rc
                                             WHERE rc.product_id = p.id
                                               AND rc.normalized_name LIKE :wholeContain) THEN 4
                                ELSE 5 END AS rank_bucket
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
