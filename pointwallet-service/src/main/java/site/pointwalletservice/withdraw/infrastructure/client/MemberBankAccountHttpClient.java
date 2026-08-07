package site.pointwalletservice.withdraw.infrastructure.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import site.common.response.ApiResponse;
import site.pointwalletservice.withdraw.application.port.BankAccount;
import site.pointwalletservice.withdraw.application.port.MemberBankAccountPort;

import java.util.Optional;

/**
 * 회원 서비스 계좌정보 조회 API 명세 기준으로 작성 (GET /internal/v1/members/{memberId}/bank-account).
 * 로컬에서는 MockMemberBankAccountClient로 대체.
 */
@Component
public class MemberBankAccountHttpClient implements MemberBankAccountPort {

    private final RestClient memberRestClient;

    public MemberBankAccountHttpClient(@Qualifier("memberRestClient") RestClient memberRestClient) {
        this.memberRestClient = memberRestClient;
    }

    @Override
    public Optional<BankAccount> getBankAccount(Long memberId) {
        try {
            ApiResponse<MemberBankAccountApiResponse> body = memberRestClient.get()
                    .uri("/internal/v1/members/{memberId}/bank-account", memberId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            MemberBankAccountApiResponse data = body.getData();
            return data == null ? Optional.empty()
                    : Optional.of(new BankAccount(data.bankName(), data.accountNumber(), data.depositorName()));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty(); // MERR-0003 계좌 미등록 - 정상 케이스
        }
    }
}