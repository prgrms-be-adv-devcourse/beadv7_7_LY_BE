package site.memberservice.auth.presentation.api;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import site.memberservice.auth.presentation.response.AccessTokenResponse;

import java.time.Duration;

@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@RestController
public class AuthApiController {

    private final AuthService authService;

    @Value("${app.jwt.refresh-token-expiration-time}")
    private long refreshTokenValidTime;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResult>> login(@RequestBody LoginRequest request) {
        final LoginResult loginResult = authService.login(request.toCommand());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", loginResult.refreshToken())
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(Duration.ofMillis(refreshTokenValidTime))
            .sameSite("Lax")
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(ApiResponse.success(loginResult));
    }

    @PostMapping("/renewal")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> renewal(
        @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        final String accessToken = authService.reissueAccessToken(refreshToken);

        return ResponseEntity.ok(ApiResponse.success(new AccessTokenResponse(accessToken)));
    }

    @DeleteMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@MemberId final Long memberId) {
        authService.logout(memberId);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(0)
            .sameSite("Lax")
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(ApiResponse.success());
    }
}
