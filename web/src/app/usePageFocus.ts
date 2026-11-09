import { createContext, useContext, useEffect, useRef } from 'react';
import { useLocation } from 'react-router';

// The app shell (AppShell) provides these to the screens it renders.
export interface ShellContextValue {
  /** Put a message in the shell's polite live region (role="status"), which outlives the element that caused it. */
  announce: (text: string) => void;
  /** True exactly once per shell: for the first screen shown since the document loaded. */
  takeFirstScreen: () => boolean;
}

export const ShellContext = createContext<ShellContextValue>({
  announce: () => {},
  takeFirstScreen: () => false,
});

export function useAnnounce(): (text: string) => void {
  return useContext(ShellContext).announce;
}

/**
 * Each screen sets document.title and moves focus to its <h1> when it appears, i.e. on a change of the router's
 * pathname, never on a query-string-only change (ux-design §3, §6; sdd §9). The screen renders the returned ref on
 * a single <h1 tabIndex={-1}> that it keeps for its whole life, so a later change of the heading text keeps focus.
 *
 * `keepFocusOnDirectLoad`: the list screen, when it is the page the document loaded on, leaves focus at the start of
 * the document so that the skip link is the first Tab stop (NFR-001 AC-2).
 */
export function usePageChrome(title: string, keepFocusOnDirectLoad = false) {
  const heading = useRef<HTMLHeadingElement>(null);
  const { pathname } = useLocation();
  const shell = useContext(ShellContext);

  useEffect(() => {
    document.title = `${title} - To-do list`;
  }, [title]);

  useEffect(() => {
    const direct = shell.takeFirstScreen();
    if (direct && keepFocusOnDirectLoad) return;
    heading.current?.focus();
    // a screen is shown again only when the pathname changes (the screens are separate routes)
  }, [pathname]);

  return heading;
}
