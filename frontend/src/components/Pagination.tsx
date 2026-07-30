interface PaginationProps {
    page: number;
    hasNext: boolean;
    totalElements?: number;
    // 총 개수 없이 페이지 수만 내려주는 API(지갑 거래 내역)용 — 있으면 totalElements 계산보다 우선한다
    totalPageCount?: number;
    size: number;
    onPage: (page: number) => void;
}

export function Pagination({ page, hasNext, totalElements, totalPageCount, size, onPage }: PaginationProps) {
    const totalPages =
        totalPageCount ?? (totalElements !== undefined ? Math.max(1, Math.ceil(totalElements / size)) : null);
    return (
        <nav aria-label="페이지 이동" className="mt-8 flex items-center justify-center gap-4">
            <button
                type="button"
                disabled={page === 0}
                onClick={() => onPage(page - 1)}
                className="rounded-lg border border-line bg-surface px-4 py-2 text-sm font-semibold text-ink transition-colors hover:border-line-strong disabled:cursor-not-allowed disabled:opacity-40"
            >
                ← 이전
            </button>
            <span className="font-mono text-sm text-muted tabular-nums">
                {page + 1}
                {totalPages !== null && ` / ${totalPages}`}
            </span>
            <button
                type="button"
                disabled={!hasNext}
                onClick={() => onPage(page + 1)}
                className="rounded-lg border border-line bg-surface px-4 py-2 text-sm font-semibold text-ink transition-colors hover:border-line-strong disabled:cursor-not-allowed disabled:opacity-40"
            >
                다음 →
            </button>
        </nav>
    );
}
