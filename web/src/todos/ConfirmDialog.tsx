import { useEffect, useId, useRef, type KeyboardEvent, type ReactNode } from 'react';

/**
 * D1 / D2 (ux-design): a native modal <dialog> opened with showModal() (ADR-0001). It is mounted only while it is
 * open. Initial focus is on Cancel; Escape and Cancel close it without any action and return focus to the control
 * that opened it. A confirmed action leaves focus to the caller (the opener may be gone).
 * REQ-TODO-006 and REQ-TODO-007 (confirmation before a delete); NFR-001 AC-2 (focus stays inside the dialog:
 * `keepFocusInside` wraps Tab and Shift+Tab).
 */
export function ConfirmDialog({
  heading,
  children,
  confirmLabel,
  busyLabel,
  busy,
  error,
  onConfirm,
  onCancel,
}: {
  heading: string;
  children: ReactNode;
  confirmLabel: string;
  busyLabel: string;
  busy: boolean;
  /** An inline alert shown above the buttons (role="alert"); the dialog stays open. */
  error: ReactNode;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const cancelButton = useRef<HTMLButtonElement>(null);
  const opener = useRef<Element | null>(document.activeElement);
  const returnFocus = useRef(false);
  const headingId = useId();
  const bodyId = useId();

  useEffect(() => {
    const element = dialog.current;
    if (!element) return;
    if (!element.open) element.showModal();
    cancelButton.current?.focus();
    return () => {
      if (element.open) element.close();
      if (returnFocus.current && opener.current instanceof HTMLElement) opener.current.focus();
    };
  }, []);

  function cancel() {
    if (busy) return;
    returnFocus.current = true;
    onCancel();
  }

  /** NFR-001 AC-2: Tab from the last control goes to the first, Shift+Tab from the first to the last (some engines would leave the page). */
  function keepFocusInside(event: KeyboardEvent<HTMLDialogElement>) {
    if (event.key !== 'Tab') return;
    const controls = Array.from(event.currentTarget.querySelectorAll<HTMLElement>('button, a[href], input, select, textarea, [tabindex]:not([tabindex="-1"])'));
    const first = controls[0];
    const last = controls[controls.length - 1];
    if (!first || !last) return;
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }

  return (
    <dialog
      ref={dialog}
      className="dialog"
      aria-labelledby={headingId}
      aria-describedby={bodyId}
      onKeyDown={keepFocusInside}
      onCancel={(event) => {
        event.preventDefault();
        cancel();
      }}
    >
      <h2 id={headingId}>{heading}</h2>
      <div id={bodyId}>{children}</div>
      {error}
      <div className="dialog-actions">
        <button type="button" className="btn secondary" ref={cancelButton} onClick={cancel}>
          Cancel
        </button>
        <button
          type="button"
          className="btn danger"
          aria-disabled={busy}
          onClick={() => {
            if (!busy) onConfirm();
          }}
        >
          {busy ? busyLabel : confirmLabel}
        </button>
      </div>
    </dialog>
  );
}
