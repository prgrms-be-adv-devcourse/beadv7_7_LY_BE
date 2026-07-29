import { useState } from "react";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    fetchPointTransactions,
    formatTransactionType,
    getWalletBalance,
    isIncomingTransaction,
    MIN_DEPOSIT_AMOUNT,
    requestDeposit,
    type DepositRequestResult,
} from "../../api/wallet";
import { ApiError } from "../../api/client";
import { formatWon } from "../../components/AuctionCard";
import { QueryState } from "../../components/QueryState";
import { Pagination } from "../../components/Pagination";
import { DateRangeFilter } from "./DateRangeFilter";
import { formatDateTime, toRangeEnd, toRangeStart } from "./format";

const PAGE_SIZE = 10;

const TYPES = [
    { value: "", label: "전체 유형" },
    { value: "DEPOSIT", label: "충전" },
    { value: "HOLD", label: "입찰 보증금 차감" },
    { value: "RELEASE", label: "입찰 보증금 반환" },
    { value: "SETTLEMENT_PAYOUT", label: "정산 입금" },
    { value: "WITHDRAW", label: "출금" },
    { value: "DEPOSIT_CANCEL", label: "충전 취소" },
];

export function WalletTab() {
    const [type, setType] = useState("");
    const [from, setFrom] = useState("");
    const [to, setTo] = useState("");
    const [page, setPage] = useState(0);

    function changeFilter(patch: { type?: string; from?: string; to?: string }) {
        if (patch.type !== undefined) setType(patch.type);
        if (patch.from !== undefined) setFrom(patch.from);
        if (patch.to !== undefined) setTo(patch.to);
        setPage(0);
    }

    const transactions = useQuery({
        queryKey: ["wallet", "transactions", type, from, to, page],
        queryFn: () =>
            fetchPointTransactions({
                type: type || undefined,
                from: toRangeStart(from),
                to: toRangeEnd(to),
                page,
                size: PAGE_SIZE,
            }),
        placeholderData: keepPreviousData,
    });

    return (
        <div>
            <div className="mb-6 grid gap-4 md:grid-cols-[1fr_1fr]">
                <BalanceCard />
                <DepositForm />
            </div>

            <h3 className="mb-1 text-base font-bold tracking-tight">거래 내역</h3>
            <p className="mb-3 text-[13.5px] text-muted">충전·입찰 보증금·정산 입금 등 예치금이 움직인 기록입니다.</p>

            <div className="mb-5 flex flex-wrap items-center gap-2.5">
                <select
                    value={type}
                    onChange={(e) => changeFilter({ type: e.target.value })}
                    aria-label="거래 유형 필터"
                    className="rounded-lg border border-line bg-surface px-2.5 py-1.5 text-[13px] font-semibold outline-none"
                >
                    {TYPES.map((t) => (
                        <option key={t.value} value={t.value}>
                            {t.label}
                        </option>
                    ))}
                </select>
                <span className="h-5 w-px bg-line" />
                <DateRangeFilter from={from} to={to} onChange={changeFilter} />
            </div>

            <QueryState
                isLoading={transactions.isPending}
                error={transactions.error}
                isEmpty={!transactions.data || transactions.data.content.length === 0}
                emptyMessage="조건에 맞는 거래 내역이 없습니다."
            >
                <ul className={`flex flex-col gap-2 ${transactions.isFetching ? "opacity-60" : ""}`}>
                    {transactions.data?.content.map((tx) => {
                        const incoming = isIncomingTransaction(tx.type);
                        return (
                            <li
                                key={tx.transactionId}
                                className="flex flex-wrap items-center gap-x-4 gap-y-1 rounded-xl border border-line bg-surface px-4 py-3"
                            >
                                <span className="rounded-md bg-surface2 px-2 py-0.5 text-[11px] font-bold text-muted">
                                    {formatTransactionType(tx.type)}
                                </span>
                                <span className="text-[13px] text-muted">{formatDateTime(tx.createdAt)}</span>
                                {tx.relatedAuctionId !== null && (
                                    <span className="font-mono text-[11px] text-faint">연관 {tx.relatedAuctionId}</span>
                                )}
                                <span
                                    className={`ml-auto font-mono text-base font-bold tabular-nums ${
                                        incoming ? "text-up" : "text-ink"
                                    }`}
                                >
                                    {incoming ? "+" : "-"}
                                    {formatWon(tx.amount)}
                                </span>
                            </li>
                        );
                    })}
                </ul>
                {transactions.data && (
                    <Pagination
                        page={page}
                        hasNext={page + 1 < transactions.data.totalPages}
                        totalPageCount={transactions.data.totalPages}
                        size={PAGE_SIZE}
                        onPage={setPage}
                    />
                )}
            </QueryState>
        </div>
    );
}

