import { Link, useParams } from "react-router";
import { useQuery } from "@tanstack/react-query";
import { getPriceTrades, getProductDetail } from "../api/products";
import { VinylCover } from "../components/VinylCover";
import { QueryState } from "../components/QueryState";
import { formatWon } from "../components/AuctionCard";
import { GRADED_CONDITIONS, PriceSparkline } from "./product-detail/PriceSparkline";

export function ProductDetailPage() {
    const { productId = "" } = useParams();
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
                        <VinylCover
                            title={detail.title}
                            artist={detail.artist.name}
                            imageUrl={detail.coverImageUrl}
                            spin
                            className="max-w-[260px] rounded-xl shadow"
                        />
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
                                <PriceSparkline trades={trades} />
                                <p className="mt-2 text-[11.5px] text-faint">M/NM 컨디션 낙찰가 기준</p>
                            </QueryState>
                        </div>
                    </section>
                </div>
            )}
        </QueryState>
    );
}
