import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router';
import { getSession, logOut, setUnauthorizedHandler, toFailure } from '../api/client';
import type { Session, User } from '../api/identity';
import type { Failure } from '../errors/userMessage';

// sdd §4.1. The API answers 401 both to a visitor who never signed in and to one whose session has ended, so the SPA
// tells them apart from its own state.
export type SessionState =
  | { status: 'unknown' }
  // `reason` is set only for the moment after the session ended ('ended') or the user logged out ('logged-out'),
  // so RequireAuth neither adds a message nor redirects twice; the log-in page clears it (acknowledge).
  | { status: 'signed-out'; reason?: 'ended' | 'logged-out' }
  | { status: 'signed-in'; user: User; expiresAt: string };

interface SessionContextValue {
  state: SessionState;
  /** The on-load probe failed with something other than a 401 (network error, 500): show the alert with Retry. */
  probeFailed: boolean;
  retryProbe: () => void;
  signIn: (session: Session) => void;
  /** Calls logOut; on success the state is signed-out and the user is at /login with no message. */
  logOutUser: () => Promise<Failure | null>;
  acknowledge: () => void;
}

const SessionContext = createContext<SessionContextValue | null>(null);

export function useSession(): SessionContextValue {
  const value = useContext(SessionContext);
  if (!value) throw new Error('useSession must be used inside SessionProvider');
  return value;
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const [state, setState] = useState<SessionState>({ status: 'unknown' });
  const [probeFailed, setProbeFailed] = useState(false);
  const [probeRun, setProbeRun] = useState(0);
  const stateRef = useRef(state);
  stateRef.current = state;

  useEffect(() => {
    let cancelled = false;
    getSession().then(
      (session) => {
        if (!cancelled) setState({ status: 'signed-in', user: session.user, expiresAt: session.expiresAt });
      },
      (error: unknown) => {
        if (cancelled) return;
        const failure = toFailure(error);
        if (failure.kind === 'http' && failure.status === 401) setState({ status: 'signed-out' });
        else setProbeFailed(true);
      },
    );
    return () => {
      cancelled = true;
    };
  }, [probeRun]);

  // Every 401 except the probe's and logIn's lands here. Only a signed-in user has a session to lose.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      if (stateRef.current.status === 'signed-in') setState({ status: 'signed-out', reason: 'ended' });
    });
    return () => setUnauthorizedHandler(null);
  }, []);

  const retryProbe = useCallback(() => {
    setProbeFailed(false);
    setProbeRun((n) => n + 1);
  }, []);

  const signIn = useCallback((session: Session) => {
    setState({ status: 'signed-in', user: session.user, expiresAt: session.expiresAt });
  }, []);

  const acknowledge = useCallback(() => {
    setState((s) => (s.status === 'signed-out' && s.reason ? { status: 'signed-out' } : s));
  }, []);

  const logOutUser = useCallback(async (): Promise<Failure | null> => {
    try {
      await logOut();
    } catch (error) {
      return toFailure(error);
    }
    setState({ status: 'signed-out', reason: 'logged-out' });
    navigate('/login');
    return null;
  }, [navigate]);

  const value = useMemo(
    () => ({ state, probeFailed, retryProbe, signIn, logOutUser, acknowledge }),
    [state, probeFailed, retryProbe, signIn, logOutUser, acknowledge],
  );
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}
