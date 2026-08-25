package site.productservice.infrastructure.seed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.productservice.domain.Artist;
import site.productservice.domain.ArtistAlias;
import site.productservice.domain.ArtistAliasRepository;
import site.productservice.domain.ArtistRepository;
import site.productservice.domain.PressType;
import site.productservice.domain.Product;
import site.productservice.domain.ProductAlias;
import site.productservice.domain.ProductAliasRepository;
import site.productservice.domain.ProductRepository;
import site.common.text.TextNormalizer;

/**
 * LP 릴리스 시드 로더. local 프로파일 + {@code product.seed.enabled=true}일 때만 동작한다(기본 OFF).
 * 커밋본은 플래그 미설정이라 팀원이 pull 받아 local로 켜도 이 시드는 돌지 않는다.
 * 로컬 적재 시엔 application-local.yml(커밋하지 않는 변경)에 {@code product.seed.enabled: true}와
 * {@code ddl-auto: update}를 함께 둔다.
 * <p>
 * 무엇을 넣을지는 {@link ProductSeedData} 원장에 있고, 이 클래스는 그걸 순회하며 넣는 절차만 맡는다.
 * <p>
 * 여러 번 실행해도 안전하다. 확인 단위는 "자원 각각"이다 — 아티스트·상품은 이미 있으면 새로 만들지 않고
 * ID만 가져오며, 별칭은 소유자가 이미 있었더라도 별칭 자체의 존재를 따로 확인해 없는 것만 넣는다.
 * (아티스트가 있다고 별칭 넣기를 통째로 건너뛰면, 나중에 시드에 별칭을 추가해도 기존 DB엔 영영 반영되지 않는다)
 * <p>
 * 카탈로그번호가 없는 상품(부틀렉 등)은 번호 대신 (제목 + 아티스트 + 발매연도 + 발매국가 + 포맷 + 프레스구분)으로
 * 중복을 확인한다 — 번호 없는 행끼리만 비교한다.
 */
@Slf4j
@Order(1)
@Profile("local")
@ConditionalOnProperty(prefix = "product.seed", name = "enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
public class ProductSeedLoader implements CommandLineRunner {

    private final ArtistRepository artistRepository;
    private final ArtistAliasRepository artistAliasRepository;
    private final ProductRepository productRepository;
    private final ProductAliasRepository productAliasRepository;

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, Long> artistIdsByName = new HashMap<>();
        for (ProductSeedData.ArtistSeed artist : ProductSeedData.ARTISTS) {
            artistIdsByName.put(artist.name(), ensureArtist(artist.name(), artist.aliases()));
        }
        for (ProductSeedData.ProductSeed seed : ProductSeedData.PRODUCTS) {
            Long artistId = artistIdsByName.get(seed.artistName());
            if (artistId == null) {
                throw new IllegalStateException("시드 데이터 오류 — 아티스트 원장에 없는 이름: " + seed.artistName());
            }
            ensureProduct(seed.catalogNumber(), artistId, seed.title(), seed.titleAliases(), seed.releaseCountry(),
                    seed.releaseYear(), seed.pressType(), seed.format(), seed.label(), seed.genre(), null,
                    seed.description());
        }
        log.info("[ProductSeedLoader] 시드 적재 완료 — 아티스트 {}팀, 상품 {}건 (여러 번 실행해도 안전)",
                ProductSeedData.ARTISTS.size(), ProductSeedData.PRODUCTS.size());
    }

    private Long ensureArtist(String name, List<String> aliases) {
        Long artistId = artistRepository.findByNormalizedName(TextNormalizer.normalize(name))
                .orElseGet(() -> artistRepository.save(Artist.of(name)))
                .getId();
        for (String alias : aliases) {
            if (!artistAliasRepository.hasAlias(artistId, TextNormalizer.normalize(alias))) {
                artistAliasRepository.save(ArtistAlias.of(artistId, alias));
            }
        }
        return artistId;
    }

    private void ensureProduct(String catalogNumber, Long artistId, String title, List<String> titleAliases,
            String releaseCountry, int releaseYear, PressType pressType, String format, String label,
            String genre, String coverImage, String description) {
        Long productId = findExistingProduct(catalogNumber, artistId, title, releaseCountry, releaseYear,
                pressType, format)
                .orElseGet(() -> productRepository.save(Product.of(catalogNumber, artistId, title, releaseCountry,
                        releaseYear, pressType, format, label, genre, coverImage, description)))
                .getId();
        for (String alias : titleAliases) {
            if (!productAliasRepository.hasAlias(productId, TextNormalizer.normalize(alias))) {
                productAliasRepository.save(ProductAlias.of(productId, alias));
            }
        }
    }

    /** 카탈로그번호가 있으면 번호 기준으로, 없으면 번호 없는 행끼리의 예비 기준으로 기존 상품을 찾는다. */
    private Optional<Product> findExistingProduct(String catalogNumber, Long artistId, String title,
            String releaseCountry, int releaseYear, PressType pressType, String format) {
        String normalizedCatalogNumber = TextNormalizer.normalize(catalogNumber);
        if (normalizedCatalogNumber != null) {
            return productRepository.findByNaturalKey(normalizedCatalogNumber, format, releaseCountry);
        }
        return productRepository.findByFallbackNaturalKey(TextNormalizer.normalize(title), artistId, releaseYear,
                releaseCountry, format, pressType);
    }
}
