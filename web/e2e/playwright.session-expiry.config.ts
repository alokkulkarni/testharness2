import { defineConfig, devices } from '@playwright/test';
import { SPA_PORT } from './support/launch';

// Session-expiry configuration (sdd §11, ADR-0013): starts the API with `spring-boot:test-run` (test source set,
// profile e2e, offset-file clock). Runs after the main configuration: `pnpm -C web e2e` then this one.
const external = process.env.E2E_EXTERNAL_SERVERS === '1';

export default defineConfig({
  testDir: '.',
  outputDir: '../test-results/e2e-session-expiry-artifacts',
  testMatch: '**/session-expiry.spec.ts',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [
    ['list'],
    ['html', { outputFolder: '../playwright-report-session-expiry', open: 'never' }],
    ['junit', { outputFile: '../test-results/e2e-session-expiry-junit.xml' }],
  ],
  globalSetup: './global-setup.session-expiry.ts',
  use: {
    baseURL: `http://localhost:${SPA_PORT}`,
    trace: 'retain-on-failure',
  },
  // chromium, firefox and webkit run one after the other (workers 1, fullyParallel false) against the one API: the
  // clock-offset file is shared, so two browsers must never run a test at the same time.
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
    { name: 'webkit', use: { ...devices['Desktop Safari'] } },
  ],
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
