package site.memberservice.auth.presentation.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.memberservice.auth.application.AuthService;
import site.memberservice.auth.application.dto.LoginResult;
import site.memberservice.auth.presentation.request.LoginRequest;

@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@RestController
public class AuthApiController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResult>> login(@RequestBody LoginRequest request) {
        final LoginResult loginResult = authService.login(request.toCommand());

        return ResponseEntity.ok(ApiResponse.success(loginResult));
    }
}
