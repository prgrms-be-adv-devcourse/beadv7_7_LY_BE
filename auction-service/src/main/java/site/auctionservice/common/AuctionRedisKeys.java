package site.auctionservice.common;

// auction-service 전체가 공유하는 Redis 키 네임스페이싱 컨벤션: "auction:{type}:{identifier}".
// 이 서비스 안의 모든 리소스는 auction 바운디드 컨텍스트에 속하므로 prefix는 고정하고,
// type(lock, cache 등)과 identifier만 실제로 달라진다. 새 타입이 생기면 of()를 감싼
// 이름 있는 정적 메서드를 여기에 하나씩 추가한다(예: cacheKey).
public final class AuctionRedisKeys {

    private static final String PREFIX = "auction";
    private static final String LOCK_TYPE = "lock";
    private static final String RATE_LIMIT_TYPE = "antibot:ratelimit";
    private static final String OUTBID_MARK_TYPE = "antibot:outbidmark";
    private static final String REACTION_HISTORY_TYPE = "antibot:reaction";
    private static final String RISK_SCORE_TYPE = "antibot:risk";

    private AuctionRedisKeys() {
    }

    public static String of(String type, Object identifier) {
        return PREFIX + ":" + type + ":" + identifier;
    }

    public static String lockKey(Object identifier) {
        return of(LOCK_TYPE, identifier);
    }

    public static String rateLimitKey(String keyPrefix, Object resourceId, Object userId) {
        return of(RATE_LIMIT_TYPE, keyPrefix + ":" + resourceId + ":" + userId);
    }

    // outbid 당한 시각을 (경매, 이전 최고입찰자) 단위로 1회성 마킹 — 재입찰 시 반응속도 계산에 소비된다.
    public static String outbidMarkKey(Object auctionId, Object bidderId) {
        return of(OUTBID_MARK_TYPE, auctionId + ":" + bidderId);
    }

    // 반응속도 이력은 경매를 넘나드는 유저 전역 단위로 쌓는다(패턴 판단의 추가 표본)
    public static String reactionHistoryKey(Object bidderId) {
        return of(REACTION_HISTORY_TYPE, bidderId);
    }

    public static String riskScoreKey(Object bidderId) {
        return of(RISK_SCORE_TYPE, bidderId);
    }
}
