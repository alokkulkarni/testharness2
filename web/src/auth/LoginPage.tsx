import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useLocation } from 'react-router';
import { isUnauthorized, logIn, toFailure } from '../api/client';
import { ErrorSummary, FormField, Message } from '../app/ui';
import { usePageChrome } from '../app/usePageFocus';
import { INVALID_LOGIN_TEXT, SESSION_ENDED_TEXT, shownFieldErrors, userMessageFor, type UserMessage } from '../errors/userMessage';
import { trimWhitespace } from '../validation/whitespace';
import { useSession } from './SessionProvider';

type Errors = Partial<Record<'identifier' | 'password', string>>;

function infoFrom(state: unknown): string | null {
  const message = (state as { message?: unknown } | null)?.message;
  if (message === 'account-created') return 'Account created. Log in.';
  if (message === 'session-ended') return SESSION_ENDED_TEXT;
  return null;
}

/** S2 Log in (ux-design S2, REQ-AUTH-002). A successful log in is followed by the PublicRoute redirect. */
export function LoginPage() {
  const heading = usePageChrome('Log in');
  const { signIn, acknowledge } = useSession();
  const location = useLocation();
  const info = infoFrom(location.state);

  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Errors>({});
  const [summaryKey, setSummaryKey] = useState(0);
  const [alert, setAlert] = useState<{ text?: string; message?: UserMessage } | null>(null);
  const [busy, setBusy] = useState(false);
  const identifierInput = useRef<HTMLInputElement>(null);

  useEffect(() => {
    acknowledge(); // the "session ended" / "logged out" hand-over has reached this page
  }, [acknowledge]);

  function refuse(next: Errors) {
    setErrors(next);
    setAlert(null);
    setSummaryKey((n) => n + 1);
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    const trimmed = trimWhitespace(identifier);
    const local: Errors = {};
    if (trimmed === '') local.identifier = 'Enter your email address or username.';
    if (password === '') local.password = 'Enter your password.';
    if (local.identifier || local.password) return refuse(local);

    setBusy(true);
    setErrors({});
    setAlert(null);
    try {
      signIn(await logIn({ identifier: trimmed, password }));
    } catch (error) {
      const failure = toFailure(error);
      if (isUnauthorized(error)) {
        // a 401 from log in means invalid credentials (never the session-ended flow); same text for every cause
        setAlert({ text: INVALID_LOGIN_TEXT });
        setPassword('');
        identifierInput.current?.focus();
      } else {
        const shown = shownFieldErrors(failure, 'log-in');
        if (shown.length > 0) {
          const next: Errors = {};
          for (const e of shown) if (e.field === 'identifier' || e.field === 'password') next[e.field] ??= e.message;
          refuse(next);
        } else {
          setAlert({ message: userMessageFor(failure, 'log-in') });
        }
      }
    } finally {
      setBusy(false);
    }
  }

  const summary = (['identifier', 'password'] as const)
    .filter((f) => errors[f])
    .map((f) => ({ fieldId: f, message: errors[f]! }));

  return (
    <div className="auth-page">
      <h1 ref={heading} tabIndex={-1}>
        Log in
      </h1>
      {info ? <Message variant="info" role="status" text={info} /> : null}
      {alert ? <Message {...alert} /> : null}
      {summary.length > 0 ? <ErrorSummary errors={summary} focusKey={summaryKey} /> : null}
      <form onSubmit={onSubmit} noValidate>
        <FormField id="identifier" label="Email address or username" requirement="required" error={errors.identifier}>
          {(props) => (
            <input
              {...props}
              ref={identifierInput}
              type="text"
              autoComplete="username"
              autoCapitalize="none"
              spellCheck={false}
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
            />
          )}
        </FormField>
        <FormField id="password" label="Password" requirement="required" error={errors.password}>
          {(props) => (
            <input
              {...props}
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          )}
        </FormField>
        <button type="submit" className="btn primary" aria-disabled={busy}>
          {busy ? 'Logging in...' : 'Log in'}
        </button>
      </form>
      <p>
        <Link to="/signup">No account yet? Sign up</Link>
      </p>
    </div>
  );
}
