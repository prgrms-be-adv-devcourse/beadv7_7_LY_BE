import { apiGet, apiPost } from "./client";

// member-service POST /api/v1/members — 필드명은 백엔드 RegisterRequest 그대로 (nickName의 N 대문자 주의)
export interface RegisterRequest {
    email: string;
    password: string;
    nickName: string;
    name: string;
    phoneNumber: string;
    zipcode: string;
    baseAddress: string;
    detailAddress: string;
}

export function register(request: RegisterRequest): Promise<void> {
    return apiPost<void>("/api/v1/members", request);
}

// member-service GET /api/v1/members/me — 응답은 MemberProfileDto(가입 때 쓰는 nickName과 달리 nickname)
export interface MemberProfile {
    email: string;
    nickname: string;
}

export function getMyProfile(): Promise<MemberProfile> {
    return apiGet<MemberProfile>("/api/v1/members/me");
}

// member-service GET /api/v1/members/address — 가입할 때 넣은 주소.
// 주문 배송지 입력의 기본값으로 쓴다 (수령인 이름·연락처는 이 응답에 없다)
export interface MemberAddress {
    zipcode: string;
    baseAddress: string;
    detailAddress: string;
}

export function getMyAddress(): Promise<MemberAddress> {
    return apiGet<MemberAddress>("/api/v1/members/address");
}
