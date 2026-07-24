package site.memberservice.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import site.memberservice.member.domain.Address;
import site.memberservice.member.domain.Email;
import site.memberservice.member.domain.Member;
import site.memberservice.member.domain.PhoneNumber;
import site.memberservice.member.exception.MemberException;
import site.memberservice.util.NullAndBlankSource;

import java.util.stream.Stream;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Member Entity")
class MemberTest {

    @DisplayName("유효한 회원 정보를 입력하면 id가 null인 Member 객체가 생성된다.")
    @Test
    void createMember() {
        // Given
        final Email email = new Email("test@email.com");
        final String password = "testPw1234!";
        final String nickname = "tester";
        final String name = "tester";
        final PhoneNumber phoneNumber = new PhoneNumber("010-1234-5678");
        final Address address = new Address("06671", "서울특별시 서초구 반포대로 45", "4층(서초동, 명정빌딩)");

        // When
        final Member member = Member.create(
            email,
            password,
            nickname,
            name,
            phoneNumber,
            address
        );

        // Then
        assertThat(member.getId()).isNull();
    }

    @DisplayName("회원 이메일로 null을 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenInputEmailNull() {
        // Given
        final Email email = null;
        final String password = "testPw1234!";
        final String nickname = "tester";
        final String name = "tester";
        final PhoneNumber phoneNumber = new PhoneNumber("010-1234-5678");
        final Address address = new Address("06671", "서울특별시 서초구 반포대로 45", "4층(서초동, 명정빌딩)");

        // When & Then
        assertThatThrownBy(() -> Member.create(
            email,
            password,
            nickname,
            name,
            phoneNumber,
            address
        ))
            .isInstanceOf(MemberException.class)
            .hasMessage("회원 이메일은 null일 수 없습니다.");
    }

    @DisplayName("회원 비밀번호로 null 혹은 공백을 입력하면 예외가 발생한다.")
    @NullAndBlankSource
    @ParameterizedTest
    void throwExceptionWhenInputPasswordNullOrEmpty(final String password) {
        // Given
        final Email email = new Email("test@email.com");
        final String nickname = "tester";
        final String name = "tester";
        final PhoneNumber phoneNumber = new PhoneNumber("010-1234-5678");
        final Address address = new Address("06671", "서울특별시 서초구 반포대로 45", "4층(서초동, 명정빌딩)");

        // When & Then
        assertThatThrownBy(() -> Member.create(
            email,
            password,
            nickname,
            name,
            phoneNumber,
            address
        ))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("회원 비밀번호는 null 혹은 공백일 수 없습니다. input: %s", password));
    }

    @DisplayName("회원 닉네임으로 null 혹은 공백을 입력하면 예외가 발생한다.")
    @NullAndBlankSource
    @ParameterizedTest
    void throwExceptionWhenInputNicknameNullOrEmpty(final String nickname) {
        // Given
        final Email email = new Email("test@email.com");
        final String password = "testPw1234!";
        final String name = "tester";
        final PhoneNumber phoneNumber = new PhoneNumber("010-1234-5678");
        final Address address = new Address("06671", "서울특별시 서초구 반포대로 45", "4층(서초동, 명정빌딩)");

        // When & Then
        assertThatThrownBy(() -> Member.create(
            email,
            password,
            nickname,
            name,
            phoneNumber,
            address
        ))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("회원 닉네임은 null 혹은 공백일 수 없습니다. input: %s", nickname));
    }

    @DisplayName("유효하지 않은 길이의 닉네임을 입력하면 예외가 발생한다.")
    @ValueSource(strings = {"a", "abcdefg"})
    @ParameterizedTest
    void throwExceptionWhenInputInvalidLengthNickname(final String nickname) {
        // Given
        final Email email = new Email("test@email.com");
        final String password = "testPw1234!";
        final String name = "tester";
        final PhoneNumber phoneNumber = new PhoneNumber("010-1234-5678");
        final Address address = new Address("06671", "서울특별시 서초구 반포대로 45", "4층(서초동, 명정빌딩)");

        // When & Then
        assertThatThrownBy(() -> Member.create(
            email,
            password,
            nickname,
            name,
            phoneNumber,
            address
        ))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("회원 닉네임은 2 ~ 6 길이의 문자열만 가능합니다. input: %s", nickname));
    }

    @DisplayName("회원 실명으로 null 혹은 공백을 입력하면 예외가 발생한다.")
    @NullAndBlankSource
    @ParameterizedTest
    void throwExceptionWhenInputNameNullOrEmpty(final String name) {
        // Given
        final Email email = new Email("test@email.com");
        final String nickname = "tester";
        final String password = "testPw1234!";
        final PhoneNumber phoneNumber = new PhoneNumber("010-1234-5678");
        final Address address = new Address("06671", "서울특별시 서초구 반포대로 45", "4층(서초동, 명정빌딩)");

        // When & Then
        assertThatThrownBy(() -> Member.create(
            email,
            password,
            nickname,
            name,
            phoneNumber,
            address
        ))
            .isInstanceOf(MemberException.class)
            .hasMessage(format("회원 실명은 null 혹은 공백일 수 없습니다. input: %s", name));
    }

    @DisplayName("회원 전화번호로 null을 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenInputPhoneNumberNull() {
        // Given
        final Email email = new Email("test@email.com");
        final String password = "testPw1234!";
        final String nickname = "tester";
        final String name = "tester";
        final PhoneNumber phoneNumber = null;
        final Address address = new Address("06671", "서울특별시 서초구 반포대로 45", "4층(서초동, 명정빌딩)");

        // When & Then
        assertThatThrownBy(() -> Member.create(
            email,
            password,
            nickname,
            name,
            phoneNumber,
            address
        ))
            .isInstanceOf(MemberException.class)
            .hasMessage("회원 전화번호는 null일 수 없습니다.");
    }

    @DisplayName("회원 주소로 null을 입력하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenInputAddressNull() {
        // Given
        final Email email = new Email("test@email.com");
        final String password = "testPw1234!";
        final String nickname = "tester";
        final String name = "tester";
        final PhoneNumber phoneNumber = new PhoneNumber("010-1234-5678");
        final Address address = null;

        // When & Then
        assertThatThrownBy(() -> Member.create(
            email,
            password,
            nickname,
            name,
            phoneNumber,
            address
        ))
            .isInstanceOf(MemberException.class)
            .hasMessage("회원 주소는 null일 수 없습니다.");
    }

    @DisplayName("회원 비밀번호 조건 충족 여부를 반환한다.")
    @MethodSource("isValidPasswordTestInputAndExpect")
    @ParameterizedTest
    void isValidPassword(final String input, final boolean expect) {
        // When
        final boolean isValid = Member.isValidPassword(input);

        // Then
        assertThat(isValid).isEqualTo(expect);
    }

    private static Stream<Arguments> isValidPasswordTestInputAndExpect() {
        return Stream.of(
            // [1] 실패 케이스: 조건 미충족 (input, expected)
            Arguments.of("testerPw1234!", true),            // 1. 유효한 비밀번호 (정상 케이스)
            Arguments.of("P@ss1", false),                   // 2. 8자 미만 (5자)
            Arguments.of("Password123456789!@#$", false),   // 3. 16자 초과 (20자)
            Arguments.of("Password!!", false),              // 4. 숫자 누락
            Arguments.of("Password123", false),             // 5. 특수문자 누락
            Arguments.of("12345678!@#$", false)             // 6. 영문자 누락
        );
    }
}
