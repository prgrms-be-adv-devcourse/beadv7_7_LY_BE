package site.explorationservice.search.exception;

import site.common.exception.BusinessException;

/** 정해지지 않은 검색 대상으로 요청했을 때. common 핸들러가 400 + PERR-4002로 변환한다. */
public class UnsupportedSearchTargetException extends BusinessException {

    public UnsupportedSearchTargetException() {
        super(SearchErrorCode.SEARCH_TARGET_UNSUPPORTED);
    }
}
