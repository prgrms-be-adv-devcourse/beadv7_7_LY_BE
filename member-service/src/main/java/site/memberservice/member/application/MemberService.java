package site.memberservice.member.application;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.common.crypto.KmsMacHasher;
import site.memberservice.member.application.dto.AddressDto;
import site.memberservice.member.application.dto.BankAccountDto;
import site.memberservice.member.application.dto.MemberProfileDto;
import site.memberservice.member.application.dto.MemberRegisterCommand;
import site.memberservice.member.application.dto.MemberRestrictionDto;
import site.memberservice.member.application.dto.RecordWinningBidOrderCancellationCommand;
import site.memberservice.member.application.dto.RestrictMemberCommand;
import site.memberservice.member.domain.Address;
import site.memberservice.member.domain.BankAccount;
import site.memberservice.member.domain.Email;
import site.memberservice.member.domain.Member;
import site.memberservice.member.domain.MemberRestriction;
import site.memberservice.member.domain.MemberViolationHistory;
import site.memberservice.member.domain.PhoneNumber;
import site.memberservice.member.domain.RestrictionType;
import site.memberservice.member.domain.repository.BankAccountRepository;
import site.memberservice.member.domain.repository.MemberRepository;
import site.memberservice.member.domain.repository.MemberRestrictionRepository;
import site.memberservice.member.domain.repository.MemberViolationHistoryRepository;
import site.memberservice.member.exception.MemberException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static site.memberservice.member.domain.ViolationType.WINNING_BID_ORDER_CANCELED;
import static site.memberservice.member.exception.MemberErrorCode.INVALID_MEMBER_INFO;
import static site.memberservice.member.exception.MemberErrorCode.INVALID_VIOLATION_HISTORY_REQUEST;
import static site.memberservice.member.exception.MemberErrorCode.MEMBER_BANK_ACCOUNT_NOT_FOUND;
import static site.memberservice.member.exception.MemberErrorCode.MEMBER_NOT_FOUND;

@RequiredArgsConstructor
@Service
public class MemberService {

    private static final int VIOLATION_HISTORY_LOOKBACK_DAYS = 30;
    private static final long RESTRICTION_THRESHOLD_COUNT = 2;
    private static final long AUCTION_BIDDING_RESTRICTION_PERIOD_DAYS = 7;
    private static final String AUCTION_BIDDING_RESTRICTION_REASON = "최근 30일간 낙찰 상품 주문 취소 3회 이상 누적";

    private final PasswordEncoder passwordEncoder;
    private final KmsMacHasher kmsMacHasher;
    private final MemberRepository memberRepository;
    private final BankAccountRepository bankAccountRepository;
    private final MemberRestrictionRepository memberRestrictionRepository;
    private final MemberViolationHistoryRepository memberViolationHistoryRepository;

    @Transactional
    public void register(final MemberRegisterCommand command) {
        final Email email = new Email(command.email());
        final String phoneNumberHash = kmsMacHasher.hash(command.phoneNumber());
        final PhoneNumber phoneNumber = new PhoneNumber(command.phoneNumber(), phoneNumberHash);
        final Address address = new Address(command.zipcode(), command.baseAddress(), command.detailAddress());

        validatePassword(command.password());
        validateDuplicateNickName(command.nickName());
        validateDuplicatePhoneNumber(phoneNumberHash);

        final String hashedPassword = passwordEncoder.encode(command.password());
        final Member createdMember = Member.create(
            email,
            hashedPassword,
            command.nickName(),
            command.name(),
            phoneNumber,
            address
        );

        memberRepository.save(createdMember);
    }

    private void validatePassword(final String password) {
        if (!Member.isValidPassword(password)) {
            throw new MemberException(INVALID_MEMBER_INFO, "비밀번호는 영문자, 숫자, 특수문자를 포함하여 8 ~ 16 길이의 문자열만 가능합니다.");
        }
    }

    private void validateDuplicateNickName(final String nickName) {
        if (memberRepository.existsByNickname(nickName)) {
            throw new MemberException(INVALID_MEMBER_INFO, format("이미 존재하는 회원 닉네임입니다. input: %s", nickName));
        }
    }

