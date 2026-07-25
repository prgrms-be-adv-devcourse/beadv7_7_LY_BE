package site.coreservice.order.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.common.web.MemberId;
import site.coreservice.order.application.OrderService;
import site.coreservice.order.presentation.dto.OrderPlaceRequest;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/{orderId}/place")
    public ApiResponse<Void> placeOrder(@MemberId Long buyerId, @PathVariable Long orderId, @RequestBody OrderPlaceRequest request) {
        orderService.placeOrder(orderId, buyerId, request.toDeliveryInfo());
        return ApiResponse.success();
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<Void> cancelOrder(@MemberId Long buyerId, @PathVariable Long orderId) {
        orderService.cancelOrder(orderId, buyerId);
        return ApiResponse.success();
    }

    @PostMapping("/{orderId}/complete")
    public ApiResponse<Void> completeOrder(@MemberId Long buyerId, @PathVariable Long orderId) {
        orderService.completeOrder(orderId, buyerId);
        return ApiResponse.success();
    }
}
