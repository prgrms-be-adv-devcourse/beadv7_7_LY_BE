package site.coreservice.pointwallet.withdraw.application.port;

public interface MemberBankAccountPort {
    java.util.Optional<BankAccount> getBankAccount(Long memberId);
}