package site.coreservice.auction.application.port;

import site.coreservice.auction.application.port.dto.ProductSnapshot;

public interface ProductPort {
    ProductSnapshot getProduct(Long productId);
}
