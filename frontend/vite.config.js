import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    // 개발 중 /api 요청은 Spring Boot(8080)로 전달 → 같은 출처라 CORS 설정 없이 동작
    proxy: {
      '/api': {
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  // vite preview 도 server.proxy 를 그대로 사용
});
