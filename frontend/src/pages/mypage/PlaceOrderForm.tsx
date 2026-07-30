import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getMyAddress } from "../../api/members";
import { placeOrder } from "../../api/orders";
import { ApiError } from "../../api/client";

interface PlaceOrderFormProps {
    orderId: number;
    onDone: () => void;
}

// 낙찰 뒤 결제 대기 상태인 주문에 배송지를 넣어 주문을 확정하는 폼.
// 회원 주소는 기본값으로만 쓰고, 이 주문에만 다른 곳으로 받고 싶을 수 있으니 그대로 고칠 수 있게 둔다
export function PlaceOrderForm({ orderId, onDone }: PlaceOrderFormProps) {
    const queryClient = useQueryClient();
    const [recipientName, setRecipientName] = useState("");
    const [phoneNumber, setPhoneNumber] = useState("");
    const [baseAddress, setBaseAddress] = useState("");
    const [detailAddress, setDetailAddress] = useState("");
    const [error, setError] = useState<string | null>(null);

    const address = useQuery({ queryKey: ["member", "address"], queryFn: getMyAddress });

    // 주소 조회가 늦게 도착해도 채워지도록, 사용자가 아직 손대지 않은 칸만 기본값으로 메운다
    useEffect(() => {
        if (!address.data) return;
        setBaseAddress((current) => current || address.data.baseAddress);
        setDetailAddress((current) => current || address.data.detailAddress);
    }, [address.data]);

    const submit = useMutation({
        mutationFn: () => placeOrder(orderId, { recipientName, phoneNumber, baseAddress, detailAddress }),
        onSuccess: () => {
            setError(null);
            queryClient.invalidateQueries({ queryKey: ["orders"] });
            queryClient.invalidateQueries({ queryKey: ["order", orderId] });
            onDone();
        },
        onError: (e: unknown) => {
            setError(e instanceof ApiError ? e.message : "주문 처리 중 문제가 생겼습니다.");
        },
    });

    function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        if (!recipientName.trim() || !phoneNumber.trim() || !baseAddress.trim() || !detailAddress.trim()) {
            setError("수령인·연락처·주소를 모두 입력해야 주문할 수 있습니다.");
            return;
        }
        submit.mutate();
    }

    return (
        <form onSubmit={handleSubmit} className="mt-3 rounded-lg border border-line bg-paper px-4 py-3.5">
            <p className="mb-1 text-[13px] font-bold">배송지 입력</p>
            <p className="mb-3 text-[12px] text-muted">
                {address.isPending && "회원 정보의 주소를 불러오는 중입니다…"}
                {address.error && "회원 주소를 불러오지 못했습니다. 주소를 직접 입력해주세요."}
                {address.data && "회원 정보의 주소를 채워뒀습니다. 이번 주문만 다른 곳으로 받으려면 고쳐서 보내세요."}
            </p>
            <div className="grid gap-2.5 sm:grid-cols-2">
                <Field label="수령인" value={recipientName} onChange={setRecipientName} placeholder="받는 분 이름" />
                <Field label="연락처" value={phoneNumber} onChange={setPhoneNumber} placeholder="010-0000-0000" />
                <Field
                    label="기본 주소"
                    value={baseAddress}
                    onChange={setBaseAddress}
                    placeholder="도로명 주소"
                    className="sm:col-span-2"
                />
                <Field
                    label="상세 주소"
                    value={detailAddress}
                    onChange={setDetailAddress}
                    placeholder="동·호수 등"
                    className="sm:col-span-2"
                />
            </div>
            {address.data?.zipcode && (
                <p className="mt-2 text-[11.5px] text-faint">회원 정보 우편번호 {address.data.zipcode}</p>
            )}
            {error && <p className="mt-2 text-xs font-semibold text-live">{error}</p>}
            <div className="mt-3 flex gap-2">
                <button
                    type="submit"
                    disabled={submit.isPending}
                    className="rounded-lg bg-brand px-3 py-1.5 text-xs font-semibold text-white hover:bg-brand-ink disabled:opacity-50"
                >
                    {submit.isPending ? "처리 중…" : "이 주소로 주문하기"}
                </button>
                <button
                    type="button"
                    onClick={onDone}
                    className="rounded-lg border border-line bg-surface px-3 py-1.5 text-xs font-semibold text-muted hover:border-line-strong hover:text-ink"
                >
                    닫기
                </button>
            </div>
        </form>
    );
}

interface FieldProps {
    label: string;
    value: string;
    onChange: (value: string) => void;
    placeholder: string;
    className?: string;
}

function Field({ label, value, onChange, placeholder, className = "" }: FieldProps) {
    return (
        <label className={`block ${className}`}>
            <span className="mb-1 block text-[11px] font-bold uppercase tracking-wider text-faint">{label}</span>
            <input
                value={value}
                onChange={(e) => onChange(e.target.value)}
                placeholder={placeholder}
                className="w-full rounded-lg border border-line bg-surface px-3 py-1.5 text-[13px] outline-none focus:border-line-strong"
            />
        </label>
    );
}
