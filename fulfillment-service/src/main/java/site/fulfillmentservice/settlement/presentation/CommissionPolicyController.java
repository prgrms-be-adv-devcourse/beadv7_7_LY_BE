package site.fulfillmentservice.settlement.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.common.web.MemberId;
import site.fulfillmentservice.settlement.application.CommissionPolicyService;
import site.fulfillmentservice.settlement.presentation.dto.CommissionPolicyResponse;
import site.fulfillmentservice.settlement.presentation.dto.CreateCommissionPolicyRequest;

// TODO : 관리자 인증 없이 우선 열어둠. 관리자 인증 도입 시 보호 추가
@RestController
@RequestMapping("/api/admin/v1/settlements/commission-policies")
@RequiredArgsConstructor
public class CommissionPolicyController {

    private final CommissionPolicyService commissionPolicyService;

    @PostMapping
    public ApiResponse<CommissionPolicyResponse> createCommissionPolicy(
            @MemberId Long adminId,
            @RequestBody CreateCommissionPolicyRequest request
    ) {
        CommissionPolicyResponse response = CommissionPolicyResponse.from(
                commissionPolicyService.createCommissionPolicy(request.toCommand(), adminId));
        return ApiResponse.success(response);
    }
}
