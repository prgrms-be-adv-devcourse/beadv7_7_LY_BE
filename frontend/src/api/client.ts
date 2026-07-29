import { loadSession } from "../auth/session";

export class ApiError extends Error {
    constructor(
        public readonly code: string,
        message: string,
        public readonly status: number,
    ) {
        super(message);
        this.name = "ApiError";
    }
}

interface ApiEnvelope<T> {
    success: boolean;
    data: T;
    error: { code: string; message: string } | null;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
    let res: Response;
    try {
        res = await fetch(path, init);
    } catch {
        throw new ApiError("NETWORK", "서버에 연결할 수 없습니다. 백엔드가 켜져 있는지 확인하세요.", 0);
    }
    const body = (await res.json().catch(() => null)) as ApiEnvelope<T> | null;
    if (!res.ok || !body || !body.success) {
        throw new ApiError(
            body?.error?.code ?? "UNKNOWN",
            body?.error?.message ?? `요청이 실패했습니다 (HTTP ${res.status})`,
            res.status,
        );
    }
    return body.data;
}

// 회원 식별이 필요한 API용 헤더 — 로그인 세션의 memberId를 X-Member-Id로 보낸다
function memberHeaders(): Record<string, string> {
    const session = loadSession();
    return session ? { "X-Member-Id": String(session.memberId) } : {};
}

export function apiGet<T>(path: string): Promise<T> {
    return request<T>(path, { headers: memberHeaders() });
}

export function apiPost<T>(path: string, payload?: unknown): Promise<T> {
    return request<T>(path, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...memberHeaders() },
        body: payload === undefined ? undefined : JSON.stringify(payload),
    });
}
