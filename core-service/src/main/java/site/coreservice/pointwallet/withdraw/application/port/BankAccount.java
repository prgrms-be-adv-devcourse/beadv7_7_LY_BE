package site.coreservice.pointwallet.withdraw.application.port;

public record BankAccount(String bankName, String accountNumber, String depositorName) {
}