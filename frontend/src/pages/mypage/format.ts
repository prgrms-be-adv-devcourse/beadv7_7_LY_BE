export function formatDateTime(value: string | null | undefined): string {
    if (!value) return "-";
    return new Date(value).toLocaleString("ko-KR", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
    });
}

export function formatDate(value: string | null | undefined): string {
    if (!value) return "-";
    return new Date(value).toLocaleDateString("ko-KR");
}

// 백엔드 기간 필터는 LocalDateTime을 받으므로 날짜 입력값(YYYY-MM-DD)에 시각을 붙여 보낸다.
// 날짜만 보내면 파라미터 변환이 실패해 조회 자체가 에러가 된다
export function toRangeStart(date: string): string | undefined {
    return date ? `${date}T00:00:00` : undefined;
}

export function toRangeEnd(date: string): string | undefined {
    return date ? `${date}T23:59:59` : undefined;
}
