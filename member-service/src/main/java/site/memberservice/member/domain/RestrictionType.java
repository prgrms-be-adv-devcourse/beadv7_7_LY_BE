package site.memberservice.member.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum RestrictionType {
    AUCTION_BIDDING("경매 입찰 제한"),
    ;

    private final String description;
}
