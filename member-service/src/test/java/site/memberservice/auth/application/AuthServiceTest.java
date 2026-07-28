package site.memberservice.auth.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import site.memberservice.auth.application.dto.LoginCommand;
import site.memberservice.auth.application.dto.LoginResult;
import site.memberservice.auth.domain.AuthTokenProvider;
import site.memberservice.auth.exception.AuthException;
import site.memberservice.auth.infrastructure.jwt.AuthTokenProviderImpl;
import site.memberservice.member.application.MemberService;
import site.memberservice.member.domain.Address;
import site.memberservice.member.domain.Email;
import site.memberservice.member.domain.Member;
import site.memberservice.member.domain.PhoneNumber;

import java.util.Optional;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // TODO : #80 반복되는 객체 생성은 Fixture 분리 고민

    private PasswordEncoder passwordEncoder;
    private MemberService memberService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        this.memberService = Mockito.mock(MemberService.class);
        this.passwordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        AuthTokenProvider authTokenProvider = new AuthTokenProviderImpl("testSecretKey12345678901234567890", 3600000L);

        authService = new AuthService(memberService, passwordEncoder, authTokenProvider);
    }

    @DisplayName("유효한 이메일, 비밀번호로 로그인하면 인증 객체를 생성해 반환한다.")
    @Test
    void login() {
        // Given
        final Member member = new Member(
            1727L,
            new Email("tester@email.com"),
            passwordEncoder.encode("testPw1234!"),
            "tester",
            "tester",
            new PhoneNumber("010-1234-5678"),
            new Address("06671", "서울특별시 서초구 반포대로 45", "4층(서초동, 명정빌딩)")
        );

        given(memberService.findMember(any()))
            .willReturn(Optional.of(member));

        final LoginCommand command = new LoginCommand(
            new Email("tester@email.com"),
            "testPw1234!"
        );

        // When
        final LoginResult loginResult = authService.login(command);

        // Then
        assertSoftly(softly -> {
            assertThat(loginResult).isNotNull();
            assertThat(loginResult.authToken()).isNotBlank();
        });
    }

    @DisplayName("존재하지 않는 회원의 이메일로 로그인을 시도하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenLoginWithNotFoundMemberEmail() {
        // Given
        given(memberService.findMember(any()))
            .willReturn(Optional.empty());

        final LoginCommand command = new LoginCommand(
            new Email("no-member@email.com"),
            "testPw1234!"
        );

        // When & Then
        assertThatThrownBy(() -> authService.login(command))
            .isInstanceOf(AuthException.class)
            .hasMessage(format("존재하지 않는 회원의 이메일입니다. input: %s", command.email()));
    }

    @DisplayName("유효하지 않는 회원 비밀번호로 로그인을 시도하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenLoginWithInvalidPassword() {
        // Given
        final Member member = new Member(
            1727L,
            new Email("tester@email.com"),
            passwordEncoder.encode("testPw1234!"),
            "tester",
            "tester",
            new PhoneNumber("010-1234-5678"),
            new Address("06671", "서울특별시 서초구 반포대로 45", "4층(서초동, 명정빌딩)")
        );

        given(memberService.findMember(any()))
            .willReturn(Optional.of(member));

        final LoginCommand command = new LoginCommand(
            new Email("tester@email.com"),
            "noMemberPw1234"
        );

        // When & Then
        assertThatThrownBy(() -> authService.login(command))
            .isInstanceOf(AuthException.class)
            .hasMessage("유효하지 않는 회원 비밀번호입니다.");
    }
}
