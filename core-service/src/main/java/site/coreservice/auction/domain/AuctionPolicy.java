package site.coreservice.auction.domain;

import lombok.NoArgsConstructor;

// 경매 관련 서비스 정책 상수
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class AuctionPolicy {

    // 경매 시작/마감 시각 설정 단위
    public static final int TIME_UNIT_MINUTES = 10;

    // 경매 연장 최소 시간 단위
    public static final int MIN_EXTENSION_MINUTES = 1;

    // 경매 최소 지속 시간
    public static final int MIN_DURATION_HOURS = 1;

    // 경매 최소 시작가
    public static final Money MIN_START_PRICE = Money.of(1000L);

    // 경매 최소 입찰 단위
    public static final Money MIN_BID_UNIT = Money.of(100L);

    // 최소 상품 설명 길이
    public static final int MIN_DESC_LENGTH = 10;

    // 최대 상품 설명 길이
    public static final int MAX_DESC_LENGTH = 500;

    //최대 이미지 업로드 수
    public static final int MAX_IMAGE_COUNT = 5;
}
