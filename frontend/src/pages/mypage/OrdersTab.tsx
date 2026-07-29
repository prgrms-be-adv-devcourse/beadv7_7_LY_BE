import { useState } from "react";
import { Link } from "react-router";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    cancelOrder,
    completeOrder,
    fetchOrders,
    formatOrderStatus,
    getOrderDetail,
    type OrderListItem,
    type OrderPerspective,
} from "../../api/orders";
import { ApiError } from "../../api/client";
import { formatWon } from "../../components/AuctionCard";
import { QueryState } from "../../components/QueryState";
import { Pagination } from "../../components/Pagination";
import { VinylCover } from "../../components/VinylCover";
import { formatDateTime } from "./format";

const PAGE_SIZE = 10;

const PERSPECTIVES: { value: OrderPerspective; label: string }[] = [
    { value: "BUYER", label: "구매" },
    { value: "SELLER", label: "판매" },
];

const STATUSES = [
    { value: "", label: "전체 상태" },
    { value: "PENDING", label: "결제 대기" },
    { value: "ORDERED", label: "주문 완료" },
    { value: "COMPLETED", label: "거래 확정" },
    { value: "CANCELLED", label: "주문 취소" },
];

export function OrdersTab() {
    const [perspective, setPerspective] = useState<OrderPerspective>("BUYER");
    const [status, setStatus] = useState("");
    const [page, setPage] = useState(0);
    const [openedOrderId, setOpenedOrderId] = useState<number | null>(null);

    function changeFilter(patch: { perspective?: OrderPerspective; status?: string }) {
        if (patch.perspective) setPerspective(patch.perspective);
        if (patch.status !== undefined) setStatus(patch.status);
        setPage(0);
        setOpenedOrderId(null);
    }

    const query = useQuery({
        queryKey: ["orders", perspective, status, page],
        queryFn: () => fetchOrders({ perspective, status: status || undefined, page, size: PAGE_SIZE }),
        placeholderData: keepPreviousData,
    });

    return (
        <div>
            <div className="mb-5 flex flex-wrap items-center gap-2.5">
                <span className="text-[11px] font-bold uppercase tracking-wider text-faint">관점</span>
                {PERSPECTIVES.map((p) => (
                    <button
                        key={p.value}
                        type="button"
                        aria-pressed={perspective === p.value}
                        onClick={() => changeFilter({ perspective: p.value })}
                        className={`rounded-lg border px-2.5 py-1.5 text-xs font-semibold transition-colors ${
                            perspective === p.value
                                ? "border-brand bg-brand text-white"
                                : "border-line bg-surface text-muted hover:border-line-strong hover:text-ink"
                        }`}
                    >
                        {p.label}
                    </button>
                ))}
                <span className="h-5 w-px bg-line" />
                <select
                    value={status}
                    onChange={(e) => changeFilter({ status: e.target.value })}
                    aria-label="주문 상태 필터"
                    className="rounded-lg border border-line bg-surface px-2.5 py-1.5 text-[13px] font-semibold outline-none"
                >
                    {STATUSES.map((s) => (
                        <option key={s.value} value={s.value}>
                            {s.label}
                        </option>
                    ))}
                </select>
            </div>

            <QueryState
                isLoading={query.isPending}
                error={query.error}
                isEmpty={!query.data || query.data.content.length === 0}
                emptyMessage={
                    perspective === "BUYER"
                        ? "낙찰받은 주문이 아직 없습니다."
                        : "판매한 주문이 아직 없습니다."
                }
            >
                <p className="mb-3 text-[13px] text-muted">
                    총 <b className="font-mono tabular-nums">{query.data?.totalElements}</b>건
                </p>
                <ul className={`flex flex-col gap-2.5 ${query.isFetching ? "opacity-60" : ""}`}>
                    {query.data?.content.map((order) => (
                        <OrderRow
                            key={order.orderId}
                            order={order}
                            perspective={perspective}
                            opened={openedOrderId === order.orderId}
                            onToggle={() => setOpenedOrderId(openedOrderId === order.orderId ? null : order.orderId)}
                        />
                    ))}
                </ul>
                {query.data && (
                    <Pagination
                        page={page}
                        hasNext={!query.data.last}
                        totalElements={query.data.totalElements}
                        size={PAGE_SIZE}
                        onPage={(next) => {
                            setPage(next);
                            setOpenedOrderId(null);
                        }}
                    />
                )}
            </QueryState>
        </div>
    );
}

interface OrderRowProps {
    order: OrderListItem;
    perspective: OrderPerspective;
    opened: boolean;
    onToggle: () => void;
}

