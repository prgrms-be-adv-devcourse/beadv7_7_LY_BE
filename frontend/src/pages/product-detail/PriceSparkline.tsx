import type { TradePoint } from "../../api/products";

// 기본은 M/NM 낙찰만 — 하위 컨디션까지 섞으면 선이 상태 차이에 출렁여 시세 흐름이 안 보인다
export const GRADED_CONDITIONS = ["MINT", "NEAR_MINT"];

export function PriceSparkline({
    trades,
    conditions = GRADED_CONDITIONS,
    width = 460,
    height = 120,
}: {
    trades: TradePoint[];
    conditions?: string[] | null; // null이면 전체 컨디션
    width?: number;
    height?: number;
}) {
    const points = trades
        .filter((t) => conditions === null || conditions.includes(t.condition))
        .sort((a, b) => a.tradedAt.localeCompare(b.tradedAt));
    if (points.length < 2) {
        return <p className="text-sm text-muted">시세 데이터가 부족합니다.</p>;
    }
    const xs = points.map((p) => new Date(p.tradedAt).getTime());
    const ys = points.map((p) => p.price);
    const minX = Math.min(...xs);
    const maxX = Math.max(...xs);
    const minY = Math.min(...ys);
    const maxY = Math.max(...ys);
    const nx = (v: number) => 4 + ((v - minX) / (maxX - minX || 1)) * (width - 8);
    const ny = (v: number) => height - 4 - ((v - minY) / (maxY - minY || 1)) * (height - 8);
    const path = points.map((_, i) => `${i ? "L" : "M"}${nx(xs[i]).toFixed(1)} ${ny(ys[i]).toFixed(1)}`).join(" ");
    const area = `${path} L ${nx(maxX).toFixed(1)} ${height} L ${nx(minX).toFixed(1)} ${height} Z`;
    const lastX = nx(xs[xs.length - 1]);
    const lastY = ny(ys[ys.length - 1]);
    return (
        <svg
            viewBox={`0 0 ${width} ${height}`}
            className="block w-full"
            role="img"
            aria-label="낙찰가 추이"
            preserveAspectRatio="none"
        >
            <path d={area} fill="var(--color-up)" opacity="0.09" />
            <path d={path} fill="none" stroke="var(--color-up)" strokeWidth="1.8" strokeLinejoin="round" strokeLinecap="round" />
            <circle cx={lastX} cy={lastY} r="3" fill="var(--color-up)" />
        </svg>
    );
}
