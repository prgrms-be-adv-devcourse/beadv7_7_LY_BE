import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { useQuery } from "@tanstack/react-query";
import { fetchAuctions } from "../api/auctions";
import { AuctionCard } from "../components/AuctionCard";
import { QueryState } from "../components/QueryState";

const QUICK_SEARCHES = ["Abbey Road", "Miles Davis", "Coltrane", "Blue Note"];

export function HomePage() {
    const navigate = useNavigate();
    const [keyword, setKeyword] = useState("");

    const ending = useQuery({
        queryKey: ["auctions", "home-ending"],
        queryFn: () => fetchAuctions({ status: "RUNNING", sort: "ending_soon", page: 0, size: 8 }),
    });
    // 시안의 "실시간 경매" — 진행 중인 것만 입찰 많은순으로. 마감 임박 레일과 같은 풀을 다른 축으로 보여준다
    const live = useQuery({
        queryKey: ["auctions", "home-live"],
        queryFn: () => fetchAuctions({ status: "RUNNING", sort: "most_bids", page: 0, size: 12 }),
    });

    function submit(e: React.FormEvent) {
        e.preventDefault();
        if (keyword.trim()) navigate(`/catalog?q=${encodeURIComponent(keyword.trim())}`);
    }

    return (
        <div>
            <section className="rounded-2xl border border-line bg-surface p-7 shadow-sm">
                <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-live">Rare Pressings, Fair Prices</p>
                <h1 className="mt-1.5 font-display text-3xl font-semibold leading-tight tracking-tight text-balance">
                    희귀 LP, <em className="italic text-brand">경매</em>로 만나고 시세로 확인하세요
                </h1>
                <p className="mt-2 max-w-[46ch] text-sm text-muted">
                    찾는 앨범을 검색해 진행 중 경매와 낙찰 시세를 한눈에 보거나, 지금 마감되는 경매부터 둘러보세요.
                </p>
                <form onSubmit={submit} className="mt-5 flex max-w-[520px] overflow-hidden rounded-xl border-[1.5px] border-line-strong bg-paper focus-within:border-brand">
                    <input
                        value={keyword}
                        onChange={(e) => setKeyword(e.target.value)}
                        placeholder="앨범 제목, 아티스트, 카탈로그 번호…"
                        aria-label="카탈로그 검색"
                        className="flex-1 bg-transparent px-4 py-3 text-[15px] outline-none placeholder:text-faint"
                    />
                    <button type="submit" className="bg-brand px-6 text-sm font-bold text-white transition-colors hover:bg-brand-ink">
                        검색
                    </button>
                </form>
                <div className="mt-3.5 flex flex-wrap items-center gap-2">
                    <span className="text-xs text-faint">인기 검색</span>
                    {QUICK_SEARCHES.map((q) => (
                        <Link
                            key={q}
                            to={`/catalog?q=${encodeURIComponent(q)}`}
                            className="rounded-full border border-line bg-paper px-2.5 py-1 text-xs font-semibold text-muted transition-colors hover:border-brand hover:text-brand"
                        >
                            {q}
                        </Link>
                    ))}
                </div>
            </section>

            <section className="mt-9">
                <div className="mb-3.5 flex items-baseline justify-between">
                    <div>
                        <p className="text-[11px] font-bold uppercase tracking-widest text-live">지금 마감 임박</p>
                        <h2 className="text-lg font-bold tracking-tight">곧 종료되는 경매</h2>
                    </div>
                    <Link to="/feed" className="text-[13px] font-semibold text-muted hover:text-ink">
                        경매 피드 전체 →
                    </Link>
                </div>
                <QueryState
                    isLoading={ending.isPending}
                    error={ending.error}
                    isEmpty={!ending.data || ending.data.items.length === 0}
                    emptyMessage="진행 중인 경매가 없습니다."
                >
                    <div className="flex snap-x gap-3.5 overflow-x-auto pb-2">
                        {ending.data?.items.map((auction) => (
                            <AuctionCard key={auction.auctionId} auction={auction} className="w-[220px] flex-none snap-start" />
                        ))}
                    </div>
                </QueryState>
            </section>

            <section className="mt-9">
                <div className="mb-3.5 flex items-baseline justify-between">
                    <div>
                        <p className="text-[11px] font-bold uppercase tracking-widest text-brand">진행 중</p>
                        <h2 className="text-lg font-bold tracking-tight">실시간 경매</h2>
                    </div>
                    <Link to="/feed" className="text-[13px] font-semibold text-muted hover:text-ink">
                        더 보기 →
                    </Link>
                </div>
                <QueryState
                    isLoading={live.isPending}
                    error={live.error}
                    isEmpty={!live.data || live.data.items.length === 0}
                    emptyMessage="진행 중인 경매가 없습니다."
                >
                    <div className="grid grid-cols-2 gap-3.5 md:grid-cols-4">
                        {live.data?.items.map((auction) => (
                            <AuctionCard key={auction.auctionId} auction={auction} />
                        ))}
                    </div>
                </QueryState>
            </section>
        </div>
    );
}
