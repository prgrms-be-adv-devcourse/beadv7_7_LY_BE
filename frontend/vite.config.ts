import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
    plugins: [react(), tailwindcss()],
    server: {
        proxy: {
            // 순서 중요: 구체적인 프리픽스가 먼저 매칭돼야 한다
            "/api/v1/auth": "http://localhost:8081", // member-service (로그인)
            "/api/v1/members/me": "http://localhost:8080", // core-service (찜·관심 경매)
            "/api/v1/members": "http://localhost:8081", // member-service (가입·주소)
            "/api": "http://localhost:8080", // core-service
        },
    },
});
