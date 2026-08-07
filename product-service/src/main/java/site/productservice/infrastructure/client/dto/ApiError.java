package site.productservice.infrastructure.client.dto;

/** 실패 응답의 에러 정보. 404가 "경매 없음"인지 "경로가 틀렸는지" 가르는 데 쓴다. */
public record ApiError(String code, String message) {
}
