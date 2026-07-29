// 로그인 세션 보관 — JWT의 subject가 memberId라서 디코드해 X-Member-Id 헤더 값으로 쓴다.
// (로컬에는 게이트웨이가 없어 프론트가 직접 X-Member-Id를 실어 보낸다)

const STORAGE_KEY = "groove_session";

export interface Session {
    token: string;
    memberId: number;
    displayName: string;
}

export function decodeMemberId(token: string): number | null {
    const payload = token.split(".")[1];
    if (!payload) return null;
    try {
        const json = JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
        const id = Number(json.sub);
        return Number.isFinite(id) ? id : null;
    } catch {
        return null;
    }
}

export function saveSession(session: Session): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
}

export function loadSession(): Session | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    try {
        const parsed = JSON.parse(raw) as Session;
        if (!parsed.token || !Number.isFinite(parsed.memberId)) return null;
        return parsed;
    } catch {
        return null;
    }
}

export function clearSession(): void {
    localStorage.removeItem(STORAGE_KEY);
}
