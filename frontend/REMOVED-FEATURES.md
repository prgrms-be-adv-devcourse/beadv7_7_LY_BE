# 실연동 전환 때 뺀 기능 (2026-07-29)

목데이터 시절에는 있었지만 **백엔드에 대응 API가 없어서** 실연동 전환(dev 602d574 기준) 때 프론트에서 뺀 기능 목록.
백엔드에 해당 API가 생기면 이 문서를 보고 복원한다. 목 구현 원본은 git 히스토리의
`src/mocks/auctions.ts`, `src/pages/product-detail/OpenAuctionList.tsx`에 있다.

## 1. 상품 상세 — "진행 중 경매" 목록 (✅ 2026-07-30 복원, #147)

- 있던 것: 상품 상세 하단에 이 상품의 열린 경매 리스트 (`OpenAuctionList.tsx`)
- 뺐던 이유: 경매 리스트 API(`GET /api/v1/auctions`)에 `productId` 필터 파라미터가 없었다.
  응답 항목에 productId가 있어 클라이언트 필터링은 가능하지만 페이징 때문에 전 페이지를 긁어야 해서 부적합
- 복원: 백엔드 `AuctionListQuery`에 productId가 생겨 `GET /api/v1/auctions?productId={id}`로 다시 붙였다.
  상태 필터가 값을 하나만 받아서 진행 중과 시작 예정을 따로 부른 뒤 이어 붙인다

## 2. 경매 피드 — 필터 3종 제거

| 필터 | 뺀 이유 |
|---|---|
| 아티스트 검색 (디바운스 입력) | 경매 리스트에 키워드 검색 파라미터 없음 |
| 컨디션 (M / NM / VG+) | 리스트 쿼리에 itemCondition 필터 없음 (검색 뷰에 컬럼 자체가 없음) |
| 가격 범위 (최소~최대) | minPrice/maxPrice 파라미터 없음 |

- 대신 백엔드가 지원하는 필터로 교체: **장르 / 프레스(ORIGINAL·REISSUE) / 상태(진행 중·시작 전·낙찰·유찰)**
- 복원 조건: `AuctionListQuery`(→ `AuctionSearchViewJpaRepository` 쿼리)에 해당 파라미터 추가

## 3. 정렬 — "최신순(newest)" 제거

- 뺀 이유: 백엔드 `AuctionSortType`은 `ending_soon` / `price_asc` / `price_desc` / `most_bids` 4종뿐
- 여파: 홈의 "실시간 경매(최신순)" 레일을 **"입찰 많은 경매"**(most_bids)로 대체
- 복원 조건: 정렬 타입에 등록순(createdAt desc) 추가

## 4. 경매 카드·상세 — 목 전용 표시 항목 제거

| 항목 | 뺀 이유 |
|---|---|
| 판매자 검증 배지 (미검증 표시) | 리스트·상세 응답에 판매자 검증 여부 없음 |
| 배송비 | 응답에 배송비 없음 |
| 슬리브 컨디션 (미디어/슬리브 분리 표기) | 백엔드는 `itemCondition` 단일 값 (슬리브 별도 등급 없음) |
| 카드의 컨디션 배지 (M/NM/VG+) | 리스트 응답에 itemCondition이 없음 → 카드에서는 프레스 배지로 대체. 상세에는 itemCondition이 있어 유지 |

## 5. 시세 차트 — "최근 90일" 범위 문구 제거

- 뺀 이유: `GET /price-trades`는 기간 파라미터 없이 원장 이력을 반환 → 90일 컷은 프론트가 임의로 만들던 목 개념
- 현재: 받은 전체 낙찰 이력 중 M/NM만 그린다 (캡션 "M/NM 컨디션 낙찰가 기준")

## 제거가 아니라 실물로 대체된 것 (참고)

- 목 입찰(화면에서만 반영) → `POST /api/v1/auctions/{id}/bids` 실제 입찰. 로그인 세션의 memberId를 `X-Member-Id` 헤더로 전송
- 목 로그인(이메일 저장만) → member-service `POST /api/v1/auth/login`. JWT의 subject가 memberId라 프론트에서 디코드해 세션에 보관
- 호가 로그(목 생성) → 경매 상세 응답의 `recentBids`
- 시세 히스토리(목 생성) → `GET /api/v1/products/{id}/price-trades` (원장 기반, #109)
