package site.pointwalletservice.withdraw.domain.event;
import java.math.BigDecimal;

/**
 * 인출 수수료가 확정됐다는 사실만 담은 내부 이벤트. site.common.event.contract의 이벤트들과 달리
 * 이건 pointwallet-service 안에서만(withdraw가 발행 → wallet이 소비) 쓰이는 내부 신호라, 다른
 * 서비스가 참조하는 공용 계약 모듈(common)에는 넣지 않았다 - 거기 넣으면 서비스 내부 구현
 * 디테일이 공용 계약에 섞이게 된다.
 * <p>
 * 이 이벤트를 도입한 이유 - 모든 인출 요청이 PLATFORM_USER_ID 지갑 한 행을 공유해서, 요청
 * 트랜잭션 안에서 동기로 charge()를 부르면 동시 인출이 몰릴 때마다 락 경합이 발생한다(반면
 * 사용자 본인 지갑은 유저마다 갈려 있어 경합이 훨씬 드묾). 이미 도는 정산 배치에 얹으면 예치금
 * 도메인이 정산 배치 로직에 종속되고, 인출 전용 배치를 새로 두면 이중 배치가 된다. 그래서
 * 요청 경로에선 이벤트만 발행하고, 실제 플랫폼 계정 반영은 컨슈머 하나가 순차 소비하며 처리한다 -
 * 원장(PointTransaction)은 즉시, 잔액 반영은 비동기라는 원장/잔액 분리 패턴이다.
 */
public record WithdrawFeeEarnedEvent(Long withdrawId, BigDecimal feeAmount) {

    public static final String TOPIC = "pointwallet.withdraw-fee-earned";
}