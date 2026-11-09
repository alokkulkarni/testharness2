import type { Problem } from '../api/todos';

// sdd §6.1: every failure the SPA shows goes through userMessageFor, chosen by action, status and code.
// Nothing from a response (title, detail, code, raw body) or from the browser's error text is ever shown.

export type Failure =
  | { kind: 'network' }
  | { kind: 'http'; status: number; problem?: Problem }
  | { kind: 'unexpected' };

export type UserAction =
  | 'session-probe'
  | 'sign-up'
  | 'log-in'
  | 'log-out'
  | 'load-list'
  | 'load-todo'
  | 'create'
  | 'save'
  | 'toggle-done'
  | 'delete-from-list'
  | 'delete-from-detail'
  | 'clear-completed';

export type NextStep =
  | 'fix-fields'
  | 'try-again'
  | 'log-in-again'
  | 'check-and-retry-login'
  | 'choose-other-identifier-or-log-in'
  | 'back-to-list'
  | 'list-refreshed'
  | 'reload-or-home';

export interface UserMessage {
  text: string;
  nextStep: NextStep;
}

type FieldError = Problem['errors'][number];

/** The sentence naming the action that failed (ux-design §7.3), without its next step. */
const FAILED: Record<UserAction, string> = {
  'session-probe': 'The app could not reach the server.',
  'sign-up': 'Your account could not be created.',
  'log-in': 'You could not be logged in.',
  'log-out': 'You could not be logged out.',
  'load-list': 'Your to-dos could not be loaded.',
  'load-todo': 'This to-do could not be loaded.',
  create: 'Your to-do could not be added.',
  save: 'Your changes could not be saved.',
  'toggle-done': 'That to-do could not be updated.',
  'delete-from-list': 'The to-do could not be deleted.',
  'delete-from-detail': 'The to-do could not be deleted.',
  'clear-completed': 'Completed to-dos could not be deleted.',
};

/** The closing sentence of an `unexpected` message; screens replace it by a Reload button and a home link. */
export const RELOAD_OR_HOME_SUFFIX = ' Reload the page, or go to home.';

export const SESSION_ENDED_TEXT = 'Your session has ended. Please log in again.';
export const REFRESHED_TEXT = 'That to-do no longer exists, and the list has been refreshed.';
export const INVALID_LOGIN_TEXT = 'Invalid identifier or password. Check them and try again.';
export const TAKEN_TEXT = 'This email address or username is already taken. Choose a different one, or log in.';

/** The fields each form shows; a field error on any other field is never displayed (sdd §6.1). */
const SHOWN_FIELDS: Partial<Record<UserAction, readonly string[]>> = {
  'sign-up': ['identifier', 'password'],
  'log-in': ['identifier', 'password'],
  create: ['title', 'description', 'dueDate', 'priority'],
  save: ['title', 'description', 'dueDate', 'priority'],
};

/** The entries of a 400 VALIDATION_FAILED (or 409) body that name a field the form for `action` shows. */
export function shownFieldErrors(failure: Failure, action: UserAction): FieldError[] {
  const shown = SHOWN_FIELDS[action];
  if (!shown || failure.kind !== 'http' || !failure.problem) return [];
  const { status, problem } = failure;
  const relevant = (status === 400 && problem.code === 'VALIDATION_FAILED') || (status === 409 && problem.code === 'IDENTIFIER_TAKEN');
  if (!relevant) return [];
  return (problem.errors ?? []).filter((e) => shown.includes(e.field));
}

export function userMessageFor(failure: Failure, action: UserAction): UserMessage {
  if (failure.kind === 'unexpected') {
    return { text: `${FAILED[action]}${RELOAD_OR_HOME_SUFFIX}`, nextStep: 'reload-or-home' };
  }
  const tryAgain: UserMessage = { text: `${FAILED[action]} Please try again.`, nextStep: 'try-again' };
  if (failure.kind === 'network') return tryAgain;

  const { status } = failure;
  if (status === 401) {
    if (action === 'log-in') return { text: INVALID_LOGIN_TEXT, nextStep: 'check-and-retry-login' };
    if (action === 'session-probe') return tryAgain;
    return { text: SESSION_ENDED_TEXT, nextStep: 'log-in-again' };
  }
  if (status === 404) {
    if (action === 'toggle-done' || action === 'delete-from-list') return { text: REFRESHED_TEXT, nextStep: 'list-refreshed' };
    if (action === 'delete-from-detail') return { text: 'That to-do no longer exists.', nextStep: 'back-to-list' };
    if (action === 'load-todo') return { text: 'This to-do does not exist or has been deleted.', nextStep: 'back-to-list' };
    if (action === 'save') {
      return { text: 'This to-do no longer exists, so your changes could not be saved.', nextStep: 'back-to-list' };
    }
    return tryAgain;
  }
  if (status === 409 && action === 'sign-up' && failure.problem?.code === 'IDENTIFIER_TAKEN') {
    return { text: TAKEN_TEXT, nextStep: 'choose-other-identifier-or-log-in' };
  }
  if (status === 400 && failure.problem?.code === 'VALIDATION_FAILED' && shownFieldErrors(failure, action).length > 0) {
    return { text: 'Fix the fields marked with an error, then try again.', nextStep: 'fix-fields' };
  }
  return tryAgain;
}
