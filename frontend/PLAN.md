# Groovid 프론트엔드 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans로 태스크 단위 실행. 체크박스(`- [ ]`)로 진행 추적.

**Goal:** docs/frontend에 LP 경매 서비스 확인용 프론트(Groove)를 구축한다 — 검색·상세는 실제 API, 나머지는 목데이터.

**Architecture:** Vite SPA. 서버 상태는 전부 TanStack Query v5, 화면 간 공유 상태는 URL searchParams(react-router v7)로만. 목데이터는 순수 TS 모듈(결정적 시드)로 두고 실제 API와 같은 Promise 인터페이스로 감싸 Query로 소비한다.

**Tech Stack:** React 19 + Vite + TypeScript + TanStack Query v5 + Tailwind CSS v4(@tailwindcss/vite) + react-router v7(라이브러리 모드)

**Visual:** A안(슬리브 노트 — 페이퍼 #F7F6F1 / 잉크 #23241F / 브랜드 #34506F / 라이브 #B24A28, 앨범 제목만 세리프) 베이스 + C안 포인트(경매 상세 VU미터 카운트다운, 커버 위 도는 vinyl + 스포트라이트 글로우, 앰버 #E5A13D는 이 두 곳에만).

## Global Constraints

- 파일 변경은 **docs/frontend/ 내부로 한정** (백엔드·gitignore 밖 파일 절대 금지. docs/는 gitignore 대상 확인됨)
- 검색어 입력 300ms 디바운스 후 요청
- 필터(아티스트·상태등급·가격범위·정렬)는 URL searchParams 반영 → 새로고침/공유 시 복원
- 무한스크롤 금지, 페이지네이션
- 모든 조회 화면에서 로딩/빈결과/에러 상태 각각 처리
- 서버 상태는 전부 TanStack Query. 전역 상태 라이브러리 도입 금지 (React 내장 context/state만)
- 디자인 시스템·공통 컴포넌트 선제작 금지 — **2회 이상 반복될 때만 추출**
- 컴포넌트 150줄 초과 시 분리
- 커밋 없음 (docs/는 gitignore 대상, 사용자 요청 시에만 커밋 규칙 유지)
- 검증은 `npx tsc --noEmit` + `npm run build` + dev 서버 스모크 (테스트 인프라 미도입 — 개인 확인용, 사용자 시간 제약으로 TDD 생략)

## 실제 API 계약 (백엔드 확인 완료)

```
GET /api/v1/search/products?q={string}&page={int,0-base}&size={int,default20}
→ { success, data: { content: Card[], page, size, totalElements, hasNext }, error }
  Card = { productId, title, artistName, coverImageUrl, releaseYear, pressType }

GET /api/v1/products/{productId}
→ { success, data: { productId, catalogNo, title, artist:{artistId,name}, label,
    country, releaseYear, pressType, format, genre, coverImageUrl, description }, error }

에러 시 { success:false, error:{ code, message } } — HTTP 4xx/5xx
```

주의: 검색 API는 q·page·size만 지원 → 아티스트/컨디션/가격/정렬 필터는 **경매 피드(목데이터)** 화면 담당.
CORS는 백엔드 수정 금지이므로 **Vite dev proxy**(`/api` → `http://localhost:8080`)로 해결.

## 파일 구조

```
docs/frontend/
├── PLAN.md (이 문서)
├── index.html / package.json / vite.config.ts / tsconfig*.json
└── src/
    ├── main.tsx              # QueryClientProvider + RouterProvider
    ├── index.css             # Tailwind v4 @theme 토큰 (A안+C포인트)
    ├── app/
    │   ├── router.tsx        # 라우트 정의
    │   └── Layout.tsx        # 헤더(로고·탭·미니검색·로그인상태) + <Outlet>
    ├── api/
    │   ├── client.ts         # fetch 래퍼: ApiResponse 언랩, 실패 시 ApiError throw
    │   └── products.ts       # searchProducts / getProductDetail + 타입
    ├── mocks/
    │   └── auctions.ts       # 결정적 시드 경매·판매자·입찰로그·시세 이력
    ├── hooks/
    │   └── useDebouncedValue.ts
    ├── components/           # 2회 이상 반복 확인된 것만
    │   ├── VinylCover.tsx    # 커버(이미지 or 그라디언트+vinyl) — 전 페이지 반복
    │   ├── QueryState.tsx    # 로딩/빈결과/에러 공통 표현 — 카탈로그·상세·피드 반복
    │   ├── Pagination.tsx    # 카탈로그·피드 반복
    │   ├── AuctionCard.tsx   # 경매 카드 — 홈(레일·그리드)·피드 반복
    │   └── Countdown.tsx     # 카운트다운 텍스트 + useNow — 카드·상세 반복
    └── pages/
        ├── HomePage.tsx          # 히어로 검색 + 마감임박 레일 + 실시간 경매(목)
        ├── CatalogPage.tsx       # 실API 검색: 디바운스·URL q/page·페이지네이션
        ├── ProductDetailPage.tsx # 실API 상세 + 진행 경매(목) + 시세(목)
        ├── FeedPage.tsx          # 경매 피드(목): 4필터 URL 반영 + 페이지네이션
        ├── AuctionDetailPage.tsx # 입찰 박스 + VU 카운트다운 + 호가 로그(목)
        └── LoginPage.tsx         # 목 로그인(localStorage)
```

라우트: `/` 홈, `/catalog` 검색, `/products/:productId` 상세, `/feed` 피드, `/auctions/:auctionId` 경매 상세, `/login`.

---

### Task 1: 스캐폴드 + 토큰 + 레이아웃

**Files:** Create: `package.json`, `vite.config.ts`, `index.html`, `tsconfig*`, `src/main.tsx`, `src/index.css`, `src/app/router.tsx`, `src/app/Layout.tsx`, 빈 페이지 6개

- [x] `npm create vite@latest . -- --template react-ts` (docs/frontend에서) 후 `npm i @tanstack/react-query react-router tailwindcss @tailwindcss/vite`
- [x] `vite.config.ts`: react + tailwindcss 플러그인, `server.proxy = { "/api": "http://localhost:8080" }`
- [x] `src/index.css`: `@import "tailwindcss";` + `@theme` 토큰:
  `--color-paper:#F7F6F1; --color-surface:#FFFFFF; --color-surface2:#EFEDE6; --color-ink:#23241F; --color-muted:#70726B; --color-faint:#9C9E94; --color-line:#E3E1D8; --color-line-strong:#C9C7BD; --color-brand:#34506F; --color-brand-ink:#263C54; --color-live:#B24A28; --color-live-bg:#F7E7DE; --color-up:#2E7D5B; --color-amber:#E5A13D; --color-glow:#F3C476; --font-serif-display:Georgia,"Noto Serif KR",serif;`
  Pretendard는 jsdelivr CDN link로 index.html에 추가, body 기본 폰트 지정
- [x] `Layout.tsx`: sticky 헤더 — 로고(Groove + 도는 disc), 탭(홈/경매 피드/카탈로그, NavLink aria-current), 미니 검색(제출 시 `/catalog?q=` 이동), 로그인 상태(localStorage `groove_user` 읽기, 없으면 `/login` 링크)
- [x] `router.tsx` + `main.tsx`: BrowserRouter 계열 createBrowserRouter, QueryClient(기본 staleTime 30s, retry 1)
- [x] 검증: `npm run dev` 기동, 6개 라우트 placeholder 렌더

### Task 2: API 레이어

**Files:** Create: `src/api/client.ts`, `src/api/products.ts`
**Produces:** `searchProducts(q, page, size): Promise<ProductSearchResponse>`, `getProductDetail(id): Promise<ProductDetail>`, `ApiError { code, message, status }`

- [x] `client.ts`:
  ```ts
  export class ApiError extends Error {
      constructor(public code: string, message: string, public status: number) { super(message); }
  }
  export async function apiGet<T>(path: string): Promise<T> {
      const res = await fetch(path);
      const body = await res.json().catch(() => null);
      if (!res.ok || !body?.success) {
          throw new ApiError(body?.error?.code ?? "NETWORK", body?.error?.message ?? `HTTP ${res.status}`, res.status);
      }
      return body.data as T;
  }
  ```
- [x] `products.ts`: 위 API 계약 그대로 타입 선언(`ProductSearchCard`, `ProductSearchResponse`, `ProductDetail`) + `searchProducts`/`getProductDetail` (URLSearchParams로 q 인코딩)
- [x] 검증: `npx tsc --noEmit`

### Task 3: 목데이터 모듈

**Files:** Create: `src/mocks/auctions.ts`
**Produces:** `MockAuction { id, productRef:{title,artist,year,pressType,genre,coverColors:[string,string]}, cond:"M"|"NM"|"VG+", sleeve, startPrice, currentPrice, bidCount, startedAt, endsAt(epoch ms), seller, verified, shipping }`, `MOCK_AUCTIONS: MockAuction[]`(20건+), 외부 공개는 전부 Promise 래퍼(150ms 지연, useQuery 소비 전제): `fetchMockAuctions(filter)`, `fetchMockAuction(id): Promise<MockAuction|null>`, `fetchBidLogs(id)`, `fetchOpenAuctionsForProduct(title, productId)`(종료 경매 제외), `fetchPriceHistory(title)`. 동기 헬퍼(findMockAuction·buildBidLogs 등)는 모듈 내부 전용
**필터 계약(피드 URL과 동일 키):** `artist(부분일치)`, `cond(M|NM|VG+)`, `minPrice`, `maxPrice`, `sort(ending|newest|price_desc|price_asc|bids)`

- [x] 기존 시안의 결정적 RNG(seed 기반 LCG) 방식으로 릴리스 10종 × 경매 2~4건 생성, endsAt은 모듈 로드 시각 기준 +7분~+3일 분포(마감임박 레일용 5건은 1시간 이내 보장)
- [x] 검증: `npx tsc --noEmit`

### Task 4: 공통 소품 (반복 확인된 3개 + 디바운스 훅)

**Files:** Create: `src/hooks/useDebouncedValue.ts`, `src/components/VinylCover.tsx`, `src/components/QueryState.tsx`, `src/components/Pagination.tsx`
**Produces:**
- `useDebouncedValue<T>(value: T, delayMs = 300): T` — setTimeout/cleanup
- `VinylCover({ title, artist, imageUrl?, colors?, spin? })` — imageUrl 있으면 img, 없으면 그라디언트+타이틀, `spin`이면 C포인트 vinyl 회전(motion-reduce:animate-none)
- `QueryState({ isLoading, error, isEmpty, emptyMessage, children })` — 로딩 스피너/에러(코드·메시지+재시도 안내)/빈결과 각각 렌더, 정상이면 children
- `Pagination({ page, hasNext, totalElements?, size, onPage })` — 이전/다음 + 페이지 표시, 0-base
- [x] 검증: `npx tsc --noEmit`

### Task 5: 카탈로그(실API 검색)

**Files:** Create: `src/pages/CatalogPage.tsx`
**Consumes:** `searchProducts`, `useDebouncedValue`, `VinylCover`, `QueryState`, `Pagination`

- [x] URL 계약: `/catalog?q=coltrane&page=0`. `useSearchParams`가 단일 소스 — input 로컬 state는 URL 초기값에서 시작, 300ms 디바운스된 값이 바뀌면 `setSearchParams({q, page:"0"})`(replace)
- [x] `useQuery({ queryKey: ["productSearch", q, page], queryFn: () => searchProducts(q, page, 20), enabled: q.trim().length > 0, placeholderData: keepPreviousData })`
- [x] 상태 4종: 로딩(스켈레톤 카드 8개), 에러(QueryState), 빈결과("'{q}' 검색 결과가 없습니다"), q 없음(안내 문구 — 인기 검색어 칩)
- [x] 결과 카드 → `/products/:productId` 링크, 하단 Pagination(totalElements 표기)
- [x] 검증: 백엔드 기동 상태에서 dev 서버로 실검색 스모크, 새로고침 시 q/page 복원 확인

### Task 6: 상품 상세(실API) + 진행 경매(목)

**Files:** Create: `src/pages/ProductDetailPage.tsx` (150줄 초과 시 `src/pages/product-detail/` 하위 분리: `SpecTable.tsx`, `PriceSparkline.tsx`, `OpenAuctionList.tsx`)
**Consumes:** `getProductDetail`, `MOCK_AUCTIONS`(제목 매칭 or productId 해시로 2~3건 배정), `priceHistoryOf`, `VinylCover`, `QueryState`

- [x] `useQuery(["product", productId])` → 좌측 커버 + 우측 제목(세리프)·아티스트·스펙 테이블(catalogNo/label/country/year/press/format/genre)
- [x] 시세 스파크라인(SVG polyline, up 컬러) + 최근 낙찰가 요약 카드 — 목데이터, "목데이터" 뱃지 표시
- [x] 진행 중 경매 리스트(목) → `/auctions/:id` 링크, 없으면 "진행 중 경매 없음"
- [x] 404(PERR 계열)·에러·로딩 각각 처리
- [x] 검증: 실제 DB의 productId로 스모크 + 존재하지 않는 id로 에러 화면 확인

### Task 7: 경매 피드(목 + 4필터 URL)

**Files:** Create: `src/pages/FeedPage.tsx` (150줄 초과 예상 → `src/pages/feed/FeedFilters.tsx` 분리)
**Consumes:** `fetchMockAuctions`, `useDebouncedValue`(아티스트 입력), `VinylCover`, `QueryState`, `Pagination`

- [x] URL 계약: `/feed?artist=coltrane&cond=NM&minPrice=50000&maxPrice=200000&sort=ending&page=0` — 전 필터 searchParams 단일 소스, 필터 변경 시 page 0으로 리셋
- [x] `useQuery({ queryKey: ["mockAuctions", artist, cond, minPrice, maxPrice, sort, page], queryFn: () => fetchMockAuctions(...) })` — 목데이터도 Query로 소비(제약 준수)
- [x] 필터 UI: 아티스트 텍스트(디바운스 300ms), 컨디션 칩(M/NM/VG+, aria-pressed), 가격 min/max number input, 정렬 select(마감임박/최신/현재가↑↓/입찰많은순)
- [x] 카드: 커버+컨디션 뱃지+현재가+입찰수+카운트다운(1초 tick, 마감 1시간 내 live 컬러 펄스) → `/auctions/:id`
- [x] 로딩/빈결과("조건에 맞는 경매 없음 — 필터 초기화" 버튼)/에러 + Pagination
- [x] 검증: 필터 조합 URL 직접 진입 시 상태 복원 스모크

### Task 8: 홈

**Files:** Create: `src/pages/HomePage.tsx`
**Consumes:** `fetchMockAuctions`, `VinylCover`, `QueryState`

- [x] 히어로: 세리프 헤드라인 + 큰 검색창(제출 시 `/catalog?q=` 이동 — 홈에서는 요청 안 함) + 인기 검색 칩
- [x] "곧 종료되는 경매" 가로 레일(목, sort=ending 상위 8) + "실시간 경매" 그리드(목 8건) — 각각 useQuery
- [x] 검증: 레일 스크롤·카드 링크 스모크

### Task 9: 경매 상세(목 입찰 + VU 카운트다운)

**Files:** Create: `src/pages/AuctionDetailPage.tsx` + `src/pages/auction-detail/VuCountdown.tsx`, `src/pages/auction-detail/BidLog.tsx`
**Consumes:** `getMockAuction`, `bidLogsOf`, `VinylCover`

- [x] `VuCountdown({ endsAt, startedAt })`: C안 VU미터 SVG — 바늘 각도 = 남은시간/전체 비율, 1초 tick, 아래 mono 카운트다운. 앰버·글로우 컬러는 여기서만
- [x] 입찰 박스: 현재가(mono 대형)·시작가·배송비, "₩{현재가+호가단위} 입찰하기" 버튼 → 로컬 state로 현재가/입찰수/로그 갱신(목 인터랙션, 새로고침 시 초기화 안내 문구)
- [x] 호가 로그 테이블(마스킹된 입찰자·금액·시각) + 같은 릴리스의 다른 경매 미니 리스트
- [x] 없는 id → "종료되었거나 없는 경매" 빈 상태
- [x] 검증: 입찰 버튼 연타 시 현재가 단조 증가·로그 prepend 확인

### Task 10: 로그인(목) + 마무리 검증

**Files:** Create: `src/pages/LoginPage.tsx`; Modify: `src/app/Layout.tsx`(로그인 상태 표시)

- [x] 이메일/비밀번호 폼(검증: 빈값 막기만) → `localStorage.setItem("groove_user", ...)` 후 홈 이동. 헤더에 닉네임 + 로그아웃. "목 로그인 — member-service 연동 전" 안내
- [x] 전체 검증: `npx tsc --noEmit` && `npm run build` 성공, 라우트 6종 스모크, 반응형(모바일 폭) 확인

## Self-Review 결과

- 스펙 커버리지: 디바운스(T5·T7), URL 필터(T5·T7), 페이지네이션(T5·T7), 상태 처리(T4 QueryState + 각 페이지), TanStack Query 전용(T5~T9 전부 useQuery 경유), 조기 추출 금지(공통 3개는 4개 페이지 반복 근거), 150줄 분리(T6·T7·T9에 분리 경로 명시) — 충족
- 타입 일관성: filter 키(artist/cond/minPrice/maxPrice/sort)가 T3 계약과 T7 URL 계약에서 동일
- 잔여 리스크: 백엔드 미기동 시 카탈로그·상세는 에러 상태 노출(의도된 동작으로 처리)
