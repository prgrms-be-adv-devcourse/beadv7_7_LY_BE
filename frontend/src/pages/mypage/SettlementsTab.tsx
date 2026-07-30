import { useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import {
    fetchSettlementBatches,
    fetchSettlementItems,
    formatSettlementStatus,
} from "../../api/settlements";
import { formatWon } from "../../components/AuctionCard";
import { QueryState } from "../../components/QueryState";
import { Pagination } from "../../components/Pagination";
import { DateRangeFilter } from "./DateRangeFilter";
import { formatDate, formatDateTime, toRangeEnd, toRangeStart } from "./format";

const PAGE_SIZE = 10;

const STATUSES = [
    { value: "", label: "전체 상태" },
    { value: "PENDING", label: "정산 대기" },
    { value: "CONFIRMED", label: "정산 확정" },
];

export function SettlementsTab() {
    const [status, setStatus] = useState("");
    const [from, setFrom] = useState("");
    const [to, setTo] = useState("");
    const [page, setPage] = useState(0);

    function changeFilter(patch: { status?: string; from?: string; to?: string }) {
        if (patch.status !== undefined) setStatus(patch.status);
        if (patch.from !== undefined) setFrom(patch.from);
        if (patch.to !== undefined) setTo(patch.to);
        setPage(0);
    }

    const items = useQuery({
        queryKey: ["settlements", "items", status, from, to, page],
        queryFn: () =>
            fetchSettlementItems({
                status: status || undefined,
                from: toRangeStart(from),
                to: toRangeEnd(to),
                page,
                size: PAGE_SIZE,
            }),
        placeholderData: keepPreviousData,
    });

    return (
        <div>
            <p className="mb-4 text-[13.5px] text-muted">
                판매자로서 받을 정산 내역입니다. 거래가 확정된 주문이 정산 항목이 되고, 확정된 항목은 정산 배치로 묶여
                지급됩니다.
            </p>

            <div className="mb-5 flex flex-wrap items-center gap-2.5">
                <select
                    value={status}
                    onChange={(e) => changeFilter({ status: e.target.value })}
                    aria-label="정산 상태 필터"
                    className="rounded-lg border border-line bg-surface px-2.5 py-1.5 text-[13px] font-semibold outline-none"
                >
                    {STATUSES.map((s) => (
                        <option key={s.value} value={s.value}>
                            {s.label}
                        </option>
                    ))}
                </select>
                <span className="h-5 w-px bg-line" />
                <DateRangeFilter from={from} to={to} onChange={changeFilter} />
            </div>

            <QueryState
                isLoading={items.isPending}
                error={items.error}
                isEmpty={!items.data || items.data.content.length === 0}
                emptyMessage="조건에 맞는 정산 항목이 없습니다."
            >
                <p className="mb-3 text-[13px] text-muted">
                    총 <b className="font-mono tabular-nums">{items.data?.totalElements}</b>건
                </p>
                <div className={`overflow-x-auto ${items.isFetching ? "opacity-60" : ""}`}>
                    <table className="w-full min-w-[46rem] border-collapse text-[13px]">
                        <thead>
                            <tr className="border-b border-line text-left text-[11px] font-bold uppercase tracking-wider text-faint">
                                <th className="py-2 pr-3 font-bold">주문번호</th>
                                <th className="py-2 pr-3 font-bold">낙찰가</th>
                                <th className="py-2 pr-3 font-bold">수수료</th>
                                <th className="py-2 pr-3 font-bold">정산금</th>
                                <th className="py-2 pr-3 font-bold">상태</th>
                                <th className="py-2 pr-3 font-bold">거래 확정</th>
                                <th className="py-2 font-bold">정산 배치</th>
                            </tr>
                        </thead>
                        <tbody>
                            {items.data?.content.map((item) => (
                                <tr key={item.settlementItemId} className="border-b border-line">
                                    <td className="py-2.5 pr-3 font-mono tabular-nums">{item.orderId}</td>
                                    <td className="py-2.5 pr-3 font-mono tabular-nums">
                                        {formatWon(item.finalBidPrice)}
                                    </td>
                                    <td className="py-2.5 pr-3 font-mono tabular-nums text-muted">
                                        -{formatWon(item.commissionAmount)}
                                        <span className="ml-1 text-[11px]">
                                            ({(item.commissionRate * 100).toFixed(1)}%)
                                        </span>
                                    </td>
                                    <td className="py-2.5 pr-3 font-mono font-bold tabular-nums">
                                        {formatWon(item.netAmount)}
                                    </td>
                                    <td className="py-2.5 pr-3">
                                        <span className="rounded-md bg-surface2 px-2 py-0.5 text-[11px] font-bold text-muted">
                                            {formatSettlementStatus(item.status)}
                                        </span>
                                    </td>
                                    <td className="py-2.5 pr-3 text-muted">{formatDateTime(item.completedAt)}</td>
                                    <td className="py-2.5 font-mono tabular-nums text-muted">
                                        {item.settlementBatchId ?? "-"}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
                {items.data && (
                    <Pagination
                        page={page}
                        hasNext={!items.data.last}
                        totalElements={items.data.totalElements}
                        size={PAGE_SIZE}
                        onPage={setPage}
                    />
                )}
            </QueryState>

            <SettlementBatches />
        </div>
    );
}

function SettlementBatches() {
    const batches = useQuery({ queryKey: ["settlements", "batches"], queryFn: fetchSettlementBatches });

    return (
        <section className="mt-10">
            <h3 className="mb-1 text-base font-bold tracking-tight">정산 배치</h3>
            <p className="mb-3 text-[13.5px] text-muted">확정된 정산 항목이 기간 단위로 묶여 지급된 기록입니다.</p>
            <QueryState
                isLoading={batches.isPending}
                error={batches.error}
                isEmpty={!batches.data || batches.data.length === 0}
                emptyMessage="아직 지급된 정산 배치가 없습니다."
            >
                <ul className="flex flex-col gap-2.5">
                    {batches.data?.map((batch) => (
                        <li
                            key={batch.settlementBatchId}
                            className="flex flex-wrap items-center gap-x-4 gap-y-1 rounded-xl border border-line bg-surface px-4 py-3"
                        >
                            <span className="font-mono text-[11px] text-faint">배치 {batch.settlementBatchId}</span>
                            <span className="text-[13px] text-muted">
                                {formatDate(batch.periodFrom)} ~ {formatDate(batch.periodTo)}
                            </span>
                            <span className="text-[13px] text-muted">확정 {formatDateTime(batch.confirmedAt)}</span>
                            <span className="ml-auto font-mono text-base font-bold tabular-nums">
                                {formatWon(batch.totalAmount)}
                            </span>
                        </li>
                    ))}
                </ul>
            </QueryState>
        </section>
    );
}
