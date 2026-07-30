import { useState } from "react";
import { Link } from "react-router";
import { keepPreviousData, useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    AUCTION_POLICY,
    deleteAuction,
    fetchHostedAuctions,
    formatAuctionStatus,
    getAuctionDetail,
    type HostedAuction,
} from "../../api/auctions";
import { ApiError } from "../../api/client";
import { QueryState } from "../../components/QueryState";
import { Pagination } from "../../components/Pagination";
import { VinylCover } from "../../components/VinylCover";
import { formatWon } from "../../components/AuctionCard";
import { isEditableNow, toInputValueFromServer } from "../auction-form/schedule";
import { formatDateTime } from "./format";

const PAGE_SIZE = 10;

export function HostedAuctionsTab() {
    const [page, setPage] = useState(0);

    const query = useQuery({
        queryKey: ["auctions", "hosted", page],
        queryFn: () => fetchHostedAuctions(page, PAGE_SIZE),
        placeholderData: keepPreviousData,
    });

    // 수정·취소 시한은 시작 시각에서 계산해야 하는데 이 목록에는 시작 시각이 없다.
    // 아직 시작하지 않은 경매만 상세를 따로 받아 시각을 채운다 (나머지는 어차피 손댈 수 없다)
    const scheduledIds = (query.data?.items ?? [])
        .filter((auction) => auction.status === "SCHEDULED")
        .map((auction) => auction.auctionId);
    const details = useQueries({
        queries: scheduledIds.map((auctionId) => ({
            queryKey: ["auction", String(auctionId)],
            queryFn: () => getAuctionDetail(auctionId),
            staleTime: 30_000,
        })),
    });
    const startAtById = new Map<number, string | undefined>(
        scheduledIds.map((auctionId, index) => [auctionId, details[index]?.data?.startAt]),
    );

    return (
        <div>
            <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
                <div>
                    <h3 className="mb-1 text-base font-bold tracking-tight">내가 등록한 경매</h3>
                    <p className="text-[13.5px] text-muted">
                        판매자로 올린 경매입니다. 시작 {AUCTION_POLICY.EDIT_DEADLINE_MINUTES}분 전까지만 고치거나
                        취소할 수 있고, 취소한 경매는 목록에서 빠집니다.
                    </p>
                </div>
                <Link
                    to="/auctions/new"
                    className="rounded-lg bg-brand px-3 py-1.5 text-sm font-semibold text-white hover:bg-brand-ink"
                >
                    경매 등록
                </Link>
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
                        <HostedRow
                            key={auction.auctionId}
                            auction={auction}
                            startAt={startAtById.get(auction.auctionId)}
                        />
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

function HostedRow({ auction, startAt }: { auction: HostedAuction; startAt: string | undefined }) {
    const queryClient = useQueryClient();
    const [error, setError] = useState<string | null>(null);

    // 시작 시각을 아직 못 받았으면 시한을 판단할 수 없으니 버튼을 잠가둔다
    const editable = startAt !== undefined && isEditableNow(toInputValueFromServer(startAt), new Date());
    const scheduled = auction.status === "SCHEDULED";

    const cancel = useMutation({
        mutationFn: () => deleteAuction(auction.auctionId),
        onSuccess: () => {
            setError(null);
            queryClient.invalidateQueries({ queryKey: ["auctions"] });
        },
        onError: (e: unknown) => {
            setError(e instanceof ApiError ? e.message : "경매 취소 중 문제가 생겼습니다.");
        },
    });

    return (
        <li className="rounded-xl border border-line bg-surface px-4 py-3.5">
            <div className="flex items-center gap-3.5">
                <VinylCover title={auction.title} artist={auction.artistName} className="w-16 shrink-0" />
                <div className="min-w-0 grow">
                    <div className="flex flex-wrap items-center gap-2">
                        <span className="rounded-md bg-surface2 px-2 py-0.5 text-[11px] font-bold text-muted">
                            {formatAuctionStatus(auction.status)}
                        </span>
                        <span className="font-mono text-[11px] text-faint">경매번호 {auction.auctionId}</span>
                    </div>
                    <Link
                        to={`/auctions/${auction.auctionId}`}
                        className="mt-1 block truncate font-semibold hover:underline"
                    >
                        {auction.title}
                    </Link>
                    <p className="truncate text-[13px] text-muted">{auction.artistName}</p>
                    {scheduled && startAt && (
                        <p className="mt-0.5 text-[11.5px] text-faint">시작 {formatDateTime(startAt)}</p>
                    )}
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
            </div>

            {scheduled && (
                <div className="mt-3 flex flex-wrap items-center gap-2 border-t border-line pt-3">
                    {editable ? (
                        <>
                            <Link
                                to={`/auctions/${auction.auctionId}/edit`}
                                className="rounded-lg border border-line bg-paper px-3 py-1.5 text-xs font-semibold hover:border-line-strong"
                            >
                                수정
                            </Link>
                            <button
                                type="button"
                                disabled={cancel.isPending}
                                onClick={() => {
                                    if (window.confirm("이 경매를 취소할까요? 목록에서 사라집니다.")) {
                                        cancel.mutate();
                                    }
                                }}
                                className="rounded-lg border border-line bg-paper px-3 py-1.5 text-xs font-semibold text-live hover:border-live disabled:opacity-50"
                            >
                                경매 취소
                            </button>
                        </>
                    ) : (
                        <span className="text-[11.5px] text-muted">
                            {startAt === undefined
                                ? "시작 시각을 불러오는 중이라 수정·취소를 잠시 막아뒀습니다."
                                : `시작 ${AUCTION_POLICY.EDIT_DEADLINE_MINUTES}분 전이 지나 더는 고치거나 취소할 수 없습니다.`}
                        </span>
                    )}
                    {error && <span className="text-xs font-semibold text-live">{error}</span>}
                </div>
            )}
        </li>
    );
}
