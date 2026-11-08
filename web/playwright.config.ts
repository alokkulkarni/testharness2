import { defineConfig, devices } from '@playwright/test';
import { SPA_PORT } from './e2e/support/launch';

// Main end-to-end configuration (sdd §11, §14): the real API and SPA started with the documented commands, no
// test clock. The API is started by e2e/global-setup.ts; the Vite dev server (which proxies /api) by webServer.
// Set E2E_EXTERNAL_SERVERS=1 to use servers that are already running.
const external = process.env.E2E_EXTERNAL_SERVERS === '1';

export default defineConfig({
  testDir: './e2e',
  outputDir: 'test-results/e2e-artifacts', // keeps the JUnit files of the other runs in test-results/ safe
  testIgnore: ['**/session-expiry.spec.ts', '**/restart.spec.ts'],
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: 0,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['junit', { outputFile: process.env.PLAYWRIGHT_JUNIT_OUTPUT_NAME ?? 'test-results/e2e-junit.xml' }],
  ],
  globalSetup: './e2e/global-setup.ts',
  use: {
    baseURL: `http://localhost:${SPA_PORT}`,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
    // Restarts the API process, so it runs alone after every other spec has finished (REQ-ACC-001 AC-3).
    {
      name: 'restart',
      testIgnore: [],
      testMatch: '**/restart.spec.ts',
      fullyParallel: false,
      dependencies: ['chromium', 'firefox'],
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: external
    ? undefined
    : {
        command: `pnpm dev --port ${SPA_PORT} --strictPort`,
        port: SPA_PORT,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
      },
});
