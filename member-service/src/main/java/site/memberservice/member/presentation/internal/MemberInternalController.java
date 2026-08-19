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
import site.memberservice.member.application.dto.BankAccountDto;
import site.memberservice.member.application.dto.MemberProfileDto;
import site.memberservice.member.application.dto.MemberRestrictionDto;
import site.memberservice.member.presentation.response.RestrictionsResponse;

import java.util.List;

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

    @GetMapping("/members/{memberId}/profile")
    public ResponseEntity<ApiResponse<MemberProfileDto>> getMemberProfile(@PathVariable final Long memberId) {
        final MemberProfileDto memberProfile = memberService.getMemberProfile(memberId);

        return ResponseEntity.ok(ApiResponse.success(memberProfile));
    }

    @GetMapping("/members/{memberId}/bank-account")
    public ResponseEntity<ApiResponse<BankAccountDto>> getMemberBankAccount(@PathVariable final Long memberId) {
        final BankAccountDto memberBankAccount = memberService.getMemberBankAccount(memberId);

        return ResponseEntity.ok(ApiResponse.success(memberBankAccount));
    }

    @GetMapping("/members/{memberId}/restrictions")
    public ResponseEntity<ApiResponse<RestrictionsResponse>> getMemberRestrictions(@PathVariable final Long memberId) {
        final List<MemberRestrictionDto> restrictions = memberService.getMemberRestrictions(memberId);

        return ResponseEntity.ok(ApiResponse.success(RestrictionsResponse.of(memberId, restrictions)));
    }
}
