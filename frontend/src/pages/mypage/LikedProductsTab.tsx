import { Link } from "react-router";
import { useInfiniteQuery } from "@tanstack/react-query";
import { fetchLikedProducts } from "../../api/wishlist";
import { QueryState } from "../../components/QueryState";
import { VinylCover } from "../../components/VinylCover";
import { LikeButton } from "../../components/LikeButton";

export function LikedProductsTab() {
    // 이 목록만 페이지 번호가 아니라 커서로 넘어와서, 다음 장을 이어 붙이는 방식으로 그린다
    const query = useInfiniteQuery({
        queryKey: ["wishlist", "likedProducts"],
        queryFn: ({ pageParam }) => fetchLikedProducts(pageParam),
        initialPageParam: null as number | null,
        getNextPageParam: (lastPage) => (lastPage.hasNext ? lastPage.nextCursor : undefined),
    });

    const items = query.data?.pages.flatMap((page) => page.content) ?? [];

    return (
        <div>
            <h3 className="mb-1 text-base font-bold tracking-tight">찜한 상품</h3>
            <p className="mb-4 text-[13.5px] text-muted">
                카탈로그에서 하트를 누른 릴리스입니다. 하트를 다시 누르면 목록에서 빠집니다.
            </p>

            <QueryState
                isLoading={query.isPending}
                error={query.error}
                isEmpty={items.length === 0}
                emptyMessage="아직 찜한 상품이 없습니다. 카탈로그에서 하트를 눌러 담아보세요."
            >
                <div className="grid grid-cols-2 gap-3.5 md:grid-cols-4">
                    {items.map((item) => (
                        <Link
                            key={item.id}
                            to={`/products/${item.productId}`}
                            className="rounded-xl border border-line bg-surface p-3 shadow-sm transition-[transform,border-color] duration-150 hover:-translate-y-0.5 hover:border-line-strong"
                        >
                            <div className="relative">
                                <VinylCover
                                    title={item.title}
                                    artist={item.artistName}
                                    imageUrl={item.coverImageUrl}
                                />
                                <LikeButton productId={item.productId} />
                            </div>
                            <div className="mt-2.5 truncate font-display text-sm font-bold">{item.title}</div>
                            <div className="truncate text-xs text-muted">
                                {item.artistName}
                                {item.releaseYear ? ` · ${item.releaseYear}` : ""}
                            </div>
                        </Link>
                    ))}
                </div>
                {query.hasNextPage && (
                    <div className="mt-6 text-center">
                        <button
                            type="button"
                            disabled={query.isFetchingNextPage}
                            onClick={() => query.fetchNextPage()}
                            className="rounded-lg border border-line bg-surface px-4 py-2 text-sm font-semibold hover:border-line-strong disabled:opacity-50"
                        >
                            {query.isFetchingNextPage ? "불러오는 중…" : "더 보기"}
                        </button>
                    </div>
                )}
            </QueryState>
        </div>
    );
}
