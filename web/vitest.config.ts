import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

// Component and unit tests (Vitest + React Testing Library + vitest-axe, ADR-0001).
// End-to-end specs live in e2e/ and run under Playwright, not here.
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./tests/setup.ts'],
    include: ['tests/**/*.test.{ts,tsx}'],
    css: false,
    restoreMocks: true,
  },
});
