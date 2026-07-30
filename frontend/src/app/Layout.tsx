import { useState } from "react";
import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { getMyProfile } from "../api/members";
import { clearSession, loadSession } from "../auth/session";

const TABS = [
    { to: "/", label: "홈" },
    { to: "/feed", label: "경매 피드" },
    { to: "/catalog", label: "카탈로그" },
];

export function Layout() {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    useLocation(); // 라우트 이동 시 세션 표시(로그인/로그아웃 직후)를 다시 읽기 위한 리렌더 트리거
    const [mini, setMini] = useState("");
    const session = loadSession();

    // 로그인 응답에는 토큰만 들어 있어서 세션 이름은 이메일 앞부분(test123@... → test123)이다.
    // 헤더에는 회원 정보의 닉네임을 쓰되, 아직 못 받았거나 조회가 실패하면 세션 이름으로 대신한다.
    // 마이페이지 프로필과 같은 키를 써서 요청이 두 번 나가지 않는다
    const profile = useQuery({ queryKey: ["member", "me"], queryFn: getMyProfile, enabled: session !== null });
    const user = session === null ? null : (profile.data?.nickname ?? session.displayName);

    function submitMiniSearch(e: React.FormEvent) {
        e.preventDefault();
        if (mini.trim()) navigate(`/catalog?q=${encodeURIComponent(mini.trim())}`);
    }

    function logout() {
        clearSession();
        // 주문·정산·지갑은 모두 로그인한 사람 기준이라, 캐시를 비우지 않으면
        // 다른 계정으로 다시 로그인했을 때 이전 사람의 내역이 잠깐 보인다
        queryClient.clear();
        navigate("/");
    }

    return (
        <div className="min-h-screen">
            <header className="sticky top-0 z-20 border-b border-line bg-paper/90 backdrop-blur">
                <div className="mx-auto flex max-w-[1180px] flex-wrap items-center gap-3 px-5 py-3 sm:gap-4">
                    <Link to="/" className="flex items-center gap-2 font-bold tracking-tight">
                        <span
                            aria-hidden="true"
                            className="block h-[22px] w-[22px] rounded-full shadow-[inset_0_0_0_1px_var(--color-line-strong)]"
                            style={{
                                background:
                                    "radial-gradient(circle, var(--color-paper) 0 14%, var(--color-ink) 15% 17%, var(--color-ink) 18% 100%)",
                            }}
                        />
                        <b className="text-base">Groove</b>
                        <span className="text-[11px] font-semibold uppercase tracking-widest text-faint">LP Auction</span>
                    </Link>
                    <nav className="flex gap-0.5" aria-label="주요 화면">
                        {TABS.map((tab) => (
                            <NavLink
                                key={tab.to}
                                to={tab.to}
                                end={tab.to === "/"}
                                className={({ isActive }) =>
                                    `rounded-lg px-3 py-1.5 text-[13.5px] font-semibold transition-colors ${
                                        isActive ? "bg-ink text-paper" : "text-muted hover:bg-surface2 hover:text-ink"
                                    }`
                                }
                            >
                                {tab.label}
                            </NavLink>
                        ))}
                    </nav>
                    <div className="grow" />
                    <form onSubmit={submitMiniSearch} className="hidden items-center gap-2 rounded-lg border border-line bg-surface px-3 py-1.5 text-sm text-faint sm:flex">
                        <span aria-hidden="true">⌕</span>
                        <input
                            value={mini}
                            onChange={(e) => setMini(e.target.value)}
                            placeholder="릴리스·아티스트 검색"
                            aria-label="검색"
                            className="w-40 bg-transparent text-ink outline-none placeholder:text-faint"
                        />
                    </form>
                    {user ? (
                        <div className="flex items-center gap-2 text-sm">
                            <NavLink
                                to="/mypage"
                                className={({ isActive }) =>
                                    `rounded-lg px-3 py-1.5 text-[13.5px] font-semibold transition-colors ${
                                        isActive ? "bg-ink text-paper" : "text-muted hover:bg-surface2 hover:text-ink"
                                    }`
                                }
                            >
                                마이페이지
                            </NavLink>
                            <span className="font-semibold">{user}</span>
                            <button type="button" onClick={logout} className="text-xs text-muted underline hover:text-ink">
                                로그아웃
                            </button>
                        </div>
                    ) : (
                        <div className="flex items-center gap-2">
                            <Link to="/login" className="rounded-lg border border-line bg-surface px-3 py-1.5 text-sm font-semibold hover:border-line-strong">
                                로그인
                            </Link>
                            <Link to="/signup" className="rounded-lg bg-brand px-3 py-1.5 text-sm font-semibold text-white hover:bg-brand-ink">
                                회원가입
                            </Link>
                        </div>
                    )}
                </div>
            </header>
            <main className="mx-auto max-w-[1180px] px-5 pb-20 pt-7">
                <Outlet />
            </main>
            <footer className="mx-auto max-w-[1180px] px-5 pb-12 text-xs text-faint">
                <div className="rounded-lg border border-dashed border-line-strong bg-surface px-4 py-3">
                    개인 확인용 프론트 — 검색·상품·시세는 core-service(8080), 경매·입찰은 core-service, 로그인은 member-service(8081) 실제 API입니다.
                </div>
            </footer>
        </div>
    );
}
