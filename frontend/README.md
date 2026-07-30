# Groove 프론트엔드 (개인 확인용)

LP 경매 서비스 확인용 프론트. 전 화면 실제 API 연동 — 검색·상품·시세·경매·입찰은 core-service, 로그인은 member-service.
백엔드에 API가 없어서 뺀 기능은 [REMOVED-FEATURES.md](./REMOVED-FEATURES.md) 참고.

## 실행

```bash
# 1) 백엔드
cd docker/local && docker compose up -d     # MySQL
# core-service를 local 프로파일로 기동 (포트 8080) — 시드 플래그를 켜야 상품·경매 데이터가 생긴다
# member-service 기동 (포트 8081) — 로그인·입찰을 쓰려면 필요

# 2) 프론트
cd frontend
npm install                  # 최초 1회
cp .env.example .env.local   # 최초 1회 — 토스 결제위젯 클라이언트 키를 넣는다
npm run dev                  # http://localhost:5173
```

`.env.local`에는 토스 **클라이언트 키만** 넣는다. 시크릿 키는 백엔드가 결제 승인에 쓰는 값이라
core-service 실행 구성(`TOSS_CLIENT_KEY`, `TOSS_SECRET_KEY` 환경변수)에 둔다.

백엔드가 꺼져 있으면 각 화면이 에러 상태로 표시된다 (목 폴백 없음).

## 연동 현황

| 화면 | API |
|---|---|
| 카탈로그 검색 | `GET /api/v1/search/products` |
| 상품 상세 | `GET /api/v1/products/{id}` |
| 시세 차트 | `GET /api/v1/products/{id}/price-trades` |
| 홈·피드 경매 리스트 | `GET /api/v1/auctions` (genre·pressType·status·sort) |
| 경매 상세 + 호가 로그 | `GET /api/v1/auctions/{id}` (`recentBids` 포함) |
| 입찰 | `POST /api/v1/auctions/{id}/bids` (`X-Member-Id` 헤더) |
| 로그인 | member-service `POST /api/v1/auth/login` → JWT subject(memberId)를 디코드해 세션 보관 |
| 상품 상세의 열린 경매 | `GET /api/v1/auctions?productId=` (진행 중·시작 예정을 따로 조회) |
| 찜 (상품 카드·상세, 마이페이지) | `PUT`·`DELETE`·`GET /api/v1/members/me/liked-products` |
| 관심 경매 (경매 카드·상세, 마이페이지) | `PUT`·`DELETE`·`GET /api/v1/members/me/watched-auctions` |
| 마이페이지 등록·참여 경매 | `GET /api/v1/auctions/hosted`, `GET /api/v1/auctions/participated` |
| 경매 등록·수정·취소 | `POST`·`PATCH`·`DELETE /api/v1/auctions` |
| 주문 확정(배송지 입력) | `POST /api/v1/orders/{orderId}/place` + member-service `GET /api/v1/members/address` |
| 예치금 충전 | `POST /api/v1/deposits` → 토스 결제위젯 → `POST /api/v1/deposits/confirm` |
| 충전 취소 | `POST /api/v1/deposits/{depositId}/cancel` |

## 구조·설계

- `PLAN.md` 참고. 스택: React 19 + Vite + TS + TanStack Query v5 + Tailwind v4 + react-router v7
- 서버 상태는 전부 TanStack Query, 필터·페이지는 URL searchParams가 단일 소스
- `/api` 요청은 Vite dev proxy가 `localhost:8080`으로 전달 (CORS 회피, 백엔드 무수정)
- 비주얼: A안(슬리브 노트) 베이스 + C안 포인트(경매 상세 VU미터 카운트다운, 커버 vinyl 회전)

## 명령

- `npm run dev` — 개발 서버
- `npm run typecheck` — tsc --noEmit
- `npm run build` — 타입체크 + 프로덕션 빌드
