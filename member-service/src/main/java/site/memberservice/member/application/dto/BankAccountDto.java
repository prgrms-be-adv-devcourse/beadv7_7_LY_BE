package site.memberservice.member.application.dto;

import site.memberservice.member.domain.BankAccount;
import site.memberservice.member.domain.Member;

public record BankAccountDto(
    String bankName,
    String accountNumber,
    String depositorName
) {
    public static BankAccountDto of(final Member member, final BankAccount bankAccount) {
        return new BankAccountDto(
            bankAccount.getBankName(),
            bankAccount.getAccountNumber(),
            member.getName()
        );
    }
}
