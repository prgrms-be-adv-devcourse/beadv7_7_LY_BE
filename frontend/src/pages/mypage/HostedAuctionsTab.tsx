import { useState } from "react";
import { Link } from "react-router";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { fetchHostedAuctions, formatAuctionStatus, type HostedAuction } from "../../api/auctions";
import { QueryState } from "../../components/QueryState";
import { Pagination } from "../../components/Pagination";
import { VinylCover } from "../../components/VinylCover";
import { formatWon } from "../../components/AuctionCard";

const PAGE_SIZE = 10;

export function HostedAuctionsTab() {
    const [page, setPage] = useState(0);

    const query = useQuery({
        queryKey: ["auctions", "hosted", page],
        queryFn: () => fetchHostedAuctions(page, PAGE_SIZE),
        placeholderData: keepPreviousData,
    });

    return (
        <div>
            <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
                <div>
                    <h3 className="mb-1 text-base font-bold tracking-tight">내가 등록한 경매</h3>
                    <p className="text-[13.5px] text-muted">
                        판매자로 올린 경매입니다. 취소한 경매는 목록에서 빠집니다.
                    </p>
                </div>
            </div>

            <QueryState
                isLoading={query.isPending}
                error={query.error}
                isEmpty={!query.data || query.data.items.length === 0}
                emptyMessage="등록한 경매가 없습니다."
            >
                <p className="mb-3 text-[13px] text-muted">
                    총 <b className="font-mono tabular-nums">{query.data?.totalElements}</b>건
                </p>
                <ul className={`flex flex-col gap-2.5 ${query.isFetching ? "opacity-60" : ""}`}>
                    {query.data?.items.map((auction) => (
                        <HostedRow key={auction.auctionId} auction={auction} />
                    ))}
                </ul>
                {query.data && (
                    <Pagination
                        page={page}
                        hasNext={query.data.hasNext}
                        totalElements={query.data.totalElements}
                        size={PAGE_SIZE}
                        onPage={setPage}
                    />
                )}
            </QueryState>
        </div>
    );
}

function HostedRow({ auction }: { auction: HostedAuction }) {
    return (
        <li className="flex items-center gap-3.5 rounded-xl border border-line bg-surface px-4 py-3.5">
            <VinylCover title={auction.title} artist={auction.artistName} className="w-16 shrink-0" />
            <div className="min-w-0 grow">
                <div className="flex flex-wrap items-center gap-2">
                    <span className="rounded-md bg-surface2 px-2 py-0.5 text-[11px] font-bold text-muted">
                        {formatAuctionStatus(auction.status)}
                    </span>
                    <span className="font-mono text-[11px] text-faint">경매번호 {auction.auctionId}</span>
                </div>
                <Link to={`/auctions/${auction.auctionId}`} className="mt-1 block truncate font-semibold hover:underline">
                    {auction.title}
                </Link>
                <p className="truncate text-[13px] text-muted">{auction.artistName}</p>
            </div>
            <div className="shrink-0 text-right">
                <div className="text-[10px] font-semibold uppercase tracking-wider text-faint">최고 입찰가</div>
                <div className="font-mono text-base font-bold tabular-nums">
                    {auction.highestBidAmount === null ? "입찰 없음" : formatWon(auction.highestBidAmount)}
                </div>
                <div className="text-[11px] text-muted">
                    <span className="font-mono tabular-nums">{auction.bidCount}</span>건 입찰
                </div>
            </div>
        </li>
    );
}
