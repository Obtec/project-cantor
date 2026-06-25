import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// 개발 서버에서 /api 요청을 백엔드로 프록시한다.
export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_API_PROXY || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
