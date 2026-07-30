import { useState } from "react";
import { Link } from "react-router";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import {
    fetchParticipatedAuctions,
    formatAuctionStatus,
    formatBidOutcome,
    type ParticipatedAuction,
} from "../../api/auctions";
import { QueryState } from "../../components/QueryState";
import { Pagination } from "../../components/Pagination";
import { VinylCover } from "../../components/VinylCover";
import { formatWon } from "../../components/AuctionCard";

const PAGE_SIZE = 10;

const OUTCOME_STYLES: Record<string, string> = {
    ACTIVE: "bg-live-bg text-live",
    OUTBID: "bg-surface2 text-muted",
    WON: "bg-up/90 text-white",
};

export function ParticipatedAuctionsTab() {
    const [page, setPage] = useState(0);

    const query = useQuery({
        queryKey: ["auctions", "participated", page],
        queryFn: () => fetchParticipatedAuctions(page, PAGE_SIZE),
        placeholderData: keepPreviousData,
    });

    return (
        <div>
            <h3 className="mb-1 text-base font-bold tracking-tight">참여한 경매</h3>
            <p className="mb-4 text-[13.5px] text-muted">
                입찰한 적 있는 경매입니다. 경매당 내 마지막 입찰 한 건씩 보여줍니다.
            </p>

            <QueryState
                isLoading={query.isPending}
                error={query.error}
                isEmpty={!query.data || query.data.items.length === 0}
                emptyMessage="입찰한 경매가 없습니다."
            >
                <p className="mb-3 text-[13px] text-muted">
                    총 <b className="font-mono tabular-nums">{query.data?.totalElements}</b>건
                </p>
                <ul className={`flex flex-col gap-2.5 ${query.isFetching ? "opacity-60" : ""}`}>
                    {query.data?.items.map((auction) => (
                        <ParticipatedRow key={auction.auctionId} auction={auction} />
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

function ParticipatedRow({ auction }: { auction: ParticipatedAuction }) {
    return (
        <li className="flex items-center gap-3.5 rounded-xl border border-line bg-surface px-4 py-3.5">
            <VinylCover title={auction.title} artist={auction.artistName} className="w-16 shrink-0" />
            <div className="min-w-0 grow">
                <div className="flex flex-wrap items-center gap-2">
                    <span className="rounded-md bg-surface2 px-2 py-0.5 text-[11px] font-bold text-muted">
                        {formatAuctionStatus(auction.status)}
                    </span>
                    <span
                        className={`rounded-md px-2 py-0.5 text-[11px] font-bold ${
                            OUTCOME_STYLES[auction.myOutcome] ?? "bg-surface2 text-muted"
                        }`}
                    >
                        {formatBidOutcome(auction.myOutcome)}
                    </span>
                </div>
                <Link to={`/auctions/${auction.auctionId}`} className="mt-1 block truncate font-semibold hover:underline">
                    {auction.title}
                </Link>
                <p className="truncate text-[13px] text-muted">{auction.artistName}</p>
            </div>
            <div className="shrink-0 text-right">
                <div className="text-[10px] font-semibold uppercase tracking-wider text-faint">내 입찰가</div>
                <div className="font-mono text-base font-bold tabular-nums">{formatWon(auction.myBidAmount)}</div>
            </div>
        </li>
    );
}
