/// <reference types="vite/client" />

interface ImportMetaEnv {
    // 토스 결제위젯 클라이언트 키. 브라우저에 실려 나가는 공개 키라 프론트에만 둔다.
    // 시크릿 키는 백엔드가 결제 승인에 쓰는 값이라 여기 오면 안 된다
    readonly VITE_TOSS_CLIENT_KEY?: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}
