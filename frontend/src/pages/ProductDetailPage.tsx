import { useState } from "react";
import { Link, useParams } from "react-router";
import { useQuery } from "@tanstack/react-query";
import { getPriceSummary, getPriceTrades, getProductDetail } from "../api/products";
import { VinylCover } from "../components/VinylCover";
import { QueryState } from "../components/QueryState";
import { LikeButton } from "../components/LikeButton";
import { formatWon } from "../components/AuctionCard";
import { GRADED_CONDITIONS, PriceSparkline } from "./product-detail/PriceSparkline";

// 요약 카드 머리글용 짧은 컨디션 표기 (백엔드 MediaCondition enum 이름 기준)
const CONDITION_SHORT: Record<string, string> = {
    MINT: "M · Mint",
    NEAR_MINT: "NM · Near Mint",
    VERY_GOOD_PLUS: "VG+",
    VERY_GOOD: "VG",
    GOOD: "G",
    POOR: "P",
};

// 차트 컨디션 필터. 기본은 M·NM — 하위 컨디션까지 섞으면 선이 출렁여 흐름이 안 보인다 (PriceSparkline 참고)
const CHART_FILTERS: { key: string; label: string; conditions: string[] | null }[] = [
    { key: "MNM", label: "M·NM", conditions: GRADED_CONDITIONS },
    { key: "ALL", label: "전체", conditions: null },
    { key: "M", label: "M", conditions: ["MINT"] },
    { key: "NM", label: "NM", conditions: ["NEAR_MINT"] },
    { key: "VG+", label: "VG+", conditions: ["VERY_GOOD_PLUS"] },
];

export function ProductDetailPage() {
    const { productId = "" } = useParams();
    const [chartFilterKey, setChartFilterKey] = useState("MNM");
    const chartFilter = CHART_FILTERS.find((f) => f.key === chartFilterKey) ?? CHART_FILTERS[0];

    const query = useQuery({
        queryKey: ["product", productId],
        queryFn: () => getProductDetail(productId),
        enabled: productId !== "",
    });
    const detail = query.data;

    const tradesQuery = useQuery({
        queryKey: ["priceTrades", productId],
        queryFn: () => getPriceTrades(productId),
        enabled: productId !== "",
    });

    const summaryQuery = useQuery({
        queryKey: ["priceSummary", productId],
        queryFn: () => getPriceSummary(productId),
        enabled: productId !== "",
    });
    const conditionSummaries = summaryQuery.data?.conditions ?? [];

    const trades = tradesQuery.data?.trades ?? [];
    const graded = trades
        .filter((t) => GRADED_CONDITIONS.includes(t.condition))
        .sort((a, b) => a.tradedAt.localeCompare(b.tradedAt));
    const lastTraded = graded.length > 0 ? graded[graded.length - 1].price : null;

    return (
        <QueryState
            isLoading={query.isPending}
            error={query.error}
            isEmpty={!detail}
            emptyMessage="상품 정보를 찾을 수 없습니다."
        >
            {detail && (
                <div>
                    <Link to="/catalog" className="mb-4 inline-flex items-center gap-1.5 text-[13px] font-semibold text-muted hover:text-ink">
                        ← 카탈로그로
                    </Link>
                    <div className="grid items-start gap-7 md:grid-cols-[260px_1fr]">
                        <div className="max-w-[260px]">
                            <VinylCover
                                title={detail.title}
                                artist={detail.artist.name}
                                imageUrl={detail.coverImageUrl}
                                spin
                                className="rounded-xl shadow"
                            />
                            <div className="mt-3">
                                <LikeButton productId={detail.productId} variant="inline" />
                            </div>
                        </div>
                        <div>
                            <h1 className="font-display text-3xl font-semibold leading-tight tracking-tight text-balance">
                                {detail.title}
                            </h1>
                            <p className="mb-4 mt-1 text-base font-bold text-brand">{detail.artist.name}</p>
                            <dl className="grid max-w-[460px] grid-cols-[auto_1fr] gap-x-5 gap-y-2 text-[13.5px]">
                                {[
                                    ["카탈로그 번호", detail.catalogNo],
                                    ["레이블", detail.label],
                                    ["국가", detail.country],
                                    ["발매 연도", String(detail.releaseYear)],
                                    ["프레스", detail.pressType === "ORIGINAL" ? "오리지널" : "재발매"],
                                    ["포맷", detail.format],
                                    ["장르", detail.genre],
                                ].map(([label, value]) => (
                                    <div key={label} className="contents">
                                        <dt className="font-semibold text-faint">{label}</dt>
                                        <dd className="m-0 font-semibold">{value || "—"}</dd>
                                    </div>
                                ))}
                            </dl>
                            {detail.description && <p className="mt-4 max-w-[65ch] text-sm text-muted">{detail.description}</p>}
                            {conditionSummaries.length > 0 && (
                                <div className="mt-5 flex flex-wrap gap-2.5">
                                    {conditionSummaries.map((c) => (
                                        <div key={c.condition} className="min-w-[130px] rounded-xl border border-line bg-surface px-4 py-2.5">
                                            <p className="text-[10.5px] font-bold uppercase tracking-wider text-faint">
                                                {CONDITION_SHORT[c.condition] ?? c.condition}
                                            </p>
                                            <p className="mt-0.5 font-mono text-lg font-bold tabular-nums">
                                                {formatWon(c.averagePrice)}
                                            </p>
                                            <p className="mt-0.5 text-[11px] text-muted">
                                                표본 {c.sampleCount}건 · {formatWon(c.lowestPrice)}~{formatWon(c.highestPrice)}
                                            </p>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>

                    <section className="mt-8 overflow-hidden rounded-2xl border border-line bg-surface shadow-sm">
                        <div className="flex items-center justify-between border-b border-line px-5 py-4">
                            <h3 className="flex items-center gap-2 text-[15px] font-bold">최근 낙찰 시세</h3>
                            {lastTraded !== null && (
                                <span className="font-mono text-lg font-bold tabular-nums text-up">{formatWon(lastTraded)}</span>
                            )}
                        </div>
                        <div className="p-5">
                            <QueryState
                                isLoading={tradesQuery.isPending}
                                error={tradesQuery.error}
                                isEmpty={trades.length === 0}
                                emptyMessage="시세 데이터가 없습니다."
                            >
                                <div className="mb-3 flex flex-wrap items-center gap-1.5">
                                    <span className="mr-1 text-[11px] font-bold uppercase tracking-wider text-faint">컨디션</span>
                                    {CHART_FILTERS.map((f) => (
                                        <button
                                            key={f.key}
                                            type="button"
                                            aria-pressed={chartFilterKey === f.key}
                                            onClick={() => setChartFilterKey(f.key)}
                                            className={`rounded-lg border px-2.5 py-1 text-xs font-semibold transition-colors ${
                                                chartFilterKey === f.key
                                                    ? "border-brand bg-brand text-white"
                                                    : "border-line bg-paper text-muted hover:border-line-strong hover:text-ink"
                                            }`}
                                        >
                                            {f.label}
                                        </button>
                                    ))}
                                </div>
                                <PriceSparkline trades={trades} conditions={chartFilter.conditions} />
                                <p className="mt-2 text-[11.5px] text-faint">{chartFilter.label} 컨디션 낙찰가 기준</p>
                            </QueryState>
                        </div>
                    </section>
                </div>
            )}
        </QueryState>
    );
}
