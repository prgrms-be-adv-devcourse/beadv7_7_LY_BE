import { Link } from "react-router";
import { useQueries } from "@tanstack/react-query";
import { formatAuctionStatus, getAuctionDetail, parseServerTime, type AuctionDetail } from "../../api/auctions";
import { MAX_WATCHED_AUCTIONS, type WatchedAuction } from "../../api/watchlist";
import { useWatchedAuctions, WatchButton } from "../../components/WatchButton";
import { QueryState } from "../../components/QueryState";
import { VinylCover } from "../../components/VinylCover";
import { Countdown } from "../../components/Countdown";
import { formatWon } from "../../components/AuctionCard";
import { formatDateTime } from "./format";

export function WatchedAuctionsTab() {
    const watched = useWatchedAuctions();
    const groups = watched.data ?? [];
    const auctionIds = groups.flatMap((group) => group.items.map((item) => item.auctionId));

    // 관심 목록 응답에는 경매 번호·현재가·시각만 있고 앨범 제목이나 커버가 없다.
    // 어떤 음반인지 보여주려면 항목마다 경매 상세를 따로 받아야 한다 (서버가 30건으로 막아둔 목록이다).
    // 경매 상세 화면과 같은 캐시 키를 써서, 상세를 이미 본 경매는 다시 부르지 않는다
    const details = useQueries({
        queries: auctionIds.map((auctionId) => ({
            queryKey: ["auction", String(auctionId)],
            queryFn: () => getAuctionDetail(auctionId),
            staleTime: 30_000,
        })),
    });
    const detailById = new Map<number, AuctionDetail | undefined>(
        auctionIds.map((auctionId, index) => [auctionId, details[index]?.data]),
    );

    return (
        <div>
            <h3 className="mb-1 text-base font-bold tracking-tight">관심 경매</h3>
            <p className="mb-4 text-[13.5px] text-muted">
                별을 눌러 담아둔 경매입니다. 진행 상태별로 묶어 보여주며, 최대 {MAX_WATCHED_AUCTIONS}건까지 담을 수
                있습니다.
            </p>

            <QueryState
                isLoading={watched.isPending}
                error={watched.error}
                isEmpty={auctionIds.length === 0}
                emptyMessage="담아둔 경매가 없습니다. 경매 피드에서 별을 눌러 담아보세요."
            >
                <div className="flex flex-col gap-6">
                    {groups.map((group) => (
                        <section key={group.status}>
                            <h4 className="mb-2 text-[11px] font-bold uppercase tracking-widest text-faint">
                                {formatAuctionStatus(group.status)}{" "}
                                <span className="font-mono tabular-nums">{group.items.length}</span>건
                            </h4>
                            <ul className="flex flex-col gap-2.5">
                                {group.items.map((item) => (
                                    <WatchedRow
                                        key={item.id}
                                        item={item}
                                        detail={detailById.get(item.auctionId)}
                                    />
                                ))}
                            </ul>
                        </section>
                    ))}
                </div>
            </QueryState>
        </div>
    );
}

function WatchedRow({ item, detail }: { item: WatchedAuction; detail: AuctionDetail | undefined }) {
    // 상세 조회가 아직이거나 실패하면 목록이 준 정보만으로 그린다
    const title = detail?.product.title ?? `경매 #${item.auctionId}`;
    const artist = detail?.product.artistName ?? "앨범 정보를 불러오지 못했습니다";
    const running = item.status === "RUNNING";

    return (
        <li className="flex items-center gap-3.5 rounded-xl border border-line bg-surface px-4 py-3.5">
            <VinylCover
                title={title}
                artist={detail?.product.artistName ?? ""}
                imageUrl={detail?.product.coverImageUrl}
                className="w-16 shrink-0"
            />
            <div className="min-w-0 grow">
                <Link to={`/auctions/${item.auctionId}`} className="truncate font-semibold hover:underline">
                    {title}
                </Link>
                <p className="truncate text-[13px] text-muted">{artist}</p>
                <p className="mt-1 text-[11.5px] text-faint">
                    {running
                        ? `마감 ${formatDateTime(item.endAt)}`
                        : `시작 ${formatDateTime(item.startAt)} · 마감 ${formatDateTime(item.endAt)}`}
                </p>
            </div>
            <div className="shrink-0 text-right">
                <div className="text-[10px] font-semibold uppercase tracking-wider text-faint">현재가</div>
                <div className="font-mono text-base font-bold tabular-nums">{formatWon(item.currentPrice)}</div>
                {running && (
                    <div className="mt-0.5">
                        <Countdown endsAt={parseServerTime(item.endAt)} />
                    </div>
                )}
            </div>
            <div className="shrink-0">
                <WatchButton auctionId={item.auctionId} status={item.status} variant="inline" />
            </div>
        </li>
    );
}
