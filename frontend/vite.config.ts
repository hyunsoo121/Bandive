import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vite.dev/config/
// API 는 src/api/client.ts 가 VITE_API_ORIGIN(기본 http://localhost:8081)으로 직접 호출한다.
// 백엔드 CORS 가 http://localhost:5173 + credentials 를 허용하므로 dev 프록시는 쓰지 않는다.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
  },
});
