import { apiGet } from "./client";

// core-service /api/v1/settlements — 판매자(X-Member-Id) 기준 조회
export type SettlementStatus = "PENDING" | "CONFIRMED";

export interface SettlementItem {
    settlementItemId: number;
    orderId: number;
    finalBidPrice: number;
    commissionRate: number;
    commissionAmount: number;
    netAmount: number;
    status: SettlementStatus;
    completedAt: string | null;
    confirmedAt: string | null;
    settlementBatchId: number | null;
}

export interface SettlementItemPage {
    content: SettlementItem[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    last: boolean;
}

export interface SettlementItemParams {
    status?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
}

export function fetchSettlementItems(params: SettlementItemParams): Promise<SettlementItemPage> {
    const query = new URLSearchParams();
    if (params.status) query.set("status", params.status);
    if (params.from) query.set("from", params.from);
    if (params.to) query.set("to", params.to);
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    return apiGet<SettlementItemPage>(`/api/v1/settlements/items?${query}`);
}

export interface SettlementBatch {
    settlementBatchId: number;
    totalAmount: number;
    periodFrom: string;
    periodTo: string;
    confirmedAt: string | null;
}

export function fetchSettlementBatches(): Promise<SettlementBatch[]> {
    return apiGet<SettlementBatch[]>("/api/v1/settlements/batches");
}

const SETTLEMENT_STATUS_LABELS: Record<string, string> = {
    PENDING: "정산 대기",
    CONFIRMED: "정산 확정",
};

export function formatSettlementStatus(status: string): string {
    return SETTLEMENT_STATUS_LABELS[status] ?? status;
}
