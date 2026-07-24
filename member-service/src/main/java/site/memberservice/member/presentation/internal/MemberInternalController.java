package site.memberservice.member.presentation.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.memberservice.member.application.MemberService;
import site.memberservice.member.application.dto.AddressDto;

@RequiredArgsConstructor
@RequestMapping("/internal/v1")
@RestController
public class MemberInternalController {

    private final MemberService memberService;

    @GetMapping("/members/{memberId}/address")
    public ResponseEntity<ApiResponse<AddressDto>> getMemberAddress(@PathVariable final Long memberId) {
        final AddressDto memberAddress = memberService.getMemberAddress(memberId);

        return ResponseEntity.ok(ApiResponse.success(memberAddress));
    }
}
