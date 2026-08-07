package site.auctionservice.application.port;

import site.auctionservice.application.port.dto.ProductDetail;
import site.auctionservice.application.port.dto.ProductSnapshot;

public interface ProductPort {
    ProductSnapshot getProduct(Long productId);

    ProductDetail getProductDetail(Long productId);
}
