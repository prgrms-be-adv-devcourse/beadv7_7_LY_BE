import { Link, useSearchParams } from "react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { confirmDeposit } from "../api/wallet";
import { ApiError } from "../api/client";
import { formatWon } from "../components/AuctionCard";

// 결제를 마치면 토스가 이 주소로 되돌려 보내면서 결제 정보를 쿼리 문자열에 붙여준다.
// 이 시점엔 아직 승인 전이라, 여기서 승인 API를 불러야 잔액에 반영된다
export function PaymentSuccessPage() {
    const queryClient = useQueryClient();
    const [params] = useSearchParams();
    const paymentKey = params.get("paymentKey") ?? "";
    const orderId = params.get("orderId") ?? "";
    const amount = Number(params.get("amount"));
    const hasParams = paymentKey !== "" && orderId !== "" && Number.isFinite(amount);

    // 새로고침이나 개발 모드의 두 번 렌더로 승인이 두 번 나가면 서버가 거절한다.
    // 주문번호를 키로 잡아 한 번만 나가게 하고, 실패해도 다시 시도하지 않는다
    const confirm = useQuery({
        queryKey: ["deposit", "confirm", orderId],
        queryFn: async () => {
            await confirmDeposit(paymentKey, orderId, amount);
            queryClient.invalidateQueries({ queryKey: ["wallet"] });
            return true;
        },
        enabled: hasParams,
        retry: false,
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
            </ResultCard>
        );
    }

    if (confirm.error) {
        const message =
            confirm.error instanceof ApiError ? confirm.error.message : "충전 승인 중 문제가 생겼습니다.";
        return (
            <ResultCard tone="error" title="충전이 반영되지 않았습니다">
                <p>{message}</p>
                <p className="mt-1 font-mono text-[11px] text-faint">주문번호 {orderId}</p>
                <p className="mt-2">
                    결제는 됐는데 반영만 안 됐을 수 있습니다. 지갑의 거래 내역을 먼저 확인해주세요.
                </p>
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
