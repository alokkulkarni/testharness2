import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// The dev server proxies /api to the API process, so the SPA and the API share one origin (ADR-0001, sdd §11).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
});
