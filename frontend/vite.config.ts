import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
    plugins: [react(), tailwindcss()],
    server: {
        proxy: {
            // 순서 중요: 구체적인 프리픽스가 먼저 매칭돼야 한다
            "/api/v1/auth": "http://localhost:8081", // member-service (로그인)
            // core-service가 가진 건 /members/me 아래 두 갈래뿐이다. "/api/v1/members/me"로 뭉뚱그리면
            // member-service의 내 정보 조회(GET /api/v1/members/me)까지 8080으로 가서 404가 난다
            "/api/v1/members/me/watched-auctions": "http://localhost:8080", // core-service (관심 경매)
            "/api/v1/members/me/liked-products": "http://localhost:8080", // core-service (찜)
            "/api/v1/members": "http://localhost:8081", // member-service (가입·주소·내 정보)
            "/api": "http://localhost:8080", // core-service
        },
    },
});
