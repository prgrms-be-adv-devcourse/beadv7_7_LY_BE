package site.pointwalletservice.ledger.presentation;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.common.web.MemberId;
import site.pointwalletservice.ledger.application.PointTransactionService;
import site.pointwalletservice.ledger.presentation.dto.PointTransactionHistoryResponse;

@RestController
@RequestMapping("/api/v1/wallet/transactions")
@RequiredArgsConstructor
public class PointTransactionController {

    private final PointTransactionService pointTransactionService;

    @GetMapping
    public ApiResponse<PointTransactionHistoryResponse> getTransactions(
            @MemberId Long memberId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
                PointTransactionHistoryResponse.from(
                        pointTransactionService.findTransactions(memberId, type, from, to, page, size)));
    }
}