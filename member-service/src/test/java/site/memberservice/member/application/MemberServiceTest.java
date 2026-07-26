package site.memberservice.member.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import site.memberservice.member.application.dto.AddressDto;
import site.memberservice.member.application.dto.MemberProfileDto;
import site.memberservice.member.application.dto.MemberRegisterCommand;
import site.memberservice.member.domain.Address;
import site.memberservice.member.domain.Email;
import site.memberservice.member.domain.Member;
import site.memberservice.member.domain.PhoneNumber;
import site.memberservice.member.domain.repository.MemberRepository;
import site.memberservice.member.exception.MemberException;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Transactional
@SpringBootTest
class MemberServiceTest {

    // TODO : #60 반복되는 객체 생성은 Fixture 분리 고민

    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;

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
        final PhoneNumber phoneNumber = new PhoneNumber(request.phoneNumber());
        assertThat(memberRepository.existsByPhoneNumber(phoneNumber)).isTrue();
    }

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
    
    @DisplayName("이미 사용중인 회원 닉네임을 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenRegisterInputDuplicateNickname() {
        // Given
        final String duplicateNickname = "kelly";

        final Member oldMember = Member.create(
            new Email("test@email.com"),
            "testerPw1234!",
            duplicateNickname,
            "tester",
            new PhoneNumber("010-1234-5678"),
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

    @DisplayName("이미 사용중인 회원 전화번호를 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenRegisterInputDuplicatePhoneNumber() {
        // Given
        final String duplicatePhoneNumber = "010-1234-5678";

        final Member oldMember = Member.create(
            new Email("test@email.com"),
            "testerPw1234!",
            "test01",
            "tester",
            new PhoneNumber(duplicatePhoneNumber),
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
            .hasMessage(format("이미 존재하는 회원 전화번호입니다. input: %s", new PhoneNumber(duplicatePhoneNumber)));
    }

    @DisplayName("회원의 주소 정보를 조회한다.")
    @Test
    void getMemberAddress() {
        // Given
        final Member savedMember = memberRepository.save(
            Member.create(
                new Email("test@email.com"),
                "testerPw1234!",
                "tester",
                "tester",
                new PhoneNumber("010-1234-5678"),
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

    @DisplayName("회원 프로필을 조회한다.")
    @Test
    void getMemberProfile() {
        // Given
        final Member savedMember = memberRepository.save(
            Member.create(
                new Email("test@email.com"),
                "testerPw1234!",
                "tester",
                "tester",
                new PhoneNumber("010-1234-5678"),
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
}
