package site.coreservice.auction.domain;

import lombok.NoArgsConstructor;

// 경매 관련 서비스 정책 상수
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class AuctionPolicy {

    // 경매 시작/마감 시각 설정 단위
    public static final int TIME_UNIT_MINUTES = 10;
}
