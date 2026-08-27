package site.auctionservice.application.port;

public interface MemberPort {
    String getNickname(Long memberId);
    boolean getMemberRestriction(Long memberId);
}
