package site.memberservice.member.application.dto;

import site.memberservice.member.domain.BankAccount;

public record BankAccountDto(
    String bankName,
    String accountNumber,
    String depositorName
) {
    public static BankAccountDto of(final String depositorName, final BankAccount bankAccount) {
        return new BankAccountDto(
            bankAccount.getBankName(),
            bankAccount.getAccountNumber(),
            depositorName
        );
    }
}
