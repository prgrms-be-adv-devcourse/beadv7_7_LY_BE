interface DateRangeFilterProps {
    from: string;
    to: string;
    onChange: (patch: { from?: string; to?: string }) => void;
}

export function DateRangeFilter({ from, to, onChange }: DateRangeFilterProps) {
    return (
        <>
            <span className="text-[11px] font-bold uppercase tracking-wider text-faint">기간</span>
            <input
                type="date"
                value={from}
                max={to || undefined}
                onChange={(e) => onChange({ from: e.target.value })}
                aria-label="조회 시작일"
                className="rounded-lg border border-line bg-surface px-2.5 py-1.5 text-[13px] font-semibold outline-none"
            />
            <span className="text-muted">~</span>
            <input
                type="date"
                value={to}
                min={from || undefined}
                onChange={(e) => onChange({ to: e.target.value })}
                aria-label="조회 종료일"
                className="rounded-lg border border-line bg-surface px-2.5 py-1.5 text-[13px] font-semibold outline-none"
            />
            {(from || to) && (
                <button
                    type="button"
                    onClick={() => onChange({ from: "", to: "" })}
                    className="text-xs text-muted underline hover:text-ink"
                >
                    기간 해제
                </button>
            )}
        </>
    );
}
