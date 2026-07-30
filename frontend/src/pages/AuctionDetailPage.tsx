import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { formatCondition, getAuctionDetail, parseServerTime, placeBid, type AuctionDetail } from "../api/auctions";
import { ApiError } from "../api/client";
import { loadSession } from "../auth/session";
import { VinylCover } from "../components/VinylCover";
import { QueryState } from "../components/QueryState";
import { WatchButton } from "../components/WatchButton";
import { formatWon } from "../components/AuctionCard";
import { VuCountdown } from "./auction-detail/VuCountdown";
import { BidLog } from "./auction-detail/BidLog";

const STATUS_LABELS: Record<string, string> = {
    SCHEDULED: "시작 전",
    RUNNING: "진행 중",
    CLOSING: "낙찰 처리 중",
    ENDED_WON: "낙찰 완료",
    ENDED_FAILED: "유찰",
    CANCELED: "취소됨",
};

function BidBox({ auction }: { auction: AuctionDetail }) {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [bidError, setBidError] = useState<string | null>(null);
    const session = loadSession();

    const currentPrice = auction.highestBidAmount ?? auction.startBidAmount;
    const nextBid = auction.nextMinBidAmount ?? auction.startBidAmount;

    const bidMutation = useMutation({
        mutationFn: () => placeBid(auction.auctionId, nextBid),
        onSuccess: () => {
            setBidError(null);
            queryClient.invalidateQueries({ queryKey: ["auction", String(auction.auctionId)] });
        },
        onError: (error) => {
            setBidError(error instanceof ApiError ? error.message : "입찰 처리 중 문제가 생겼습니다.");
        },
    });

    return (
        <aside className="rounded-2xl border-[1.5px] border-line-strong bg-surface p-5 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-wider text-faint">
                현재가 · <span className="font-mono tabular-nums">{auction.bidCount ?? 0}</span>건 입찰
            </p>
            <p className="mt-1 font-mono text-3xl font-bold tabular-nums tracking-tight">{formatWon(currentPrice)}</p>
            <p className="mt-1.5 text-xs text-muted">
                시작가 {formatWon(auction.startBidAmount)} · 입찰 단위 {formatWon(auction.bidUnit)}
            </p>
            {auction.myHighest && <p className="mt-1.5 text-xs font-bold text-up">내가 최고 입찰자입니다</p>}
            <VuCountdown startedAt={parseServerTime(auction.startAt)} endsAt={parseServerTime(auction.endAt)} />
            {session ? (
                <button
                    type="button"
                    onClick={() => bidMutation.mutate()}
                    disabled={bidMutation.isPending}
                    className="mt-4 w-full rounded-xl bg-live py-3.5 text-[15px] font-extrabold text-white transition-colors hover:bg-live/90 disabled:opacity-60"
                >
                    {bidMutation.isPending ? "입찰 중…" : `${formatWon(nextBid)} 입찰하기`}
                </button>
            ) : (
                <button
                    type="button"
                    onClick={() => navigate("/login")}
                    className="mt-4 w-full rounded-xl border-[1.5px] border-line-strong bg-paper py-3.5 text-[15px] font-extrabold transition-colors hover:border-brand hover:text-brand"
                >
                    로그인 후 입찰하기
                </button>
            )}
            {bidError && <p className="mt-2.5 text-center text-[12px] font-semibold text-live">{bidError}</p>}
        </aside>
    );
}

function EndedBox({ auction }: { auction: AuctionDetail }) {
    return (
        <aside className="rounded-2xl border-[1.5px] border-line-strong bg-surface p-5 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-wider text-faint">경매 결과</p>
            <p className="mt-1 text-xl font-bold">{STATUS_LABELS[auction.status] ?? auction.status}</p>
            {auction.status === "SCHEDULED" && (
                <p className="mt-2 text-sm text-muted">
                    {new Date(parseServerTime(auction.startAt)).toLocaleString("ko-KR")} 시작 예정 · 시작가{" "}
                    {formatWon(auction.startBidAmount)}
                </p>
            )}
            {auction.winningBid && (
                <p className="mt-2 text-sm text-muted">
                    낙찰가 <b className="font-mono tabular-nums text-ink">{formatWon(auction.winningBid.amount)}</b> ·{" "}
                    {auction.winningBid.bidder}
                </p>
            )}
            {auction.status === "ENDED_FAILED" && <p className="mt-2 text-sm text-muted">입찰자가 없어 유찰되었습니다.</p>}
            {auction.status === "CLOSING" && <p className="mt-2 text-sm text-muted">마감 후 낙찰 결과를 계산하고 있습니다.</p>}
        </aside>
    );
}