function OrderRow({ order, perspective, opened, onToggle }: OrderRowProps) {
    const queryClient = useQueryClient();
    const [actionError, setActionError] = useState<string | null>(null);

    // 완료·취소는 백엔드가 X-Member-Id를 구매자로 보고 검증한다 — 판매 관점에서는 버튼을 내린다
    const canAct = perspective === "BUYER" && order.status === "ORDERED";

    const detail = useQuery({
        queryKey: ["order", order.orderId],
        queryFn: () => getOrderDetail(order.orderId),
        enabled: opened,
    });

    function runAction(action: () => Promise<void>) {
        setActionError(null);
        return action()
            .then(() => {
                queryClient.invalidateQueries({ queryKey: ["orders"] });
                queryClient.invalidateQueries({ queryKey: ["order", order.orderId] });
            })
            .catch((error: unknown) => {
                setActionError(error instanceof ApiError ? error.message : "처리 중 문제가 생겼습니다.");
            });
    }

    const completeMutation = useMutation({ mutationFn: () => runAction(() => completeOrder(order.orderId)) });
    const cancelMutation = useMutation({ mutationFn: () => runAction(() => cancelOrder(order.orderId)) });
    const acting = completeMutation.isPending || cancelMutation.isPending;

    return (
        <li className="rounded-xl border border-line bg-surface px-4 py-3.5">
            <div className="flex items-center gap-3.5">
                <VinylCover
                    title={order.product.albumTitle}
                    artist={order.product.artistName}
                    imageUrl={order.product.coverImage}
                    className="w-16 shrink-0"
                />
                <div className="min-w-0 grow">
                    <div className="flex flex-wrap items-center gap-2">
                        <span className="rounded-md bg-surface2 px-2 py-0.5 text-[11px] font-bold text-muted">
                            {formatOrderStatus(order.status)}
                        </span>
                        <span className="font-mono text-[11px] text-faint">주문번호 {order.orderId}</span>
                    </div>
                    <p className="mt-1 truncate font-semibold">{order.product.albumTitle}</p>
                    <p className="truncate text-[13px] text-muted">{order.product.artistName}</p>
                </div>
                <div className="shrink-0 text-right">
                    <div className="font-mono text-base font-bold tabular-nums">{formatWon(order.finalBidPrice)}</div>
                    <Link to={`/auctions/${order.auctionId}`} className="text-[11px] text-muted underline hover:text-ink">
                        경매 보기
                    </Link>
                </div>
            </div>

            <div className="mt-3 flex flex-wrap items-center gap-2 border-t border-line pt-3">
                <button
                    type="button"
                    onClick={onToggle}
                    aria-expanded={opened}
                    className="rounded-lg border border-line bg-paper px-3 py-1.5 text-xs font-semibold hover:border-line-strong"
                >
                    {opened ? "상세 닫기" : "상세 보기"}
                </button>
                {canAct && (
                    <>
                        <button
                            type="button"
                            disabled={acting}
                            onClick={() => completeMutation.mutate()}
                            className="rounded-lg bg-brand px-3 py-1.5 text-xs font-semibold text-white hover:bg-brand-ink disabled:opacity-50"
                        >
                            거래 확정
                        </button>
                        <button
                            type="button"
                            disabled={acting}
                            onClick={() => cancelMutation.mutate()}
                            className="rounded-lg border border-line bg-paper px-3 py-1.5 text-xs font-semibold text-muted hover:border-line-strong hover:text-ink disabled:opacity-50"
                        >
                            주문 취소
                        </button>
                    </>
                )}
                {order.confirmationDeadline && order.status === "PENDING" && (
                    <span className="text-[11px] text-muted">
                        주문 기한 {formatDateTime(order.confirmationDeadline)}
                    </span>
                )}
                {order.completionDeadline && order.status === "ORDERED" && (
                    <span className="text-[11px] text-muted">
                        확정 기한 {formatDateTime(order.completionDeadline)}
                    </span>
                )}
            </div>

            {actionError && <p className="mt-2 text-xs font-semibold text-live">{actionError}</p>}

            {opened && (
                <div className="mt-3 rounded-lg border border-line bg-paper px-4 py-3 text-[13px]">
                    <QueryState
                        isLoading={detail.isPending}
                        error={detail.error}
                        isEmpty={false}
                        emptyMessage=""
                        loadingFallback={<p className="text-muted">상세를 불러오는 중…</p>}
                    >
                        {detail.data && (
                            <dl className="grid grid-cols-[7rem_1fr] gap-y-1.5">
                                <dt className="text-muted">컨디션</dt>
                                <dd>{detail.data.product.conditionGrade ?? "-"}</dd>
                                <dt className="text-muted">주문 일시</dt>
                                <dd>{formatDateTime(detail.data.orderedAt)}</dd>
                                <dt className="text-muted">확정 일시</dt>
                                <dd>{formatDateTime(detail.data.completedAt)}</dd>
                                {detail.data.cancelledAt && (
                                    <>
                                        <dt className="text-muted">취소 일시</dt>
                                        <dd>
                                            {formatDateTime(detail.data.cancelledAt)}
                                            {detail.data.cancelReason && ` (${detail.data.cancelReason})`}
                                        </dd>
                                    </>
                                )}
                                <dt className="text-muted">배송지</dt>
                                <dd>
                                    {detail.data.deliveryAddress ? (
                                        <>
                                            {detail.data.deliveryAddress.recipientName} ·{" "}
                                            {detail.data.deliveryAddress.phoneNumber}
                                            <br />
                                            {detail.data.deliveryAddress.baseAddress}{" "}
                                            {detail.data.deliveryAddress.detailAddress}
                                        </>
                                    ) : (
                                        <span className="text-muted">아직 입력 전입니다</span>
                                    )}
                                </dd>
                            </dl>
                        )}
                    </QueryState>
                </div>
            )}
        </li>
    );
}
