import { apiGet } from "./client";

export interface ProductSearchCard {
    productId: number;
    title: string;
    artistName: string;
    coverImageUrl: string | null;
    releaseYear: number;
    pressType: string;
}

export interface ProductSearchResponse {
    content: ProductSearchCard[];
    page: number;
    size: number;
    totalElements: number;
    hasNext: boolean;
}

export interface ProductDetail {
    productId: number;
    catalogNo: string;
    title: string;
    artist: { artistId: number; name: string };
    label: string;
    country: string;
    releaseYear: number;
    pressType: string;
    format: string;
    genre: string;
    coverImageUrl: string | null;
    description: string | null;
}

// GET /api/v1/products/{id}/price-trades — 원장 기반 낙찰 이력 (시세 추이용)
export interface TradePoint {
    condition: string;
    price: number;
    tradedAt: string;
}

export interface PriceTradesResponse {
    productId: number;
    trades: TradePoint[];
}

export function getPriceTrades(productId: string | number): Promise<PriceTradesResponse> {
    return apiGet<PriceTradesResponse>(`/api/v1/products/${productId}/price-trades`);
}

// GET /api/v1/products/{id}/price-summary — 컨디션별 낙찰가 통계 (거래가 있는 컨디션만 내려온다)
export interface ConditionPriceSummary {
    condition: string;
    sampleCount: number;
    averagePrice: number;
    lowestPrice: number;
    highestPrice: number;
}

export interface PriceSummaryResponse {
    productId: number;
    conditions: ConditionPriceSummary[];
}

export function getPriceSummary(productId: string | number): Promise<PriceSummaryResponse> {
    return apiGet<PriceSummaryResponse>(`/api/v1/products/${productId}/price-summary`);
}

export function searchProducts(q: string, page: number, size = 20): Promise<ProductSearchResponse> {
    const params = new URLSearchParams({ q, page: String(page), size: String(size) });
    return apiGet<ProductSearchResponse>(`/api/v1/search/products?${params}`);
}

// 임시 트릭 — 전체 상품 목록 API가 없어서(검색은 q 필수), 모음 글자 브로드 검색 여러 번을 합쳐
// 근사 전체 목록을 만든다. 제목·아티스트·별칭 어디든 해당 글자가 있으면 걸리는 LIKE 검색이라
// 로마자 표기 별칭이 있는 시드 상품은 대부분 잡힌다. 목록 API가 생기면 이 함수를 교체할 것.
const BROWSE_PROBES = ["a", "e", "i", "o", "u"];

export async function browseProducts(): Promise<ProductSearchCard[]> {
    const pages = await Promise.all(BROWSE_PROBES.map((probe) => searchProducts(probe, 0, 100)));
    const byId = new Map<number, ProductSearchCard>();
    for (const page of pages) {
        for (const card of page.content) {
            byId.set(card.productId, card);
        }
    }
    return [...byId.values()].sort((a, b) => a.title.localeCompare(b.title));
}

export function getProductDetail(productId: string | number): Promise<ProductDetail> {
    return apiGet<ProductDetail>(`/api/v1/products/${productId}`);
}
