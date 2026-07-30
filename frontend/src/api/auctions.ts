import { apiGet, apiPost } from "./client";

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

export interface AuctionPage {
    page: number;
    size: number;
    totalElements: number;
    hasNext: boolean;
    items: AuctionListItem[];
}

export type AuctionStatus = "SCHEDULED" | "RUNNING" | "CLOSING" | "ENDED_WON" | "ENDED_FAILED" | "CANCELED";
export type AuctionSort = "ending_soon" | "price_asc" | "price_desc" | "most_bids";

export interface AuctionListParams {
    genre?: string;
    pressType?: string;
    status?: string;
    sort?: AuctionSort;
    page?: number;
    size?: number;
}

export function fetchAuctions(params: AuctionListParams): Promise<AuctionPage> {
    const query = new URLSearchParams();
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

// 백엔드 ItemCondition enum → 표시 라벨
const CONDITION_LABELS: Record<string, string> = {
    MINT: "M (새 제품)",
    NEAR_MINT: "NM (거의 새것)",
    VERY_GOOD_PLUS: "VG+ (약간의 사용감)",
    VERY_GOOD: "VG (사용감 있음)",
    GOOD: "G (많은 사용감)",
    POOR: "P (심한 손상)",
};

export function formatCondition(condition: string): string {
    return CONDITION_LABELS[condition] ?? condition;
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
