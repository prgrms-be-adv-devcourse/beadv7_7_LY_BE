package site.auctionservice.infrastructure.macro;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import site.auctionservice.application.port.BidOutbidMarkPort;
import site.auctionservice.common.AuctionRedisKeys;

/**
 * outbid 발생 시각을 Redis에 마킹한다.
 * placeBid()가 락을 이미 반환하고 트랜잭션 커밋까지 성공을 확인한 뒤에만 호출되므로 롤백된 입찰에 대해 마킹이 남는 일이 없다
 * 마킹 실패는 매크로 탐지 신호 하나를 잃는 것일 뿐 입찰 자체엔 영향이 없어야 한다(fail-open).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BidOutbidMarker implements BidOutbidMarkPort {

    private static final Duration MARK_TTL = Duration.ofMinutes(5); // 5분 넘게 재입찰 없으면 의미 없는 기록이라 자동 소멸

    private final StringRedisTemplate redisTemplate;

    @Override
    public void markOutbid(Long auctionId, Long previousBidderId) {
        String key = AuctionRedisKeys.outbidMarkKey(auctionId, previousBidderId);
        try {
            redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()), MARK_TTL);
        } catch (DataAccessException e) {
            log.warn("outbid 기록 실패 (매크로 탐지 신호 손실, 입찰 자체엔 영향 없음): key={}", key, e);
        }
    }
}
