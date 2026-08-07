package site.pointwalletservice.withdraw.application.port;

public interface MemberBankAccountPort {
    java.util.Optional<BankAccount> getBankAccount(Long memberId);
}