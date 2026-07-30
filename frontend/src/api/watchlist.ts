import { apiDelete, apiGet, apiPut } from "./client";
import type { AuctionStatus } from "./auctions";

// core-service /api/v1/members/me/watched-auctions — 경매 단위 관심 목록.
// 페이징 없이 상태별로 묶인 그룹 배열이 한 번에 내려온다 (서버가 회원당 30건으로 제한)
export interface WatchedAuction {
    id: number;
    auctionId: number;
    status: AuctionStatus;
    currentPrice: number;
    startAt: string;
    endAt: string;
}

export interface WatchedAuctionGroup {
    status: AuctionStatus;
    items: WatchedAuction[];
}

export const MAX_WATCHED_AUCTIONS = 30;

export function fetchWatchedAuctions(): Promise<WatchedAuctionGroup[]> {
    return apiGet<WatchedAuctionGroup[]>("/api/v1/members/me/watched-auctions");
}

// 서버는 시작 전·진행 중인 경매만 담을 수 있게 막는다 (끝난 경매를 넣으면 실패 응답)
export function watchAuction(auctionId: number): Promise<void> {
    return apiPut<void>(`/api/v1/members/me/watched-auctions/${auctionId}`);
}

export function unwatchAuction(auctionId: number): Promise<void> {
    return apiDelete<void>(`/api/v1/members/me/watched-auctions/${auctionId}`);
}

export function toWatchedAuctionIds(groups: WatchedAuctionGroup[]): Set<number> {
    return new Set(groups.flatMap((group) => group.items.map((item) => item.auctionId)));
}
