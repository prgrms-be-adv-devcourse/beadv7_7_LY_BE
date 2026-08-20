package site.explorationservice.productindex.application.port;

import site.explorationservice.productindex.application.port.dto.ProductPage;

public interface ProductPort {

    /**
     * id 오름차순으로 순회한다(cursor가 null이면 처음부터). product-service의 백필용 내부 API를 그대로 반영한 계약이다. 판매 상태와 무관하게
     * 전부 내려온다 — 필터링은 소비하는 쪽이 아니라 kNN 조회 쪽의 몫이다.
     */
    ProductPage findAllProducts(Long cursor, int limit);
}
