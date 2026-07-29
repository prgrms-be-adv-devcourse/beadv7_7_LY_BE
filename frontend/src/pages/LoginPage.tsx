import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router";
import { login } from "../api/auth";
import { ApiError } from "../api/client";

export function LoginPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const signedUp = Boolean((location.state as { signedUp?: boolean } | null)?.signedUp);
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState<string | null>(null);
    const [pending, setPending] = useState(false);

    async function submit(e: React.FormEvent) {
        e.preventDefault();
        if (!email.trim() || !password.trim()) {
            setError("이메일과 비밀번호를 모두 입력하세요.");
            return;
        }
        setPending(true);
        setError(null);
        try {
            await login(email.trim(), password);
            navigate("/");
        } catch (err) {
            setError(err instanceof ApiError ? err.message : "로그인 처리 중 문제가 생겼습니다.");
        } finally {
            setPending(false);
        }
    }

    return (
        <div className="mx-auto max-w-sm">
            <div className="rounded-2xl border border-line bg-surface p-7 shadow-sm">
                <h1 className="font-display text-2xl font-semibold tracking-tight">로그인</h1>
                <p className="mt-1 text-[13px] text-muted">member-service 계정으로 로그인합니다.</p>
                {signedUp && (
                    <p className="mt-3 rounded-lg bg-up/10 px-3 py-2 text-[13px] font-semibold text-up">
                        가입이 완료됐습니다. 로그인해주세요.
                    </p>
                )}
                <form onSubmit={submit} className="mt-5 flex flex-col gap-3">
                    <label className="text-[13px] font-semibold">
                        이메일
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="you@example.com"
                            className="mt-1 w-full rounded-lg border border-line bg-paper px-3 py-2.5 text-sm outline-none placeholder:text-faint focus:border-brand"
                        />
                    </label>
                    <label className="text-[13px] font-semibold">
                        비밀번호
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="••••••••"
                            className="mt-1 w-full rounded-lg border border-line bg-paper px-3 py-2.5 text-sm outline-none placeholder:text-faint focus:border-brand"
                        />
                    </label>
                    {error && <p className="text-[13px] font-semibold text-live">{error}</p>}
                    <button
                        type="submit"
                        disabled={pending}
                        className="mt-2 rounded-xl bg-brand py-3 text-sm font-bold text-white transition-colors hover:bg-brand-ink disabled:opacity-60"
                    >
                        {pending ? "로그인 중…" : "로그인"}
                    </button>
                </form>
                <p className="mt-4 text-center text-[13px] text-muted">
                    계정이 없나요?{" "}
                    <Link to="/signup" className="font-semibold text-brand hover:underline">
                        회원가입
                    </Link>
                </p>
            </div>
        </div>
    );
}
