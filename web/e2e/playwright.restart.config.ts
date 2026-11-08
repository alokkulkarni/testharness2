import { defineConfig, devices } from '@playwright/test';
import { SPA_PORT } from './support/launch';

// Restart configuration (REQ-ACC-001 AC-3, REQ-DATA-001 AC-1): starts the API with the documented run (same global
// setup as the main configuration), then restarts it inside the test on the same database file. Its own run, so it
// executes whatever happens in the main run; `pnpm -C web e2e` runs it after the main configuration.
const external = process.env.E2E_EXTERNAL_SERVERS === '1';

export default defineConfig({
  testDir: '.',
  outputDir: '../test-results/e2e-restart-artifacts',
  testMatch: '**/restart.spec.ts',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [
    ['list'],
    ['html', { outputFolder: '../playwright-report-restart', open: 'never' }],
    ['junit', { outputFile: '../test-results/e2e-restart-junit.xml' }],
  ],
  globalSetup: './global-setup.ts',
  use: {
    baseURL: `http://localhost:${SPA_PORT}`,
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: external
    ? undefined
    : {
        command: `pnpm dev --port ${SPA_PORT} --strictPort`,
        cwd: '..',
        port: SPA_PORT,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
      },
});
