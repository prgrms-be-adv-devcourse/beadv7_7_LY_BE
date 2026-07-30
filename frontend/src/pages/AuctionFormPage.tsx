import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    AUCTION_POLICY,
    createAuction,
    deleteAuction,
    getAuctionDetail,
    ITEM_CONDITIONS,
    modifyAuction,
    type AuctionPayload,
} from "../api/auctions";
import { ApiError } from "../api/client";
import { loadSession } from "../auth/session";
import { QueryState } from "../components/QueryState";
import { formatWon } from "../components/AuctionCard";
import { ProductPicker, type PickedProduct } from "./auction-form/ProductPicker";
import {
    addHours,
    earliestStartAt,
    isEditableNow,
    parseInputValue,
    toInputValue,
    toInputValueFromServer,
    toServerDateTime,
    validateAuctionForm,
    type AuctionFormValues,
} from "./auction-form/schedule";

function emptyValues(now: Date): AuctionFormValues {
    const start = earliestStartAt(now);
    return {
        productId: null,
        itemCondition: "",
        itemDescription: "",
        startPrice: "",
        shippingFee: "",
        bidUnit: "",
        startAt: toInputValue(start),
        endAt: toInputValue(addHours(start, 24)),
        extensionEnabled: false,
        extensionTime: "",
    };
}

export function AuctionFormPage() {
    const { auctionId } = useParams();
    const editing = auctionId !== undefined;
    const session = loadSession();

    if (!session) {
        return (
            <div className="rounded-xl border border-line bg-surface px-5 py-14 text-center">
                <p className="font-semibold">로그인이 필요한 화면입니다.</p>
                <p className="mt-1 text-sm text-muted">경매는 본인 계정으로만 등록할 수 있습니다.</p>
                <Link
                    to="/login"
                    className="mt-4 inline-block rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white hover:bg-brand-ink"
                >
                    로그인하러 가기
                </Link>
            </div>
        );
    }

    return editing ? <EditAuction auctionId={Number(auctionId)} /> : <AuctionForm mode="create" />;
}

function EditAuction({ auctionId }: { auctionId: number }) {
    const query = useQuery({
        queryKey: ["auction", String(auctionId)],
        queryFn: () => getAuctionDetail(auctionId),
    });
    const detail = query.data;

    return (
        <QueryState
            isLoading={query.isPending}
            error={query.error}
            isEmpty={!detail}
            emptyMessage="존재하지 않는 경매입니다."
        >
            {detail && (
                <AuctionForm
                    mode="edit"
                    auctionId={auctionId}
                    originalStartAt={toInputValueFromServer(detail.startAt)}
                    originalStatus={detail.status}
                    originalStartBidAmount={detail.startBidAmount}
                    originalImages={detail.itemImages ?? []}
                    initialProduct={{
                        productId: detail.product.productId,
                        title: detail.product.title,
                        artistName: detail.product.artistName,
                        coverImageUrl: detail.product.coverImageUrl,
                    }}
                    initialValues={{
                        productId: detail.product.productId,
                        itemCondition: detail.itemCondition,
                        itemDescription: detail.itemDescription ?? "",
                        // 서버가 시작가와 배송비를 합쳐서만 주기 때문에 되살릴 수 없다 — 다시 받는다
                        startPrice: "",
                        shippingFee: "",
                        bidUnit: String(detail.bidUnit),
                        startAt: toInputValueFromServer(detail.startAt),
                        endAt: toInputValueFromServer(detail.endAt),
                        extensionEnabled: detail.extensionEnabled,
                        extensionTime: detail.extensionTime === null ? "" : String(detail.extensionTime),
                    }}
                />
            )}
        </QueryState>
    );
}

interface AuctionFormProps {
    mode: "create" | "edit";
    auctionId?: number;
    initialValues?: AuctionFormValues;
    initialProduct?: PickedProduct;
    originalStartAt?: string;
    originalStatus?: string;
    originalStartBidAmount?: number;
    originalImages?: string[];
}

