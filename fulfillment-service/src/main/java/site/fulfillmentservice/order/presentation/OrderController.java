package site.fulfillmentservice.order.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.common.web.MemberId;
import site.fulfillmentservice.order.application.OrderService;
import site.fulfillmentservice.order.presentation.dto.OrderDetailResponse;
import site.fulfillmentservice.order.presentation.dto.OrderPageResponse;
import site.fulfillmentservice.order.presentation.dto.OrderPlaceRequest;
import site.fulfillmentservice.order.presentation.dto.RefundRequest;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/{orderId}/place")
    public ApiResponse<Void> placeOrder(@MemberId Long buyerId, @PathVariable Long orderId,
                                        @RequestBody OrderPlaceRequest request) {
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

    @PostMapping("/{orderId}/refund-request")
    public ApiResponse<Void> requestRefund(@MemberId Long buyerId, @PathVariable Long orderId,
                                           @RequestBody RefundRequest request) {
        orderService.requestRefund(orderId, buyerId, request.toCommand());
        return ApiResponse.success();
    }

    // TODO : #209 관리자 인증 없이 우선 열어둠. 관리자 인증 도입 시 보호 추가
    @PostMapping("/{orderId}/refund-approve")
    public ApiResponse<Void> approveRefund(@PathVariable Long orderId) {
        orderService.approveRefund(orderId);
        return ApiResponse.success();
    }

    // TODO : #209 관리자 인증 없이 우선 열어둠. 관리자 인증 도입 시 보호 추가
    @PostMapping("/{orderId}/refund-reject")
    public ApiResponse<Void> rejectRefund(@PathVariable Long orderId) {
        orderService.rejectRefund(orderId);
        return ApiResponse.success();
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailResponse> getOrder(@MemberId Long memberId, @PathVariable Long orderId) {
        return ApiResponse.success(OrderDetailResponse.from(orderService.getOrderDetail(orderId, memberId)));
    }

    @GetMapping
    public ApiResponse<OrderPageResponse> getOrders(
            @MemberId Long memberId,
            @RequestParam(required = false) String perspective,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
                OrderPageResponse.from(orderService.findOrders(memberId, perspective, status, page, size)));
    }
}
