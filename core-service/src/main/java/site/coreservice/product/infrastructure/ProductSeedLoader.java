package site.coreservice.product.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.product.domain.Artist;
import site.coreservice.product.domain.ArtistRepository;
import site.coreservice.product.domain.PressType;
import site.coreservice.product.domain.Product;
import site.coreservice.product.domain.ProductRepository;

import java.util.List;

/**
 * LP 릴리스 시드 로더. local 프로파일 + {@code product.seed.enabled=true}일 때만 동작한다(기본 OFF).
 * 커밋본은 플래그 미설정이라 팀원이 pull 받아 local로 켜도 이 시드는 돌지 않는다
 * (커밋본 ddl-auto=none이라 테이블도 없어, 게이트가 없으면 부팅이 깨진다).
 * 로컬 적재 시엔 application-local.yml(커밋하지 않는 변경)에 {@code product.seed.enabled: true}와
 * {@code ddl-auto: update}를 함께 둔다.
 * <p>
 * 멱등성: 자연키(정규화된_카탈로그넘버 + 포맷 + 발매국가)로 존재를 확인하고 없을 때만 적재하므로,
 * 카탈로그번호가 있는 행은 앱을 몇 번 재실행해도 중복이 안 쌓인다. D1 시드는 전부 카탈로그번호를 가진다.
 * <p>
 * 한계 — 카탈로그번호가 null이면 파생 쿼리가 '= NULL'이 되어 매칭이 안 되고(매 실행마다 재삽입),
 * MySQL의 NULL-distinct 특성상 유니크 제약도 이를 막지 못한다. 카탈로그번호 없는 상품의 dedup은
 * 폴백 자연키(정규화된_제목 + 아티스트 + 발매연도)와 함께 D3에서 다룬다. (D1엔 해당 행이 없어 무해)
 * <p>
 * 정규화 로직은 D3 작업이라, 여기서는 시드 전용 간이 정규화(소문자화 + 영숫자만)로
 * 정규화된 값을 직접 주입한다.
 */
@Slf4j
@Order(1)
@Profile("local")
@ConditionalOnProperty(prefix = "product.seed", name = "enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
public class ProductSeedLoader implements CommandLineRunner {

    private final ArtistRepository artistRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void run(String... args) {
        Long beatles = ensureArtist("The Beatles", List.of("비틀즈", "The Beatles", "Beatles"));
        Long pinkFloyd = ensureArtist("Pink Floyd", List.of("핑크 플로이드", "Pink Floyd"));
        Long milesDavis = ensureArtist("Miles Davis", List.of("마일스 데이비스", "Miles Davis"));

        ensureProduct("PCS 7088", beatles, "Abbey Road", "UK", 1969, PressType.ORIGINAL, "LP",
                "Apple Records", "Rock", null, "1969년 영국 오리지널 프레싱");
        ensureProduct("0602577915096", beatles, "Abbey Road", "UK", 2019, PressType.REISSUE, "180g",
                "Apple Records", "Rock", null, "2019년 50주년 리마스터 리이슈");
        ensureProduct("SHVL 804", pinkFloyd, "The Dark Side of the Moon", "UK", 1973, PressType.ORIGINAL, "LP",
                "Harvest", "Progressive Rock", null, "1973년 영국 오리지널 프레싱");
        ensureProduct("PFRLP8", pinkFloyd, "The Dark Side of the Moon", "Europe", 2016, PressType.REISSUE, "180g",
                "Pink Floyd Records", "Progressive Rock", null, "2016년 리마스터 리이슈");
        ensureProduct("CL 1355", milesDavis, "Kind of Blue", "US", 1959, PressType.ORIGINAL, "LP",
                "Columbia", "Jazz", null, "1959년 미국 오리지널 프레싱");

        log.info("[ProductSeedLoader] 시드 적재 완료 (멱등)");
    }

    private Long ensureArtist(String name, List<String> aliases) {
        String normalizedName = normalize(name);
        return artistRepository.findByNormalizedName(normalizedName)
                .orElseGet(() -> artistRepository.save(Artist.of(name, normalizedName, aliases)))
                .getId();
    }

    private void ensureProduct(String catalogNumber, Long artistId, String title, String releaseCountry,
            int releaseYear, PressType pressType, String format, String label, String genre, String coverImage,
            String description) {
        String normalizedCatalogNumber = normalize(catalogNumber);
        if (productRepository.findByNaturalKey(normalizedCatalogNumber, format, releaseCountry).isPresent()) {
            return;
        }
        productRepository.save(Product.of(catalogNumber, normalizedCatalogNumber, artistId, title, normalize(title),
                releaseCountry, releaseYear, pressType, format, label, genre, coverImage, description));
    }

    /** 시드 전용 간이 정규화. 실제 쓰기 모델 정규화 로직은 D3에서 구현한다. */
    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
