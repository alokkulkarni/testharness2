import { codePointLength, isBlank, trimWhitespace } from './whitespace';

// SPA mirror of the server's validation (sdd §5, ux-design §4 message catalogue). The server stays authoritative.

const LOCAL_CHARS = /^[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+$/;
const LABEL = /^[A-Za-z0-9-]+$/;
const USERNAME = /^[A-Za-z0-9._-]+$/;

function isValidEmail(value: string): boolean {
  const parts = value.split('@');
  if (parts.length !== 2) return false;
  const [local, domain] = parts as [string, string];
  if (local.length < 1 || local.length > 64) return false;
  if (!LOCAL_CHARS.test(local) || local.startsWith('.') || local.endsWith('.') || local.includes('..')) return false;
  if (domain.length < 1 || domain.length > 253) return false;
  const labels = domain.split('.');
  if (labels.length < 2) return false;
  for (const label of labels) {
    if (label.length < 1 || label.length > 63 || !LABEL.test(label) || label.startsWith('-') || label.endsWith('-')) return false;
  }
  const last = labels[labels.length - 1]!;
  return /^[A-Za-z]{2,}$/.test(last);
}

/** Sign-up identifier: returns the message, or null when valid. `value` is the already trimmed identifier. */
export function signupIdentifierMessage(trimmed: string): string | null {
  if (trimmed === '') return 'Enter an email address or a username.';
  const length = codePointLength(trimmed);
  if (length < 3 || length > 254) return 'The email address or username must be 3 to 254 characters.';
  if (trimmed.includes('@')) {
    return isValidEmail(trimmed) ? null : 'Enter a valid email address, for example name@example.com.';
  }
  return USERNAME.test(trimmed) ? null : 'A username can contain only letters A to Z, digits 0 to 9, and the characters . _ -';
}

export function signupPasswordMessage(password: string): string | null {
  if (password === '') return 'Enter a password.';
  const length = codePointLength(password);
  if (length < 8) return 'The password must be at least 8 characters.';
  if (length > 128) return 'The password must be 128 characters or fewer.';
  return null;
}

export function titleMessage(raw: string): string | null {
  const trimmed = trimWhitespace(raw);
  if (trimmed === '') return 'Enter a title.';
  if (codePointLength(trimmed) > 200) return 'The title must be 200 characters or fewer.';
  return null;
}

export function descriptionMessage(raw: string): string | null {
  return codePointLength(raw) > 2000 ? 'The description must be 2000 characters or fewer.' : null;
}

/** A real calendar date `YYYY-MM-DD`, year 0001 to 9999, checked from its parts (never through Date). */
export function isRealDate(value: string): boolean {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!m) return false;
  const year = Number(m[1]);
  const month = Number(m[2]);
  const day = Number(m[3]);
  if (year < 1 || month < 1 || month > 12 || day < 1) return false;
  const leap = (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
  const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][month - 1]!;
  return day <= days;
}

export function dueDateMessage(value: string): string | null {
  if (value === '') return 'Enter a due date.';
  return isRealDate(value) ? null : 'Enter a real date, for example 2026-11-06.';
}

export { isBlank };
