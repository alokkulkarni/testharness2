import { describe, expect, it } from 'vitest';
import { load } from '../helpers/lazy';

const returnTo = () => load<typeof import('../../src/auth/returnTo')>('/src/auth/returnTo');
const todoId = () => load<typeof import('../../src/todos/todoId')>('/src/todos/todoId');

const UUID = '3f2b8c1e-5a4d-4e6f-9a1b-0c2d3e4f5a6b';

describe('safeReturnTo (sdd §4.2)', () => {
  it.each([
    '/todos',
    '/todos?status=active&page=2',
    `/todos/${UUID}`,
    `/todos/${UUID}/edit`,
    '/todos/new',
  ])('REQ-AUTH-004 AC-2 honours the app route %s', async (path) => {
    const { safeReturnTo } = await returnTo();
    expect(safeReturnTo(path)).toBe(path);
  });

  it.each([
    [null],
    [''],
    ['//evil.example'],
    ['https://evil.example'],
    ['/\\evil.example'],
    ['javascript:alert(1)'],
    ['/login'],
    ['/signup'],
    ['/todos/not-a-uuid'],
    ['/nope'],
    ['/todos#fragment'],
  ])('REQ-AUTH-002 AC-1 / REQ-NAV-001 AC-8 gives /todos for %s', async (raw) => {
    const { safeReturnTo } = await returnTo();
    expect(safeReturnTo(raw)).toBe('/todos');
  });
});

describe('isTodoId (sdd §8)', () => {
  it('REQ-NAV-001 AC-4 admits a lower-case UUID', async () => {
    const { isTodoId } = await todoId();
    expect(isTodoId(UUID)).toBe(true);
  });

  it.each([[UUID.toUpperCase()], ['not-a-uuid'], ['a;b'], ['a/b'], ['%'], [''], ['a%2Fb']])(
    'REQ-NAV-001 AC-4 refuses %s',
    async (value) => {
      const { isTodoId } = await todoId();
      expect(isTodoId(value)).toBe(false);
    },
  );
});
