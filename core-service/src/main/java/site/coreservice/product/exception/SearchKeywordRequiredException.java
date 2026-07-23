package site.coreservice.product.exception;

import site.common.exception.BusinessException;

/** 검색어(q) 없이 검색을 요청했을 때. common 핸들러가 400 + PERR-4001로 변환한다. */
public class SearchKeywordRequiredException extends BusinessException {

    public SearchKeywordRequiredException() {
        super(ProductErrorCode.SEARCH_KEYWORD_REQUIRED);
    }
}
