import { startApi, stopApi } from './support/api-process';
import { mainLaunch } from './support/launch';

// Starts the API with the documented run (sdd §11). Set E2E_EXTERNAL_SERVERS=1 when the API and the Vite dev
// server are already running (for example while the SPA or API does not exist yet).
export default async function globalSetup(): Promise<() => Promise<void>> {
  if (process.env.E2E_EXTERNAL_SERVERS === '1') return async () => {};
  await startApi(mainLaunch);
  return stopApi;
}
