// pointwallet-service PUT /internal/v1/wallet/hold 부하테스트
//
// 목적: HoldApplicationService.hold()의 두 가지 락 경합 지점(지갑 NOWAIT 락 + 재시도 백오프 /
// Hold row NOWAIT 락 + 즉시실패)이 실제 HTTP 요청량 하에서 어떻게 동작하는지 검증한다.
// (기존 JUnit 벤치마크 HoldNormalScenarioPerformanceTest / HoldStressPerformanceTest는
//  스프링 컨텍스트 내부 호출이라 HTTP 계층·커넥션풀 전체 스택을 통과하지 않음 — 이 스크립트가 그 갭을 메운다)
//
// 실행 전 준비:
//   1) docker compose -f docker/local/docker-compose.yml up -d mysql prometheus grafana kafka
//   2) ./gradlew :pointwallet-service:bootRun --args='--spring.profiles.active=local'
//   3) seed-wallets.sql 로 90001~90050, 91001~91100 지갑에 잔액 시딩
//
// 실행:
//   k6 run hold-load-test.js
//   (Prometheus로 실시간 전송하려면: k6 run -o experimental-prometheus-rw hold-load-test.js
//    필요 시 K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write 환경변수 지정)

import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = 'http://localhost:8086';

// 시나리오별 커스텀 지표 — Grafana에서 시나리오 단위로 구분해서 보기 위함
const walletLockRetryExhausted = new Counter('wallet_lock_retry_exhausted');
const holdRowLockRejected = new Counter('hold_row_lock_rejected');
const insufficientBalance = new Counter('insufficient_balance_errors');
const holdDuration = new Trend('hold_request_duration', true);

export const options = {
  scenarios: {
    // 1) 베이스라인 — 서로 다른 유저, 서로 다른 경매. 락 경합이 거의 없는 정상 상태의 처리량/지연시간.
    baseline: {
      executor: 'constant-arrival-rate',
      exec: 'baseline',
      rate: 30,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 20,
      maxVUs: 50,
      startTime: '0s',
    },
    // 2) 지갑 락 경합 — 기존 JUnit 벤치마크(HoldNormalScenarioPerformanceTest)와 동일한 패턴을
    //    HTTP 레벨로 재현: 같은 유저(90001~90010, 유저당 5개 VU)가 여러 경매에 "동시에" 입찰.
    //    지갑 row에서만 경합이 나고, Hold row 경합은 안 남(경매ID가 다 다르므로).
    wallet_lock_contention: {
      executor: 'per-vu-iterations',
      exec: 'walletLockContention',
      vus: 50,          // 90001~90010(10명)을 5개 VU씩 묶어서 동시 경합 유발
      iterations: 5,     // 유저당(=5VU 묶음당) 총 25개 경매에 연속 입찰
      startTime: '35s',
      maxDuration: '30s',
    },
    // 3) Hold row 락 경합 — 기존 테스트에 없던 케이스: 100명이 "같은 경매(auctionId=999001)"에
    //    거의 동시에 입찰. Hold row NOWAIT 경합(HoldRowLockContentionException, 재시도 없음)이
    //    얼마나 자주 나는지, 그리고 최고 입찰자 교체가 지갑 두 개(이전/이후) 락까지 걸면서
    //    지연시간에 어떤 영향을 주는지를 본다.
    hold_row_contention: {
      executor: 'shared-iterations',
      exec: 'holdRowContention',
      vus: 100,          // 91001~91100 유저
      iterations: 100,   // 유저당 1회, 전부 같은 auctionId
      startTime: '70s',
      maxDuration: '30s',
    },
  },
  thresholds: {
    'hold_request_duration': ['p(95)<1000'], // 필요에 맞게 조정
  },
};

