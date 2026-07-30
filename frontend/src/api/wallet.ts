import { apiGet, apiPost } from "./client";

// core-service /api/v1/wallet — 홀드는 잔액에서 즉시 빠지는 모델이라 이 값이 곧 입찰 가능액이다
export interface WalletBalance {
    availableBalance: number;
}

export function getWalletBalance(): Promise<WalletBalance> {
    return apiGet<WalletBalance>("/api/v1/wallet");
}

export type PointTransactionType =
    | "DEPOSIT"
    | "HOLD"
    | "RELEASE"
    | "DEPOSIT_CANCEL"
    | "WITHDRAW"
    | "FEE_INCOME"
    | "SETTLEMENT_PAYOUT";

export interface PointTransaction {
    transactionId: number;
    type: PointTransactionType;
    amount: number;
    // 이름은 auction이지만 실제로는 홀드·충전 등 거래를 만든 쪽의 id가 들어온다
    relatedAuctionId: number | null;
    createdAt: string;
}

// 다른 목록 API와 달리 totalElements·last가 없다 (백엔드 PointTransactionHistoryResponse 그대로)
export interface PointTransactionPage {
    content: PointTransaction[];
    page: number;
    totalPages: number;
}

export interface PointTransactionParams {
    type?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
}

export function fetchPointTransactions(params: PointTransactionParams): Promise<PointTransactionPage> {
    const query = new URLSearchParams();
    if (params.type) query.set("type", params.type);
    if (params.from) query.set("from", params.from);
    if (params.to) query.set("to", params.to);
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    return apiGet<PointTransactionPage>(`/api/v1/wallet/transactions?${query}`);
}

// POST /api/v1/deposits — 충전 요청만 만든다. 실제 입금은 결제사 결제까지 끝나야 반영된다
export const MIN_DEPOSIT_AMOUNT = 1000;

export interface DepositRequestResult {
    orderId: string;
    amount: number;
}

export function requestDeposit(amount: number): Promise<DepositRequestResult> {
    return apiPost<DepositRequestResult>("/api/v1/deposits", { amount });
}

// POST /api/v1/deposits/{depositId}/cancel — 충전을 되돌린다.
// 서버가 사유를 필수로 받고, 잔액이 충전액보다 적으면 거절한다
export function cancelDeposit(depositId: number, reason: string): Promise<void> {
    return apiPost<void>(`/api/v1/deposits/${depositId}/cancel`, { reason });
}

// 충전 요청 응답에는 취소에 쓸 id가 없다. 대신 충전이 반영될 때 남는 거래 기록에
// 그 충전 건의 id가 함께 저장되므로, 충전 기록에서 되짚어 쓴다
export function findDepositId(transaction: PointTransaction): number | null {
    return transaction.type === "DEPOSIT" ? transaction.relatedAuctionId : null;
}

const TRANSACTION_TYPE_LABELS: Record<string, string> = {
    DEPOSIT: "충전",
    HOLD: "입찰 보증금 차감",
    RELEASE: "입찰 보증금 반환",
    DEPOSIT_CANCEL: "충전 취소",
    WITHDRAW: "출금",
    FEE_INCOME: "수수료",
    SETTLEMENT_PAYOUT: "정산 입금",
};

export function formatTransactionType(type: string): string {
    return TRANSACTION_TYPE_LABELS[type] ?? type;
}

// 잔액이 늘어나는 거래인지 — 목록에서 +/- 부호와 색을 정하는 데 쓴다
export function isIncomingTransaction(type: string): boolean {
    return type === "DEPOSIT" || type === "RELEASE" || type === "SETTLEMENT_PAYOUT";
}
