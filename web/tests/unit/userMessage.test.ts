import { describe, expect, expectTypeOf, it } from 'vitest';
import type { Problem as IdentityProblem } from '../../src/api/identity';
import type { Problem } from '../../src/api/todos';
import type { Failure, UserAction } from '../../src/errors/userMessage';
import { load } from '../helpers/lazy';

const um = () => load<typeof import('../../src/errors/userMessage')>('/src/errors/userMessage');
const msg = async (failure: Failure, action: UserAction) => (await um()).userMessageFor(failure, action);

const ALL_ACTIONS: UserAction[] = [
  'session-probe', 'sign-up', 'log-in', 'log-out', 'load-list', 'load-todo', 'create', 'save',
  'toggle-done', 'delete-from-list', 'delete-from-detail', 'clear-completed',
];
const FORM_ACTIONS: UserAction[] = ['sign-up', 'log-in', 'create', 'save'];
const CODES: Problem['code'][] = [
  'VALIDATION_FAILED', 'MALFORMED_REQUEST', 'IDENTIFIER_TAKEN', 'INVALID_CREDENTIALS', 'UNAUTHENTICATED',
  'CLIENT_HEADER_MISSING', 'NOT_FOUND', 'METHOD_NOT_ALLOWED', 'UNSUPPORTED_MEDIA_TYPE', 'REQUEST_REJECTED',
  'INTERNAL_ERROR',
];

function prob(status: number, code: Problem['code'], errors: Problem['errors'] = []): Problem {
  return { type: `urn:todo:problem:${code}`, title: 'SENTINEL', status, detail: 'SENTINEL', code, errors };
}
const http = (status: number, problem?: Problem): Failure => ({ kind: 'http', status, ...(problem ? { problem } : {}) });
const SHOWN_FIELD: Record<string, string> = { 'sign-up': 'identifier', 'log-in': 'password', create: 'title', save: 'dueDate' };
const forbidden = (text: string) => {
  for (const bad of [...CODES, 'Exception', 'SQL', 'stack', 'Failed to fetch', 'SENTINEL']) {
    expect(text).not.toContain(bad);
  }
};

