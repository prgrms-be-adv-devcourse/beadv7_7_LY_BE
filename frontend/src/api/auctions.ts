import { apiDelete, apiGet, apiPatch, apiPost } from "./client";

// 백엔드 AuctionPolicy 상수를 그대로 옮긴 값.
// 이 제약을 어기면 서버가 검증에 실패하므로, 폼에서 먼저 막아 요청을 아낀다
export const AUCTION_POLICY = {
    MIN_DESC_LENGTH: 10,
    MAX_DESC_LENGTH: 500,
    MAX_IMAGE_COUNT: 5,
    MIN_START_PRICE: 1000,
    MIN_BID_UNIT: 100,
    // 등록할 때 시작 시각은 지금으로부터 최소 이만큼 뒤여야 한다
    MIN_START_LEAD_MINUTES: 30,
    MIN_DURATION_HOURS: 1,
    MIN_EXTENSION_MINUTES: 1,
    // 수정·취소는 시작 이만큼 전까지만 된다
    EDIT_DEADLINE_MINUTES: 10,
    // 시각 입력 단위. 서버 검증은 없고 화면에서만 쓴다
    TIME_UNIT_MINUTES: 10,
} as const;

// GET /api/v1/auctions — AuctionListItemResponse + PageResponse
export interface AuctionListItem {
    auctionId: number;
    productId: number;
    title: string;
    artistName: string;
    releaseYear: number | null;
    genre: string | null;
    pressType: string | null;
    thumbnail: string | null;
    sellerId: number;
    sellerNickname: string;
    status: AuctionStatus;
    finalPrice: number;
    bidCount: number;
    startAt: string;
    endAt: string;
}

// 경매 도메인의 목록 API는 모두 이 봉투로 내려온다 (백엔드 PageResponse<T> 그대로)
export interface AuctionPage<T = AuctionListItem> {
    page: number;
    size: number;
    totalElements: number;
    hasNext: boolean;
    items: T[];
}

export type AuctionStatus = "SCHEDULED" | "RUNNING" | "CLOSING" | "ENDED_WON" | "ENDED_FAILED" | "CANCELED";
export type AuctionSort = "ending_soon" | "price_asc" | "price_desc" | "most_bids";

export interface AuctionListParams {
    productId?: number;
    genre?: string;
    pressType?: string;
    status?: string;
    sort?: AuctionSort;
    page?: number;
    size?: number;
}

export function fetchAuctions(params: AuctionListParams): Promise<AuctionPage> {
    const query = new URLSearchParams();
    if (params.productId !== undefined) query.set("productId", String(params.productId));
    if (params.genre) query.set("genre", params.genre);
    if (params.pressType) query.set("pressType", params.pressType);
    if (params.status) query.set("status", params.status);
    if (params.sort) query.set("sort", params.sort);
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    return apiGet<AuctionPage>(`/api/v1/auctions?${query}`);
}

// GET /api/v1/auctions/{id} — 상태별로 채워지는 필드가 다르다 (RUNNING 전용 / 종료 전용)
export interface BidInfo {
    bidder: string;
    amount: number;
    placedAt: string;
}

export interface AuctionDetail {
    auctionId: number;
    status: AuctionStatus;
    itemCondition: string;
    itemDescription: string | null;
    itemImages: string[] | null;
    startBidAmount: number;
    bidUnit: number;
    startAt: string;
    endAt: string;
    extensionEnabled: boolean;
    extensionTime: number | null;
    product: {
        productId: number;
        title: string;
        artistName: string;
        coverImageUrl: string | null;
        releaseYear: string;
        pressType: string | null;
        genre: string | null;
    };
    seller: { sellerId: number; nickname: string };
    // RUNNING 전용
    highestBidAmount?: number;
    nextMinBidAmount?: number;
    bidCount?: number;
    recentBids?: BidInfo[];
    myHighest?: boolean;
    // 종료 전용
    won?: boolean;
    winningBid?: BidInfo;
}

export function getAuctionDetail(auctionId: string | number): Promise<AuctionDetail> {
    return apiGet<AuctionDetail>(`/api/v1/auctions/${auctionId}`);
}

// POST /api/v1/auctions/{id}/bids — X-Member-Id 필요 (로그인 세션에서 자동 첨부)
export interface PlaceBidResult {
    bidId: number;
    auctionId: number;
    bidAmount: number;
    outcome: string;
    nextMinBidAmount: number;
    placedAt: string;
    endAt: string;
    extended: boolean;
}