function BalanceCard() {
    const balance = useQuery({ queryKey: ["wallet", "balance"], queryFn: getWalletBalance });

    return (
        <section className="rounded-2xl border-[1.5px] border-line-strong bg-surface px-5 py-4">
            <p className="text-[11px] font-bold uppercase tracking-widest text-faint">사용 가능 잔액</p>
            {balance.isPending && <p className="mt-2 text-muted">불러오는 중…</p>}
            {balance.error && (
                <p className="mt-2 text-[13px] font-semibold text-live">
                    {balance.error instanceof ApiError ? balance.error.message : "잔액을 불러오지 못했습니다."}
                </p>
            )}
            {balance.data && (
                <p className="mt-1 font-mono text-2xl font-bold tabular-nums">
                    {formatWon(balance.data.availableBalance)}
                </p>
            )}
            <p className="mt-2 text-[13px] text-muted">
                입찰하면 보증금이 잔액에서 바로 빠지므로, 여기 보이는 금액이 지금 입찰에 쓸 수 있는 돈입니다.
            </p>
        </section>
    );
}

function DepositForm() {
    const queryClient = useQueryClient();
    const [amount, setAmount] = useState("");
    const [error, setError] = useState<string | null>(null);
    const [issued, setIssued] = useState<DepositRequestResult | null>(null);

    const deposit = useMutation({
        mutationFn: () => requestDeposit(Number(amount)),
        onSuccess: (result) => {
            setError(null);
            setIssued(result);
            queryClient.invalidateQueries({ queryKey: ["wallet"] });
        },
        onError: (e: unknown) => {
            setIssued(null);
            setError(e instanceof ApiError ? e.message : "충전 요청 중 문제가 생겼습니다.");
        },
    });

    function submit(e: React.FormEvent) {
        e.preventDefault();
        const parsed = Number(amount);
        if (!Number.isFinite(parsed) || parsed < MIN_DEPOSIT_AMOUNT) {
            setIssued(null);
            setError(`충전 금액은 ${formatWon(MIN_DEPOSIT_AMOUNT)} 이상이어야 합니다.`);
            return;
        }
        deposit.mutate();
    }

    return (
        <section className="rounded-2xl border border-line bg-surface px-5 py-4">
            <p className="text-[11px] font-bold uppercase tracking-widest text-faint">예치금 충전</p>
            <form onSubmit={submit} className="mt-2 flex flex-wrap items-center gap-2">
                <input
                    type="number"
                    inputMode="numeric"
                    min={MIN_DEPOSIT_AMOUNT}
                    step={1000}
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    placeholder={String(MIN_DEPOSIT_AMOUNT)}
                    aria-label="충전 금액"
                    className="w-40 rounded-lg border border-line bg-paper px-3 py-1.5 font-mono text-sm outline-none focus:border-line-strong"
                />
                <button
                    type="submit"
                    disabled={deposit.isPending}
                    className="rounded-lg bg-brand px-3 py-1.5 text-sm font-semibold text-white hover:bg-brand-ink disabled:opacity-50"
                >
                    충전 요청
                </button>
            </form>
            {error && <p className="mt-2 text-xs font-semibold text-live">{error}</p>}
            {issued && (
                <p className="mt-2 text-[13px] text-muted">
                    충전 요청이 생성됐습니다 — 주문번호{" "}
                    <b className="font-mono text-ink">{issued.orderId}</b> · {formatWon(issued.amount)}
                </p>
            )}
            <p className="mt-2 text-[13px] text-muted">
                요청을 만드는 데까지만 동작합니다. 실제 입금은 결제사 결제창을 거쳐야 반영되며, 결제 연동은 아직
                붙지 않았습니다.
            </p>
        </section>
    );
}
