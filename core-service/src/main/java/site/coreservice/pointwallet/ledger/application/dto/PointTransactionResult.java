package site.coreservice.pointwallet.ledger.application.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import site.coreservice.pointwallet.ledger.domain.PointTransaction;
import site.coreservice.pointwallet.ledger.domain.PointTransactionType;

/** relatedId는 문맥에 따라 holdId·depositId 등을 가리키는 근원 애그리거트 id다 — auctionId 전용이 아님. */
public record PointTransactionResult(Long transactionId, String type, BigDecimal amount,
                                     Long relatedId, LocalDateTime occurredAt) {

    public static PointTransactionResult from(PointTransaction transaction) {
        BigDecimal rawAmount = transaction.getAmount().getValue();
        BigDecimal signedAmount = DEBIT_TYPES.contains(transaction.getType()) ? rawAmount.negate() : rawAmount;

        return new PointTransactionResult(
                transaction.getId(),
                transaction.getType().name(),
                signedAmount,
                transaction.getRelatedId(),
                transaction.getOccurredAt()
        );
    }

    private static final Set<PointTransactionType> DEBIT_TYPES =
            EnumSet.of(PointTransactionType.HOLD, PointTransactionType.DEPOSIT_CANCEL, PointTransactionType.WITHDRAW);
}