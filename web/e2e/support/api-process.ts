import { spawn } from 'node:child_process';
import { mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { createConnection } from 'node:net';
import { dirname, join } from 'node:path';
import { tmpdir } from 'node:os';
import { API_PORT, REPO_ROOT, type ApiLaunch } from './launch';

// Owns the API process of an end-to-end run so that REQ-ACC-001 AC-3 can restart it (stop, then start on the
// same database file). Playwright's own webServer cannot be restarted from a test, so it is not used for the API.
const PID_FILE = join(tmpdir(), 'todo-e2e-api.pid');

function portOpen(port: number): Promise<boolean> {
  return new Promise((resolveOpen) => {
    const socket = createConnection({ port, host: '127.0.0.1' });
    socket.once('connect', () => {
      socket.destroy();
      resolveOpen(true);
    });
    socket.once('error', () => resolveOpen(false));
  });
}

async function waitForPort(open: boolean, timeoutMs: number): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if ((await portOpen(API_PORT)) === open) return;
    await new Promise((r) => setTimeout(r, 250));
  }
  throw new Error(`API port ${API_PORT} did not become ${open ? 'reachable' : 'free'} within ${timeoutMs} ms`);
}

export async function startApi(launch: ApiLaunch): Promise<void> {
  if (await portOpen(API_PORT)) throw new Error(`Port ${API_PORT} is already in use; stop the other API first`);
  mkdirSync(dirname(launch.env.TODO_DB_PATH ?? join(tmpdir(), 'todo-e2e', 'todo.db')), { recursive: true });
  const child = spawn('./api/mvnw', launch.args, {
    cwd: REPO_ROOT,
    env: { ...process.env, ...launch.env },
    detached: true,
    stdio: 'ignore',
  });
  child.unref();
  if (child.pid === undefined) throw new Error('Could not start ./api/mvnw');
  writeFileSync(PID_FILE, String(child.pid));
  await waitForPort(true, 180_000);
}

export async function stopApi(): Promise<void> {
  let pid: number;
  try {
    pid = Number(readFileSync(PID_FILE, 'utf8'));
  } catch {
    return;
  }
  try {
    process.kill(-pid, 'SIGTERM'); // the whole process group: mvnw and the JVM it started
  } catch {
    // already gone
  }
  await waitForPort(false, 60_000);
  rmSync(PID_FILE, { force: true });
}

export async function restartApi(launch: ApiLaunch): Promise<void> {
  await stopApi();
  await startApi(launch); // same TODO_DB_PATH, so the data must still be there
}
