import { Link } from 'react-router';
import { usePageChrome } from './usePageFocus';

/**
 * S7 (ux-design §7.2, sdd §8, REQ-NAV-001): shown to a signed-in user for an address the app does not define.
 * The illustration is decorative (aria-hidden), plays once for 1.2 s, and never hides or moves the text.
 */
export function NotFoundPage() {
  const heading = usePageChrome('Page not found');
  return (
    <>
      <div className="nf-illustration" aria-hidden="true">
        <div className="nf-card">
          <span className="nf-line" />
          <span className="nf-line" />
          <span className="nf-line nf-strike" />
        </div>
      </div>
      <h1 ref={heading} tabIndex={-1}>
        Page not found
      </h1>
      <p>We could not find that page. Check the address, or go to home.</p>
      <p>
        <Link className="btn primary" to="/">
          Go to home
        </Link>
      </p>
    </>
  );
}
