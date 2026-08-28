package site.explorationservice.searchlog.domain;

import java.time.Instant;
import site.explorationservice.searchlog.exception.InvalidSearchClickException;

/**
 * 검색 결과에서 무엇을 눌렀는지의 기록.
 * <p>
 * {@code rank}는 눌린 항목이 결과의 몇 번째였는지다. 1부터 세고 페이지를 넘어가도 이어 센다. 이 값이 있어야
 * "검색이 원하는 것을 위로 올렸나"를 사람의 판단 없이 계산할 수 있다.
 * <p>
 * 값 검증을 여기서 하는 이유는 잘못된 기록이 섞이면 나중에 분석할 때 걸러내야 하기 때문이다. 걸러내는 기준을
 * 그때 가서 정하는 것보다 들어오는 자리에서 막는 편이 확실하다.
 */
public record SearchClickLog(String searchId, Long productId, int rank, Instant clickedAt) {

    private static final int MIN_RANK = 1;

    public static SearchClickLog of(final String searchId, final Long productId, final Integer rank) {
        validate(searchId, productId, rank);
        return new SearchClickLog(searchId, productId, rank, Instant.now());
    }

    private static void validate(final String searchId, final Long productId, final Integer rank) {
        if (searchId == null || searchId.isBlank()) {
            throw new InvalidSearchClickException();
        }
        if (productId == null) {
            throw new InvalidSearchClickException();
        }
        if (rank == null || rank < MIN_RANK) {
            throw new InvalidSearchClickException();
        }
    }
}
