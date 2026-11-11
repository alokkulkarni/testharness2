import { mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';

// How the end-to-end runs start the API (sdd §11, ADR-0013). Paths are relative to the repository root.
export const REPO_ROOT = resolve(import.meta.dirname, '..', '..', '..');
export const API_PORT = 8080;
export const SPA_PORT = 5173;
export const CLOCK_OFFSET_FILE = join(tmpdir(), 'todo-e2e-clock-offset.txt');

export interface ApiLaunch {
  /** arguments to ./api/mvnw */
  args: string[];
  env: Record<string, string>;
}

/**
 * A fresh temporary directory per run (sdd §11, §14: "temporary DB"), so that no data of an earlier run (for example
 * the 500 to-dos of the performance spec) is ever present. The first caller, the global setup, creates it and
 * publishes it in the environment, which the Playwright workers inherit; restart.spec reuses it to restart the API
 * on the same database file.
 */
function runDir(): string {
  let dir = process.env.TODO_E2E_RUN_DIR;
  if (!dir) {
    dir = mkdtempSync(join(tmpdir(), 'todo-e2e-'));
    process.env.TODO_E2E_RUN_DIR = dir;
  }
  return dir;
}

/** Main configuration: exactly the documented run (sdd §11); no test clock. */
export function mainLaunch(): ApiLaunch {
  return {
    args: ['-f', 'api/pom.xml', 'spring-boot:run'],
    env: {
      TODO_DB_PATH: join(runDir(), 'main', 'todo.db'),
      TODO_COOKIE_SECURE: 'false',
      SERVER_PORT: String(API_PORT),
    },
  };
}

/** Session-expiry configuration only: the test-source-set application with the offset-file clock (ADR-0013). */
export function expiryLaunch(): ApiLaunch {
  return {
    args: ['-f', 'api/pom.xml', 'spring-boot:test-run'],
    env: {
      TODO_DB_PATH: join(runDir(), 'expiry', 'todo.db'),
      TODO_COOKIE_SECURE: 'false',
      SERVER_PORT: String(API_PORT),
      SPRING_PROFILES_ACTIVE: 'e2e',
      TODO_E2E_CLOCK_OFFSET_FILE: CLOCK_OFFSET_FILE,
    },
  };
}
