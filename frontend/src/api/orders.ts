import { apiGet, apiPost } from "./client";

// core-service /api/v1/orders — 필드명은 백엔드 OrderPageResponse / OrderDetailResponse 그대로
export type OrderPerspective = "BUYER" | "SELLER";
export type OrderStatus = "PENDING" | "ORDERED" | "CANCELLED" | "COMPLETED";

export interface OrderListItem {
    orderId: number;
    auctionId: number;
    status: OrderStatus;
    finalBidPrice: number;
    confirmationDeadline: string | null;
    completionDeadline: string | null;
    product: {
        productId: number;
        albumTitle: string;
        artistName: string;
        conditionGrade: string | null;
        coverImage: string | null;
    };
}

export interface OrderPage {
    content: OrderListItem[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    last: boolean;
}

export interface OrderListParams {
    perspective: OrderPerspective;
    status?: string;
    page?: number;
    size?: number;
}

export function fetchOrders(params: OrderListParams): Promise<OrderPage> {
    const query = new URLSearchParams();
    query.set("perspective", params.perspective);
    if (params.status) query.set("status", params.status);
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    return apiGet<OrderPage>(`/api/v1/orders?${query}`);
}

export interface OrderDetail {
    orderId: number;
    auctionId: number;
    buyerId: number;
    sellerId: number;
    finalBidPrice: number;
    status: OrderStatus;
    cancelReason: string | null;
    confirmationDeadline: string | null;
    completionDeadline: string | null;
    orderedAt: string | null;
    completedAt: string | null;
    cancelledAt: string | null;
    product: {
        productId: number;
        artistName: string;
        albumTitle: string;
        releaseYear: number | null;
        pressType: string | null;
        conditionGrade: string | null;
        coverImage: string | null;
    };
    // 배송지는 주문(place) 전이면 null로 내려온다
    deliveryAddress: {
        recipientName: string;
        phoneNumber: string;
        baseAddress: string;
        detailAddress: string;
    } | null;
}

export function getOrderDetail(orderId: number): Promise<OrderDetail> {
    return apiGet<OrderDetail>(`/api/v1/orders/${orderId}`);
}

// POST /api/v1/orders/{orderId}/place — 낙찰 뒤 결제 대기 상태의 주문에 배송지를 넣어 주문을 확정한다.
// 네 항목 모두 서버에서 필수라 하나라도 비면 요청이 거절된다
export interface OrderPlacePayload {
    recipientName: string;
    phoneNumber: string;
    baseAddress: string;
    detailAddress: string;
}

export function placeOrder(orderId: number, payload: OrderPlacePayload): Promise<void> {
    return apiPost<void>(`/api/v1/orders/${orderId}/place`, payload);
}

export function completeOrder(orderId: number): Promise<void> {
    return apiPost<void>(`/api/v1/orders/${orderId}/complete`);
}

export function cancelOrder(orderId: number): Promise<void> {
    return apiPost<void>(`/api/v1/orders/${orderId}/cancel`);
}

const ORDER_STATUS_LABELS: Record<string, string> = {
    PENDING: "결제 대기",
    ORDERED: "주문 완료",
    CANCELLED: "주문 취소",
    COMPLETED: "거래 확정",
};

export function formatOrderStatus(status: string): string {
    return ORDER_STATUS_LABELS[status] ?? status;
}
