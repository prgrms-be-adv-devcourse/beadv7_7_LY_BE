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
import site.memberservice.member.domain.repository.MemberCredentials;
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
        Email.validateFormat(command.email());
        PhoneNumber.validateFormat(command.phoneNumber());
        validatePassword(command.password());

        final String emailHash = kmsMacHasher.hash(command.email());
        final String phoneNumberHash = kmsMacHasher.hash(command.phoneNumber());
        final Email email = new Email(command.email(), emailHash);
        final PhoneNumber phoneNumber = new PhoneNumber(command.phoneNumber(), phoneNumberHash);
        final Address address = new Address(command.zipcode(), command.baseAddress(), command.detailAddress());

        validateDuplicateNickName(command.nickName());
        validateDuplicateEmail(emailHash);
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

    private void validateDuplicateEmail(final String emailHash) {
        if (memberRepository.existsByEmailHash(emailHash)) {
            throw new MemberException(INVALID_MEMBER_INFO, "이미 존재하는 회원 이메일입니다.");
        }
    }

    private void validateDuplicatePhoneNumber(final String phoneNumberHash) {
        if (memberRepository.existsByPhoneNumberHash(phoneNumberHash)) {
            throw new MemberException(INVALID_MEMBER_INFO, "이미 존재하는 회원 전화번호입니다.");
        }
    }

    public AddressDto getMemberAddress(final Long memberId) {
        return memberRepository.findAddressViewById(memberId)
            .map(AddressDto::from)
            .orElseThrow(() -> new MemberException(MEMBER_NOT_FOUND, format("해당 id의 회원 정보가 존재하지 않습니다. input: %s", memberId)));
    }

    private void requireMemberExists(final Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new MemberException(MEMBER_NOT_FOUND, format("해당 id의 회원 정보가 존재하지 않습니다. input: %s", memberId));
        }
    }

    public Optional<MemberCredentials> findMemberCredentials(final String email) {
        final String emailHash = kmsMacHasher.hash(email);
        return memberRepository.findCredentialsByEmailHash(emailHash);
    }

    public String getMemberNickname(final Long memberId) {
        return memberRepository.findNicknameById(memberId)
            .orElseThrow(() -> new MemberException(MEMBER_NOT_FOUND, format("해당 id의 회원 정보가 존재하지 않습니다. input: %s", memberId)));
    }

    public MemberProfileDto getMemberProfile(final Long memberId) {
        return memberRepository.findProfileById(memberId)
            .map(MemberProfileDto::from)
            .orElseThrow(() -> new MemberException(MEMBER_NOT_FOUND, format("해당 id의 회원 정보가 존재하지 않습니다. input: %s", memberId)));
    }

    // TODO : #102 파이널에서 은행 계좌 실명 조회 Open API를 적용해 구현할 예정
//    public void saveMemberBankAccount() {
//    }

    public BankAccountDto getMemberBankAccount(final Long memberId) {
        final String depositorName = memberRepository.findNameById(memberId)
            .orElseThrow(() -> new MemberException(MEMBER_NOT_FOUND, format("해당 id의 회원 정보가 존재하지 않습니다. input: %s", memberId)));
        final Member memberReference = memberRepository.getReferenceById(memberId);
        final BankAccount bankAccount = bankAccountRepository.findByMember(memberReference)
            .orElseThrow(() -> new MemberException(MEMBER_BANK_ACCOUNT_NOT_FOUND, format("회원 은행 계좌 정보가 존재하지 않습니다. memberId: %s", memberId)));

        return BankAccountDto.of(depositorName, bankAccount);
    }

    @Transactional
    public void restrictMember(final RestrictMemberCommand command) {
        requireMemberExists(command.memberId());
        final Member memberReference = memberRepository.getReferenceById(command.memberId());
        final MemberRestriction memberRestriction = MemberRestriction.create(
            command.restrictionType(),
            command.reason(),
            command.restrictedAt(),
            command.restrictedUntil(),
            memberReference
        );

        memberRestrictionRepository.save(memberRestriction);
    }

    public List<MemberRestrictionDto> getMemberRestrictions(final Long memberId) {
        requireMemberExists(memberId);
        final Member memberReference = memberRepository.getReferenceById(memberId);
        final List<MemberRestriction> activeRestrictions = memberRestrictionRepository.findActiveByMember(memberReference, LocalDateTime.now());

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
        requireMemberExists(command.memberId());
        final Member memberReference = memberRepository.getReferenceById(command.memberId());
        final LocalDateTime since = command.occurredAt().minusDays(VIOLATION_HISTORY_LOOKBACK_DAYS);

        if (memberViolationHistoryRepository.hasWinningBidOrderCancellationRecord(memberReference, WINNING_BID_ORDER_CANCELED, command.orderId())) {
            throw new MemberException(INVALID_VIOLATION_HISTORY_REQUEST, String.format("이미 해당 주문에 대한 취소 행위 기록 요청이 들어왔습니다. input: %d", command.orderId()));
        }

        final long recentViolationCount = memberViolationHistoryRepository.countByMemberAndViolationTypeSince(
            memberReference,
            WINNING_BID_ORDER_CANCELED,
            since
        );

        if (recentViolationCount >= RESTRICTION_THRESHOLD_COUNT) {
            final MemberRestriction memberRestriction = MemberRestriction.create(
                RestrictionType.AUCTION_BIDDING,
                AUCTION_BIDDING_RESTRICTION_REASON,
                command.occurredAt(),
                command.occurredAt().plusDays(AUCTION_BIDDING_RESTRICTION_PERIOD_DAYS),
                memberReference
            );

            memberRestrictionRepository.save(memberRestriction);
        }

        final MemberViolationHistory memberViolationHistory = MemberViolationHistory.create(
            WINNING_BID_ORDER_CANCELED,
            command.occurredAt(),
            Map.of("orderId", command.orderId(), "auctionId", command.auctionId()),
            memberReference
        );

        memberViolationHistoryRepository.save(memberViolationHistory);
    }
}
