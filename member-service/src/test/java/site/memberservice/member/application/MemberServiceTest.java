package site.memberservice.member.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import site.common.crypto.KmsMacHasher;
import site.memberservice.member.application.dto.AddressDto;
import site.memberservice.member.application.dto.BankAccountDto;
import org.mockito.ArgumentCaptor;
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
import site.memberservice.member.domain.ViolationType;
import site.memberservice.member.domain.repository.BankAccountRepository;
import site.memberservice.member.domain.repository.MemberRepository;
import site.memberservice.member.domain.repository.MemberRestrictionRepository;
import site.memberservice.member.domain.repository.MemberViolationHistoryRepository;
import site.memberservice.member.exception.MemberException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    // TODO : #60 반복되는 객체 생성은 Fixture 분리 고민
    // TODO : #229 @Disabled 처리된 기존 테스트들을 Mock 기반으로 하나씩 재작성

    private PasswordEncoder passwordEncoder;
    private KmsMacHasher kmsMacHasher;
    private MemberRepository memberRepository;
    private BankAccountRepository bankAccountRepository;
    private MemberRestrictionRepository memberRestrictionRepository;
    private MemberViolationHistoryRepository memberViolationHistoryRepository;
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        this.passwordEncoder = Mockito.mock(PasswordEncoder.class);
        this.kmsMacHasher = Mockito.mock(KmsMacHasher.class);
        this.memberRepository = Mockito.mock(MemberRepository.class);
        this.bankAccountRepository = Mockito.mock(BankAccountRepository.class);
        this.memberRestrictionRepository = Mockito.mock(MemberRestrictionRepository.class);
        this.memberViolationHistoryRepository = Mockito.mock(MemberViolationHistoryRepository.class);

        memberService = new MemberService(
            passwordEncoder,
            kmsMacHasher,
            memberRepository,
            bankAccountRepository,
            memberRestrictionRepository,
            memberViolationHistoryRepository
        );
    }

    @Disabled("Mock 기반 테스트로 전환 예정 - #229")
    @DisplayName("회원 가입을 수행한다.")
    @Test
    void register() {
        // Given
        final MemberRegisterCommand request = new MemberRegisterCommand(
            "test@email.com",
            "testPw1234!",
            "tester",
            "tester",
            "010-1234-5678",
            "06671",
            "서울특별시 서초구 반포대로 45",
            "4층(서초동, 명정빌딩)"
        );

        // When
        memberService.register(request);

        // Then
        assertThat(memberRepository.existsByPhoneNumberHash(kmsMacHasher.hash(request.phoneNumber()))).isTrue();
    }

    @DisplayName("회원 가입 요청의 전화번호가 null이면 kmsMacHasher를 호출하지 않고 예외가 발생한다.")
    @Test
    void throwExceptionWhenRegisterInputNullPhoneNumber() {
        // Given
        final MemberRegisterCommand request = new MemberRegisterCommand(
            "test@email.com",
            "testPw1234!",
            "tester",
            "tester",
            null,
            "06671",
            "서울특별시 서초구 반포대로 45",
            "4층(서초동, 명정빌딩)"
        );

        // When & Then
        assertThatThrownBy(() -> memberService.register(request))
            .isInstanceOf(MemberException.class)
            .hasMessage("회원 전화번호는 null 혹은 공백일 수 없습니다. input : null");
        Mockito.verifyNoInteractions(kmsMacHasher);
    }

    @DisplayName("이미 사용중인 회원 이메일을 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenRegisterInputDuplicateEmail() {
        // Given
        final MemberRegisterCommand request = new MemberRegisterCommand(
            "test@email.com",
            "testerPw1234!",
            "tester",
            "tester",
            "010-1234-5678",
            "06671",
            "서울특별시 서초구 반포대로 45",
            "4층(서초동, 명정빌딩)"
        );

        given(kmsMacHasher.hash(request.email()))
            .willReturn("test-email-hash");
        given(kmsMacHasher.hash(request.phoneNumber()))
            .willReturn("test-phone-hash");
        given(memberRepository.existsByEmailHash("test-email-hash"))
            .willReturn(true);

        // When & Then
        assertThatThrownBy(() -> memberService.register(request))
            .isInstanceOf(MemberException.class)
            .hasMessage("이미 존재하는 회원 이메일입니다.");
    }

    @Disabled("Mock 기반 테스트로 전환 예정 - #229")
    @DisplayName("회원 가입 요청에 유효하지 않은 비밀번호를 입력하면 예외가 발생한다.")
    @ValueSource(strings = {
        "P@ss1",                  // 8자 미만
        "Password123456789!@#$",  // 16자 초과
        "Password!!",             // 숫자 누락
        "Password123",            // 특수문자 누락
        "12345678!@#$"            // 영문자 누락
    })
    @ParameterizedTest
    void throwExceptionWhenRegisterInputInvalidPassword(final String password) {
        // Given
        final MemberRegisterCommand request = new MemberRegisterCommand(
            "test@email.com",
            password,
            "tester",
            "tester",
            "010-1234-5678",
            "06671",
            "서울특별시 서초구 반포대로 45",
            "4층(서초동, 명정빌딩)"
        );

        // When & Then
        assertThatThrownBy(() -> memberService.register(request))
            .isInstanceOf(MemberException.class)
            .hasMessage("비밀번호는 영문자, 숫자, 특수문자를 포함하여 8 ~ 16 길이의 문자열만 가능합니다.");
    }

    @Disabled("Mock 기반 테스트로 전환 예정 - #229")
    @DisplayName("이미 사용중인 회원 닉네임을 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenRegisterInputDuplicateNickname() {
        // Given
        final String duplicateNickname = "kelly";

        final Member oldMember = Member.create(
            new Email("test@email.com", "test-email-hash"),
            "testerPw1234!",
            duplicateNickname,
            "tester",
            new PhoneNumber("010-1234-5678", "test-phone-hash"),
            new Address(
                "06671",
                "서울특별시 서초구 반포대로 45",
                "4층(서초동, 명정빌딩)"
            )
        );
        memberRepository.save(oldMember);

        final MemberRegisterCommand request = new MemberRegisterCommand(
            "test@email.com",
            "testerPw1234!",
            duplicateNickname,
            "tester",
            "010-5555-5555",
            "06671",
            "서울특별시 서초구 반포대로 45",
            "4층(서초동, 명정빌딩)"
        );

        // When & Then
        assertThatThrownBy(() -> memberService.register(request))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("이미 존재하는 회원 닉네임입니다. input: %s", duplicateNickname));
    }

    @Disabled("Mock 기반 테스트로 전환 예정 - #229")
    @DisplayName("이미 사용중인 회원 전화번호를 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenRegisterInputDuplicatePhoneNumber() {
        // Given
        final String duplicatePhoneNumber = "010-1234-5678";

        final Member oldMember = Member.create(
            new Email("test@email.com", "test-email-hash"),
            "testerPw1234!",
            "test01",
            "tester",
            new PhoneNumber(duplicatePhoneNumber, "test-phone-hash"),
            new Address(
                "06671",
                "서울특별시 서초구 반포대로 45",
                "4층(서초동, 명정빌딩)"
            )
        );
        memberRepository.save(oldMember);

        final MemberRegisterCommand request = new MemberRegisterCommand(
            "test@email.com",
            "testerPw1234!",
            "tester02",
            "tester",
            duplicatePhoneNumber,
            "06671",
            "서울특별시 서초구 반포대로 45",
            "4층(서초동, 명정빌딩)"
        );

        // When & Then
        assertThatThrownBy(() -> memberService.register(request))
            .isInstanceOf(MemberException.class)
            .hasMessage("이미 존재하는 회원 전화번호입니다.");
    }

    @Disabled("Mock 기반 테스트로 전환 예정 - #229")
    @DisplayName("회원의 주소 정보를 조회한다.")
    @Test
    void getMemberAddress() {
        // Given
        final Member savedMember = memberRepository.save(
            Member.create(
                new Email("test@email.com", "test-email-hash"),
                "testerPw1234!",
                "tester",
                "tester",
                new PhoneNumber("010-1234-5678", "test-phone-hash"),
                new Address(
                    "06671",
                    "서울특별시 서초구 반포대로 45",
                    "4층(서초동, 명정빌딩)"
                )
            )
        );

        // When
        final AddressDto result = memberService.getMemberAddress(savedMember.getId());

        // Then
        final Address savedMemberAddress = savedMember.getAddress();

        assertSoftly(softly -> {
            softly.assertThat(result.zipcode()).isEqualTo(savedMemberAddress.getZipcode());
            softly.assertThat(result.baseAddress()).isEqualTo(savedMemberAddress.getBaseAddress());
            softly.assertThat(result.detailAddress()).isEqualTo(savedMemberAddress.getDetailAddress());
        });
    }

    @Disabled("Mock 기반 테스트로 전환 예정 - #229")
    @DisplayName("회원의 주소 정보 조회에 존재하지 않는 회원 id를 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenGetMemberAddressNotFoundMemberId() {
        // Given
        final Long notFoundMemberId = -99999L;

        // When & Then
        assertThatThrownBy(() -> memberService.getMemberAddress(notFoundMemberId))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("해당 id의 회원 정보가 존재하지 않습니다. input: %s", notFoundMemberId));
    }

    @Disabled("Mock 기반 테스트로 전환 예정 - #229")
    @DisplayName("회원 프로필을 조회한다.")
    @Test
    void getMemberProfile() {
        // Given
        final Member savedMember = memberRepository.save(
            Member.create(
                new Email("test@email.com", "test-email-hash"),
                "testerPw1234!",
                "tester",
                "tester",
                new PhoneNumber("010-1234-5678", "test-phone-hash"),
                new Address(
                    "06671",
                    "서울특별시 서초구 반포대로 45",
                    "4층(서초동, 명정빌딩)"
                )
            )
        );

        // When
        final MemberProfileDto result = memberService.getMemberProfile(savedMember.getId());

        // Then
        assertSoftly(softly -> {
            softly.assertThat(result.email()).isEqualTo(savedMember.getEmail().getValue());
            softly.assertThat(result.nickname()).isEqualTo(savedMember.getNickname());
        });
    }

    @Disabled("Mock 기반 테스트로 전환 예정 - #229")
    @DisplayName("회원 프로필 조회에 존재하지 않는 회원 id를 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenGetMemberProfileNotFoundMemberId() {
        // Given
        final Long notFoundMemberId = -99999L;

        // When & Then
        assertThatThrownBy(() -> memberService.getMemberProfile(notFoundMemberId))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("해당 id의 회원 정보가 존재하지 않습니다. input: %s", notFoundMemberId));
    }

    @Disabled("Mock 기반 테스트로 전환 예정 - #229")
    @DisplayName("회원 계좌 정보를 조회한다.")
    @Test
    void getMemberBankAccount() {
        // Given
        final Member member = new Member(
            null,
            new Email("test@email.com", "test-email-hash"),
            "testPw1234!",
            "tester",
            "tester",
            new PhoneNumber("010-1234-5678", "test-phone-hash"),
            new Address(
                "06671",
                "서울특별시 서초구 반포대로 45",
                "4층(서초동, 명정빌딩)"
            )
        );
        final BankAccount bankAccount = new BankAccount(null, "110-123-456789", "켈리뱅크", member);

        memberRepository.save(member);
        bankAccountRepository.save(bankAccount);

        // When
        final BankAccountDto memberBankAccount = memberService.getMemberBankAccount(member.getId());

        // Then
        assertThat(memberBankAccount).isNotNull();
    }

    @Disabled("Mock 기반 테스트로 전환 예정 - #229")
    @DisplayName("회원 계좌 정보가 존재하지 않는 상태에서 조회를 시도하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenGetNotFoundBankAccount() {
        // Given
        final Member member = new Member(
            null,
            new Email("test@email.com", "test-email-hash"),
            "testPw1234!",
            "tester",
            "tester",
            new PhoneNumber("010-1234-5678", "test-phone-hash"),
            new Address(
                "06671",
                "서울특별시 서초구 반포대로 45",
                "4층(서초동, 명정빌딩)"
            )
        );

        memberRepository.save(member);

        // When & Then
        assertThatThrownBy(() -> memberService.getMemberBankAccount(member.getId()))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("회원 은행 계좌 정보가 존재하지 않습니다. memberId: %s", member.getId()));
    }

    @DisplayName("회원을 제재한다.")
    @Test
    void restrictMember() {
        // Given
        final Member member = new Member(
            1L,
            new Email("test@email.com", "test-email-hash"),
            "testPw1234!",
            "tester",
            "tester",
            new PhoneNumber("010-1234-5678", "test-phone-hash"),
            new Address(
                "06671",
                "서울특별시 서초구 반포대로 45",
                "4층(서초동, 명정빌딩)"
            )
        );
        final RestrictMemberCommand command = new RestrictMemberCommand(
            member.getId(),
            RestrictionType.AUCTION_BIDDING,
            "낙찰 후 미결제",
            LocalDateTime.of(2026, 8, 13, 0, 0),
            LocalDateTime.of(2026, 8, 20, 0, 0)
        );

        given(memberRepository.existsById(member.getId()))
            .willReturn(true);
        given(memberRepository.getReferenceById(member.getId()))
            .willReturn(member);

        // When
        memberService.restrictMember(command);

        // Then
        verify(memberRestrictionRepository).save(any(MemberRestriction.class));
    }

    @DisplayName("회원 제재 요청에 존재하지 않는 회원 id를 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenRestrictMemberInputNotFoundMemberId() {
        // Given
        final Long notFoundMemberId = -99999L;
        final RestrictMemberCommand command = new RestrictMemberCommand(
            notFoundMemberId,
            RestrictionType.AUCTION_BIDDING,
            "낙찰 후 미결제",
            LocalDateTime.of(2026, 8, 13, 0, 0),
            LocalDateTime.of(2026, 8, 20, 0, 0)
        );

        given(memberRepository.existsById(notFoundMemberId))
            .willReturn(false);

        // When & Then
        assertThatThrownBy(() -> memberService.restrictMember(command))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("해당 id의 회원 정보가 존재하지 않습니다. input: %s", notFoundMemberId));
    }

    @DisplayName("회원의 활성 제재 목록을 조회하면 같은 제재 유형 중 종료일이 가장 늦은 것만 반환한다.")
    @Test
    void getMemberRestrictions() {
        // Given
        final Member member = new Member(
            1L,
            new Email("test@email.com", "test-email-hash"),
            "testPw1234!",
            "tester",
            "tester",
            new PhoneNumber("010-1234-5678", "test-phone-hash"),
            new Address(
                "06671",
                "서울특별시 서초구 반포대로 45",
                "4층(서초동, 명정빌딩)"
            )
        );
        final MemberRestriction laterRestriction = MemberRestriction.create(
            RestrictionType.AUCTION_BIDDING,
            "낙찰 후 미결제",
            LocalDateTime.of(2026, 8, 13, 0, 0),
            LocalDateTime.of(2026, 8, 20, 0, 0),
            member
        );
        final MemberRestriction earlierRestriction = MemberRestriction.create(
            RestrictionType.AUCTION_BIDDING,
            "허위 매물 등록",
            LocalDateTime.of(2026, 8, 10, 0, 0),
            LocalDateTime.of(2026, 8, 15, 0, 0),
            member
        );

        given(memberRepository.existsById(member.getId()))
            .willReturn(true);
        given(memberRepository.getReferenceById(member.getId()))
            .willReturn(member);
        given(memberRestrictionRepository.findActiveByMember(any(Member.class), any(LocalDateTime.class)))
            .willReturn(List.of(laterRestriction, earlierRestriction));

        // When
        final List<MemberRestrictionDto> result = memberService.getMemberRestrictions(member.getId());

        // Then
        assertSoftly(softly -> {
            softly.assertThat(result).hasSize(1);
            softly.assertThat(result.get(0).restrictionType()).isEqualTo(RestrictionType.AUCTION_BIDDING);
            softly.assertThat(result.get(0).reason()).isEqualTo("낙찰 후 미결제");
            softly.assertThat(result.get(0).restrictedUntil()).isEqualTo(LocalDateTime.of(2026, 8, 20, 0, 0));
        });
    }

    @DisplayName("제재 이력 조회에 존재하지 않는 회원 id를 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenGetMemberRestrictionsInputNotFoundMemberId() {
        // Given
        final Long notFoundMemberId = -99999L;

        given(memberRepository.existsById(notFoundMemberId))
            .willReturn(false);

        // When & Then
        assertThatThrownBy(() -> memberService.getMemberRestrictions(notFoundMemberId))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("해당 id의 회원 정보가 존재하지 않습니다. input: %s", notFoundMemberId));
    }

    @DisplayName("낙찰 상품 주문 취소 시 최근 30일 이내 이력이 2회 미만이면 위반 이력만 저장하고 회원을 제재하지 않는다.")
    @Test
    void recordWinningBidOrderCancellationWithoutRestriction() {
        // Given
        final Member member = new Member(
            1L,
            new Email("test@email.com", "test-email-hash"),
            "testPw1234!",
            "tester",
            "tester",
            new PhoneNumber("010-1234-5678", "test-phone-hash"),
            new Address(
                "06671",
                "서울특별시 서초구 반포대로 45",
                "4층(서초동, 명정빌딩)"
            )
        );
        final RecordWinningBidOrderCancellationCommand command = new RecordWinningBidOrderCancellationCommand(
            member.getId(),
            10L,
            20L,
            LocalDateTime.of(2026, 8, 13, 0, 0)
        );

        given(memberRepository.existsById(member.getId()))
            .willReturn(true);
        given(memberRepository.getReferenceById(member.getId()))
            .willReturn(member);
        given(memberViolationHistoryRepository.countByMemberAndViolationTypeSince(
            member,
            ViolationType.WINNING_BID_ORDER_CANCELED,
            LocalDateTime.of(2026, 7, 14, 0, 0)
        ))
            .willReturn(1L);

        // When
        memberService.recordWinningBidOrderCancellation(command);

        // Then
        final ArgumentCaptor<MemberViolationHistory> captor = ArgumentCaptor.forClass(MemberViolationHistory.class);
        verify(memberViolationHistoryRepository).save(captor.capture());
        verify(memberRestrictionRepository, never()).save(any(MemberRestriction.class));

        final MemberViolationHistory savedHistory = captor.getValue();
        assertSoftly(softly -> {
            softly.assertThat(savedHistory.getViolationType()).isEqualTo(ViolationType.WINNING_BID_ORDER_CANCELED);
            softly.assertThat(savedHistory.getOccurredAt()).isEqualTo(command.occurredAt());
            softly.assertThat(savedHistory.getDetails()).isEqualTo(Map.of("orderId", command.orderId(), "auctionId", command.auctionId()));
            softly.assertThat(savedHistory.getMember()).isEqualTo(member);
        });
    }

    @DisplayName("낙찰 상품 주문 취소 시 최근 30일 이내 이력이 2회 이상이면 위반 이력을 저장하고 회원을 제재한다.")
    @Test
    void recordWinningBidOrderCancellationWithRestriction() {
        // Given
        final Member member = new Member(
            1L,
            new Email("test@email.com", "test-email-hash"),
            "testPw1234!",
            "tester",
            "tester",
            new PhoneNumber("010-1234-5678", "test-phone-hash"),
            new Address(
                "06671",
                "서울특별시 서초구 반포대로 45",
                "4층(서초동, 명정빌딩)"
            )
        );
        final RecordWinningBidOrderCancellationCommand command = new RecordWinningBidOrderCancellationCommand(
            member.getId(),
            10L,
            20L,
            LocalDateTime.of(2026, 8, 13, 0, 0)
        );

        given(memberRepository.existsById(member.getId()))
            .willReturn(true);
        given(memberRepository.getReferenceById(member.getId()))
            .willReturn(member);
        given(memberViolationHistoryRepository.countByMemberAndViolationTypeSince(
            member,
            ViolationType.WINNING_BID_ORDER_CANCELED,
            LocalDateTime.of(2026, 7, 14, 0, 0)
        ))
            .willReturn(2L);

        // When
        memberService.recordWinningBidOrderCancellation(command);

        // Then
        verify(memberViolationHistoryRepository).save(any(MemberViolationHistory.class));

        final ArgumentCaptor<MemberRestriction> captor = ArgumentCaptor.forClass(MemberRestriction.class);
        verify(memberRestrictionRepository).save(captor.capture());

        final MemberRestriction savedRestriction = captor.getValue();
        assertSoftly(softly -> {
            softly.assertThat(savedRestriction.getRestrictionType()).isEqualTo(RestrictionType.AUCTION_BIDDING);
            softly.assertThat(savedRestriction.getRestrictedAt()).isEqualTo(command.occurredAt());
            softly.assertThat(savedRestriction.getRestrictedUntil()).isEqualTo(command.occurredAt().plusDays(7));
            softly.assertThat(savedRestriction.getMember()).isEqualTo(member);
        });
    }

    @DisplayName("낙찰 상품 주문 취소 이력 저장 시 이미 해당 주문에 대한 취소 행위 기록이 존재하면 예외가 발생하고 이력을 저장하지 않는다.")
    @Test
    void throwExceptionWhenRecordWinningBidOrderCancellationInputDuplicateOrderId() {
        // Given
        final Member member = new Member(
            1L,
            new Email("test@email.com", "test-email-hash"),
            "testPw1234!",
            "tester",
            "tester",
            new PhoneNumber("010-1234-5678", "test-phone-hash"),
            new Address(
                "06671",
                "서울특별시 서초구 반포대로 45",
                "4층(서초동, 명정빌딩)"
            )
        );
        final RecordWinningBidOrderCancellationCommand command = new RecordWinningBidOrderCancellationCommand(
            member.getId(),
            10L,
            20L,
            LocalDateTime.of(2026, 8, 13, 0, 0)
        );

        given(memberRepository.existsById(member.getId()))
            .willReturn(true);
        given(memberRepository.getReferenceById(member.getId()))
            .willReturn(member);
        given(memberViolationHistoryRepository.hasWinningBidOrderCancellationRecord(
            member,
            ViolationType.WINNING_BID_ORDER_CANCELED,
            command.orderId()
        ))
            .willReturn(true);

        // When & Then
        assertThatThrownBy(() -> memberService.recordWinningBidOrderCancellation(command))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("이미 해당 주문에 대한 취소 행위 기록 요청이 들어왔습니다. input: %d", command.orderId()));

        verify(memberViolationHistoryRepository, never()).countByMemberAndViolationTypeSince(any(Member.class), any(ViolationType.class), any(LocalDateTime.class));
        verify(memberRestrictionRepository, never()).save(any(MemberRestriction.class));
        verify(memberViolationHistoryRepository, never()).save(any(MemberViolationHistory.class));
    }

    @DisplayName("낙찰 상품 주문 취소 이력 저장 시 존재하지 않는 회원 id를 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenRecordWinningBidOrderCancellationInputNotFoundMemberId() {
        // Given
        final Long notFoundMemberId = -99999L;
        final RecordWinningBidOrderCancellationCommand command = new RecordWinningBidOrderCancellationCommand(
            notFoundMemberId,
            10L,
            20L,
            LocalDateTime.of(2026, 8, 13, 0, 0)
        );

        given(memberRepository.existsById(notFoundMemberId))
            .willReturn(false);

        // When & Then
        assertThatThrownBy(() -> memberService.recordWinningBidOrderCancellation(command))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("해당 id의 회원 정보가 존재하지 않습니다. input: %s", notFoundMemberId));
    }
}
