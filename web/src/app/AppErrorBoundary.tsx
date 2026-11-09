import { Component, useEffect, useRef, type ReactNode } from 'react';
import { Link, useLocation } from 'react-router';

// sdd §6.1, NFR-006 AC-3. The boundary catches errors thrown while rendering. Exactly one thing resets it besides a
// full reload: a change of the router's pathname while it shows its error page. It is never given a location-derived
// key, so a query-string change or a navigation with no error never remounts anything.

function ErrorPage() {
  const heading = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    document.title = 'Something went wrong - To-do list';
    heading.current?.focus();
  }, []);
  return (
    <main id="main" className="error-page">
      <h1 ref={heading} tabIndex={-1}>
        Something went wrong
      </h1>
      <p>This page could not be shown. Reload the page, or go to home.</p>
      <p className="message-actions">
        <button type="button" className="btn primary" onClick={() => window.location.reload()}>
          Reload the page
        </button>
        <Link className="btn secondary" to="/">
          Go to home
        </Link>
      </p>
    </main>
  );
}

interface InnerProps {
  pathname: string;
  children: ReactNode;
}

interface InnerState {
  hasError: boolean;
  /** The pathname at the moment the error was caught (null until recorded). */
  caughtAt: string | null;
}

/**
 * React renders a tree that threw a second time, from scratch, before it commits the error page; when that second
 * render does not throw (a transient fault), the boundary's state, and with it the error page, is discarded. The
 * rule above says only a pathname change clears a caught error, so the catch is also remembered here, outside the
 * render, until the boundary resets or unmounts.
 */
let remembered: { pathname: string | null } | null = null;

class InnerBoundary extends Component<InnerProps, InnerState> {
  constructor(props: InnerProps) {
    super(props);
    this.state =
      remembered?.pathname != null ? { hasError: true, caughtAt: remembered.pathname } : { hasError: false, caughtAt: null };
  }

  static getDerivedStateFromError(): Partial<InnerState> {
    // nothing is logged or sent: no client telemetry (sdd §10), and the error text is never shown
    remembered = { pathname: null };
    return { hasError: true };
  }

  static getDerivedStateFromProps(props: InnerProps, state: InnerState): Partial<InnerState> | null {
    // record the pathname of the render that follows the catch (the props are those of the failed render)
    if (!state.hasError || state.caughtAt !== null) return null;
    if (remembered) remembered.pathname = props.pathname;
    return { caughtAt: props.pathname };
  }

  componentDidUpdate() {
    if (this.state.hasError && this.state.caughtAt !== null && this.props.pathname !== this.state.caughtAt) {
      remembered = null;
      this.setState({ hasError: false, caughtAt: null });
    }
  }

  componentWillUnmount() {
    remembered = null;
  }

  render() {
    return this.state.hasError ? <ErrorPage /> : this.props.children;
  }
}

export function AppErrorBoundary({ children }: { children: ReactNode }) {
  const { pathname } = useLocation();
  return <InnerBoundary pathname={pathname}>{children}</InnerBoundary>;
}
