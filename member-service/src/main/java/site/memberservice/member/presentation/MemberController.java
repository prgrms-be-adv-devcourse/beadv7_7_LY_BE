package site.memberservice.member.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.memberservice.member.application.MemberService;
import site.memberservice.member.presentation.request.RegisterRequest;

@RequiredArgsConstructor
@RequestMapping("/api/v1")
@RestController
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/members")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody RegisterRequest request) {
        memberService.register(request.toCommand());

        return ResponseEntity.ok(ApiResponse.success());
    }
}
