package site.explorationservice.productindex.application.port.dto;

import java.util.List;
import site.explorationservice.productindex.application.dto.ProductIndexCommand;

/**
 * product-service 목록 API 응답과 같은 모양이다({@code items, nextCursor, hasNext}) — 필드명이 그대로 일치해서 별도 전송용 타입
 * 없이 이 레코드를 바로 역직렬화 대상으로 쓴다. items도 ProductIndexCommand와 필드가 일치해 곧장 색인 입력으로 쓸 수 있다.
 */
public record ProductPage(List<ProductIndexCommand> items, Long nextCursor, boolean hasNext) {

}
