import { ApiError, apiPost } from "./client";
import { decodeMemberId, saveSession, type Session } from "../auth/session";

interface LoginResult {
    authToken: string;
}

// member-service(8081) 로그인 — vite proxy가 /api/v1/auth를 8081로 넘긴다
export async function login(email: string, password: string): Promise<Session> {
    const result = await apiPost<LoginResult>("/api/v1/auth/login", { email, password });
    const memberId = decodeMemberId(result.authToken);
    if (memberId === null) {
        throw new ApiError("TOKEN_PARSE", "로그인 토큰을 해석하지 못했습니다.", 0);
    }
    const session: Session = {
        token: result.authToken,
        memberId,
        displayName: email.split("@")[0],
    };
    saveSession(session);
    return session;
}
