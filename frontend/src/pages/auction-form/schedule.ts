import { AUCTION_POLICY } from "../../api/auctions";

// datetime-local 입력값은 "YYYY-MM-DDTHH:mm" 형태의 지역 시각이다.
// 서버 LocalDateTime도 시간대 없는 지역 시각이라 그대로 초만 붙여 보내면 된다.
// toISOString()을 쓰면 UTC로 변환돼 9시간이 어긋난다
export function toInputValue(date: Date): string {
    const pad = (n: number) => String(n).padStart(2, "0");
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(
        date.getMinutes(),
    )}`;
}

export function parseInputValue(value: string): Date | null {
    if (!value) return null;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
}

export function toServerDateTime(value: string): string {
    return `${value}:00`;
}

// 서버가 준 "2026-07-31T15:00:00"을 입력칸이 읽는 "2026-07-31T15:00"으로 자른다
export function toInputValueFromServer(value: string): string {
    return value.slice(0, 16);
}

// 등록·수정 모두 시작 시각이 지금으로부터 최소 리드타임 뒤여야 한다.
// 입력 단위(10분)에 맞춰 올림해서 고르기 쉬운 시각을 기본값으로 준다
export function earliestStartAt(now: Date): Date {
    const unitMs = AUCTION_POLICY.TIME_UNIT_MINUTES * 60_000;
    const earliest = now.getTime() + AUCTION_POLICY.MIN_START_LEAD_MINUTES * 60_000;
    return new Date(Math.ceil(earliest / unitMs) * unitMs);
}

export function addHours(date: Date, hours: number): Date {
    return new Date(date.getTime() + hours * 3600_000);
}

export interface AuctionFormValues {
    productId: number | null;
    itemCondition: string;
    itemDescription: string;
    startPrice: string;
    shippingFee: string;
    bidUnit: string;
    startAt: string;
    endAt: string;
    extensionEnabled: boolean;
    extensionTime: string;
}

// 서버 검증에 걸리면 실패 응답만 오고 어느 칸이 문제인지 알기 어렵다.
// 같은 규칙을 여기서 먼저 확인해 사람이 읽을 수 있는 문장으로 돌려준다
export function validateAuctionForm(values: AuctionFormValues, now: Date): string[] {
    const errors: string[] = [];

    if (values.productId === null) {
        errors.push("어떤 음반의 경매인지 마스터 상품을 골라주세요.");
    }
    if (!values.itemCondition) {
        errors.push("음반 상태를 골라주세요.");
    }

    const description = values.itemDescription.trim();
    if (description.length > 0
        && (description.length < AUCTION_POLICY.MIN_DESC_LENGTH || description.length > AUCTION_POLICY.MAX_DESC_LENGTH)) {
        errors.push(
            `설명은 비우거나 ${AUCTION_POLICY.MIN_DESC_LENGTH}~${AUCTION_POLICY.MAX_DESC_LENGTH}자로 써주세요 (지금 ${description.length}자).`,
        );
    }

    const startPrice = Number(values.startPrice);
    if (values.startPrice === "" || !Number.isFinite(startPrice) || startPrice < AUCTION_POLICY.MIN_START_PRICE) {
        errors.push(`시작가는 ${AUCTION_POLICY.MIN_START_PRICE.toLocaleString("ko-KR")}원 이상이어야 합니다.`);
    }

    const shippingFee = Number(values.shippingFee);
    if (values.shippingFee === "" || !Number.isFinite(shippingFee) || shippingFee < 0) {
        errors.push("배송비를 0원 이상으로 입력해주세요.");
    }

    const bidUnit = Number(values.bidUnit);
    if (values.bidUnit === "" || !Number.isFinite(bidUnit) || bidUnit < AUCTION_POLICY.MIN_BID_UNIT) {
        errors.push(`입찰 단위는 ${AUCTION_POLICY.MIN_BID_UNIT.toLocaleString("ko-KR")}원 이상이어야 합니다.`);
    }

    const startAt = parseInputValue(values.startAt);
    const endAt = parseInputValue(values.endAt);

    if (startAt === null) {
        errors.push("시작 시각을 입력해주세요.");
    } else if (startAt.getTime() < now.getTime() + AUCTION_POLICY.MIN_START_LEAD_MINUTES * 60_000) {
        errors.push(
            `시작 시각은 지금으로부터 ${AUCTION_POLICY.MIN_START_LEAD_MINUTES}분 뒤부터 고를 수 있습니다 (가장 빠른 시각 ${toInputValue(
                earliestStartAt(now),
            ).replace("T", " ")}).`,
        );
    }

    if (endAt === null) {
        errors.push("마감 시각을 입력해주세요.");
    } else if (startAt !== null && endAt.getTime() < addHours(startAt, AUCTION_POLICY.MIN_DURATION_HOURS).getTime()) {
        errors.push(`마감 시각은 시작 시각보다 최소 ${AUCTION_POLICY.MIN_DURATION_HOURS}시간 뒤여야 합니다.`);
    }

    if (values.extensionEnabled) {
        const extensionTime = Number(values.extensionTime);
        if (values.extensionTime === "" || !Number.isFinite(extensionTime)
            || extensionTime < AUCTION_POLICY.MIN_EXTENSION_MINUTES) {
            errors.push(`자동 연장을 켰다면 연장 시간을 ${AUCTION_POLICY.MIN_EXTENSION_MINUTES}분 이상으로 넣어주세요.`);
        }
    }

    return errors;
}

// 수정·취소는 시작 시각 직전까지만 된다. 시한을 넘기면 서버가 거절하므로 버튼을 미리 잠근다
export function isEditableNow(startAt: string, now: Date): boolean {
    const start = parseInputValue(startAt);
    if (start === null) return false;
    return now.getTime() < start.getTime() - AUCTION_POLICY.EDIT_DEADLINE_MINUTES * 60_000;
}
