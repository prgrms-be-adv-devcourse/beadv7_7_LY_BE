package site.auctionservice.infrastructure.macro;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import site.auctionservice.common.AuctionRedisKeys;

/**
 * 매크로 의심 점수 저장소
 * 즉시 차단하지 않고 점수만 누적했다가 RateLimitAspect가 다음 요청부터 적용할 limit을 좁히는 데(adjustLimit) 쓴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BidRiskScoreManager {

    private static final Duration SCORE_TTL = Duration.ofMinutes(30); // 오탐으로 인한 영구 불이익 방지, 시간 지나면 자연 소멸
    private static final int HIGH_RISK_THRESHOLD = 80;
    private static final int MEDIUM_RISK_THRESHOLD = 40;

    private final StringRedisTemplate redisTemplate;

    public void addScore(Long bidderId, int delta) {
        String key = AuctionRedisKeys.riskScoreKey(bidderId);
        try {
            Long newScore = redisTemplate.opsForValue().increment(key, delta);
            if (newScore != null && newScore == delta) {
                redisTemplate.expire(key, SCORE_TTL); // 최초 생성 시에만 TTL 설정
            }
        } catch (RuntimeException e) {
            log.warn("위험 점수 갱신 실패 (매크로 탐지 신호 손실, 입찰 자체엔 영향 없음): bidderId={}", bidderId, e);
        }
    }

    /** 점수에 따라 rate limit의 허용치를 동적으로 조인다. Redis 장애 시 조이지 않고 baseLimit 그대로 통과시킨다(fail-open). */
    public int adjustLimit(int baseLimit, Long bidderId) {
        int score = getScore(bidderId);
        if (score >= HIGH_RISK_THRESHOLD) {
            return 1;
        }
        if (score >= MEDIUM_RISK_THRESHOLD) {
            return Math.max(1, baseLimit / 2);
        }
        return baseLimit;
    }

    private int getScore(Long bidderId) {
        String key = AuctionRedisKeys.riskScoreKey(bidderId);
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value == null ? 0 : Integer.parseInt(value);
        } catch (RuntimeException e) {
            log.warn("위험 점수 조회 실패로 fail-open(0점) 처리: bidderId={}", bidderId, e);
            return 0;
        }
    }
}
