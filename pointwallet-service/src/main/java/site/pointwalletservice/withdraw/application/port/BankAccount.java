package site.pointwalletservice.withdraw.application.port;

public record BankAccount(String bankName, String accountNumber, String depositorName) {
}