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
import site.memberservice.auth.domain.LoginType;
import site.memberservice.auth.exception.AuthException;
import site.memberservice.auth.infrastructure.jwt.AuthTokenProviderImpl;
import site.memberservice.auth.infrastructure.redis.RefreshTokenStore;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // TODO : #80 반복되는 객체 생성은 Fixture 분리 고민

    private static final String TEST_SECRET_KEY = "testSecretKey12345678901234567890";
    private static final long TOKEN_VALID_TIME = 3600000L;

    private PasswordEncoder passwordEncoder;
    private MemberService memberService;
    private AuthTokenProvider authTokenProvider;
    private AuthService authService;
    private RefreshTokenStore refreshTokenStore;

    @BeforeEach
    void setUp() {
        this.memberService = Mockito.mock(MemberService.class);
        this.passwordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        this.authTokenProvider = new AuthTokenProviderImpl(
            TEST_SECRET_KEY,
            TEST_SECRET_KEY,
            TOKEN_VALID_TIME,
            TOKEN_VALID_TIME
        );
        this.refreshTokenStore = Mockito.mock(RefreshTokenStore.class);
        // 허가증이 부족해서 막히는 상황은 이 단위테스트의 관심사가 아니라, 넉넉하게 잡아둔다.
        final Semaphore argon2ConcurrencyLimiter = new Semaphore(100);

        authService = new AuthService(memberService, passwordEncoder, authTokenProvider, refreshTokenStore, argon2ConcurrencyLimiter);
    }

    private Member createTestMember() {
        return new Member(
            1727L,
            new Email("tester@email.com", "test-email-hash"),
            passwordEncoder.encode("testPw1234!"),
            "tester",
            "tester",
            new PhoneNumber("010-1234-5678", "test-phone-hash"),
            new Address("06671", "서울특별시 서초구 반포대로 45", "4층(서초동, 명정빌딩)")
        );
    }

    @DisplayName("유효한 이메일, 비밀번호로 로그인하면 인증 객체를 생성해 반환한다.")
    @Test
    void login() {
        // Given
        final Member member = createTestMember();

        given(memberService.findMemberCredentials(any()))
            .willReturn(Optional.of(new MemberCredentials(member.getId(), member.getPassword())));

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
        verify(refreshTokenStore).save(eq(member.getId()), eq(LoginType.NORMAL), eq(loginResult.refreshToken()), eq(TOKEN_VALID_TIME));
    }

    @DisplayName("공용 PC(로그인 유지 미사용)로 로그인하면 PUBLIC_PC 타입, 12시간 TTL로 저장된다.")
    @Test
    void loginOnPublicPcStoresWithPublicPcTypeAndTwelveHourTtl() {
        // Given
        final Member member = createTestMember();

        given(memberService.findMemberCredentials(any()))
            .willReturn(Optional.of(new MemberCredentials(member.getId(), member.getPassword())));

        final LoginCommand command = new LoginCommand(
            "tester@email.com",
            "testPw1234!",
            false
        );

        // When
        final LoginResult loginResult = authService.login(command);

        // Then
        verify(refreshTokenStore).save(
            eq(member.getId()), eq(LoginType.PUBLIC_PC), eq(loginResult.refreshToken()), eq(Duration.ofHours(12).toMillis())
        );

        final Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes()))
            .build()
            .parseSignedClaims(loginResult.refreshToken())
            .getPayload();
        final long validTime = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

        assertThat(validTime).isEqualTo(Duration.ofHours(12).toMillis());
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
        final Member member = createTestMember();

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

    @DisplayName("Redis에 저장된 값과 일치하는 리프레시 토큰으로 재발급을 요청하면 새 access token을 발급한다.")
    @Test
    void reissueAccessTokenSucceedsWhenTokenMatches() {
        // Given
        final Long memberId = 1727L;
        final String refreshTokenValue = authTokenProvider.createRefreshToken(memberId, LoginType.NORMAL).getValue();

        given(refreshTokenStore.matches(memberId, LoginType.NORMAL, refreshTokenValue)).willReturn(true);

        // When
        final String accessToken = authService.reissueAccessToken(refreshTokenValue);

        // Then
        assertThat(accessToken).isNotBlank();
    }

    @DisplayName("Redis에 저장된 값과 다른 리프레시 토큰으로 재발급을 요청하면 예외가 발생한다.")
    @Test
    void reissueAccessTokenThrowsWhenTokenDoesNotMatch() {
        // Given
        final Long memberId = 1727L;
        final String refreshTokenValue = authTokenProvider.createRefreshToken(memberId, LoginType.NORMAL).getValue();

        given(refreshTokenStore.matches(memberId, LoginType.NORMAL, refreshTokenValue)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.reissueAccessToken(refreshTokenValue))
            .isInstanceOf(AuthException.class)
            .hasMessage("유효하지 않은 리프레쉬 토큰 입니다.");
    }

    @DisplayName("정상적인 refreshToken 쿠키로 로그아웃하면 해당 회원/타입의 저장값을 제거한다.")
    @Test
    void logoutRemovesMatchingRefreshToken() {
        // Given
        final Long memberId = 1727L;
        final String refreshTokenValue = authTokenProvider.createRefreshToken(memberId, LoginType.PUBLIC_PC).getValue();

        // When
        authService.logout(memberId, refreshTokenValue);

        // Then
        verify(refreshTokenStore).remove(memberId, LoginType.PUBLIC_PC, refreshTokenValue);
    }

    @DisplayName("refreshToken 쿠키가 없으면 로그아웃은 성공하되 Redis에는 손대지 않는다.")
    @Test
    void logoutSkipsRemovalWhenRefreshTokenIsMissing() {
        // When
        authService.logout(1727L, null);

        // Then
        verify(refreshTokenStore, never()).remove(anyLong(), any(), any());
    }

    @DisplayName("refreshToken 쿠키 값이 손상되어 있으면 로그아웃은 성공하되 Redis에는 손대지 않는다.")
    @Test
    void logoutSkipsRemovalWhenRefreshTokenIsInvalid() {
        // When
        authService.logout(1727L, "garbage-value");

        // Then
        verify(refreshTokenStore, never()).remove(anyLong(), any(), any());
    }

    @DisplayName("refreshToken 쿠키의 소유자가 로그아웃 요청자와 다르면 Redis에는 손대지 않는다.")
    @Test
    void logoutSkipsRemovalWhenOwnerMismatches() {
        // Given
        final Long ownerId = 1727L;
        final Long requesterId = 9999L;
        final String refreshTokenValue = authTokenProvider.createRefreshToken(ownerId, LoginType.NORMAL).getValue();

        // When
        authService.logout(requesterId, refreshTokenValue);

        // Then
        verify(refreshTokenStore, never()).remove(anyLong(), any(), any());
    }
}
