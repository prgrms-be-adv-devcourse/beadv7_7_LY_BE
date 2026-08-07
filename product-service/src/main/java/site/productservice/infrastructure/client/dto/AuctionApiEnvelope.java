package site.productservice.infrastructure.client.dto;

/**
 * 경매 응답의 공통 봉투. common의 ApiResponse를 그대로 쓰지 않는 이유는 그쪽이 역직렬화가 불가능하기
 * 때문이다 — 생성자가 private이고 @JsonCreator가 없어 Jackson이 인스턴스를 만들 방법이 없다.
 * 레코드는 canonical 생성자를 Jackson이 자동으로 인식한다.
 */
public record AuctionApiEnvelope<T>(boolean success, T data, ApiError error) {
}
