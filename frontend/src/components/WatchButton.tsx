import { useState } from "react";
import { useNavigate } from "react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { AuctionStatus } from "../api/auctions";
import { fetchWatchedAuctions, toWatchedAuctionIds, unwatchAuction, watchAuction } from "../api/watchlist";
import { ApiError } from "../api/client";
import { loadSession } from "../auth/session";

export const WATCHED_AUCTIONS_KEY = ["watchlist", "auctions"];

export function useWatchedAuctions() {
    const session = loadSession();
    return useQuery({
        queryKey: WATCHED_AUCTIONS_KEY,
        queryFn: fetchWatchedAuctions,
        enabled: session !== null,
        staleTime: 30_000,
    });
}

// 서버는 아직 시작 전이거나 진행 중인 경매만 관심 목록에 넣어준다.
// 끝난 경매는 새로 담을 수 없고, 이미 담아둔 것을 빼는 것만 가능하다
function isWatchable(status: AuctionStatus): boolean {
    return status === "SCHEDULED" || status === "RUNNING" || status === "CLOSING";
}

interface WatchButtonProps {
    auctionId: number;
    status: AuctionStatus;
    variant?: "overlay" | "inline";
}

export function WatchButton({ auctionId, status, variant = "overlay" }: WatchButtonProps) {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [error, setError] = useState<string | null>(null);
    const session = loadSession();
    const watched = useWatchedAuctions();
    const isWatched = watched.data ? toWatchedAuctionIds(watched.data).has(auctionId) : false;

    const toggle = useMutation({
        mutationFn: async () => {
            if (isWatched) await unwatchAuction(auctionId);
            else await watchAuction(auctionId);
        },
        onSuccess: () => {
            setError(null);
            queryClient.invalidateQueries({ queryKey: ["watchlist"] });
        },
        onError: (e: unknown) => {
            setError(e instanceof ApiError ? e.message : "관심 경매 처리 중 문제가 생겼습니다.");
        },
    });

    function click(e: React.MouseEvent) {
        e.preventDefault();
        e.stopPropagation();
        if (!session) {
            navigate("/login");
            return;
        }
        toggle.mutate();
    }

    if (!isWatched && !isWatchable(status)) {
        return null;
    }

    const label = isWatched ? "관심 해제" : "관심 등록";

    if (variant === "overlay") {
        return (
            <button
                type="button"
                onClick={click}
                disabled={toggle.isPending}
                aria-pressed={isWatched}
                aria-label={label}
                title={error ?? label}
                className={`absolute bottom-2 right-2 z-[2] grid h-8 w-8 place-items-center rounded-full border text-[14px] leading-none shadow-sm transition-colors disabled:opacity-50 ${
                    isWatched
                        ? "border-amber bg-amber text-white"
                        : "border-line bg-white/90 text-muted hover:border-amber hover:text-amber"
                }`}
            >
                {isWatched ? "★" : "☆"}
            </button>
        );
    }

    return (
        <div>
            <button
                type="button"
                onClick={click}
                disabled={toggle.isPending}
                aria-pressed={isWatched}
                className={`inline-flex items-center gap-1.5 rounded-lg border px-3 py-1.5 text-[13px] font-semibold transition-colors disabled:opacity-50 ${
                    isWatched
                        ? "border-amber bg-surface2 text-amber"
                        : "border-line bg-surface text-muted hover:border-amber hover:text-amber"
                }`}
            >
                <span aria-hidden="true">{isWatched ? "★" : "☆"}</span>
                {label}
            </button>
            {error && <p className="mt-1.5 text-xs font-semibold text-live">{error}</p>}
        </div>
    );
}
