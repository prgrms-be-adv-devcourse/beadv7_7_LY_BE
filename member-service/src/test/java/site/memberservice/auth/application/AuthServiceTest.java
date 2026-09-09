package site.memberservice.auth.application;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
import site.memberservice.auth.domain.RefreshToken;
import site.memberservice.auth.domain.repository.RefreshTokenRepository;
import site.memberservice.auth.exception.AuthException;
import site.memberservice.auth.infrastructure.jwt.AuthTokenProviderImpl;
import site.memberservice.member.application.MemberService;
import site.memberservice.member.domain.Address;
import site.memberservice.member.domain.Email;
import site.memberservice.member.domain.Member;
import site.memberservice.member.domain.PhoneNumber;
import site.memberservice.member.domain.repository.MemberCredentials;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Semaphore;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // TODO : #80 반복되는 객체 생성은 Fixture 분리 고민

    private static final String TEST_SECRET_KEY = "testSecretKey12345678901234567890";

    private PasswordEncoder passwordEncoder;
    private MemberService memberService;
    private AuthService authService;
    private RefreshTokenRepository refreshTokenRepository;
    private RefreshTokenIssuer refreshTokenIssuer;

    @BeforeEach
    void setUp() {
        this.memberService = Mockito.mock(MemberService.class);
        this.passwordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        AuthTokenProvider authTokenProvider = new AuthTokenProviderImpl(
            TEST_SECRET_KEY,
            TEST_SECRET_KEY,
            3600000L,
            3600000L
        );
        this.refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        this.refreshTokenIssuer = Mockito.mock(RefreshTokenIssuer.class);
        // 허가증이 부족해서 막히는 상황은 이 단위테스트의 관심사가 아니라, 넉넉하게 잡아둔다.
        final Semaphore argon2ConcurrencyLimiter = new Semaphore(100);

        authService = new AuthService(memberService, passwordEncoder, authTokenProvider, refreshTokenRepository, refreshTokenIssuer, argon2ConcurrencyLimiter);
    }

    @DisplayName("유효한 이메일, 비밀번호로 로그인하면 인증 객체를 생성해 반환한다.")
    @Test
    void login() {
        // Given
        final Member member = new Member(
            1727L,
            new Email("tester@email.com", "test-email-hash"),
            passwordEncoder.encode("testPw1234!"),
            "tester",
            "tester",
            new PhoneNumber("010-1234-5678", "test-phone-hash"),
            new Address("06671", "서울특별시 서초구 반포대로 45", "4층(서초동, 명정빌딩)")
        );
        final RefreshToken refreshToken = new RefreshToken(1L, "REFRESH_TOKEN_VALUE", member.getId());

        given(memberService.findMemberCredentials(any()))
            .willReturn(Optional.of(new MemberCredentials(member.getId(), member.getPassword())));
        given(refreshTokenIssuer.upsert(any(), any()))
            .willReturn(refreshToken);

        final LoginCommand command = new LoginCommand(
            "tester@email.com",
            "testPw1234!",
            true
        );

        // When
        final LoginResult loginResult = authService.login(command);

        // Then
        assertSoftly(softly -> {
            assertThat(loginResult).isNotNull();
            assertThat(loginResult.accessToken()).isNotBlank();
            assertThat(loginResult.refreshToken()).isNotBlank();
        });
    }

    @DisplayName("존재하지 않는 회원의 이메일로 로그인을 시도하면 예외가 발생한다.")
    @Test
    void throwExceptionWhenLoginWithNotFoundMemberEmail() {
        // Given
        given(memberService.findMemberCredentials(any()))
            .willReturn(Optional.empty());

        final LoginCommand command = new LoginCommand(
            "no-member@email.com",
            "testPw1234!",
            true
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
            new Email("tester@email.com", "test-email-hash"),
            passwordEncoder.encode("testPw1234!"),
            "tester",
            "tester",
            new PhoneNumber("010-1234-5678", "test-phone-hash"),
            new Address("06671", "서울특별시 서초구 반포대로 45", "4층(서초동, 명정빌딩)")
        );

        given(memberService.findMemberCredentials(any()))
            .willReturn(Optional.of(new MemberCredentials(member.getId(), member.getPassword())));

        final LoginCommand command = new LoginCommand(
            "tester@email.com",
            "noMemberPw1234",
            true
        );

        // When & Then
        assertThatThrownBy(() -> authService.login(command))
            .isInstanceOf(AuthException.class)
            .hasMessage("유효하지 않는 회원 비밀번호입니다.");
    }

    @DisplayName("공용 PC(로그인 유지 미사용)로 로그인하면 refresh token의 유효 기간이 12시간으로 제한된다.")
    @Test
    void loginOnPublicPcLimitsRefreshTokenValidTimeToTwelveHours() {
        // Given
        final Member member = new Member(
            1727L,
            new Email("tester@email.com", "test-email-hash"),
            passwordEncoder.encode("testPw1234!"),
            "tester",
            "tester",
            new PhoneNumber("010-1234-5678", "test-phone-hash"),
            new Address("06671", "서울특별시 서초구 반포대로 45", "4층(서초동, 명정빌딩)")
        );

        given(memberService.findMemberCredentials(any()))
            .willReturn(Optional.of(new MemberCredentials(member.getId(), member.getPassword())));
        given(refreshTokenIssuer.upsert(any(), any()))
            .willAnswer(invocation -> new RefreshToken(1L, invocation.getArgument(1), member.getId()));

        final LoginCommand command = new LoginCommand(
            "tester@email.com",
            "testPw1234!",
            false
        );

        // When
        final LoginResult loginResult = authService.login(command);

        // Then
        final Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes()))
            .build()
            .parseSignedClaims(loginResult.refreshToken())
            .getPayload();
        final long validTime = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

        assertThat(validTime).isEqualTo(Duration.ofHours(12).toMillis());
    }
}
