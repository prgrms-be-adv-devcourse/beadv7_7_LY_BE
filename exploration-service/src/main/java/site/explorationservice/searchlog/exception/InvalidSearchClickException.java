package site.explorationservice.searchlog.exception;

import site.common.exception.BusinessException;

/** 클릭 기록 요청에 검색 식별자나 상품 번호가 없거나 순위가 1보다 작을 때. common 핸들러가 400 + SLERR-6001로 변환한다. */
public class InvalidSearchClickException extends BusinessException {

    public InvalidSearchClickException() {
        super(SearchLogErrorCode.SEARCH_CLICK_INVALID);
    }
}
