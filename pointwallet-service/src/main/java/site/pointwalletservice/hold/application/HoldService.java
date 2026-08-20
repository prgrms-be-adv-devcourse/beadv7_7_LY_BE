package site.pointwalletservice.hold.application;
import site.pointwalletservice.shared.Money;

public interface HoldService {

    /**
     * 경매(auctionId)에 새 최고 입찰(userId, amount)을 홀드한다.
     * 이미 이 경매에 활성 홀드가 있으면(다른 유저의 이전 최고 입찰) 그 홀드를 먼저 해제하고 새로 홀드한다.
     */
    HoldResult hold(Long auctionId, Long userId, Money amount);

    /** 주문 취소 등으로 낙찰이 무산됐을 때 — 홀드를 지갑에 환원하고 소멸시킨다. */
    void release(Long auctionId);

    /** 거래 확정(정산 확정)됐을 때 — 이미 hold() 시점에 지갑에서 빠져나간 돈이라, 지갑을 추가로 건드리지 않고 홀드 레코드만 소멸시킨다. */
    void consume(Long auctionId);

    /**
     * auction-service가 hold() 호출 이후 자기 쪽 트랜잭션(입찰 저장 등)이 실패했을 때 부르는 보상 API.
     * release(auctionId)와 달리 "지금 그 경매에 걸려있는 홀드"가 아니라 hold() 응답으로 받은 holdId
     * 그 자체를 원장(PointTransaction) 기준으로 찾아서 되돌린다 — 그 사이 다른 입찰로 이미 교체됐거나
     * (원장에 RELEASE가 이미 있음) 이미 정산 소멸됐으면 손대지 않고 그대로 둔다. auctionId 기준으로
     * 하면 이미 교체된 "남의" 새 홀드를 잘못 풀어버릴 위험이 있어서 holdId 기준으로 분리했다.
     * <p>
     * auctionId/userId/amount는 요청 측(auction-service)이 자기가 hold() 호출 때 보냈던 값을 그대로
     * 실어 보내는 최소한의 검증용 파라미터다 - holdId 하나만 믿고 진행하지 않고, 서버가 원장·Hold로
     * 재구성한 실제 값과 대조해서 하나라도 다르면(HOLD_MISMATCH) 그 자리에서 거부한다. 불일치를
     * "원장을 더 뒤져서 그럴듯한 값 찾아 처리"하지 않는 이유 - 원장은 append-only라 유일하게
     * 특정할 방법이 없고, 돈이 움직이는 API에서 추측 매칭은 위험만 키운다.
     */
    void rollback(Long holdId, Long auctionId, Long userId, Money amount);
}