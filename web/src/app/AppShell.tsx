import { useCallback, useMemo, useRef, useState } from 'react';
import { Outlet } from 'react-router';
import { useSession } from '../auth/SessionProvider';
import { userMessageFor, type UserMessage } from '../errors/userMessage';
import { ShellContext } from './usePageFocus';
import { Message } from './ui';

/** G1 (ux-design §3): skip link, header with Log out, <main>, and the one always-present polite live region. */
export function AppShell() {
  const { state, logOutUser } = useSession();
  const [announcement, setAnnouncement] = useState('');
  const [logoutError, setLogoutError] = useState<UserMessage | null>(null);
  const [loggingOut, setLoggingOut] = useState(false);
  const firstScreen = useRef(true);

  const clearTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const announce = useCallback((text: string) => {
    setAnnouncement(text);
    clearTimeout(clearTimer.current);
    // the confirmation is visible as well as spoken; it is not kept for ever
    clearTimer.current = setTimeout(() => setAnnouncement(''), 10_000);
  }, []);
  const takeFirstScreen = useCallback(() => {
    const first = firstScreen.current;
    firstScreen.current = false;
    return first;
  }, []);
  const context = useMemo(() => ({ announce, takeFirstScreen }), [announce, takeFirstScreen]);

  async function onLogOut() {
    if (loggingOut) return;
    setLoggingOut(true);
    setLogoutError(null);
    const failure = await logOutUser();
    setLoggingOut(false);
    if (failure) setLogoutError(userMessageFor(failure, 'log-out'));
  }

  return (
    <ShellContext.Provider value={context}>
      <a
        className="skip-link"
        href="#main"
        onClick={(event) => {
          event.preventDefault();
          document.getElementById('main')?.focus();
        }}
      >
        Skip to main content
      </a>
      <header className="app-header">
        <span className="app-name">To-do list</span>
        {state.status === 'signed-in' ? (
          <button type="button" className="btn secondary" aria-disabled={loggingOut} onClick={onLogOut}>
            Log out
          </button>
        ) : null}
      </header>
      <main id="main" tabIndex={-1}>
        {logoutError ? <Message message={logoutError} focusOnMount /> : null}
        <Outlet />
      </main>
      <div role="status" aria-live="polite" className="live-region">
        {announcement}
      </div>
    </ShellContext.Provider>
  );
}
