import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { signUp, toFailure } from '../api/client';
import { ErrorSummary, FormField, Message } from '../app/ui';
import { usePageChrome } from '../app/usePageFocus';
import { shownFieldErrors, userMessageFor, type UserMessage } from '../errors/userMessage';
import { signupIdentifierMessage, signupPasswordMessage } from '../validation/rules';
import { trimWhitespace } from '../validation/whitespace';

type Errors = Partial<Record<'identifier' | 'password', string>>;

/** S1 Sign up (ux-design S1, REQ-AUTH-001). Sign-up starts no session: success goes to the log-in page. */
export function SignupPage() {
  const heading = usePageChrome('Sign up');
  const navigate = useNavigate();
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Errors>({});
  const [summaryKey, setSummaryKey] = useState(0);
  const [alert, setAlert] = useState<UserMessage | null>(null);
  const [busy, setBusy] = useState(false);

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
    const idMessage = signupIdentifierMessage(trimmed);
    const pwMessage = signupPasswordMessage(password);
    if (idMessage) local.identifier = idMessage;
    if (pwMessage) local.password = pwMessage;
    if (idMessage || pwMessage) return refuse(local);

    setBusy(true);
    setErrors({});
    setAlert(null);
    try {
      await signUp({ identifier: trimmed, password });
      navigate('/login', { state: { message: 'account-created' } });
    } catch (error) {
      const failure = toFailure(error);
      const shown = shownFieldErrors(failure, 'sign-up');
      if (shown.length > 0) {
        const next: Errors = {};
        for (const e of shown) if (e.field === 'identifier' || e.field === 'password') next[e.field] ??= e.message;
        refuse(next);
      } else if (!(failure.kind === 'http' && failure.status === 401)) {
        setAlert(userMessageFor(failure, 'sign-up'));
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
        Sign up
      </h1>
      {alert ? <Message message={alert} /> : null}
      {summary.length > 0 ? <ErrorSummary errors={summary} focusKey={summaryKey} /> : null}
      <form onSubmit={onSubmit} noValidate>
        <FormField
          id="identifier"
          label="Email address or username"
          requirement="required"
          help="Use an email address, or a username of letters, digits, . _ -. 3 to 254 characters."
          error={errors.identifier}
        >
          {(props) => (
            <input
              {...props}
              type="text"
              autoComplete="username"
              autoCapitalize="none"
              spellCheck={false}
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
            />
          )}
        </FormField>
        <FormField id="password" label="Password" requirement="required" help="8 to 128 characters." error={errors.password}>
          {(props) => (
            <input
              {...props}
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          )}
        </FormField>
        <button type="submit" className="btn primary" aria-disabled={busy}>
          {busy ? 'Creating account...' : 'Sign up'}
        </button>
      </form>
      <p>
        <Link to="/login">Already have an account? Log in</Link>
      </p>
    </div>
  );
}
