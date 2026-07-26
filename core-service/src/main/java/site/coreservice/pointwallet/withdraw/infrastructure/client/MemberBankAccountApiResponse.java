package site.coreservice.pointwallet.withdraw.infrastructure.client;
import com.fasterxml.jackson.annotation.JsonProperty;


record MemberBankAccountApiResponse(
        String bankName,
        String accountNumber,
        String depositorName
) {
}