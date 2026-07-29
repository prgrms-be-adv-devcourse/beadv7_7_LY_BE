import { parseServerTime, type BidInfo } from "../../api/auctions";
import { formatWon } from "../../components/AuctionCard";

function formatAgo(placedAt: string): string {
    const diffMin = Math.floor((Date.now() - parseServerTime(placedAt)) / 60_000);
    if (diffMin < 1) return "방금";
    if (diffMin < 60) return `${diffMin}분 전`;
    const diffHour = Math.floor(diffMin / 60);
    if (diffHour < 24) return `${diffHour}시간 전`;
    return `${Math.floor(diffHour / 24)}일 전`;
}

export function BidLog({ entries }: { entries: BidInfo[] }) {
    if (entries.length === 0) {
        return <p className="px-5 py-10 text-center text-sm text-muted">아직 입찰이 없습니다. 첫 입찰자가 되어보세요.</p>;
    }
    return (
        <div>
            {entries.map((entry, i) => (
                <div
                    key={`${entry.bidder}-${entry.amount}-${i}`}
                    className="grid grid-cols-[1fr_auto_auto] items-center gap-4 border-b border-line px-5 py-3 last:border-b-0"
                >
                    <span className="text-[13px] text-muted">{entry.bidder}</span>
                    <span className="font-mono text-sm font-bold tabular-nums">{formatWon(entry.amount)}</span>
                    <span className="min-w-[62px] text-right text-[11.5px] text-faint">{formatAgo(entry.placedAt)}</span>
                </div>
            ))}
        </div>
    );
}