    private void validateDuplicatePhoneNumber(final String phoneNumberHash) {
        if (memberRepository.existsByPhoneNumberHash(phoneNumberHash)) {
            throw new MemberException(INVALID_MEMBER_INFO, "이미 존재하는 회원 전화번호입니다.");
        }
    }

    public AddressDto getMemberAddress(final Long memberId) {
        final Member member = getMember(memberId);
        return AddressDto.from(member.getAddress());
    }

    private Member getMember(final Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberException(MEMBER_NOT_FOUND, format("해당 id의 회원 정보가 존재하지 않습니다. input: %s", memberId)));
    }

    public Optional<Member> findMember(final Email email) {
        return memberRepository.findByEmail(email);
    }

    public MemberProfileDto getMemberProfile(final Long memberId) {
        final Member member = getMember(memberId);
        return MemberProfileDto.from(member);
    }

    // TODO : #102 파이널에서 은행 계좌 실명 조회 Open API를 적용해 구현할 예정
//    public void saveMemberBankAccount() {
//    }

    public BankAccountDto getMemberBankAccount(final Long memberId) {
        final Member member = getMember(memberId);
        final BankAccount bankAccount = bankAccountRepository.findByMember(member)
            .orElseThrow(() -> new MemberException(MEMBER_BANK_ACCOUNT_NOT_FOUND, format("회원 은행 계좌 정보가 존재하지 않습니다. memberId: %s", memberId)));

        return BankAccountDto.of(member, bankAccount);
    }

    @Transactional
    public void restrictMember(final RestrictMemberCommand command) {
        final Member member = getMember(command.memberId());
        final MemberRestriction memberRestriction = MemberRestriction.create(
            command.restrictionType(),
            command.reason(),
            command.restrictedAt(),
            command.restrictedUntil(),
            member
        );

        memberRestrictionRepository.save(memberRestriction);
    }

    public List<MemberRestrictionDto> getMemberRestrictions(final Long memberId) {
        final Member member = getMember(memberId);
        final List<MemberRestriction> activeRestrictions = memberRestrictionRepository.findActiveByMember(member, LocalDateTime.now());

        return activeRestrictions.stream()
            .collect(Collectors.toMap(
                MemberRestriction::getRestrictionType,
                Function.identity(),
                BinaryOperator.maxBy(Comparator.comparing(MemberRestriction::getRestrictedUntil))
            ))
            .values().stream()
            .map(MemberRestrictionDto::from)
            .toList();
    }

    @Transactional
    public void recordWinningBidOrderCancellation(final RecordWinningBidOrderCancellationCommand command) {
        final Member member = getMember(command.memberId());
        final LocalDateTime since = command.occurredAt().minusDays(VIOLATION_HISTORY_LOOKBACK_DAYS);

        if (memberViolationHistoryRepository.hasWinningBidOrderCancellationRecord(member, WINNING_BID_ORDER_CANCELED, command.orderId())) {
            throw new MemberException(INVALID_VIOLATION_HISTORY_REQUEST, String.format("이미 해당 주문에 대한 취소 행위 기록 요청이 들어왔습니다. input: %d", command.orderId()));
        }

        final long recentViolationCount = memberViolationHistoryRepository.countByMemberAndViolationTypeSince(
            member,
            WINNING_BID_ORDER_CANCELED,
            since
        );

        if (recentViolationCount >= RESTRICTION_THRESHOLD_COUNT) {
            final MemberRestriction memberRestriction = MemberRestriction.create(
                RestrictionType.AUCTION_BIDDING,
                AUCTION_BIDDING_RESTRICTION_REASON,
                command.occurredAt(),
                command.occurredAt().plusDays(AUCTION_BIDDING_RESTRICTION_PERIOD_DAYS),
                member
            );

            memberRestrictionRepository.save(memberRestriction);
        }

        final MemberViolationHistory memberViolationHistory = MemberViolationHistory.create(
            WINNING_BID_ORDER_CANCELED,
            command.occurredAt(),
            Map.of("orderId", command.orderId(), "auctionId", command.auctionId()),
            member
        );

        memberViolationHistoryRepository.save(memberViolationHistory);
    }
}
