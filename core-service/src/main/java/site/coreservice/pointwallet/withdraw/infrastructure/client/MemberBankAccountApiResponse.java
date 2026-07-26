package site.coreservice.pointwallet.withdraw.infrastructure.client;
import com.fasterxml.jackson.annotation.JsonProperty;

// 회원 서비스 응답 필드가 account_number만 snake_case라 여기서만 흡수한다.
record MemberBankAccountApiResponse(
        String bankName,
        @JsonProperty("account_number") String accountNumber,
        String depositorName
) {
}