import type { ReactNode } from 'react';
import { Navigate, useLocation, useSearchParams } from 'react-router';
import { userMessageFor } from '../errors/userMessage';
import { Message } from '../app/ui';
import { useSession } from './SessionProvider';
import { safeReturnTo } from './returnTo';

/** G1 probe failure (sdd §4.1): the alert moves focus to itself and offers Retry; state stays `unknown`. */
function SessionProbeError() {
  const { retryProbe } = useSession();
  return (
    <Message message={userMessageFor({ kind: 'network' }, 'session-probe')} focusOnMount>
      <button type="button" className="btn secondary" onClick={retryProbe}>
        Retry
      </button>
    </Message>
  );
}

/** Wraps the to-do routes and the catch-all (sdd §4.1, §8): nothing while `unknown`, login redirect when signed out. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { state, probeFailed } = useSession();
  const location = useLocation();
  if (state.status === 'unknown') return probeFailed ? <SessionProbeError /> : null;
  if (state.status === 'signed-out') {
    if (state.reason === 'logged-out') return null; // the provider has navigated to /login
    const returnTo = encodeURIComponent(`${location.pathname}${location.search}`);
    return (
      <Navigate
        replace
        to={`/login?returnTo=${returnTo}`}
        {...(state.reason === 'ended' ? { state: { message: 'session-ended' } } : {})}
      />
    );
  }
  return <>{children}</>;
}

/** /signup and /login: wait for the probe; a signed-in visitor is sent on (sdd §4.1). */
export function PublicRoute({ children, goesToReturnTo = false }: { children: ReactNode; goesToReturnTo?: boolean }) {
  const { state, probeFailed } = useSession();
  const [params] = useSearchParams();
  if (state.status === 'unknown') return probeFailed ? <SessionProbeError /> : null;
  if (state.status === 'signed-in') {
    return <Navigate replace to={goesToReturnTo ? safeReturnTo(params.get('returnTo')) : '/todos'} />;
  }
  return <>{children}</>;
}
