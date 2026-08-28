package site.auctionservice.common;

// auction-service 전체가 공유하는 Redis 키 네임스페이싱 컨벤션: "auction:{type}:{identifier}".
// 이 서비스 안의 모든 리소스는 auction 바운디드 컨텍스트에 속하므로 prefix는 고정하고,
// type(lock, cache 등)과 identifier만 실제로 달라진다. 새 타입이 생기면 of()를 감싼
// 이름 있는 정적 메서드를 여기에 하나씩 추가한다(예: cacheKey).
public final class AuctionRedisKeys {

    private static final String PREFIX = "auction";
    private static final String LOCK_TYPE = "lock";
    private static final String RATE_LIMIT_TYPE = "antibot:ratelimit";

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
}
