import { useQuery } from "@tanstack/react-query";
import { fetchAuctions } from "../../api/auctions";
import { AuctionCard } from "../../components/AuctionCard";
import { QueryState } from "../../components/QueryState";

const LIMIT = 8;

// 상품 상세에서 "이 음반의 열린 경매"를 보여준다.
// 목록 API의 상태 필터는 값을 하나만 받아서, 진행 중과 시작 예정을 따로 부른 뒤 이어 붙인다
export function OpenAuctionList({ productId }: { productId: number }) {
    const running = useQuery({
        queryKey: ["auctions", "byProduct", productId, "RUNNING"],
        queryFn: () => fetchAuctions({ productId, status: "RUNNING", sort: "ending_soon", page: 0, size: LIMIT }),
    });
    const scheduled = useQuery({
        queryKey: ["auctions", "byProduct", productId, "SCHEDULED"],
        queryFn: () => fetchAuctions({ productId, status: "SCHEDULED", sort: "ending_soon", page: 0, size: LIMIT }),
    });

    const items = [...(running.data?.items ?? []), ...(scheduled.data?.items ?? [])];
    const total = (running.data?.totalElements ?? 0) + (scheduled.data?.totalElements ?? 0);

    return (
        <section className="mt-8 overflow-hidden rounded-2xl border border-line bg-surface shadow-sm">
            <div className="flex items-center justify-between border-b border-line px-5 py-4">
                <h3 className="text-[15px] font-bold">이 음반의 열린 경매</h3>
                {total > 0 && (
                    <span className="text-xs text-muted">
                        진행 중 <span className="font-mono tabular-nums">{running.data?.totalElements ?? 0}</span>건 ·
                        시작 예정 <span className="font-mono tabular-nums">{scheduled.data?.totalElements ?? 0}</span>건
                    </span>
                )}
            </div>
            <div className="p-5">
                <QueryState
                    isLoading={running.isPending || scheduled.isPending}
                    error={running.error ?? scheduled.error}
                    isEmpty={items.length === 0}
                    emptyMessage="이 음반으로 열린 경매가 없습니다."
                >
                    <div className="grid grid-cols-2 gap-3.5 md:grid-cols-4">
                        {items.map((auction) => (
                            <AuctionCard key={auction.auctionId} auction={auction} />
                        ))}
                    </div>
                    {total > items.length && (
                        <p className="mt-3 text-[11.5px] text-faint">
                            상태별로 최근 {LIMIT}건까지만 보여줍니다.
                        </p>
                    )}
                </QueryState>
            </div>
        </section>
    );
}