function AuctionForm({
    mode,
    auctionId,
    initialValues,
    initialProduct,
    originalStartAt,
    originalStatus,
    originalStartBidAmount,
    originalImages = [],
}: AuctionFormProps) {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [values, setValues] = useState<AuctionFormValues>(() => initialValues ?? emptyValues(new Date()));
    const [product, setProduct] = useState<PickedProduct | null>(initialProduct ?? null);
    const [errors, setErrors] = useState<string[]>([]);
    const [serverError, setServerError] = useState<string | null>(null);

    const editing = mode === "edit";
    // 시한 판정은 폼에 입력한 값이 아니라 지금 등록돼 있는 시작 시각으로 해야 한다
    const pastDeadline = editing && originalStartAt !== undefined && !isEditableNow(originalStartAt, new Date());
    const notScheduled = editing && originalStatus !== undefined && originalStatus !== "SCHEDULED";
    const locked = pastDeadline || notScheduled;

    // 시작 시각 입력칸이 허용하는 가장 이른 값. 화면을 열어둔 채 시간이 흐르는 경우까지는 보지 않고,
    // 실제 판정은 보내기 직전 검증에서 다시 한다
    const minStartAt = useMemo(() => toInputValue(earliestStartAt(new Date())), []);

    function patch(next: Partial<AuctionFormValues>) {
        setValues((current) => ({ ...current, ...next }));
    }

    useEffect(() => {
        patch({ productId: product?.productId ?? null });
    }, [product]);

    const submit = useMutation({
        mutationFn: (payload: AuctionPayload) =>
            editing ? modifyAuction(auctionId as number, payload) : createAuction(payload),
        onSuccess: (result) => {
            queryClient.invalidateQueries({ queryKey: ["auctions"] });
            queryClient.invalidateQueries({ queryKey: ["auction", String(result.auctionId)] });
            navigate(`/auctions/${result.auctionId}`);
        },
        onError: (e: unknown) => {
            setServerError(e instanceof ApiError ? e.message : "경매 저장 중 문제가 생겼습니다.");
        },
    });

    const cancelAuction = useMutation({
        mutationFn: () => deleteAuction(auctionId as number),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["auctions"] });
            navigate("/mypage?tab=hosted");
        },
        onError: (e: unknown) => {
            setServerError(e instanceof ApiError ? e.message : "경매 취소 중 문제가 생겼습니다.");
        },
    });

    function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        setServerError(null);
        const found = validateAuctionForm(values, new Date());
        setErrors(found);
        if (found.length > 0) return;

        const description = values.itemDescription.trim();
        // 이미지 업로드 창구가 없어서 고른 음반의 대표 커버 한 장을 매물 사진으로 쓴다.
        // 음반을 바꾸지 않았다면 원래 걸려 있던 사진을 그대로 둔다 (서버가 null을 안 받아 최소 빈 배열)
        const productUnchanged = editing && product?.productId === initialProduct?.productId;
        const images = productUnchanged
            ? originalImages
            : product?.coverImageUrl
              ? [product.coverImageUrl]
              : [];

        submit.mutate({
            productId: values.productId as number,
            itemCondition: values.itemCondition,
            itemDescription: description === "" ? null : description,
            itemImages: images.slice(0, AUCTION_POLICY.MAX_IMAGE_COUNT),
            startPrice: Number(values.startPrice),
            shippingFee: Number(values.shippingFee),
            bidUnit: Number(values.bidUnit),
            startAt: toServerDateTime(values.startAt),
            endAt: toServerDateTime(values.endAt),
            extensionEnabled: values.extensionEnabled,
            extensionTime: values.extensionEnabled ? Number(values.extensionTime) : null,
        });
    }

    const startAtDate = parseInputValue(values.startAt);
    const description = values.itemDescription.trim();

    return (
        <div className="max-w-[760px]">
            <Link
                to={editing ? "/mypage?tab=hosted" : "/feed"}
                className="mb-4 inline-flex items-center gap-1.5 text-[13px] font-semibold text-muted hover:text-ink"
            >
                ← {editing ? "등록한 경매로" : "경매 피드로"}
            </Link>
            <h1 className="font-display text-2xl font-semibold tracking-tight">
                {editing ? "경매 수정" : "경매 등록"}
            </h1>
            <p className="mt-1 text-[13.5px] text-muted">
                {editing
                    ? `시작 ${AUCTION_POLICY.EDIT_DEADLINE_MINUTES}분 전까지, 아직 시작하지 않은 경매만 고칠 수 있습니다.`
                    : "카탈로그에 있는 음반을 골라 경매를 엽니다."}
            </p>

            {locked && (
                <div className="mt-4 rounded-xl border border-live/30 bg-live-bg px-5 py-4">
                    <p className="font-semibold text-live">
                        {notScheduled ? "이미 시작했거나 끝난 경매입니다." : "수정·취소 가능 시한이 지났습니다."}
                    </p>
                    <p className="mt-1 text-sm text-muted">
                        경매는 시작 {AUCTION_POLICY.EDIT_DEADLINE_MINUTES}분 전까지만 고치거나 취소할 수 있습니다.
                    </p>
                </div>
            )}

            <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-6">
                <Section title="음반" hint="경매로 올릴 마스터 상품">
                    <ProductPicker picked={product} onPick={setProduct} />
                </Section>

                <Section title="매물 정보" hint="이 판의 상태와 설명">
                    <label className="block">
                        <span className="mb-1 block text-[11px] font-bold uppercase tracking-wider text-faint">
                            음반 상태
                        </span>
                        <select
                            value={values.itemCondition}
                            onChange={(e) => patch({ itemCondition: e.target.value })}
                            className="rounded-lg border border-line bg-surface px-2.5 py-1.5 text-[13.5px] font-semibold outline-none"
                        >
                            <option value="">상태를 고르세요</option>
                            {ITEM_CONDITIONS.map((condition) => (
                                <option key={condition.value} value={condition.value}>
                                    {condition.label}
                                </option>
                            ))}
                        </select>
                    </label>
                    <label className="mt-3 block">
                        <span className="mb-1 block text-[11px] font-bold uppercase tracking-wider text-faint">
                            설명 (선택)
                        </span>
                        <textarea
                            value={values.itemDescription}
                            onChange={(e) => patch({ itemDescription: e.target.value })}
                            rows={4}
                            placeholder="스크래치·슬리브 상태 등 사진으로 안 보이는 부분을 적어주세요"
                            className="w-full rounded-lg border border-line bg-surface px-3 py-2 text-[13.5px] outline-none focus:border-line-strong"
                        />
                        <span className="mt-1 block text-[11.5px] text-faint">
                            비우거나 {AUCTION_POLICY.MIN_DESC_LENGTH}~{AUCTION_POLICY.MAX_DESC_LENGTH}자 · 지금{" "}
                            <span className="font-mono tabular-nums">{description.length}</span>자
                        </span>
                    </label>
                    <p className="mt-3 text-[12px] text-muted">
                        사진은 따로 올릴 수 없어, 고른 음반의 대표 커버가 매물 사진으로 들어갑니다.
                    </p>
                </Section>

                <Section title="가격" hint="시작가·배송비·입찰 단위">
                    {editing && originalStartBidAmount !== undefined && (
                        <p className="mb-3 rounded-lg border border-line bg-paper px-3 py-2 text-[12.5px] text-muted">
                            지금 등록된 시작가와 배송비를 합치면 {formatWon(originalStartBidAmount)}입니다. 서버가 둘을
                            나눠서 알려주지 않아 다시 입력해야 합니다.
                        </p>
                    )}
                    <div className="grid gap-3 sm:grid-cols-3">
                        <NumberField
                            label="시작가"
                            value={values.startPrice}
                            onChange={(v) => patch({ startPrice: v })}
                            min={AUCTION_POLICY.MIN_START_PRICE}
                            step={100}
                            hint={`${AUCTION_POLICY.MIN_START_PRICE.toLocaleString("ko-KR")}원 이상`}
                        />
                        <NumberField
                            label="배송비"
                            value={values.shippingFee}
                            onChange={(v) => patch({ shippingFee: v })}
                            min={0}
                            step={100}
                            hint="무료면 0"
                        />
                        <NumberField
                            label="입찰 단위"
                            value={values.bidUnit}
                            onChange={(v) => patch({ bidUnit: v })}
                            min={AUCTION_POLICY.MIN_BID_UNIT}
                            step={100}
                            hint={`${AUCTION_POLICY.MIN_BID_UNIT.toLocaleString("ko-KR")}원 이상`}
                        />
                    </div>
                    <p className="mt-2 text-[12px] text-muted">
                        입찰은 시작가와 배송비를 합친 금액부터 시작합니다.
                    </p>
                </Section>

                <Section title="일정" hint="시작·마감과 자동 연장">
                    <div className="grid gap-3 sm:grid-cols-2">
                        <label className="block">
                            <span className="mb-1 block text-[11px] font-bold uppercase tracking-wider text-faint">
                                시작 시각
                            </span>
                            <input
                                type="datetime-local"
                                value={values.startAt}
                                min={minStartAt}
                                step={AUCTION_POLICY.TIME_UNIT_MINUTES * 60}
                                onChange={(e) => patch({ startAt: e.target.value })}
                                className="w-full rounded-lg border border-line bg-surface px-3 py-1.5 text-[13.5px] outline-none focus:border-line-strong"
                            />
                            <span className="mt-1 block text-[11.5px] text-faint">
                                지금으로부터 {AUCTION_POLICY.MIN_START_LEAD_MINUTES}분 뒤부터
                            </span>
                        </label>
                        <label className="block">
                            <span className="mb-1 block text-[11px] font-bold uppercase tracking-wider text-faint">
                                마감 시각
                            </span>
                            <input
                                type="datetime-local"
                                value={values.endAt}
                                min={
                                    startAtDate === null
                                        ? minStartAt
                                        : toInputValue(addHours(startAtDate, AUCTION_POLICY.MIN_DURATION_HOURS))
                                }
                                step={AUCTION_POLICY.TIME_UNIT_MINUTES * 60}
                                onChange={(e) => patch({ endAt: e.target.value })}
                                className="w-full rounded-lg border border-line bg-surface px-3 py-1.5 text-[13.5px] outline-none focus:border-line-strong"
                            />
                            <span className="mt-1 block text-[11.5px] text-faint">
                                시작보다 최소 {AUCTION_POLICY.MIN_DURATION_HOURS}시간 뒤
                            </span>
                        </label>
                    </div>
                    <label className="mt-3 flex items-center gap-2 text-[13.5px] font-semibold">
                        <input
                            type="checkbox"
                            checked={values.extensionEnabled}
                            onChange={(e) => patch({ extensionEnabled: e.target.checked })}
                        />
                        마감 직전 입찰이 들어오면 자동으로 연장
                    </label>
                    {values.extensionEnabled && (
                        <div className="mt-3 max-w-[220px]">
                            <NumberField
                                label="연장 시간 (분)"
                                value={values.extensionTime}
                                onChange={(v) => patch({ extensionTime: v })}
                                min={AUCTION_POLICY.MIN_EXTENSION_MINUTES}
                                step={1}
                                hint={`${AUCTION_POLICY.MIN_EXTENSION_MINUTES}분 이상`}
                            />
                        </div>
                    )}
                </Section>

                {errors.length > 0 && (
                    <div className="rounded-xl border border-live/30 bg-live-bg px-5 py-4">
                        <p className="font-semibold text-live">아래를 고쳐야 등록할 수 있습니다.</p>
                        <ul className="mt-2 list-disc pl-5 text-[13px] text-ink">
                            {errors.map((message) => (
                                <li key={message}>{message}</li>
                            ))}
                        </ul>
                    </div>
                )}
                {serverError && (
                    <p className="rounded-xl border border-live/30 bg-live-bg px-5 py-4 font-semibold text-live">
                        {serverError}
                    </p>
                )}

                <div className="flex flex-wrap gap-2">
                    <button
                        type="submit"
                        disabled={submit.isPending || locked}
                        className="rounded-xl bg-brand px-5 py-2.5 text-sm font-bold text-white hover:bg-brand-ink disabled:opacity-50"
                    >
                        {submit.isPending ? "저장 중…" : editing ? "수정 저장" : "경매 등록"}
                    </button>
                    {editing && (
                        <button
                            type="button"
                            disabled={cancelAuction.isPending || locked}
                            onClick={() => {
                                if (window.confirm("이 경매를 취소할까요? 목록에서 사라집니다.")) {
                                    setServerError(null);
                                    cancelAuction.mutate();
                                }
                            }}
                            className="rounded-xl border border-line bg-surface px-5 py-2.5 text-sm font-bold text-live hover:border-live disabled:opacity-50"
                        >
                            경매 취소
                        </button>
                    )}
                </div>
            </form>
        </div>
    );
}

function Section({ title, hint, children }: { title: string; hint: string; children: React.ReactNode }) {
    return (
        <section className="rounded-2xl border border-line bg-surface px-5 py-4">
            <h2 className="text-[15px] font-bold">{title}</h2>
            <p className="mb-3 text-[12.5px] text-muted">{hint}</p>
            {children}
        </section>
    );
}

interface NumberFieldProps {
    label: string;
    value: string;
    onChange: (value: string) => void;
    min: number;
    step: number;
    hint: string;
}

function NumberField({ label, value, onChange, min, step, hint }: NumberFieldProps) {
    return (
        <label className="block">
            <span className="mb-1 block text-[11px] font-bold uppercase tracking-wider text-faint">{label}</span>
            <input
                type="number"
                inputMode="numeric"
                min={min}
                step={step}
                value={value}
                onChange={(e) => onChange(e.target.value)}
                placeholder={String(min)}
                className="w-full rounded-lg border border-line bg-surface px-3 py-1.5 font-mono text-[13.5px] outline-none focus:border-line-strong"
            />
            <span className="mt-1 block text-[11.5px] text-faint">{hint}</span>
        </label>
    );
}
