import { useEffect, useRef, useState } from "react";
import {
    loadTossPayments,
    type TossPaymentsWidgets,
    type WidgetAgreementWidget,
    type WidgetPaymentMethodWidget,
} from "@tosspayments/tosspayments-sdk";
import { formatWon } from "../../components/AuctionCard";

const CLIENT_KEY = import.meta.env.VITE_TOSS_CLIENT_KEY;

const METHODS_SELECTOR = "toss-payment-methods";
const AGREEMENT_SELECTOR = "toss-agreement";

// 약관 위젯은 한 화면에 하나만 존재할 수 있다. 두 번 그리면 토스가
// "하나의 약관 위젯만을 사용할 수 있어요"로 거절한다.
// 그리기와 지우기를 이 줄에 세워, 앞의 위젯이 완전히 지워진 뒤에만 다음 것을 그린다.
// (개발 모드에서 React가 effect를 두 번 실행하는 경우, 그리고 충전을 다시 요청해
//  위젯이 새로 만들어지는 경우 둘 다 여기서 걸린다)
let renderQueue: Promise<unknown> = Promise.resolve();

function enqueue(task: () => Promise<void>): void {
    renderQueue = renderQueue.then(task, task);
}

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
            // 설정 누락은 사용자가 할 수 있는 게 없다. 자세한 원인은 콘솔에만 남긴다
            console.error("VITE_TOSS_CLIENT_KEY가 설정되지 않았습니다. frontend/.env.local을 확인하세요.");
            setError("결제 모듈을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.");
            return;
        }

        // 이 실행이 정리된 뒤인지 표시한다. 그리기 '전에' 확인해야
        // 지워질 위젯을 굳이 그려놓고 버리는 일이 없다
        let cancelled = false;
        // 정리할 때 지워야 할 것들. 그리는 즉시 담아둔다
        const drawn: { methods?: WidgetPaymentMethodWidget; agreement?: WidgetAgreementWidget } = {};
        setReady(false);
        setError(null);

        enqueue(async () => {
            if (cancelled) return;
            try {
                const tossPayments = await loadTossPayments(CLIENT_KEY);
                const widgets = tossPayments.widgets({ customerKey });
                await widgets.setAmount({ currency: "KRW", value: amount });
                if (cancelled) return;
                drawn.methods = await widgets.renderPaymentMethods({
                    selector: `#${METHODS_SELECTOR}`,
                    variantKey: "DEFAULT",
                });
                drawn.agreement = await widgets.renderAgreement({
                    selector: `#${AGREEMENT_SELECTOR}`,
                    variantKey: "AGREEMENT",
                });
                if (cancelled) return;
                widgetsRef.current = widgets;
                setReady(true);
            } catch (e: unknown) {
                if (cancelled) return;
                setError(e instanceof Error ? e.message : "결제위젯을 불러오지 못했습니다.");
            }
        });

        return () => {
            cancelled = true;
            widgetsRef.current = null;
            enqueue(async () => {
                try {
                    await Promise.all([drawn.methods?.destroy(), drawn.agreement?.destroy()]);
                } catch {
                    // 화면을 떠나며 지우는 중이라, 실패해도 사용자에게 보여줄 것이 없다
                }
            });
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
