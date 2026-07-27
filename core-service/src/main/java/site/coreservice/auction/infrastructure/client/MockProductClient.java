package site.coreservice.auction.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import site.coreservice.auction.application.port.ProductPort;
import site.coreservice.auction.application.port.dto.ProductDetail;
import site.coreservice.auction.application.port.dto.ProductSnapshot;


@Component
@Profile("local")
@RequiredArgsConstructor
public class MockProductClient implements ProductPort {

    @Override
    public ProductSnapshot getProduct(Long productId) {
        return new ProductSnapshot(productId, "Abbey Road", "The Beatles", 1969, "Rock", "ORIGINAL", true);
    }

    @Override
    public ProductDetail getProductDetail(Long productId) {
        return new ProductDetail(productId, "Abbey Road", "The Beatles", "https://cdn.../55.jpg", 1969, "Rock", "ORIGINAL", true);
    }

}
