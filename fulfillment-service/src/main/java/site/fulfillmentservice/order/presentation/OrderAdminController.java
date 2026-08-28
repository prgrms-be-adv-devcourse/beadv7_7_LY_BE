package site.fulfillmentservice.order.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.fulfillmentservice.order.application.OrderService;
import site.fulfillmentservice.order.presentation.dto.OrderDetailResponse;
import site.fulfillmentservice.order.presentation.dto.OrderPageResponse;

@RestController
@RequestMapping("/api/admin/v1/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<OrderPageResponse> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(OrderPageResponse.from(orderService.findOrdersForAdmin(status, page, size)));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailResponse> getOrder(@PathVariable Long orderId) {
        return ApiResponse.success(OrderDetailResponse.from(orderService.getOrderDetailForAdmin(orderId)));
    }

    @PostMapping("/{orderId}/refund-approve")
    public ApiResponse<Void> approveRefund(@PathVariable Long orderId) {
        orderService.approveRefund(orderId);
        return ApiResponse.success();
    }

    @PostMapping("/{orderId}/refund-reject")
    public ApiResponse<Void> rejectRefund(@PathVariable Long orderId) {
        orderService.rejectRefund(orderId);
        return ApiResponse.success();
    }
}