describe('userMessageFor (sdd §6.1, NFR-006)', () => {
  it('NFR-006 AC-2 the Problem type of identity.ts equals the Problem type of todos.ts', async () => {
    // generated from the contracts (ADR-0001); the module must exist at run time. The type-level equality below is
    // enforced by the TypeScript compiler, i.e. by `pnpm -C web typecheck` (tsconfig includes tests/); Vitest's
    // `run` does not check types, so that command is a gate for this criterion, not only this test.
    await load('/src/api/identity');
    await load('/src/api/todos');
    expectTypeOf<IdentityProblem>().toEqualTypeOf<Problem>();
  });

  // every ProblemCode with the status the contracts give it (common-problem-v1.yaml; 413 is one of REQUEST_REJECTED's)
  const CODE_STATUS: Record<string, number> = {
    VALIDATION_FAILED: 400,
    MALFORMED_REQUEST: 400,
    IDENTIFIER_TAKEN: 409,
    INVALID_CREDENTIALS: 401,
    UNAUTHENTICATED: 401,
    CLIENT_HEADER_MISSING: 403,
    NOT_FOUND: 404,
    METHOD_NOT_ALLOWED: 405,
    UNSUPPORTED_MEDIA_TYPE: 415,
    REQUEST_REJECTED: 413,
    INTERNAL_ERROR: 500,
  };

  // sdd §14 excludes three rows from "non-empty text and a next step": a 400 VALIDATION_FAILED form row with a shown
  // field (not built here, the errors are empty), the 409 IDENTIFIER_TAKEN row on sign-up (its text is errors[].message)
  // and the session-probe 401 row (shows nothing).
  const excluded = (action: UserAction, code: Problem['code']) =>
    (action === 'sign-up' && code === 'IDENTIFIER_TAKEN') || (action === 'session-probe' && CODE_STATUS[code] === 401);

  it.each(ALL_ACTIONS.flatMap((a) => CODES.filter((c) => !excluded(a, c)).map((c) => [a, c] as const)))(
    'NFR-006 AC-1 AC-2 %s with %s gives a non-empty text, a next step and no internals',
    async (action, code) => {
      const status = CODE_STATUS[code] ?? 500;
      const { text, nextStep } = await msg(http(status, prob(status, code)), action);
      expect(text.length).toBeGreaterThan(0);
      expect(nextStep).toBeTruthy();
      forbidden(text);
    },
  );

  it.each(ALL_ACTIONS.flatMap((a) => [403, 405, 413, 415, 500, 502].map((s) => [a, s] as const)))(
    'NFR-006 AC-1 %s with status %i gives try-again with no internals',
    async (action, status) => {
      const m = await msg(http(status), action);
      expect(m.nextStep).toBe('try-again');
      expect(m.text.length).toBeGreaterThan(0);
      forbidden(m.text);
    },
  );

  it.each(ALL_ACTIONS)(
    'NFR-006 AC-1 network failure on %s gives try-again naming no browser error text',
    async (action) => {
      const m = await msg({ kind: 'network' }, action);
      expect(m.nextStep).toBe('try-again');
      forbidden(m.text);
    },
  );

  it('NFR-006 AC-1 a body that is not a Problem gives try-again', async () => {
    expect((await msg(http(500), 'load-list')).nextStep).toBe('try-again');
  });

  it.each(['load-list', 'toggle-done', 'delete-from-list', 'delete-from-detail', 'clear-completed'] as UserAction[])(
    'NFR-006 AC-1 a 400 VALIDATION_FAILED on %s gives try-again, not fix-fields',
    async (action) => {
      const p = prob(400, 'VALIDATION_FAILED', [{ field: 'page', code: 'INVALID_VALUE', message: 'x' }]);
      expect((await msg(http(400, p), action)).nextStep).toBe('try-again');
    },
  );

  it.each(FORM_ACTIONS)(
    'NFR-006 AC-1 a 400 on %s with errors [], errors absent or only UNKNOWN_FIELD on owner gives try-again',
    async (action) => {
      expect((await msg(http(400, prob(400, 'VALIDATION_FAILED', [])), action)).nextStep).toBe('try-again');
      const absent = { ...prob(400, 'VALIDATION_FAILED') } as Partial<Problem>;
      delete absent.errors;
      expect((await msg({ kind: 'http', status: 400, problem: absent as Problem }, action)).nextStep).toBe('try-again');
      const owner = [{ field: 'owner', code: 'UNKNOWN_FIELD' as const, message: 'Unknown field.' }];
      expect((await msg(http(400, prob(400, 'VALIDATION_FAILED', owner)), action)).nextStep).toBe('try-again');
    },
  );

  it.each(FORM_ACTIONS)(
    'NFR-006 AC-1 a 400 on %s with an error on a field that form shows gives fix-fields, even beside one on owner',
    async (action) => {
      const errors = [
        { field: SHOWN_FIELD[action]!, code: 'REQUIRED' as const, message: 'Enter a value.' },
        { field: 'owner', code: 'UNKNOWN_FIELD' as const, message: 'Unknown field.' },
      ];
      expect((await msg(http(400, prob(400, 'VALIDATION_FAILED', errors)), action)).nextStep).toBe('fix-fields');
    },
  );

  it('NFR-006 AC-1 a 409 IDENTIFIER_TAKEN on sign-up names choosing another identifier or logging in', async () => {
    const taken = prob(409, 'IDENTIFIER_TAKEN', [{ field: 'identifier', code: 'TAKEN', message: 'Taken.' }]);
    expect((await msg(http(409, taken), 'sign-up')).nextStep).toBe('choose-other-identifier-or-log-in');
  });

  it('NFR-006 AC-4 a wrong password and an unknown identifier map to identical text and next step', async () => {
    const a = await msg(http(401, { ...prob(401, 'INVALID_CREDENTIALS'), detail: 'wrong password' }), 'log-in');
    const b = await msg(http(401, { ...prob(401, 'INVALID_CREDENTIALS'), detail: 'no such identifier' }), 'log-in');
    expect(a).toEqual(b);
    expect(a.text).toBe('Invalid identifier or password. Check them and try again.');
    expect(a.nextStep).toBe('check-and-retry-login');
  });

  it.each(['toggle-done', 'delete-from-list'] as UserAction[])(
    'NFR-006 AC-1 a 404 on %s gives list-refreshed with the refreshed-list sentence (Q48)',
    async (action) => {
      const m = await msg(http(404, prob(404, 'NOT_FOUND')), action);
      expect(m.nextStep).toBe('list-refreshed');
      expect(m.text).toBe('That to-do no longer exists, and the list has been refreshed.');
    },
  );

  it.each(['delete-from-detail', 'load-todo', 'save'] as UserAction[])(
    'NFR-006 AC-1 a 404 on %s gives back-to-list (Q48)',
    async (action) => {
      expect((await msg(http(404, prob(404, 'NOT_FOUND')), action)).nextStep).toBe('back-to-list');
    },
  );

  it.each(ALL_ACTIONS)('NFR-006 AC-3 an unexpected failure on %s gives reload-or-home with no internals', async (action) => {
    const m = await msg({ kind: 'unexpected' }, action);
    expect(m.nextStep).toBe('reload-or-home');
    expect(m.text.length).toBeGreaterThan(0);
    forbidden(m.text);
  });
});