// scenarioTag: 'baseline' | 'wallet_lock' | 'hold_row_lock' — HTTP 응답 코드만으로는
// 지갑 락 재시도 소진(HERR-3005)과 Hold row 락 즉시실패가 같은 코드(LOCK_ACQUISITION_FAILED,
// HERR-3005)로 나와서 구분이 안 되기 때문에, 어떤 시나리오에서 발생했는지로 나눠서 집계한다.
// (이건 스크립트의 한계라기보다 API 쪽 에러코드 설계가 두 원인을 뭉뚱그리고 있다는 뜻이라,
//  포폴에서 "관찰 가능성 관점에서 개선 여지"로 짚을 수 있는 지점이기도 함)
//
// HERR-3002(INSUFFICIENT_BALANCE)도 같은 문제가 있다 - HoldApplicationService.hold()가
// WalletNotFoundException과 진짜 InsufficientBalanceException을 똑같은 코드/메시지로 던져서
// 클라이언트 쪽에서 원인을 구분할 수 없다. 그래서 이 코드가 찍히면 요청 내용을 최대
// MAX_LOGGED_FAILURES건까지 콘솔에 그대로 남겨서, 실행 후 해당 userId의 실제 지갑 존재 여부를
// 수동으로 대조해볼 수 있게 한다.
let loggedFailures = 0;
const MAX_LOGGED_FAILURES = 20;

function postHold(auctionId, memberId, amount, scenarioTag) {
  const payload = JSON.stringify({ auctionId, memberId, amount });
  const params = { headers: { 'Content-Type': 'application/json' } };
  const res = http.put(`${BASE_URL}/internal/v1/wallet/hold`, payload, params);
  holdDuration.add(res.timings.duration, { scenario: scenarioTag });

  if (res.status !== 200) {
    let code = '';
    let message = '';
    try {
      const parsed = JSON.parse(res.body);
      code = parsed.error.code || '';
      message = parsed.error.message || '';
    } catch (e) {
      // ignore
    }
    if (code === 'HERR-3005' && scenarioTag === 'wallet_lock') {
      walletLockRetryExhausted.add(1);
    }
    if (code === 'HERR-3005' && scenarioTag === 'hold_row_lock') {
      holdRowLockRejected.add(1);
    }
    if (code === 'HERR-3002') {
      insufficientBalance.add(1);
      if (loggedFailures < MAX_LOGGED_FAILURES) {
        loggedFailures++;
        console.log(`[HERR-3002] scenario=${scenarioTag} auctionId=${auctionId} memberId=${memberId} amount=${amount} message="${message}"`);
      }
    }
  }

  check(res, { 'status is 200': (r) => r.status === 200 });
  return res;
}

// ===== 시나리오 1: 베이스라인 =====
export function baseline() {
  const userId = 90001 + Math.floor(Math.random() * 50);
  const auctionId = 800000 + Math.floor(Math.random() * 100000); // 매번 새 경매
  postHold(auctionId, userId, 1000, 'baseline');
}

// ===== 시나리오 2: 지갑 락 경합 (같은 유저, 다른 경매) =====
// VU를 유저 단위로 5개씩 묶는다 - 90001~90010(10명), 유저당 5개 VU가 동시에 서로 다른
// auctionId로 그 유저의 지갑을 동시에 두드리게 만든다. (VU마다 다른 유저를 쓰면 지갑끼리
// 겹치지 않아 락 경합 자체가 거의 안 생긴다 - 원래 JUnit 벤치마크의 "한 유저가 여러 경매에
// 동시 입찰" 패턴을 재현하려면 이렇게 유저를 VU보다 적게 둬서 묶어야 한다.)
export function walletLockContention() {
  const userId = 90001 + Math.floor((__VU - 1) / 5); // 90001~90010
  const auctionId = userId * 1000 + __VU * 10 + __ITER; // VU+ITER로 경매ID는 항상 다르게
  postHold(auctionId, userId, 1000, 'wallet_lock');
}

// ===== 시나리오 3: Hold row 락 경합 (다른 유저, 같은 경매) =====
const HOT_AUCTION_ID = 999001;

export function holdRowContention() {
  const userId = 91000 + __VU;
  // 다들 같은 경매에, 유저마다 입찰가를 조금씩 올려서 실제 입찰 경쟁처럼
  const amount = 1000 + __VU * 10;
  postHold(HOT_AUCTION_ID, userId, amount, 'hold_row_lock');
}
