export interface FeedFilterValues {
    genre: string;
    pressType: string;
    status: string;
    sort: string;
}

interface FeedFiltersProps {
    values: FeedFilterValues;
    onChange: (patch: Partial<Record<"genre" | "pressType" | "status" | "sort", string | null>>) => void;
}

// 시드 데이터에서 실제로 쓰이는 장르 위주
const GENRES = ["Rock", "Pop", "K-Pop", "Jazz", "Ballad", "R&B", "Folk", "Electronic", "Metal", "Soul"];

const PRESS_TYPES = [
    { value: "ORIGINAL", label: "오리지널" },
    { value: "REISSUE", label: "재발매" },
];

const STATUSES = [
    { value: "", label: "전체 상태" },
    { value: "RUNNING", label: "진행 중" },
    { value: "SCHEDULED", label: "시작 전" },
    { value: "ENDED_WON", label: "낙찰" },
    { value: "ENDED_FAILED", label: "유찰" },
];

const SORTS = [
    { value: "ending_soon", label: "마감 임박순" },
    { value: "price_desc", label: "현재가 높은순" },
    { value: "price_asc", label: "현재가 낮은순" },
    { value: "most_bids", label: "입찰 많은순" },
];

export function FeedFilters({ values, onChange }: FeedFiltersProps) {
    return (
        <div className="mb-5 flex flex-wrap items-center gap-2.5">
            <select
                value={values.genre}
                onChange={(e) => onChange({ genre: e.target.value || null })}
                aria-label="장르 필터"
                className="rounded-lg border border-line bg-surface px-2.5 py-1.5 text-[13px] font-semibold outline-none"
            >
                <option value="">전체 장르</option>
                {GENRES.map((g) => (
                    <option key={g} value={g}>
                        {g}
                    </option>
                ))}
            </select>
            <span className="h-5 w-px bg-line" />
            <span className="text-[11px] font-bold uppercase tracking-wider text-faint">프레스</span>
            {PRESS_TYPES.map((p) => (
                <button
                    key={p.value}
                    type="button"
                    aria-pressed={values.pressType === p.value}
                    onClick={() => onChange({ pressType: values.pressType === p.value ? null : p.value })}
                    className={`rounded-lg border px-2.5 py-1.5 text-xs font-semibold transition-colors ${
                        values.pressType === p.value
                            ? "border-brand bg-brand text-white"
                            : "border-line bg-surface text-muted hover:border-line-strong hover:text-ink"
                    }`}
                >
                    {p.label}
                </button>
            ))}
            <span className="h-5 w-px bg-line" />
            <select
                value={values.status}
                onChange={(e) => onChange({ status: e.target.value || null })}
                aria-label="상태 필터"
                className="rounded-lg border border-line bg-surface px-2.5 py-1.5 text-[13px] font-semibold outline-none"
            >
                {STATUSES.map((s) => (
                    <option key={s.value} value={s.value}>
                        {s.label}
                    </option>
                ))}
            </select>
            <select
                value={values.sort}
                onChange={(e) => onChange({ sort: e.target.value })}
                aria-label="정렬"
                className="ml-auto rounded-lg border border-line bg-surface px-2.5 py-1.5 text-[13px] font-semibold outline-none"
            >
                {SORTS.map((s) => (
                    <option key={s.value} value={s.value}>
                        {s.label}
                    </option>
                ))}
            </select>
        </div>
    );
}
