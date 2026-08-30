package site.auctionservice.infrastructure.macro;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import site.auctionservice.application.port.BidReactionPort;
import site.auctionservice.common.AuctionRedisKeys;

/**
 * outbid 당했다가 재입찰하기까지 걸린 시간(반응속도)으로 매크로를 의심한다.
 * 락/트랜잭션과 무관하게 판단(차단 아님)에만 쓰이므로 placeBid() 진입 시 락 이전에 호출하고, 실패해도 입찰 자체는 계속 진행돼야 하므로 예외를 삼킨다(fail-open).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BidReactionTracker implements BidReactionPort {

    private static final int REACTION_SAMPLE_SIZE = 5; // 표본 부족 시 오탐 방지를 위해 판단 보류
    private static final long ABSOLUTE_SUSPICIOUS_REACTION_MS = 150L; // 편차와 무관하게 사람 반응시간 물리적 하한 아래
    private static final long REGULARITY_CHECK_MAX_AVG_MS = 2000L; // 이보다 느리면 아무리 일정해도 의심하지 않음(느리지만 습관적인 사람 배제)
    private static final double SUSPICIOUS_CV_THRESHOLD = 0.15; // 변동계수가 이 이하로 일정하면 기계적 패턴으로 간주
    private static final int SUSPICIOUS_REACTION_SCORE = 30;
    private static final Duration HISTORY_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final BidRiskScoreManager riskScoreManager; // 같은 infrastructure 계층 내부 협력이라 포트 우회 없이 구체 클래스로 직접 참조

    /** outbid 이력이 없으면(최초 입찰) 분석 대상이 아니므로 아무 것도 하지 않는다. */
    @Override
    public void recordReactionIfApplicable(Long auctionId, Long bidderId) {
        try {
            String markKey = AuctionRedisKeys.outbidMarkKey(auctionId, bidderId);
            String markedAt = redisTemplate.opsForValue().getAndDelete(markKey); // 조회와 동시에 소비(1회성)
            if (markedAt == null) {
                return;
            }

            long reactionMs = System.currentTimeMillis() - Long.parseLong(markedAt);

            String historyKey = AuctionRedisKeys.reactionHistoryKey(bidderId);
            redisTemplate.opsForList().leftPush(historyKey, String.valueOf(reactionMs));
            redisTemplate.opsForList().trim(historyKey, 0, REACTION_SAMPLE_SIZE - 1);
            redisTemplate.expire(historyKey, HISTORY_TTL);

            List<String> history = redisTemplate.opsForList().range(historyKey, 0, -1);
            if (history != null && history.size() >= REACTION_SAMPLE_SIZE && isSuspiciousPattern(history)) {
                riskScoreManager.addScore(bidderId, SUSPICIOUS_REACTION_SCORE);
            }
        } catch (RuntimeException e) {
            log.warn("반응속도 분석 실패 (입찰 자체엔 영향 없음): auctionId={}, bidderId={}", auctionId, bidderId, e);
        }
    }

    /**
     * 평균이 사람 반응시간 하한 아래로 확실히 빠르거나 그보다는 느려도 편차(CV) 없이 기계적으로 일정하면 의심 처리한다.
     * 평균만으로는 동시 요청 충돌 등 우연한 저지연 1건에도 흔들리지만 CV는 표본 전체가 일관되게 좁아야만 낮게 나와 그런 우연에 덜 흔들린다.
     */
    private boolean isSuspiciousPattern(List<String> history) {
        double[] intervals = history.stream().mapToLong(Long::parseLong).asDoubleStream().toArray();
        double avg = Arrays.stream(intervals).average().orElse(Double.MAX_VALUE);

        if (avg < ABSOLUTE_SUSPICIOUS_REACTION_MS) {
            return true;
        }
        if (avg >= REGULARITY_CHECK_MAX_AVG_MS) {
            return false;
        }

        double variance = Arrays.stream(intervals).map(v -> Math.pow(v - avg, 2)).average().orElse(0);
        double cv = Math.sqrt(variance) / avg;
        return cv < SUSPICIOUS_CV_THRESHOLD;
    }
}
