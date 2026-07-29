import { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { browseProducts, searchProducts, type ProductSearchCard } from "../api/products";
import { useDebouncedValue } from "../hooks/useDebouncedValue";
import { VinylCover } from "../components/VinylCover";
import { QueryState } from "../components/QueryState";
import { Pagination } from "../components/Pagination";

const PAGE_SIZE = 20;

function SkeletonGrid() {
    return (
        <div className="grid grid-cols-2 gap-3.5 md:grid-cols-4">
            {Array.from({ length: 8 }, (_, i) => (
                <div key={i} className="animate-pulse rounded-xl border border-line bg-surface p-3">
                    <div className="aspect-square rounded-md bg-surface2" />
                    <div className="mt-3 h-4 w-3/4 rounded bg-surface2" />
                    <div className="mt-2 h-3 w-1/2 rounded bg-surface2" />
                </div>
            ))}
        </div>
    );
}

function ProductCard({ card }: { card: ProductSearchCard }) {
    return (
        <Link
            to={`/products/${card.productId}`}
            className="rounded-xl border border-line bg-surface p-3 shadow-sm transition-[transform,border-color] duration-150 hover:-translate-y-0.5 hover:border-line-strong"
        >
            <VinylCover title={card.title} artist={card.artistName} imageUrl={card.coverImageUrl} />
            <div className="mt-2.5 truncate font-display text-sm font-bold">{card.title}</div>
            <div className="truncate text-xs text-muted">{card.artistName}</div>
            <div className="mt-2 flex gap-1.5">
                <span className="rounded bg-surface2 px-1.5 py-0.5 text-[10.5px] font-semibold text-muted">
                    {card.releaseYear}
                </span>
                <span className="rounded bg-surface2 px-1.5 py-0.5 text-[10.5px] font-semibold text-muted">
                    {card.pressType === "ORIGINAL" ? "오리지널" : "재발매"}
                </span>
            </div>
        </Link>
    );
}

function BrowseList() {
    const query = useQuery({
        queryKey: ["productBrowse"],
        queryFn: browseProducts,
    });
    return (
        <QueryState
            isLoading={query.isPending}
            error={query.error}
            isEmpty={!query.data || query.data.length === 0}
            emptyMessage="등록된 상품이 없습니다. 시드 플래그(product.seed.enabled)를 켜고 core-service를 재기동했는지 확인하세요."
            loadingFallback={<SkeletonGrid />}
        >
            <p className="mb-3 text-[13px] text-muted">
                등록된 릴리스 <b className="font-mono tabular-nums">{query.data?.length}</b>건
            </p>
            <div className="grid grid-cols-2 gap-3.5 md:grid-cols-4">
                {query.data?.map((card) => (
                    <ProductCard key={card.productId} card={card} />
                ))}
            </div>
        </QueryState>
    );
}

export function CatalogPage() {
    const [searchParams, setSearchParams] = useSearchParams();
    const urlQuery = searchParams.get("q") ?? "";
    const page = Math.max(0, Number(searchParams.get("page") ?? "0") || 0);

    const [input, setInput] = useState(urlQuery);
    const debounced = useDebouncedValue(input, 300);
    const lastSyncedQuery = useRef(urlQuery);

    // 입력창 → URL: 디바운스된 입력이 바뀌었을 때만 URL을 갱신한다
    useEffect(() => {
        const next = debounced.trim();
        if (next === urlQuery) {
            lastSyncedQuery.current = urlQuery;
            return;
        }
        lastSyncedQuery.current = next;
        setSearchParams(next ? { q: next, page: "0" } : {}, { replace: true });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [debounced]);

    // URL → 입력창: 헤더 검색이나 뒤로가기 등 바깥에서 URL이 바뀌면 입력창을 URL에 맞춘다
    useEffect(() => {
        if (urlQuery !== lastSyncedQuery.current) {
            lastSyncedQuery.current = urlQuery;
            setInput(urlQuery);
        }
    }, [urlQuery]);

    const query = useQuery({
        queryKey: ["productSearch", urlQuery, page],
        queryFn: () => searchProducts(urlQuery, page, PAGE_SIZE),
        enabled: urlQuery.trim().length > 0,
        placeholderData: keepPreviousData,
    });

    return (
        <div>
            <p className="text-[11px] font-bold uppercase tracking-widest text-brand">카탈로그</p>
            <h2 className="text-lg font-bold tracking-tight">릴리스 검색</h2>
            <p className="mb-4 mt-1 text-[13.5px] text-muted">
                앨범(릴리스) 단위로 탐색합니다 — 검색어가 없으면 등록된 릴리스를 둘러봅니다.
            </p>
            <div className="mb-5 flex max-w-[520px] overflow-hidden rounded-xl border-[1.5px] border-line-strong bg-surface focus-within:border-brand">
                <span aria-hidden="true" className="flex items-center pl-4 text-faint">⌕</span>
                <input
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    placeholder="제목·아티스트·카탈로그 번호로 검색"
                    aria-label="카탈로그 검색"
                    className="flex-1 bg-transparent px-3 py-3 text-[15px] outline-none placeholder:text-faint"
                />
            </div>

            {urlQuery.trim() === "" ? (
                <BrowseList />
            ) : (
                <QueryState
                    isLoading={query.isPending}
                    error={query.error}
                    isEmpty={!query.data || query.data.content.length === 0}
                    emptyMessage={<>‘{urlQuery}’ 검색 결과가 없습니다. 다른 검색어를 시도해보세요.</>}
                    loadingFallback={<SkeletonGrid />}
                >
                    <p className="mb-3 text-[13px] text-muted">
                        총 <b className="font-mono tabular-nums">{query.data?.totalElements}</b>건
                    </p>
                    <div className={`grid grid-cols-2 gap-3.5 md:grid-cols-4 ${query.isFetching ? "opacity-60" : ""}`}>
                        {query.data?.content.map((card) => (
                            <ProductCard key={card.productId} card={card} />
                        ))}
                    </div>
                    {query.data && (
                        <Pagination
                            page={page}
                            hasNext={query.data.hasNext}
                            totalElements={query.data.totalElements}
                            size={PAGE_SIZE}
                            onPage={(next) => setSearchParams({ q: urlQuery, page: String(next) })}
                        />
                    )}
                </QueryState>
            )}
        </div>
    );
}
