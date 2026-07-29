import { Link, useSearchParams } from "react-router";
import { useQuery } from "@tanstack/react-query";
import { getMyProfile } from "../api/members";
import { loadSession } from "../auth/session";
import { OrdersTab } from "./mypage/OrdersTab";
import { SettlementsTab } from "./mypage/SettlementsTab";

const TABS = [
    { value: "orders", label: "주문" },
    { value: "settlements", label: "정산" },
] as const;

type TabValue = (typeof TABS)[number]["value"];

export function MyPage() {
    const [searchParams, setSearchParams] = useSearchParams();
    const session = loadSession();
    const tabParam = searchParams.get("tab");
    const tab: TabValue = TABS.some((t) => t.value === tabParam) ? (tabParam as TabValue) : "orders";

    if (!session) {
        return (
            <div className="rounded-xl border border-line bg-surface px-5 py-14 text-center">
                <p className="font-semibold">로그인이 필요한 화면입니다.</p>
                <p className="mt-1 text-sm text-muted">주문·정산·지갑 내역은 본인 계정으로만 볼 수 있습니다.</p>
                <Link
                    to="/login"
                    className="mt-4 inline-block rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white hover:bg-brand-ink"
                >
                    로그인하러 가기
                </Link>
            </div>
        );
    }

    return (
        <div>
            <ProfileHeader fallbackName={session.displayName} />
            <nav className="mb-5 flex gap-1 border-b border-line" aria-label="마이페이지 탭">
                {TABS.map((t) => (
                    <button
                        key={t.value}
                        type="button"
                        aria-current={tab === t.value ? "page" : undefined}
                        onClick={() => setSearchParams({ tab: t.value }, { replace: true })}
                        className={`-mb-px border-b-2 px-4 py-2 text-[13.5px] font-semibold transition-colors ${
                            tab === t.value
                                ? "border-brand text-ink"
                                : "border-transparent text-muted hover:text-ink"
                        }`}
                    >
                        {t.label}
                    </button>
                ))}
            </nav>
            {tab === "orders" && <OrdersTab />}
            {tab === "settlements" && <SettlementsTab />}
        </div>
    );
}

function ProfileHeader({ fallbackName }: { fallbackName: string }) {
    // 프로필 조회가 실패해도 탭은 쓸 수 있어야 하므로 로그인 세션의 이름으로 대체한다
    const profile = useQuery({ queryKey: ["member", "me"], queryFn: getMyProfile });

    return (
        <header className="mb-6 flex items-center gap-4 rounded-2xl border-[1.5px] border-line-strong bg-surface px-5 py-4">
            <div
                aria-hidden="true"
                className="h-12 w-12 shrink-0 rounded-full shadow-[inset_0_0_0_1px_var(--color-line-strong)]"
                style={{
                    background:
                        "radial-gradient(circle, var(--color-paper) 0 14%, var(--color-ink) 15% 17%, var(--color-ink) 18% 100%)",
                }}
            />
            <div className="min-w-0">
                <p className="text-[11px] font-bold uppercase tracking-widest text-faint">마이페이지</p>
                <h2 className="truncate text-lg font-bold tracking-tight">
                    {profile.data?.nickname ?? fallbackName}
                </h2>
                <p className="truncate text-[13px] text-muted">
                    {profile.data?.email ?? (profile.isPending ? "불러오는 중…" : "회원 정보를 불러오지 못했습니다")}
                </p>
            </div>
        </header>
    );
}
