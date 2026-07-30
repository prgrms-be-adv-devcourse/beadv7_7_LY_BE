import { apiDelete, apiGet, apiPut } from "./client";

// core-service /api/v1/members/me/liked-products — 상품 단위 찜.
// 다른 목록 API와 달리 페이지 번호가 아니라 커서(마지막으로 받은 항목 id)로 넘긴다
export interface LikedProduct {
    id: number;
    productId: number;
    title: string;
    artistName: string;
    coverImageUrl: string | null;
    releaseYear: number;
}

export interface LikedProductPage {
    content: LikedProduct[];
    nextCursor: number | null;
    hasNext: boolean;
}

export const LIKED_PAGE_SIZE = 20;

export function fetchLikedProducts(cursor: number | null, size = LIKED_PAGE_SIZE): Promise<LikedProductPage> {
    const query = new URLSearchParams({ size: String(size) });
    if (cursor !== null) query.set("cursor", String(cursor));
    return apiGet<LikedProductPage>(`/api/v1/members/me/liked-products?${query}`);
}

export function likeProduct(productId: number): Promise<{ id: number; productId: number }> {
    return apiPut<{ id: number; productId: number }>(`/api/v1/members/me/liked-products/${productId}`);
}

export function unlikeProduct(productId: number): Promise<void> {
    return apiDelete<void>(`/api/v1/members/me/liked-products/${productId}`);
}

// 한 상품이 찜 상태인지 묻는 API가 따로 없다. 하트 버튼의 초기 표시를 정하려면
// 찜 목록을 커서로 끝까지 받아 상품 id 집합을 만들어두는 수밖에 없다.
// 목록이 비정상적으로 길어져도 요청이 무한히 이어지지 않도록 페이지 수를 막아둔다
const MAX_PAGES = 20;

export async function fetchLikedProductIds(): Promise<Set<number>> {
    const ids = new Set<number>();
    let cursor: number | null = null;
    for (let i = 0; i < MAX_PAGES; i++) {
        const page: LikedProductPage = await fetchLikedProducts(cursor, 100);
        for (const item of page.content) ids.add(item.productId);
        if (!page.hasNext || page.nextCursor === null) break;
        cursor = page.nextCursor;
    }
    return ids;
}
