package site.memberservice.auth.presentation.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.common.web.MemberId;
import site.memberservice.auth.application.AuthService;
import site.memberservice.auth.application.dto.LoginResult;
import site.memberservice.auth.presentation.request.LoginRequest;
import site.memberservice.auth.presentation.support.AuthCookieProvider;

@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@RestController
public class AuthApiController {

    private final AuthService authService;
    private final AuthCookieProvider authCookieProvider;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(@RequestBody LoginRequest request) {
        final LoginResult loginResult = authService.login(request.toCommand());

        final ResponseCookie accessTokenCookie = authCookieProvider.createAccessTokenCookie(loginResult.accessToken());
        final ResponseCookie refreshTokenCookie = authCookieProvider.createRefreshTokenCookie(loginResult.refreshToken());

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
            .body(ApiResponse.success());
    }

    @PostMapping("/renewal")
    public ResponseEntity<ApiResponse<Void>> renewal(
        @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        final String accessToken = authService.reissueAccessToken(refreshToken);

        final ResponseCookie accessTokenCookie = authCookieProvider.createAccessTokenCookie(accessToken);

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
            .body(ApiResponse.success());
    }

    @DeleteMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@MemberId final Long memberId) {
        authService.logout(memberId);

        final ResponseCookie expiredAccessTokenCookie = authCookieProvider.expireAccessTokenCookie();
        final ResponseCookie expiredRefreshTokenCookie = authCookieProvider.expireRefreshTokenCookie();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, expiredAccessTokenCookie.toString())
            .header(HttpHeaders.SET_COOKIE, expiredRefreshTokenCookie.toString())
            .body(ApiResponse.success());
    }
}
