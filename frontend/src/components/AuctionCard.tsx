import { Link } from "react-router";
import { parseServerTime, type AuctionListItem } from "../api/auctions";
import { Countdown } from "./Countdown";
import { VinylCover } from "./VinylCover";
import { WatchButton } from "./WatchButton";

export function formatWon(n: number): string {
    return "₩" + n.toLocaleString("ko-KR");
}

const STATUS_BADGES: Record<string, { label: string; className: string }> = {
    SCHEDULED: { label: "시작 전", className: "bg-surface2 text-muted" },
    ENDED_WON: { label: "낙찰", className: "bg-up/90 text-white" },
    ENDED_FAILED: { label: "유찰", className: "bg-faint/80 text-white" },
    CLOSING: { label: "처리 중", className: "bg-amber/90 text-white" },
};

export function AuctionCard({ auction, className = "" }: { auction: AuctionListItem; className?: string }) {
    const badge = STATUS_BADGES[auction.status];
    return (
        <Link
            to={`/auctions/${auction.auctionId}`}
            className={`flex flex-col rounded-xl border border-line bg-surface p-3 shadow-sm transition-[transform,border-color] duration-150 hover:-translate-y-0.5 hover:border-line-strong ${className}`}
        >
            <div className="relative">
                {auction.pressType && (
                    <span className="absolute left-2 top-2 z-[2] rounded bg-white/90 px-1.5 py-0.5 text-[10px] font-bold text-ink">
                        {auction.pressType === "ORIGINAL" ? "오리지널" : "재발매"}
                    </span>
                )}
                {badge && (
                    <span className={`absolute right-2 top-2 z-[2] rounded px-1.5 py-0.5 text-[10px] font-bold ${badge.className}`}>
                        {badge.label}
                    </span>
                )}
                <VinylCover title={auction.title} artist={auction.artistName} imageUrl={auction.thumbnail} />
                <WatchButton auctionId={auction.auctionId} status={auction.status} />
            </div>
            <div className="mt-2.5 truncate font-display text-sm font-bold">{auction.title}</div>
            <div className="truncate text-xs text-muted">
                {auction.artistName}
                {auction.releaseYear ? ` · ${auction.releaseYear}` : ""}
            </div>
            <div className="mt-2.5 flex items-end justify-between border-t border-line pt-2.5">
                <div>
                    <div className="text-[10px] font-semibold uppercase tracking-wider text-faint">
                        {auction.bidCount > 0 ? "현재가" : "시작가"}
                    </div>
                    <div className="font-mono text-base font-bold tabular-nums">{formatWon(auction.finalPrice)}</div>
                </div>
                <div className="text-right text-[11px] text-muted">
                    <span className="font-mono">{auction.bidCount}</span> 입찰
                    <br />
                    <Countdown endsAt={parseServerTime(auction.endAt)} />
                </div>
            </div>
        </Link>
    );
}
