package site.productservice.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import site.productservice.application.dto.ProductDetailResult;
import site.productservice.application.dto.ProductListQuery;
import site.productservice.application.dto.ProductListResult;
import site.productservice.application.dto.ProductPageResult;
import site.productservice.application.dto.ProductSnapshotResult;
import site.productservice.application.port.AuctionOpenCountPort;
import site.productservice.domain.Artist;
import site.productservice.domain.ArtistAliasRepository;
import site.productservice.domain.ArtistRepository;
import site.productservice.domain.price.PriceHistory;
import site.productservice.domain.price.PriceHistoryRepository;
import site.productservice.domain.Product;
import site.productservice.domain.ProductAliasRepository;
import site.productservice.domain.search.ProductSearchHit;
import site.productservice.domain.search.ProductSearchPage;
import site.productservice.domain.search.ProductSearchRepository;
import site.productservice.exception.ProductNotFoundException;
import site.productservice.domain.ProductRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 상품 조회 서비스. getProductSnapshot은 경매(07)·주문(06)팀이 쓰는 내부 조회로,
 * 같은 core-service 안에서는 이 서비스를 직접 호출하는 것이 정식 경로다 (internal HTTP API는 이를 감싼 통로일 뿐).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final int LIST_DEFAULT_SIZE = 20;
    private static final int LIST_MAX_SIZE = 100;

    // 카탈로그 브라우징(사람이 보는 화면)과 다른 상수를 쓴다 — 이건 백필처럼 기계가 전체를 훑는 내부 API라
    // 한 번에 더 많이 가져가는 게 유리하다.
    private static final int PAGE_DEFAULT_SIZE = 100;
    private static final int PAGE_MAX_SIZE = 500;

    private final ProductRepository productRepository;
    private final ArtistRepository artistRepository;
    private final ProductSearchRepository productSearchRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final AuctionOpenCountPort auctionOpenCountPort;
    private final ProductAliasRepository productAliasRepository;
    private final ArtistAliasRepository artistAliasRepository;

    /** 공개 상세(1-3). 비활성 상품은 사용자에겐 없는 상품이므로 404로 취급한다. */
    public ProductDetailResult getActiveProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(ProductNotFoundException::new);
        return ProductDetailResult.of(product, getArtist(product.getArtistId()));
    }

    /** 내부 조회(명세 2-1의 getProduct). 비활성 상품도 반환한다 — 호출한 쪽(경매)이 active 값을 보고 등록 가능 여부를 직접 판단한다. */
    public ProductSnapshotResult getProductSnapshot(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);
        return ProductSnapshotResult.withoutAliases(product, getArtist(product.getArtistId()));
    }

    /** getProductSnapshot의 배치 버전. */
    public List<ProductSnapshotResult> getProductSnapshots(List<Long> productIds) {
        List<Product> products = productRepository.findAllByIds(productIds);
        Map<Long, Artist> artistsById = getArtists(products);
        return products.stream()
                .map(product -> ProductSnapshotResult.withoutAliases(product, artistsById.get(product.getArtistId())))
                .toList();
    }

    /**
     * id 오름차순으로 상품을 순회한다. 백필이 소비하는 창구다 — 판매 상태와 무관하게 전체를 훑어야 ES에
     * 실제 active 상태가 정확히 반영된다(비활성 상품을 추천에서 빼는 건 kNN 필터의 몫이지 여기서 걸러낼 일이
     * 아니다).
     * <p>
     * 다음 페이지 존재 여부를 알아내려고 size보다 한 건 더 조회한다 — findPage(위시리스트)와 같은 방식이다.
     */
    public ProductPageResult getProductPage(Long cursor, int size) {
        int pageSize = clampPageSize(size);
        List<Product> fetched = productRepository.findAllOrderByIdAfter(cursor, pageSize + 1);

        boolean hasNext = fetched.size() > pageSize;
        List<Product> products = hasNext ? fetched.subList(0, pageSize) : fetched;
        Map<Long, Artist> artistsById = getArtists(products);

        // 별칭은 검색 색인만 쓰므로 이 경로에서만 채운다
        List<Long> productIds = products.stream().map(Product::getId).toList();
        List<Long> artistIds = products.stream().map(Product::getArtistId).distinct().toList();
        Map<Long, List<String>> titleAliasesByProduct =
                productAliasRepository.findNamesByProductIds(productIds);
        Map<Long, List<String>> artistAliasesByArtist =
                artistAliasRepository.findNamesByArtistIds(artistIds);

        List<ProductSnapshotResult> items = products.stream()
                .map(product -> ProductSnapshotResult.of(
                        product,
                        artistsById.get(product.getArtistId()),
                        titleAliasesByProduct.getOrDefault(product.getId(), List.of()),
                        artistAliasesByArtist.getOrDefault(product.getArtistId(), List.of())))
                .toList();
        Long nextCursor = hasNext ? products.getLast().getId() : null;

        return new ProductPageResult(items, nextCursor, hasNext);
    }

    private int clampPageSize(int size) {
        if (size < 1) {
            return PAGE_DEFAULT_SIZE;
        }
        return Math.min(size, PAGE_MAX_SIZE);
    }

    private Artist getArtist(Long artistId) {
        return artistRepository.findById(artistId)
                .orElseThrow(() -> new IllegalStateException("상품이 참조하는 아티스트가 없습니다. artistId=" + artistId));
    }

    private Map<Long, Artist> getArtists(List<Product> products) {
        List<Long> artistIds = products.stream().map(Product::getArtistId).distinct().toList();
        Map<Long, Artist> artistsById = artistRepository.findAllByIds(artistIds).stream()
                .collect(Collectors.toMap(Artist::getId, Function.identity()));
        for (Product product : products) {
            if (!artistsById.containsKey(product.getArtistId())) {
                throw new IllegalStateException(
                        "상품이 참조하는 아티스트가 없습니다. artistId=" + product.getArtistId());
            }
        }
        return artistsById;
    }

    /**
     * 카탈로그 브라우징(스펙 2026-07-29). 검색어 없이 활성 상품 전체를 훑는 것이 기존 검색과의 차이다.
     * 검색은 ProductSearchService가 그대로 담당한다 — 여기에 검색어 매칭을 넣지 않는다.
     * <p>
     * 클래스에 걸린 읽기 트랜잭션을 여기서만 끈다. 중간에 경매 HTTP 호출이 끼어 있어서, 하나로 묶으면
     * 응답을 기다리는 3초 동안 DB 커넥션을 쥐고 있게 된다. 커넥션 풀은 기본 10개라 동시 10건이면
     * 카탈로그뿐 아니라 서비스 전체가 커넥션을 못 받는다. 조회 각각은 저장소가 자기 읽기 트랜잭션 안에서
     * 처리하므로 안전성은 그대로다 (open-in-view도 꺼져 있어 커넥션이 요청 끝까지 남지 않는다).
     * 대신 목록 쿼리와 건수 쿼리가 같은 시점을 보지 않아, 조회 도중 상품이 등록되면 totalElements가
     * 한 건 어긋날 수 있다 — 브라우징 화면에선 문제가 되지 않는 수준이라 받아들인다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ProductListResult getProductList(ProductListQuery query) {
        int page = Math.max(query.page(), 0);
        int size = clampListSize(query.size());
        ProductSearchPage searchPage = productSearchRepository.findActivePage(page, size);
        List<Long> productIds = searchPage.content().stream().map(ProductSearchHit::productId).toList();
        Map<Long, Long> lastTradedPrices = findLastTradedPrices(productIds);
        Optional<Map<Long, Long>> openAuctionCounts = findOpenAuctionCounts(productIds);

        List<ProductListResult.Item> items = searchPage.content().stream()
                .map(hit -> new ProductListResult.Item(hit.productId(), hit.title(), hit.artistName(),
                        hit.coverImageUrl(), hit.releaseYear(), hit.pressType(), hit.country(),
                        lastTradedPrices.get(hit.productId()),
                        openAuctionCounts.map(counts -> counts.getOrDefault(hit.productId(), 0L)).orElse(null)))
                .toList();
        return ProductListResult.of(items, page, size, searchPage.totalElements());
    }

    private Map<Long, Long> findLastTradedPrices(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return priceHistoryRepository.findLatestTrades(productIds).stream()
                .collect(Collectors.toMap(PriceHistory::getProductId, PriceHistory::getFinalPrice));
    }

    /** 실패하면 비어 있는 Optional — 경매 서비스 장애가 카탈로그 목록까지 죽이면 안 된다. */
    private Optional<Map<Long, Long>> findOpenAuctionCounts(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Optional.of(Map.of());
        }
        try {
            return Optional.of(auctionOpenCountPort.findOpenAuctionCounts(productIds));
        } catch (RuntimeException e) {
            log.warn("진행 중 경매 수 조회 실패 — 목록은 건수 없이 응답한다. productIds: {}", productIds, e);
            return Optional.empty();
        }
    }

    private int clampListSize(int size) {
        if (size < 1) {
            return LIST_DEFAULT_SIZE;
        }
        return Math.min(size, LIST_MAX_SIZE);
    }
}
