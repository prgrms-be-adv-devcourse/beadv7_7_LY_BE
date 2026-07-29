import { formatRemaining, useNow } from "../../components/Countdown";

// C안 포인트 — VU미터 바늘이 경매 진행률(시작→마감)을 가리킨다. 앰버 컬러는 이 컴포넌트 전용.
export function VuCountdown({ startedAt, endsAt }: { startedAt: number; endsAt: number }) {
    const now = useNow();
    const total = Math.max(1, endsAt - startedAt);
    const elapsed = Math.min(Math.max(now - startedAt, 0), total);
    const ratio = elapsed / total;
    // 바늘 각도: 수직 기준 -72°(시작, 왼쪽) → +72°(마감, 오른쪽)
    const angleRad = ((-72 + ratio * 144) * Math.PI) / 180;
    const needleX = 100 + 62 * Math.sin(angleRad);
    const needleY = 88 - 62 * Math.cos(angleRad);
    return (
        <div className="mt-4 rounded-xl bg-[#201C15] p-4">
            <svg viewBox="0 0 200 100" role="img" aria-label="마감까지 남은 시간 게이지" className="block w-full">
                <path d="M 30 88 A 74 74 0 0 1 170 88" fill="none" stroke="#35301F" strokeWidth="7" strokeLinecap="round" />
                <path
                    d="M 30 88 A 74 74 0 0 1 170 88"
                    fill="none"
                    stroke="var(--color-amber)"
                    strokeWidth="7"
                    strokeLinecap="round"
                    strokeDasharray={`${ratio * 232} 232`}
                />
                <line x1="100" y1="88" x2={needleX.toFixed(1)} y2={needleY.toFixed(1)} stroke="var(--color-glow)" strokeWidth="2.5" />
                <circle cx="100" cy="88" r="5" fill="var(--color-glow)" />
                <text x="26" y="80" fill="#6B6455" fontSize="9" fontFamily="monospace">
                    시작
                </text>
                <text x="158" y="80" fill="#E06A3A" fontSize="9" fontFamily="monospace">
                    마감
                </text>
            </svg>
            <p className="mt-1 text-center font-mono text-sm font-bold tabular-nums text-glow [text-shadow:0_0_14px_rgba(243,196,118,.35)]">
                마감까지 {formatRemaining(endsAt - now)}
            </p>
        </div>
    );
}
