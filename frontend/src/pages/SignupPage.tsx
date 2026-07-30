import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { register } from "../api/members";
import { ApiError } from "../api/client";

interface Field {
    key: keyof FormValues;
    label: string;
    type: string;
    placeholder: string;
}

interface FormValues {
    email: string;
    password: string;
    nickName: string;
    name: string;
    phoneNumber: string;
    zipcode: string;
    baseAddress: string;
    detailAddress: string;
}

// 백엔드 Member 도메인 검증 규칙과 동일하게 맞춘다 (서버는 통짜 "유효하지 않은 회원 정보" 메시지만 주므로 프론트에서 먼저 거른다)
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]).{8,16}$/;
const PHONE_PATTERN = /^01[016789]-\d{3,4}-\d{4}$/;

const FIELDS: Field[] = [
    { key: "email", label: "이메일", type: "email", placeholder: "you@example.com" },
    { key: "password", label: "비밀번호 (8~16자, 영문+숫자+특수문자)", type: "password", placeholder: "예: groovid1!" },
    { key: "nickName", label: "닉네임 (2~6자)", type: "text", placeholder: "그루버" },
    { key: "name", label: "이름", type: "text", placeholder: "홍길동" },
    { key: "phoneNumber", label: "전화번호 (하이픈 포함)", type: "tel", placeholder: "010-1234-5678" },
    { key: "zipcode", label: "우편번호", type: "text", placeholder: "06236" },
    { key: "baseAddress", label: "기본 주소", type: "text", placeholder: "서울시 강남구 테헤란로 123" },
    { key: "detailAddress", label: "상세 주소", type: "text", placeholder: "101동 1001호" },
];

function validateForm(values: FormValues): string | null {
    if (!PASSWORD_PATTERN.test(values.password)) {
        return "비밀번호는 8~16자이면서 영문·숫자·특수문자를 각각 1개 이상 포함해야 합니다.";
    }
    const nickLength = values.nickName.trim().length;
    if (nickLength < 2 || nickLength > 6) {
        return "닉네임은 2~6자여야 합니다.";
    }
    if (!PHONE_PATTERN.test(values.phoneNumber.trim())) {
        return "전화번호는 010-1234-5678 형식(하이픈 포함)이어야 합니다.";
    }
    return null;
}

const EMPTY_FORM: FormValues = {
    email: "",
    password: "",
    nickName: "",
    name: "",
    phoneNumber: "",
    zipcode: "",
    baseAddress: "",
    detailAddress: "",
};

export function SignupPage() {
    const navigate = useNavigate();
    const [values, setValues] = useState<FormValues>(EMPTY_FORM);
    const [error, setError] = useState<string | null>(null);
    const [pending, setPending] = useState(false);

    async function submit(e: React.FormEvent) {
        e.preventDefault();
        const missing = FIELDS.find((f) => !values[f.key].trim());
        if (missing) {
            setError(`${missing.label.replace(/ \(.*\)$/, "")}을(를) 입력하세요.`);
            return;
        }
        const invalid = validateForm(values);
        if (invalid) {
            setError(invalid);
            return;
        }
        setPending(true);
        setError(null);
        try {
            await register({
                ...values,
                email: values.email.trim(),
                nickName: values.nickName.trim(),
                name: values.name.trim(),
                phoneNumber: values.phoneNumber.trim(),
                zipcode: values.zipcode.trim(),
                baseAddress: values.baseAddress.trim(),
                detailAddress: values.detailAddress.trim(),
            });
            navigate("/login", { state: { signedUp: true } });
        } catch (err) {
            setError(err instanceof ApiError ? err.message : "회원가입 처리 중 문제가 생겼습니다.");
        } finally {
            setPending(false);
        }
    }

    return (
        <div className="mx-auto max-w-md">
            <div className="rounded-2xl border border-line bg-surface p-7 shadow-sm">
                <h1 className="font-display text-2xl font-semibold tracking-tight">회원가입</h1>
                <p className="mt-1 text-[13px] text-muted">몇 가지만 입력하면 바로 경매에 참여할 수 있습니다.</p>
                <form onSubmit={submit} className="mt-5 flex flex-col gap-3">
                    {FIELDS.map((field) => (
                        <label key={field.key} className="text-[13px] font-semibold">
                            {field.label}
                            <input
                                type={field.type}
                                value={values[field.key]}
                                onChange={(e) => setValues((prev) => ({ ...prev, [field.key]: e.target.value }))}
                                placeholder={field.placeholder}
                                className="mt-1 w-full rounded-lg border border-line bg-paper px-3 py-2.5 text-sm outline-none placeholder:text-faint focus:border-brand"
                            />
                        </label>
                    ))}
                    {error && <p className="text-[13px] font-semibold text-live">{error}</p>}
                    <button
                        type="submit"
                        disabled={pending}
                        className="mt-2 rounded-xl bg-brand py-3 text-sm font-bold text-white transition-colors hover:bg-brand-ink disabled:opacity-60"
                    >
                        {pending ? "가입 중…" : "가입하기"}
                    </button>
                </form>
                <p className="mt-4 text-center text-[13px] text-muted">
                    이미 계정이 있나요?{" "}
                    <Link to="/login" className="font-semibold text-brand hover:underline">
                        로그인
                    </Link>
                </p>
            </div>
        </div>
    );
}
