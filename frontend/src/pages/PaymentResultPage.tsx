import { Link, useSearchParams } from "react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { confirmDeposit, DEPOSIT_ALREADY_PROCESSED, DEPOSIT_PG_ERROR } from "../api/wallet";
import { ApiError } from "../api/client";
import { formatWon } from "../components/AuctionCard";

// 앱 승인이 끝나기를 기다리는 폭. 실제로 결제 생성부터 승인까지 1분 넘게 걸린 사례가 있어
// 넉넉히 잡는다 (5초 × 12회 ≈ 1분)
const CONFIRM_RETRIES = 12;
const CONFIRM_RETRY_DELAY_MS = 5000;

// 결제를 마치면 토스가 이 주소로 되돌려 보내면서 결제 정보를 쿼리 문자열에 붙여준다.
// 이 시점엔 아직 승인 전이라, 여기서 승인 API를 불러야 잔액에 반영된다
export function PaymentSuccessPage() {
    const queryClient = useQueryClient();
    const [params] = useSearchParams();
    const paymentKey = params.get("paymentKey") ?? "";
    const orderId = params.get("orderId") ?? "";
    const amount = Number(params.get("amount"));
    const hasParams = paymentKey !== "" && orderId !== "" && Number.isFinite(amount);

    // QR을 찍어 앱에서 결제하는 흐름에서는, 브라우저가 이 화면으로 먼저 돌아오고
    // 앱 승인이 1~2분 뒤에 끝나는 일이 있다. 그 사이에 물어보면 결제사가 거절하므로
    // 곧바로 실패로 단정하지 않고 몇 번 더 물어본다.
    // 여러 번 물어봐도 잔액은 한 번만 늘어난다 — 서버가 이미 처리된 충전을 막는다
    const confirm = useQuery({
        queryKey: ["deposit", "confirm", orderId],
        queryFn: async () => {
            try {
                await confirmDeposit(paymentKey, orderId, amount);
            } catch (e: unknown) {
                // 앞선 시도가 이미 통과한 경우다. 잔액에는 들어가 있으니 성공으로 본다
                if (e instanceof ApiError && e.code === DEPOSIT_ALREADY_PROCESSED) {
                    queryClient.invalidateQueries({ queryKey: ["wallet"] });
                    return true;
                }
                throw e;
            }
            queryClient.invalidateQueries({ queryKey: ["wallet"] });
            return true;
        },
        enabled: hasParams,
        retry: (failureCount, error) =>
            error instanceof ApiError && error.code === DEPOSIT_PG_ERROR && failureCount < CONFIRM_RETRIES,
        retryDelay: CONFIRM_RETRY_DELAY_MS,
        staleTime: Infinity,
        gcTime: Infinity,
    });

    if (!hasParams) {
        return (
            <ResultCard tone="error" title="결제 정보가 없습니다">
                <p>결제 결과에 필요한 값이 주소에 없습니다. 지갑에서 충전을 다시 시작해주세요.</p>
            </ResultCard>
        );
    }

    if (confirm.isPending) {
        return (
            <ResultCard tone="neutral" title="결제를 확인하는 중입니다">
                <p>결제사에 승인을 요청하고 있습니다. 창을 닫지 마세요.</p>
                {confirm.failureCount > 0 && (
                    <p className="mt-2">
                        결제 앱에서 승인이 아직 안 끝난 것 같습니다. 앱에서 결제를 마치면 이 화면이 알아서
                        넘어갑니다 — <span className="font-mono tabular-nums">{confirm.failureCount}</span>번째
                        확인 중입니다.
                    </p>
                )}
            </ResultCard>
        );
    }

    if (confirm.error) {
        const message =
            confirm.error instanceof ApiError ? confirm.error.message : "충전 승인 중 문제가 생겼습니다.";
        return (
            <ResultCard tone="error" title="충전이 아직 반영되지 않았습니다">
                <p>{message}</p>
                <p className="mt-1 font-mono text-[11px] text-faint">주문번호 {orderId}</p>
                <p className="mt-2">
                    QR을 찍어 앱에서 결제하셨다면, 앱에서 승인을 마친 뒤 아래 버튼을 눌러주세요. 여러 번 눌러도
                    잔액은 한 번만 늘어납니다.
                </p>
                <button
                    type="button"
                    onClick={() => confirm.refetch()}
                    disabled={confirm.isFetching}
                    className="mt-3 rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white hover:bg-brand-ink disabled:opacity-50"
                >
                    {confirm.isFetching ? "확인 중…" : "다시 확인하기"}
                </button>
            </ResultCard>
        );
    }

    return (
        <ResultCard tone="success" title="충전이 완료됐습니다">
            <p>
                <b className="font-mono tabular-nums">{formatWon(amount)}</b>이 예치금 잔액에 더해졌습니다.
            </p>
            <p className="mt-1 font-mono text-[11px] text-faint">주문번호 {orderId}</p>
        </ResultCard>
    );
}

export function PaymentFailPage() {
    const [params] = useSearchParams();
    const code = params.get("code");
    const message = params.get("message");

    return (
        <ResultCard tone="error" title="결제가 완료되지 않았습니다">
            <p>{message ?? "결제사에서 결제를 마치지 못했습니다."}</p>
            {code && <p className="mt-1 font-mono text-[11px] text-faint">{code}</p>}
            <p className="mt-2">충전 요청은 그대로 남아 있습니다. 지갑에서 다시 시도할 수 있습니다.</p>
        </ResultCard>
    );
}

interface ResultCardProps {
    tone: "success" | "error" | "neutral";
    title: string;
    children: React.ReactNode;
}

const TONE_STYLES: Record<string, string> = {
    success: "border-up/40 bg-surface",
    error: "border-live/30 bg-live-bg",
    neutral: "border-line bg-surface",
};

function ResultCard({ tone, title, children }: ResultCardProps) {
    return (
        <div className={`mx-auto max-w-[520px] rounded-2xl border px-6 py-10 text-center ${TONE_STYLES[tone]}`}>
            <h1 className="text-lg font-bold tracking-tight">{title}</h1>
            <div className="mt-2 text-[13.5px] text-muted">{children}</div>
            <div className="mt-5 flex justify-center gap-2">
                <Link
                    to="/mypage?tab=wallet"
                    className="rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white hover:bg-brand-ink"
                >
                    지갑으로 가기
                </Link>
                <Link
                    to="/feed"
                    className="rounded-lg border border-line bg-surface px-4 py-2 text-sm font-semibold hover:border-line-strong"
                >
                    경매 둘러보기
                </Link>
            </div>
        </div>
    );
}
