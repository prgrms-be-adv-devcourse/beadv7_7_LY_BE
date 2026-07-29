import { useEffect, useState } from "react";

export function formatRemaining(ms: number): string {
    if (ms <= 0) return "종료";
    const totalSec = Math.floor(ms / 1000);
    const days = Math.floor(totalSec / 86400);
    const hours = Math.floor((totalSec % 86400) / 3600);
    const minutes = Math.floor((totalSec % 3600) / 60);
    const seconds = totalSec % 60;
    const pad = (n: number) => String(n).padStart(2, "0");
    if (days > 0) return `${days}일 ${pad(hours)}:${pad(minutes)}`;
    return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
}

export function useNow(intervalMs = 1000): number {
    const [now, setNow] = useState(() => Date.now());
    useEffect(() => {
        const timer = setInterval(() => setNow(Date.now()), intervalMs);
        return () => clearInterval(timer);
    }, [intervalMs]);
    return now;
}

export function Countdown({ endsAt }: { endsAt: number }) {
    const now = useNow();
    const remaining = endsAt - now;
    const soon = remaining > 0 && remaining < 3600_000;
    return (
        <span
            className={`inline-flex items-center gap-1.5 font-mono text-xs font-bold tabular-nums ${
                soon ? "text-live" : "text-ink"
            }`}
        >
            <span
                className={`h-1.5 w-1.5 rounded-full ${
                    soon ? "bg-live animate-[live-pulse_1.1s_infinite] motion-reduce:animate-none" : "bg-faint"
                }`}
            />
            {formatRemaining(remaining)}
        </span>
    );
}
