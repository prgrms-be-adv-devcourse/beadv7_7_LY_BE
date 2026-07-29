import { useSearchParams } from "react-router";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { fetchAuctions, type AuctionSort } from "../api/auctions";
import { AuctionCard } from "../components/AuctionCard";
import { QueryState } from "../components/QueryState";
import { Pagination } from "../components/Pagination";
import { FeedFilters } from "./feed/FeedFilters";

const PAGE_SIZE = 12;
const VALID_SORTS: AuctionSort[] = ["ending_soon", "price_asc", "price_desc", "most_bids"];

export function FeedPage() {
    const [searchParams, setSearchParams] = useSearchParams();
    const genre = searchParams.get("genre") ?? "";
    const pressType = searchParams.get("pressType") ?? "";
    const status = searchParams.get("status") ?? "";
    const sortParam = searchParams.get("sort");
    const sort = VALID_SORTS.includes(sortParam as AuctionSort) ? (sortParam as AuctionSort) : "ending_soon";
    const page = Math.max(0, Number(searchParams.get("page") ?? "0") || 0);

    function patchParams(patch: Partial<Record<string, string | null>>, resetPage = true) {
        const next = new URLSearchParams(searchParams);
        for (const [key, value] of Object.entries(patch)) {
            if (value === null || value === undefined || value === "") next.delete(key);
            else next.set(key, value);
        }
        if (resetPage) next.set("page", "0");
        if (next.get("page") === "0") next.delete("page");
        setSearchParams(next, { replace: resetPage });
    }

    const query = useQuery({
        queryKey: ["auctions", "feed", genre, pressType, status, sort, page],
        queryFn: () =>
            fetchAuctions({
                genre: genre || undefined,
                pressType: pressType || undefined,
                status: status || undefined,
                sort,
                page,
                size: PAGE_SIZE,
            }),
        placeholderData: keepPreviousData,
    });

    return (
        <div>
            <p className="text-[11px] font-bold uppercase tracking-widest text-live">둘러보기</p>
            <h2 className="text-lg font-bold tracking-tight">경매 피드</h2>
            <p className="mb-4 mt-1 text-[13.5px] text-muted">등록된 경매를 장르·프레스·상태로 골라보세요.</p>
            <FeedFilters values={{ genre, pressType, status, sort }} onChange={patchParams} />
            <QueryState
                isLoading={query.isPending}
                error={query.error}
                isEmpty={!query.data || query.data.items.length === 0}
                emptyMessage={
                    <div>
                        조건에 맞는 경매가 없습니다.
                        <div className="mt-3">
                            <button
                                type="button"
                                onClick={() => setSearchParams({}, { replace: true })}
                                className="rounded-lg border border-line bg-paper px-4 py-2 text-sm font-semibold hover:border-line-strong"
                            >
                                필터 초기화
                            </button>
                        </div>
                    </div>
                }
            >
                <p className="mb-3 text-[13px] text-muted">
                    총 <b className="font-mono tabular-nums">{query.data?.totalElements}</b>건
                </p>
                <div className={`grid grid-cols-2 gap-3.5 md:grid-cols-4 ${query.isFetching ? "opacity-60" : ""}`}>
                    {query.data?.items.map((auction) => (
                        <AuctionCard key={auction.auctionId} auction={auction} />
                    ))}
                </div>
                {query.data && (
                    <Pagination
                        page={page}
                        hasNext={query.data.hasNext}
                        totalElements={query.data.totalElements}
                        size={PAGE_SIZE}
                        onPage={(next) => patchParams({ page: String(next) }, false)}
                    />
                )}
            </QueryState>
        </div>
    );
}
