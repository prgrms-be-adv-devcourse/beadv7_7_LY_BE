package site.coreservice.auction.application.port;

import site.coreservice.auction.application.port.dto.ProductDetail;
import site.coreservice.auction.application.port.dto.ProductSnapshot;

public interface ProductPort {
    ProductSnapshot getProduct(Long productId);

    ProductDetail getProductDetail(Long productId);
}
