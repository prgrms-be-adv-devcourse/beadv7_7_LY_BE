package site.coreservice.pointwallet.withdraw.infrastructure.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import site.common.response.ApiResponse;
import site.coreservice.pointwallet.withdraw.application.port.BankAccount;
import site.coreservice.pointwallet.withdraw.application.port.MemberBankAccountPort;

import java.util.Optional;

/**
 * 회원 서비스 계좌정보 조회 API 명세 기준으로 작성 (GET /internal/v1/members/{memberId}/bank-account).
 * 로컬에서는 MockMemberBankAccountClient로 대체.
 */
@Component
@Profile("!local")
public class MemberBankAccountHttpClient implements MemberBankAccountPort {

    private final RestClient memberRestClient;

    public MemberBankAccountHttpClient(@Qualifier("memberRestClient") RestClient memberRestClient) {
        this.memberRestClient = memberRestClient;
    }

    @Override
    public Optional<BankAccount> getBankAccount(Long memberId) {
        ApiResponse<MemberBankAccountApiResponse> body = memberRestClient.get()
                .uri("/internal/v1/members/{memberId}/bank-account", memberId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        MemberBankAccountApiResponse data = body.getData();
        if (data == null || data.accountNumber() == null || data.accountNumber().isBlank()) {
            return Optional.empty(); // 계좌 미등록 — 응답 스펙에 에러 케이스가 없어 방어적으로 처리
        }
        return Optional.of(new BankAccount(data.bankName(), data.accountNumber(), data.depositorName()));
    }
}