export function placeBid(auctionId: number, bidAmount: number): Promise<PlaceBidResult> {
    return apiPost<PlaceBidResult>(`/api/v1/auctions/${auctionId}/bids`, { bidAmount });
}

// 경매 등록·수정 본문. 두 API가 같은 DTO를 받는다.
// itemImages는 서버에서 null을 허용하지 않아, 넣을 이미지가 없어도 빈 배열로 보내야 한다
export interface AuctionPayload {
    productId: number;
    itemCondition: string;
    itemDescription: string | null;
    itemImages: string[];
    startPrice: number;
    shippingFee: number;
    bidUnit: number;
    startAt: string;
    endAt: string;
    extensionEnabled: boolean;
    extensionTime: number | null;
}

export interface AuctionMutationResult {
    auctionId: number;
    status: string;
}

export function createAuction(payload: AuctionPayload): Promise<AuctionMutationResult> {
    return apiPost<AuctionMutationResult>("/api/v1/auctions", payload);
}

export function modifyAuction(auctionId: number, payload: AuctionPayload): Promise<AuctionMutationResult> {
    return apiPatch<AuctionMutationResult>(`/api/v1/auctions/${auctionId}`, payload);
}

export function deleteAuction(auctionId: number): Promise<void> {
    return apiDelete<void>(`/api/v1/auctions/${auctionId}`);
}

// 백엔드 ItemCondition enum → 표시 라벨. 등급이 좋은 순서다
export const ITEM_CONDITIONS = [
    { value: "MINT", label: "M (새 제품)" },
    { value: "NEAR_MINT", label: "NM (거의 새것)" },
    { value: "VERY_GOOD_PLUS", label: "VG+ (약간의 사용감)" },
    { value: "VERY_GOOD", label: "VG (사용감 있음)" },
    { value: "GOOD", label: "G (많은 사용감)" },
    { value: "POOR", label: "P (심한 손상)" },
] as const;

const CONDITION_LABELS: Record<string, string> = Object.fromEntries(
    ITEM_CONDITIONS.map((condition) => [condition.value, condition.label]),
);

export function formatCondition(condition: string): string {
    return CONDITION_LABELS[condition] ?? condition;
}

// GET /api/v1/auctions/hosted — 내가 판매자로 등록한 경매.
// 목록에는 시작 시각이 없어서 수정·취소 가능 시한은 여기서 판단할 수 없다
export interface HostedAuction {
    auctionId: number;
    productId: number;
    title: string;
    artistName: string;
    status: AuctionStatus;
    highestBidAmount: number | null;
    bidCount: number;
}

export function fetchHostedAuctions(page: number, size = 10): Promise<AuctionPage<HostedAuction>> {
    const query = new URLSearchParams({ page: String(page), size: String(size) });
    return apiGet<AuctionPage<HostedAuction>>(`/api/v1/auctions/hosted?${query}`);
}

// GET /api/v1/auctions/participated — 내가 입찰한 경매. 경매당 내 최신 입찰 한 건씩 내려온다
export type BidOutcome = "ACTIVE" | "OUTBID" | "WON";

export interface ParticipatedAuction {
    auctionId: number;
    productId: number;
    title: string;
    artistName: string;
    status: AuctionStatus;
    myBidAmount: number;
    myOutcome: BidOutcome;
}

export function fetchParticipatedAuctions(page: number, size = 10): Promise<AuctionPage<ParticipatedAuction>> {
    const query = new URLSearchParams({ page: String(page), size: String(size) });
    return apiGet<AuctionPage<ParticipatedAuction>>(`/api/v1/auctions/participated?${query}`);
}

const OUTCOME_LABELS: Record<string, string> = {
    ACTIVE: "최고 입찰 중",
    OUTBID: "밀림",
    WON: "낙찰",
};

export function formatBidOutcome(outcome: string): string {
    return OUTCOME_LABELS[outcome] ?? outcome;
}

const STATUS_LABELS: Record<string, string> = {
    SCHEDULED: "시작 전",
    RUNNING: "진행 중",
    CLOSING: "낙찰 처리 중",
    ENDED_WON: "낙찰 완료",
    ENDED_FAILED: "유찰",
    CANCELED: "취소됨",
};

export function formatAuctionStatus(status: string): string {
    return STATUS_LABELS[status] ?? status;
}

export function parseServerTime(value: string): number {
    return new Date(value).getTime();
}
