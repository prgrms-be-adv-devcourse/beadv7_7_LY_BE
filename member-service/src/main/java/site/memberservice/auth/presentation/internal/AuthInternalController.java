package site.memberservice.auth.presentation.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.memberservice.auth.application.AuthService;

@RequiredArgsConstructor
@RequestMapping("/internal/v1/auth")
@RestController
public class AuthInternalController {

    private final AuthService authService;

    @GetMapping("/validate-token")
    public ResponseEntity<ApiResponse<Void>> validateAuthToken(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) final String authToken) {
        final Long authenticatedMemberId = authService.validateAuthToken(authToken);

        return ResponseEntity.ok()
            .header("X-Member-Id", String.valueOf(authenticatedMemberId))
            .body(ApiResponse.success());
    }
}
