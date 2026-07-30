import type { ReactNode } from "react";
import { ApiError } from "../api/client";

interface QueryStateProps {
    isLoading: boolean;
    error: unknown;
    isEmpty: boolean;
    emptyMessage: ReactNode;
    loadingFallback?: ReactNode;
    children: ReactNode;
}

export function QueryState({ isLoading, error, isEmpty, emptyMessage, loadingFallback, children }: QueryStateProps) {
    if (isLoading) {
        return (
            loadingFallback ?? (
                <div className="flex items-center justify-center gap-3 py-16 text-muted">
                    <span className="h-5 w-5 animate-spin rounded-full border-2 border-line-strong border-t-brand" />
                    불러오는 중…
                </div>
            )
        );
    }
    if (error) {
        const message = error instanceof ApiError ? error.message : "요청 처리 중 문제가 생겼습니다.";
        const code = error instanceof ApiError ? error.code : null;
        return (
            <div className="rounded-xl border border-live/30 bg-live-bg px-5 py-8 text-center">
                <p className="font-semibold text-live">{message}</p>
                {code && <p className="mt-1 font-mono text-xs text-muted">{code}</p>}
                <p className="mt-2 text-sm text-muted">잠시 후 새로고침해주세요.</p>
            </div>
        );
    }
    if (isEmpty) {
        return <div className="rounded-xl border border-line bg-surface px-5 py-14 text-center text-muted">{emptyMessage}</div>;
    }
    return <>{children}</>;
}
