import { useState } from "react";
import { useNavigate } from "react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchLikedProductIds, likeProduct, unlikeProduct } from "../api/wishlist";
import { ApiError } from "../api/client";
import { loadSession } from "../auth/session";

export const LIKED_IDS_KEY = ["wishlist", "likedIds"];

// 찜한 상품 id 집합. 카드가 여러 개 붙어도 같은 쿼리 키를 쓰므로 요청은 화면당 한 번만 나간다
export function useLikedProductIds() {
    const session = loadSession();
    return useQuery({
        queryKey: LIKED_IDS_KEY,
        queryFn: fetchLikedProductIds,
        enabled: session !== null,
        staleTime: 30_000,
    });
}

interface LikeButtonProps {
    productId: number;
    // overlay는 커버 위에 겹치는 카드용, inline은 상세 화면의 글자 있는 버튼
    variant?: "overlay" | "inline";
}

export function LikeButton({ productId, variant = "overlay" }: LikeButtonProps) {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [error, setError] = useState<string | null>(null);
    const session = loadSession();
    const likedIds = useLikedProductIds();
    const liked = likedIds.data?.has(productId) ?? false;

    const toggle = useMutation({
        mutationFn: async () => {
            if (liked) await unlikeProduct(productId);
            else await likeProduct(productId);
        },
        onSuccess: () => {
            setError(null);
            queryClient.invalidateQueries({ queryKey: ["wishlist"] });
        },
        onError: (e: unknown) => {
            setError(e instanceof ApiError ? e.message : "찜 처리 중 문제가 생겼습니다.");
        },
    });

    // 카드가 링크 안에 있어서, 막지 않으면 하트를 눌러도 상세로 이동해버린다
    function click(e: React.MouseEvent) {
        e.preventDefault();
        e.stopPropagation();
        if (!session) {
            navigate("/login");
            return;
        }
        toggle.mutate();
    }

    const label = liked ? "찜 해제" : "찜하기";

    if (variant === "overlay") {
        return (
            <button
                type="button"
                onClick={click}
                disabled={toggle.isPending}
                aria-pressed={liked}
                aria-label={label}
                title={error ?? label}
                className={`absolute bottom-2 right-2 z-[2] grid h-8 w-8 place-items-center rounded-full border text-[15px] leading-none shadow-sm transition-colors disabled:opacity-50 ${
                    liked
                        ? "border-live bg-live text-white"
                        : "border-line bg-white/90 text-muted hover:border-live hover:text-live"
                }`}
            >
                {liked ? "♥" : "♡"}
            </button>
        );
    }

    return (
        <div>
            <button
                type="button"
                onClick={click}
                disabled={toggle.isPending}
                aria-pressed={liked}
                className={`inline-flex items-center gap-1.5 rounded-lg border px-3 py-1.5 text-[13px] font-semibold transition-colors disabled:opacity-50 ${
                    liked
                        ? "border-live bg-live-bg text-live"
                        : "border-line bg-surface text-muted hover:border-live hover:text-live"
                }`}
            >
                <span aria-hidden="true">{liked ? "♥" : "♡"}</span>
                {label}
            </button>
            {error && <p className="mt-1.5 text-xs font-semibold text-live">{error}</p>}
        </div>
    );
}
