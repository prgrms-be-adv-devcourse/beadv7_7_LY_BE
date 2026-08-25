package site.pointwalletservice.wallet.deadletter.presentation.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import site.pointwalletservice.wallet.deadletter.domain.DeadLetterStatus;
import site.pointwalletservice.wallet.deadletter.domain.WithdrawFeeDeadLetter;

public record WithdrawFeeDeadLetterResponse(
        Long id,
        Long withdrawId,
        BigDecimal feeAmount,
        String causeMessage,
        DeadLetterStatus status,
        String resolvedNote,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
    public static WithdrawFeeDeadLetterResponse from(WithdrawFeeDeadLetter deadLetter) {
        return new WithdrawFeeDeadLetterResponse(
                deadLetter.getId(), deadLetter.getWithdrawId(), deadLetter.getFeeAmount(),
                deadLetter.getCauseMessage(), deadLetter.getStatus(), deadLetter.getResolvedNote(),
                deadLetter.getCreatedAt(), deadLetter.getResolvedAt()
        );
    }
}