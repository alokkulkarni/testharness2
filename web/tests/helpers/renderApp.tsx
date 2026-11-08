import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { MemoryRouter, useLocation, useNavigationType } from 'react-router';
import type { FakeApi } from './fakeApi';
import { load } from './lazy';

export interface LocationRecord {
  pathname: string;
  search: string;
  state: unknown;
  navigationType: string;
}

/** Records the router's current location so tests can assert redirects without reading implementation details. */
function LocationSpy({ into }: { into: LocationRecord }) {
  const location = useLocation();
  const navigationType = useNavigationType();
  into.pathname = location.pathname;
  into.search = location.search;
  into.state = location.state;
  into.navigationType = navigationType;
  return null;
}

/**
 * Renders the whole SPA the way the SDD composes it (sdd §6.1, §8): a router, the SessionProvider, and
 * AppErrorBoundary around AppRoutes. The API is the FakeApi installed as global fetch.
 * Application modules are loaded at call time (see lazy.ts) so a missing module fails the calling test only.
 */
export async function renderApp(entry: string, api: FakeApi, wrap?: (routes: ReactNode) => ReactNode) {
  const { AppErrorBoundary } = await load<typeof import('../../src/app/AppErrorBoundary')>('/src/app/AppErrorBoundary');
  const { AppRoutes } = await load<typeof import('../../src/app/AppRoutes')>('/src/app/AppRoutes');
  const { SessionProvider } = await load<typeof import('../../src/auth/SessionProvider')>('/src/auth/SessionProvider');
  api.install();
  const location: LocationRecord = { pathname: '', search: '', state: null, navigationType: '' };
  const user = userEvent.setup();
  const routes = <AppRoutes />;
  const view = render(
    <MemoryRouter initialEntries={[entry]}>
      <LocationSpy into={location} />
      <SessionProvider>
        <AppErrorBoundary>{wrap ? wrap(routes) : routes}</AppErrorBoundary>
      </SessionProvider>
    </MemoryRouter>,
  );
  return { ...view, user, location };
}
