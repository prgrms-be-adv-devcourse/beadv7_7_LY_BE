package site.coreservice.product.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.coreservice.product.application.dto.ProductDetailResult;
import site.coreservice.product.application.dto.ProductSnapshotResult;
import site.coreservice.product.domain.Artist;
import site.coreservice.product.domain.ArtistRepository;
import site.coreservice.product.domain.Product;
import site.coreservice.product.exception.ProductNotFoundException;
import site.coreservice.product.domain.ProductRepository;

/**
 * 카탈로그 조회 유스케이스. getProductSnapshot은 경매(07)·주문(06)이 소비하는 내부 계약으로,
 * 같은 core-service 안에서는 이 서비스를 직접 호출하는 것이 정식 창구다(internal HTTP는 어댑터).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ArtistRepository artistRepository;

    /** 공개 상세(1-3). 비활성 상품은 사용자에겐 없는 상품이므로 404로 취급한다. */
    public ProductDetailResult getActiveProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(ProductNotFoundException::new);
        return ProductDetailResult.of(product, getArtist(product.getArtistId()));
    }

    /** 내부 스냅샷 조회(명세 2-1의 getProduct). 비활성도 반환한다 — 소비자(경매)가 active로 등록 가능 여부를 직접 판단한다. */
    public ProductSnapshotResult getProductSnapshot(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);
        return ProductSnapshotResult.of(product, getArtist(product.getArtistId()));
    }

    private Artist getArtist(Long artistId) {
        return artistRepository.findById(artistId)
                .orElseThrow(() -> new IllegalStateException("상품이 참조하는 아티스트가 없습니다. artistId=" + artistId));
    }
}
