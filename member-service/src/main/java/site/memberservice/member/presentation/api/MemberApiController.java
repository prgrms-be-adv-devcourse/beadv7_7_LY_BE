package site.memberservice.member.presentation.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.common.web.MemberId;
import site.memberservice.member.application.MemberService;
import site.memberservice.member.application.dto.AddressDto;
import site.memberservice.member.application.dto.MemberProfileDto;
import site.memberservice.member.presentation.request.RegisterRequest;

@RequiredArgsConstructor
@RequestMapping("/api/v1")
@RestController
public class MemberApiController {

    private final MemberService memberService;

    @PostMapping("/members")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody RegisterRequest request) {
        memberService.register(request.toCommand());

        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/members/me")
    public ResponseEntity<ApiResponse<MemberProfileDto>> getMyProfile(@MemberId final Long memberId) {
        final MemberProfileDto memberProfile = memberService.getMemberProfile(memberId);

        return ResponseEntity.ok(ApiResponse.success(memberProfile));
    }

    @GetMapping("/members/address")
    public ResponseEntity<ApiResponse<AddressDto>> getMemberAddress(@MemberId final Long memberId) {
        final AddressDto memberAddress = memberService.getMemberAddress(memberId);

        return ResponseEntity.ok(ApiResponse.success(memberAddress));
    }
}
