import { useEffect, useRef, type ReactNode } from 'react';
import { Link } from 'react-router';
import { RELOAD_OR_HOME_SUFFIX, type UserMessage } from '../errors/userMessage';

// Small native-HTML building blocks shared by the screens (ux-design §5). No component library (ADR-0001).

export function ReloadOrHome() {
  return (
    <span className="message-actions">
      <button type="button" className="btn secondary" onClick={() => window.location.reload()}>
        Reload the page
      </button>
      <Link className="btn secondary" to="/">
        Go to home
      </Link>
    </span>
  );
}

/**
 * A message from userMessageFor (or a fixed text), with its next step shown as a control where it needs one:
 * an unexpected failure gets a Reload button and a home link in place of its closing sentence.
 */
export function Message({
  message,
  text,
  variant = 'danger',
  role = 'alert',
  focusOnMount = false,
  children,
}: {
  message?: UserMessage;
  text?: string;
  variant?: 'danger' | 'info' | 'success';
  role?: 'alert' | 'status';
  /** Move focus to the message when it appears (ux-design §6: probe error, log-out failure). */
  focusOnMount?: boolean;
  children?: ReactNode;
}) {
  const box = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (focusOnMount) box.current?.focus();
  }, [focusOnMount]);
  let shown = text ?? message?.text ?? '';
  const reload = message?.nextStep === 'reload-or-home' && shown.endsWith(RELOAD_OR_HOME_SUFFIX);
  if (reload) shown = shown.slice(0, -RELOAD_OR_HOME_SUFFIX.length);
  return (
    <div role={role} className={`message ${variant}`} ref={box} {...(focusOnMount ? { tabIndex: -1 } : {})}>
      <span className="message-text">{shown}</span>
      {reload ? (
        <>
          {' '}
          <ReloadOrHome />
        </>
      ) : null}
      {children ? <> {children}</> : null}
    </div>
  );
}

export function LoadingText({ children }: { children: ReactNode }) {
  return (
    <p role="status" className="loading">
      {children}
    </p>
  );
}

export function BackToList() {
  return <Link to="/todos">Back to my to-dos</Link>;
}

export interface FieldControlProps {
  id: string;
  'aria-invalid'?: 'true';
  'aria-describedby'?: string;
}

/** Label, optional help, control and the "Error:" message tied to the control (NFR-001 AC-3, ux-design §4). */
export function FormField({
  id,
  label,
  requirement,
  help,
  error,
  children,
}: {
  id: string;
  label: string;
  requirement?: 'required' | 'optional';
  help?: string;
  error?: string | null;
  children: (props: FieldControlProps) => ReactNode;
}) {
  const helpId = `${id}-help`;
  const errorId = `${id}-error`;
  const describedBy = [error ? errorId : null, help ? helpId : null].filter(Boolean).join(' ');
  const props: FieldControlProps = { id };
  if (error) props['aria-invalid'] = 'true';
  if (describedBy) props['aria-describedby'] = describedBy;
  return (
    <div className="field">
      <label htmlFor={id}>{requirement ? `${label} (${requirement})` : label}</label>
      {help ? (
        <p id={helpId} className="help">
          {help}
        </p>
      ) : null}
      {error ? (
        <p id={errorId} className="field-error">
          <span className="field-error-prefix">Error:</span> {error}
        </p>
      ) : null}
      {children(props)}
    </div>
  );
}

/** The error summary of a refused submit: role="alert", receives focus, one link per error (ux-design §4 rule 3). */
export function ErrorSummary({
  errors,
  focusKey,
}: {
  errors: { fieldId: string; message: string }[];
  focusKey: number;
}) {
  const box = useRef<HTMLDivElement>(null);
  useEffect(() => {
    box.current?.focus();
  }, [focusKey]);
  return (
    <div role="alert" tabIndex={-1} ref={box} className="error-summary">
      <h2>There is a problem</h2>
      <ul>
        {errors.map((e) => (
          <li key={e.fieldId}>
            <a
              href={`#${e.fieldId}`}
              onClick={(event) => {
                event.preventDefault();
                document.getElementById(e.fieldId)?.focus();
              }}
            >
              {e.message}
            </a>
          </li>
        ))}
      </ul>
    </div>
  );
}
