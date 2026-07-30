import { loadSession } from "../auth/session";

// 백엔드(게이트웨이) 주소. 비워두면 지금까지처럼 개발 서버의 중계 설정을 탄다.
// 값을 넣으면 브라우저가 그 주소로 직접 요청한다 — 이때 게이트웨이에 출처 허용 설정이 있어야 한다
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

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
        res = await fetch(`${API_BASE_URL}${path}`, init);
    } catch {
        throw new ApiError("NETWORK", "서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.", 0);
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

// 회원 식별이 필요한 API용 헤더 — 로그인 토큰을 보낸다.
// 게이트웨이가 이 토큰을 검사해 회원 번호를 알아내고, 그 번호를 헤더에 담아 서비스로 넘긴다.
// 프론트가 회원 번호를 직접 만들어 보내던 방식은 번호만 바꿔 남의 계정으로 요청할 수 있어 없앴다
function memberHeaders(): Record<string, string> {
    const session = loadSession();
    return session ? { Authorization: `Bearer ${session.token}` } : {};
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

export function apiPut<T>(path: string, payload?: unknown): Promise<T> {
    return request<T>(path, {
        method: "PUT",
        headers: { "Content-Type": "application/json", ...memberHeaders() },
        body: payload === undefined ? undefined : JSON.stringify(payload),
    });
}

export function apiPatch<T>(path: string, payload?: unknown): Promise<T> {
    return request<T>(path, {
        method: "PATCH",
        headers: { "Content-Type": "application/json", ...memberHeaders() },
        body: payload === undefined ? undefined : JSON.stringify(payload),
    });
}

export function apiDelete<T>(path: string): Promise<T> {
    return request<T>(path, { method: "DELETE", headers: memberHeaders() });
}
