import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

// 경매 마감 직전처럼 로그인 요청이 순간적으로 몰리는 상황을 게이트웨이를 통해 재현한다.
// LoginConcurrencyTest.java(member-service, ExecutorService+CountDownLatch 기반)와 같은 시나리오를
// 실제 HTTP·게이트웨이·Tomcat 스레드풀까지 포함해서 검증하기 위한 k6 버전이다.
//
// 사전 준비:
//   1) member-service를 local 프로필로 실행 (기본 포트 8081)
//   2) gateway-service를 local 프로필로 실행 (기본 포트 8080) — MEMBER_SERVICE_URI가 8081을 보게 함
//   3) k6 설치: brew install k6
//
// 실행 (Tier별 동접 수를 VUS로 넘긴다 — Tier 1=15, Tier 2=75, Tier 3=300):
//   k6 run -e VUS=15  k6/login-concurrency-test.js
//   k6 run -e VUS=75  k6/login-concurrency-test.js
//   k6 run -e VUS=300 k6/login-concurrency-test.js가
//
// 커넥션 풀/타임아웃은 member-service의 운영 기본값(HikariCP maximum-pool-size=10, connection-timeout=30s)을
// 그대로 쓴다 — 별도로 줄이지 않는다.

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const VUS = parseInt(__ENV.VUS || '15', 10);
const RAW_PASSWORD = 'testPw1234!';

// 기본 http_req_duration은 setup()의 순차 회원가입과 본 시나리오의 동시 로그인이 하나로 섞여
// 집계된다 — 회원가입은 부하가 없어 상대적으로 빠르고 일정해서, 섞인 채로 보면 로그인만의
// 지연(특히 p50)이 실제보다 낮게 보인다. 이를 분리해서 로그인 구간만 따로 집계한다.
const registerDuration = new Trend('register_duration', true);
const loginDuration = new Trend('login_duration', true);
// login_duration은 성공(200)·실패(503, 세마포어 타임아웃) 요청이 섞여 있어서, "성공한 요청만"의
// 순수 지연을 보려면 따로 분리해서 집계해야 한다.
const loginSuccessDuration = new Trend('login_success_duration', true);

export const options = {
    scenarios: {
        loginBurst: {
            executor: 'per-vu-iterations',
            vus: VUS,
            iterations: 1,
            maxDuration: '90s',
        },
    },
    // 실패율에 대한 강한 기준선 없이, 우선은 관찰 목적 — 필요하면 이후에 조정한다.
    thresholds: {
        http_req_failed: ['rate<1'],
    },
};

// setup()은 VU들이 실제로 요청을 쏘기 전에 딱 한 번만 실행된다 — 여기서 VU 수만큼 회원가입을
// 미리 끝내서, 본 시나리오(로그인)의 타이밍에 회원가입 비용이 섞이지 않게 한다.
// VU마다 서로 다른 계정을 쓰게 하는 게 중요하다 — 같은 계정을 여러 VU가 동시에 로그인하면
// refresh_token 테이블의 같은 행을 두고 락 경합이 생겨, Hikari 풀/Argon2와는 다른 병목이 섞인다.
export function setup() {
    const base = Date.now();
    const accounts = [];

    for (let i = 0; i < VUS; i++) {
        const seed = base + i;
        const email = `k6-login-load-${seed}@email.com`;
        const nickname = String(seed % 1000000).padStart(6, '0');
        const phoneDigits = String(seed % 100000000).padStart(8, '0');
        const phoneNumber = `010-${phoneDigits.slice(0, 4)}-${phoneDigits.slice(4, 8)}`;

        const registerRes = http.post(
            `${BASE_URL}/api/v1/members`,
            JSON.stringify({
                email: email,
                password: RAW_PASSWORD,
                nickName: nickname,
                name: 'k6부하테스트',
                phoneNumber: phoneNumber,
                zipcode: '06671',
                baseAddress: '서울특별시 서초구 반포대로 45',
                detailAddress: '4층(서초동, 명정빌딩)',
            }),
            {
                headers: { 'Content-Type': 'application/json' },
                tags: { name: 'register' },
            }
        );

        if (registerRes.status !== 200) {
            throw new Error(
                `setup 중 회원가입 실패 (i=${i}, status=${registerRes.status}): ${registerRes.body}`
            );
        }

        registerDuration.add(registerRes.timings.duration);

        accounts.push({ email: email, password: RAW_PASSWORD, nickname: nickname });
    }

    return { accounts: accounts };
}

// VU마다 setup()이 만들어둔 계정을 하나씩 배정받아 동시에 로그인한다.
// k6는 __VU가 1부터 시작한다.
export default function (data) {
    const account = data.accounts[__VU - 1];

    const loginRes = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({ email: account.email, password: account.password }),
        {
            headers: { 'Content-Type': 'application/json' },
            tags: { name: 'login' },
        }
    );

    loginDuration.add(loginRes.timings.duration);
    if (loginRes.status === 200) {
        loginSuccessDuration.add(loginRes.timings.duration);
    }

    check(loginRes, {
        '로그인 200 응답': (res) => res.status === 200,
    });
}

// teardown()은 시나리오 종료 후 한 번 실행된다. 회원 삭제 API가 따로 없어서 여기서 자동 정리는
// 못 하고, 대신 DB에서 수동으로 지울 수 있도록 만든 계정의 닉네임을 로그로 남긴다.
// 정리 SQL 예시: DELETE FROM member WHERE nickname IN ('...', '...');
export function teardown(data) {
    const nicknames = data.accounts.map((a) => a.nickname);
    console.log(`정리 필요한 테스트 계정 닉네임 (${nicknames.length}개): ${nicknames.join(',')}`);
}