// 판매자가 올린 이 판의 사진과 설명. 위쪽 커버는 음반 자체의 대표 이미지라
// "내가 파는 판이 어떤 상태인가"는 알려주지 않는다 — 사는 사람이 실제로 보고 판단하는 곳이다
function ItemNotes({ description, images }: { description: string | null; images: string[] | null }) {
    const photos = images ?? [];
    const [zoomed, setZoomed] = useState<string | null>(null);

    return (
        <section className="mt-7 overflow-hidden rounded-2xl border border-line bg-surface shadow-sm">
            <div className="flex items-center justify-between border-b border-line px-5 py-4">
                <h3 className="text-[15px] font-bold">판매자가 올린 매물 정보</h3>
                {photos.length > 0 && (
                    <span className="text-xs text-muted">
                        사진 <span className="font-mono tabular-nums">{photos.length}</span>장
                    </span>
                )}
            </div>
            <div className="px-5 py-4">
                {photos.length > 0 && (
                    <div className="mb-4 flex flex-wrap gap-2.5">
                        {photos.map((photo, index) => (
                            <button
                                key={`${index}-${photo.slice(0, 32)}`}
                                type="button"
                                onClick={() => setZoomed(photo)}
                                className="overflow-hidden rounded-lg border border-line transition-colors hover:border-line-strong"
                            >
                                <img
                                    src={photo}
                                    alt={`매물 사진 ${index + 1}`}
                                    loading="lazy"
                                    className="h-32 w-32 object-cover"
                                />
                            </button>
                        ))}
                    </div>
                )}
                {description ? (
                    // 판매자가 넣은 줄바꿈을 그대로 살린다
                    <p className="max-w-[70ch] whitespace-pre-line text-[14px] leading-relaxed">{description}</p>
                ) : (
                    <p className="text-[13.5px] text-muted">판매자가 남긴 설명이 없습니다.</p>
                )}
                {photos.length === 0 && (
                    <p className="mt-2 text-[13.5px] text-muted">올라온 매물 사진이 없습니다.</p>
                )}
            </div>

            {zoomed && (
                <div
                    role="presentation"
                    onClick={() => setZoomed(null)}
                    className="fixed inset-0 z-50 grid place-items-center bg-black/70 p-6"
                >
                    <img src={zoomed} alt="매물 사진 크게 보기" className="max-h-full max-w-full rounded-lg" />
                </div>
            )}
        </section>
    );
}

export function AuctionDetailPage() {
    const { auctionId = "" } = useParams();

    const auctionQuery = useQuery({
        queryKey: ["auction", auctionId],
        queryFn: () => getAuctionDetail(auctionId),
        enabled: auctionId !== "",
    });

    const auction = auctionQuery.data;

    return (
        <QueryState
            isLoading={auctionQuery.isPending}
            error={auctionQuery.error}
            isEmpty={!auction}
            emptyMessage={
                <div>
                    존재하지 않는 경매입니다.
                    <div className="mt-3">
                        <Link to="/feed" className="font-semibold text-brand hover:underline">
                            경매 피드로 돌아가기 →
                        </Link>
                    </div>
                </div>
            }
        >
            {auction && (
                <div>
                    <Link to="/feed" className="mb-4 inline-flex items-center gap-1.5 text-[13px] font-semibold text-muted hover:text-ink">
                        ← 경매 피드로
                    </Link>
                    <div className="grid items-start gap-7 md:grid-cols-[1fr_380px]">
                        <div>
                            <div className="grid items-start gap-6 sm:grid-cols-[220px_1fr]">
                                <div>
                                    <VinylCover
                                        title={auction.product.title}
                                        artist={auction.product.artistName}
                                        imageUrl={auction.product.coverImageUrl}
                                        spin={auction.status === "RUNNING"}
                                        className="rounded-xl shadow"
                                    />
                                    <div className="mt-3">
                                        <WatchButton
                                            auctionId={auction.auctionId}
                                            status={auction.status}
                                            variant="inline"
                                        />
                                    </div>
                                </div>
                                <div>
                                    <h1 className="font-display text-2xl font-semibold leading-tight tracking-tight text-balance">
                                        <Link to={`/products/${auction.product.productId}`} className="hover:underline">
                                            {auction.product.title}
                                        </Link>
                                    </h1>
                                    <p className="mt-1 text-base font-bold text-brand">{auction.product.artistName}</p>
                                    <dl className="mt-4 grid grid-cols-[auto_1fr] gap-x-5 gap-y-2 text-[13.5px]">
                                        {[
                                            ["컨디션", formatCondition(auction.itemCondition)],
                                            ["프레스", auction.product.pressType === "ORIGINAL" ? "오리지널" : "재발매"],
                                            ["발매", auction.product.releaseYear],
                                            ["판매자", auction.seller.nickname],
                                            ["상태", STATUS_LABELS[auction.status] ?? auction.status],
                                        ].map(([label, value]) => (
                                            <div key={label} className="contents">
                                                <dt className="font-semibold text-faint">{label}</dt>
                                                <dd className="m-0 font-semibold">{value || "—"}</dd>
                                            </div>
                                        ))}
                                    </dl>
                                </div>
                            </div>

                            <ItemNotes description={auction.itemDescription} images={auction.itemImages} />

                            <section className="mt-7 overflow-hidden rounded-2xl border border-line bg-surface shadow-sm">
                                <div className="flex items-center justify-between border-b border-line px-5 py-4">
                                    <h3 className="text-[15px] font-bold">호가 로그</h3>
                                    <span className="text-xs text-muted">
                                        최근 <span className="font-mono tabular-nums">{auction.recentBids?.length ?? 0}</span>건
                                    </span>
                                </div>
                                <BidLog entries={auction.recentBids ?? []} />
                            </section>
                        </div>

                        {auction.status === "RUNNING" ? <BidBox auction={auction} /> : <EndedBox auction={auction} />}
                    </div>
                </div>
            )}
        </QueryState>
    );
}
