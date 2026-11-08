import { rmSync } from 'node:fs';
import { startApi, stopApi } from './support/api-process';
import { CLOCK_OFFSET_FILE, expiryLaunch } from './support/launch';

// Session-expiry configuration only (sdd §11, ADR-0013): the API runs from the test source set with the
// offset-file clock. No offset file means no offset.
export default async function globalSetup(): Promise<() => Promise<void>> {
  rmSync(CLOCK_OFFSET_FILE, { force: true });
  if (process.env.E2E_EXTERNAL_SERVERS === '1') return async () => {};
  await startApi(expiryLaunch());
  return stopApi;
}
