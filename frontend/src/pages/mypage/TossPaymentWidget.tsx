import { useEffect, useRef, useState } from "react";
import { loadTossPayments, type TossPaymentsWidgets } from "@tosspayments/tosspayments-sdk";
import { formatWon } from "../../components/AuctionCard";

const CLIENT_KEY = import.meta.env.VITE_TOSS_CLIENT_KEY;

const METHODS_SELECTOR = "toss-payment-methods";
const AGREEMENT_SELECTOR = "toss-agreement";

interface TossPaymentWidgetProps {
    orderId: string;
    amount: number;
    customerKey: string;
}

// 결제위젯을 띄우고 결제창까지 보내는 부분.
// 결제가 끝나면 토스가 successUrl로 되돌려 보내주고, 거기서 승인 API를 부른다
export function TossPaymentWidget({ orderId, amount, customerKey }: TossPaymentWidgetProps) {
    const widgetsRef = useRef<TossPaymentsWidgets | null>(null);
    const [ready, setReady] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [paying, setPaying] = useState(false);

    useEffect(() => {
        if (!CLIENT_KEY) {
            setError("토스 클라이언트 키가 없습니다. frontend/.env.local에 VITE_TOSS_CLIENT_KEY를 넣고 개발 서버를 다시 켜세요.");
            return;
        }

        // 위젯을 그리는 동안 다른 충전 요청이 생기면 앞선 렌더 결과를 버린다
        let stale = false;
        setReady(false);
        setError(null);

        loadTossPayments(CLIENT_KEY)
            .then(async (tossPayments) => {
                const widgets = tossPayments.widgets({ customerKey });
                await widgets.setAmount({ currency: "KRW", value: amount });
                await Promise.all([
                    widgets.renderPaymentMethods({ selector: `#${METHODS_SELECTOR}`, variantKey: "DEFAULT" }),
                    widgets.renderAgreement({ selector: `#${AGREEMENT_SELECTOR}`, variantKey: "AGREEMENT" }),
                ]);
                if (stale) return;
                widgetsRef.current = widgets;
                setReady(true);
            })
            .catch((e: unknown) => {
                if (stale) return;
                setError(e instanceof Error ? e.message : "결제위젯을 불러오지 못했습니다.");
            });

        return () => {
            stale = true;
        };
    }, [orderId, amount, customerKey]);

    async function pay() {
        const widgets = widgetsRef.current;
        if (!widgets) return;
        setPaying(true);
        setError(null);
        try {
            await widgets.requestPayment({
                orderId,
                orderName: "예치금 충전",
                successUrl: `${window.location.origin}/payments/success`,
                failUrl: `${window.location.origin}/payments/fail`,
            });
        } catch (e: unknown) {
            // 사용자가 결제창을 닫은 경우도 여기로 온다
            setError(e instanceof Error ? e.message : "결제를 시작하지 못했습니다.");
            setPaying(false);
        }
    }

    return (
        <div className="mt-4 border-t border-line pt-4">
            <p className="text-[13px] font-bold">
                결제 수단을 고르고 {formatWon(amount)}를 결제하세요
            </p>
            <p className="mb-3 font-mono text-[11px] text-faint">주문번호 {orderId}</p>
            <div id={METHODS_SELECTOR} />
            <div id={AGREEMENT_SELECTOR} />
            {error && <p className="mt-2 text-xs font-semibold text-live">{error}</p>}
            <button
                type="button"
                onClick={pay}
                disabled={!ready || paying}
                className="mt-3 w-full rounded-xl bg-brand py-3 text-sm font-bold text-white hover:bg-brand-ink disabled:opacity-50"
            >
                {!ready ? "결제 수단을 불러오는 중…" : paying ? "결제창으로 이동 중…" : `${formatWon(amount)} 결제하기`}
            </button>
        </div>
    );
}
