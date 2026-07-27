package site.coreservice.settlement.presentation;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.common.web.MemberId;
import site.coreservice.settlement.application.SettlementQueryService;
import site.coreservice.settlement.presentation.dto.SettlementBatchResponse;
import site.coreservice.settlement.presentation.dto.SettlementItemPageResponse;

@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementQueryService settlementQueryService;

    @GetMapping("/items")
    public ApiResponse<SettlementItemPageResponse> getItems(
            @MemberId Long sellerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
                SettlementItemPageResponse.from(
                        settlementQueryService.findItems(sellerId, status, from, to, page, size)));
    }

    @GetMapping("/batches")
    public ApiResponse<List<SettlementBatchResponse>> getBatches(@MemberId Long sellerId) {
        List<SettlementBatchResponse> batches = settlementQueryService.findBatches(sellerId).stream()
                .map(SettlementBatchResponse::from)
                .toList();
        return ApiResponse.success(batches);
    }
}
