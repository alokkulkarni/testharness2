import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { FakeApi, json, seed, type ApiCall } from '../helpers/fakeApi';
import { renderApp } from '../helpers/renderApp';

const TODO_HREF = /\/todos\/[0-9a-f-]{36}$/;
const titles = () =>
  screen
    .queryAllByRole('link')
    .filter((a) => TODO_HREF.test(a.getAttribute('href') ?? ''))
    .map((a) => a.textContent);
const rowOf = (title: string) => screen.getByRole('link', { name: title }).closest('li')!;
const listPage = async (api: FakeApi, entry = '/todos') => {
  const r = await renderApp(entry, api);
  await screen.findByRole('heading', { level: 1, name: 'My to-dos' });
  return r;
};
const lastList = (api: FakeApi): ApiCall => api.listCalls().at(-1)!;
const disabledControl = (name: RegExp) => {
  const nav = screen.getByRole('navigation', { name: 'Pagination' });
  const el = within(nav).getByText(name);
  const ctl = el.closest('a, button');
  return !ctl || ctl.hasAttribute('disabled') || ctl.getAttribute('aria-disabled') === 'true';
};
const clearButton = () => screen.getByRole('button', { name: /clear all completed/i });

describe('S3 list: view and order (REQ-TODO-003)', () => {
  it('REQ-TODO-003 AC-1 REQ-TODO-002 AC-7 to-dos are listed newest first with title, done status, due date and priority', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(3, (n) => (n === 2 ? { done: true, priority: 'HIGH', dueDate: '2026-02-03' } : {})) });
    await listPage(api);
    expect(titles()).toEqual(['Todo 3', 'Todo 2', 'Todo 1']);
    const row = within(rowOf('Todo 2'));
    expect(row.getByRole('checkbox')).toBeChecked();
    expect(row.getByText(/due 3 feb 2026/i)).toBeInTheDocument();
    expect(row.getByText(/priority: high/i)).toBeInTheDocument();
    expect(row.getByText('Completed')).toBeInTheDocument();
    expect(within(rowOf('Todo 3')).getByRole('checkbox')).not.toBeChecked();
    expect(within(rowOf('Todo 3')).getByText(/priority: medium/i)).toBeInTheDocument();
  });

  it('REQ-TODO-003 AC-2 a user with no to-dos sees the first-use empty state with an Add a to-do link instead of a list', async () => {
    const api = new FakeApi({ signedIn: true, todos: [] });
    await listPage(api);
    expect(await screen.findByRole('heading', { level: 2, name: 'You have no to-dos yet' })).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Add a to-do' }).some((a) => a.getAttribute('href') === '/todos/new')).toBe(true);
    expect(screen.queryByText('No matching to-dos')).not.toBeInTheDocument();
    expect(clearButton()).toBeDisabled();
  });

  it('REQ-TODO-003 AC-1 the list shows the result summary in a polite live region', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(45) });
    await listPage(api);
    expect(await screen.findByText('Showing 1-20 of 45')).toBeInTheDocument();
  });

  it('NFR-003 AC-1 the list shows a loading status while the first load is in flight', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(1) });
    let release!: () => void;
    const gate = new Promise<void>((r) => (release = r));
    api.on('GET', '/api/v1/todos', async () => {
      await gate;
      return json(200, { items: [], page: 1, pageSize: 20, totalItems: 0, totalPages: 0, totals: { all: 0, active: 0, completed: 0 } });
    });
    await renderApp('/todos', api);
    await screen.findByRole('heading', { level: 1, name: 'My to-dos' });
    expect(screen.getByText('Loading your to-dos...')).toBeInTheDocument();
    release();
    await screen.findByRole('heading', { level: 2, name: 'You have no to-dos yet' });
    expect(screen.queryByText('Loading your to-dos...')).not.toBeInTheDocument();
  });
});

describe('S3 list: filter (REQ-LIST-001)', () => {
  const mixed = () => seed(4, (n) => ({ done: n % 2 === 0 })); // 2 and 4 completed

  it('REQ-LIST-001 AC-5 AC-1 All is selected by default and lists active and completed to-dos', async () => {
    await listPage(new FakeApi({ signedIn: true, todos: mixed() }));
    expect(screen.getByRole('radio', { name: 'All' })).toBeChecked();
    expect(titles()).toEqual(['Todo 4', 'Todo 3', 'Todo 2', 'Todo 1']);
    expect(screen.getByRole('group', { name: 'Show' })).toBeInTheDocument();
  });

  it('REQ-LIST-001 AC-2 selecting Active asks for status=active and lists only not-done to-dos, in the URL', async () => {
    const api = new FakeApi({ signedIn: true, todos: mixed() });
    const { user, location } = await listPage(api);
    await user.click(screen.getByRole('radio', { name: 'Active' }));
    await waitFor(() => expect(titles()).toEqual(['Todo 3', 'Todo 1']));
    expect(lastList(api).query.get('status')).toBe('active');
    expect(location.search).toContain('status=active');
  });

  it('REQ-LIST-001 AC-3 selecting Completed lists only done to-dos', async () => {
    const api = new FakeApi({ signedIn: true, todos: mixed() });
    const { user } = await listPage(api);
    await user.click(screen.getByRole('radio', { name: 'Completed' }));
    await waitFor(() => expect(titles()).toEqual(['Todo 4', 'Todo 2']));
    expect(lastList(api).query.get('status')).toBe('completed');
  });

  it('REQ-LIST-001 AC-4 a filter that matches nothing shows "No matching to-dos", even for a user with no to-dos', async () => {
    const api = new FakeApi({ signedIn: true, todos: [] });
    const { user } = await listPage(api);
    await user.click(screen.getByRole('radio', { name: 'Completed' }));
    expect(await screen.findByRole('heading', { level: 2, name: 'No matching to-dos' })).toBeInTheDocument();
    expect(screen.queryByText('You have no to-dos yet')).not.toBeInTheDocument();
  });

  it('REQ-LIST-001 AC-5 an unknown status in the URL is removed (filter All) and never sent to the API', async () => {
    const api = new FakeApi({ signedIn: true, todos: mixed() });
    const { location } = await renderApp('/todos?status=bogus', api);
    await screen.findByRole('heading', { level: 1, name: 'My to-dos' });
    await waitFor(() => expect(location.search).not.toContain('bogus'));
    expect(screen.getByRole('radio', { name: 'All' })).toBeChecked();
    expect(api.listCalls().every((c) => c.query.get('status') !== 'bogus')).toBe(true);
  });
});

describe('S3 list: search (REQ-LIST-002)', () => {
  const search = () => screen.getByRole('searchbox', { name: /search to-dos/i });

  it('REQ-LIST-002 AC-1 AC-2 searching sends q, lists matches in the title or description and puts q in the URL', async () => {
    const api = new FakeApi({
      signedIn: true,
      todos: seed(3, (n) => ({ title: n === 1 ? 'Buy Milk' : `Other ${n}`, description: n === 2 ? 'ask about MILK prices' : null })),
    });
    const { user, location } = await listPage(api);
    await user.type(search(), 'milk{Enter}');
    await waitFor(() => expect(titles()).toEqual(['Other 2', 'Buy Milk']));
    expect(lastList(api).query.get('q')).toBe('milk');
    expect(location.search).toContain('q=milk');
  });

  it('REQ-LIST-002 AC-3 a search and a filter are applied together', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(4, (n) => ({ title: `Milk ${n}`, done: n % 2 === 0 })) });
    const { user } = await listPage(api);
    await user.click(screen.getByRole('radio', { name: 'Completed' }));
    await user.type(search(), 'milk');
    await user.click(screen.getByRole('button', { name: /^search$/i }));
    await waitFor(() => expect(titles()).toEqual(['Milk 4', 'Milk 2']));
    const call = lastList(api);
    expect(call.query.get('status')).toBe('completed');
    expect(call.query.get('q')).toBe('milk');
  });

  it('REQ-LIST-002 AC-4 a search that matches nothing shows "No matching to-dos"', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(2) });
    const { user } = await listPage(api);
    await user.type(search(), 'zzz{Enter}');
    expect(await screen.findByRole('heading', { level: 2, name: 'No matching to-dos' })).toBeInTheDocument();
    expect(screen.getByText('Try a different search or filter.')).toBeInTheDocument();
  });

  it.each([
    ['an empty box', ''],
    ['only ASCII spaces', '   '],
    ['only non-breaking and ideographic spaces', ' 　'],
  ])('REQ-LIST-002 AC-5 AC-7 %s sends no q', async (_label, text) => {
    const api = new FakeApi({ signedIn: true, todos: seed(2) });
    const { user, location } = await listPage(api);
    if (text) fireEvent.change(search(), { target: { value: text } });
    await user.click(screen.getByRole('button', { name: /^search$/i }));
    await waitFor(() => expect(api.listCalls().length).toBeGreaterThan(0));
    expect(api.listCalls().every((c) => !c.query.has('q'))).toBe(true);
    expect(location.search).not.toContain('q=');
  });

  it('REQ-LIST-002 AC-6 characters such as % _ and quotes are sent as typed and cause no error', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(1, () => ({ title: '100% "done"_x' })) });
    const { user } = await listPage(api);
    fireEvent.change(search(), { target: { value: `100% "done"_x` } });
    await user.click(screen.getByRole('button', { name: /^search$/i }));
    await waitFor(() => expect(lastList(api).query.get('q')).toBe('100% "done"_x'));
    expect(await screen.findByRole('link', { name: '100% "done"_x' })).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('REQ-LIST-002 AC-1 a search is trimmed with the Unicode whitespace set before it is sent', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(1, () => ({ title: 'Milk' })) });
    const { user } = await listPage(api);
    fireEvent.change(search(), { target: { value: ' milk　' } });
    await user.click(screen.getByRole('button', { name: /^search$/i }));
    await waitFor(() => expect(lastList(api).query.get('q')).toBe('milk'));
  });
});

describe('S3 list: pagination (REQ-LIST-003)', () => {
  it('REQ-LIST-003 AC-1 AC-3 more than 20 matches show exactly 20 on page 1 with numbered links, Next enabled and Previous disabled', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(45) });
    await listPage(api);
    expect(titles()).toHaveLength(20);
    expect(titles()[0]).toBe('Todo 45');
    const nav = screen.getByRole('navigation', { name: 'Pagination' });
    expect(within(nav).getByText('1').closest('[aria-current="page"]')).not.toBeNull();
    expect(within(nav).getByText('2')).toBeInTheDocument();
    expect(within(nav).getByText('3')).toBeInTheDocument();
    expect(disabledControl(/previous/i)).toBe(true);
    expect(disabledControl(/next/i)).toBe(false);
  });

  it('REQ-LIST-003 AC-2 AC-3 Next shows page 2 then page 3 with no to-do repeated or skipped, and Next is disabled on the last page', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(45) });
    const { user, location } = await listPage(api);
    const seen: (string | null)[] = [...titles()];
    await user.click(within(screen.getByRole('navigation', { name: 'Pagination' })).getByText(/next/i));
    await waitFor(() => expect(titles()[0]).toBe('Todo 25'));
    expect(location.search).toContain('page=2');
    expect(lastList(api).query.get('page')).toBe('2');
    seen.push(...titles());
    await user.click(within(screen.getByRole('navigation', { name: 'Pagination' })).getByText('3'));
    await waitFor(() => expect(titles()).toHaveLength(5));
    seen.push(...titles());
    expect(seen).toEqual(Array.from({ length: 45 }, (_, i) => `Todo ${45 - i}`));
    expect(disabledControl(/next/i)).toBe(true);
    expect(disabledControl(/previous/i)).toBe(false);
  });

  it('REQ-LIST-003 AC-4 20 or fewer matches show a single page and no pagination', async () => {
    await listPage(new FakeApi({ signedIn: true, todos: seed(20) }));
    expect(titles()).toHaveLength(20);
    expect(screen.queryByRole('navigation', { name: 'Pagination' })).not.toBeInTheDocument();
  });

  it('REQ-LIST-003 AC-5 changing the filter or the search returns to page 1 and drops page from the URL', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(45, (n) => ({ title: `Item ${n}` })) });
    const { user, location } = await listPage(api, '/todos?page=2');
    expect(location.search).toContain('page=2');
    await user.click(screen.getByRole('radio', { name: 'Active' }));
    await waitFor(() => expect(location.search).not.toContain('page='));
    expect(lastList(api).query.has('page') ? lastList(api).query.get('page') : '1').toBe('1');
    await user.click(within(screen.getByRole('navigation', { name: 'Pagination' })).getByText(/next/i));
    await waitFor(() => expect(location.search).toContain('page=2'));
    fireEvent.change(screen.getByRole('searchbox', { name: /search to-dos/i }), { target: { value: 'item' } });
    await user.click(screen.getByRole('button', { name: /^search$/i }));
    await waitFor(() => expect(location.search).not.toContain('page='));
  });

  it.each(['0', '-1', 'abc', '1.5'])('REQ-LIST-003 AC-7 a page of %s in the URL is replaced with page 1 before the API is called', async (bad) => {
    const api = new FakeApi({ signedIn: true, todos: seed(45) });
    const { location } = await renderApp(`/todos?page=${bad}`, api);
    await screen.findByRole('heading', { level: 1, name: 'My to-dos' });
    await waitFor(() => expect(titles()).toHaveLength(20));
    expect(api.listCalls().every((c) => !c.query.has('page') || c.query.get('page') === '1')).toBe(true);
    expect(location.search).not.toContain(`page=${bad}`);
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('REQ-LIST-003 AC-6 a page past the last shows the last valid page and the URL is replaced with it', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(45) });
    const { location } = await renderApp('/todos?page=99', api);
    await waitFor(() => expect(titles()).toHaveLength(5));
    expect(location.search).toContain('page=3');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('REQ-LIST-003 AC-8 the page size is fixed and the SPA never sends a size parameter', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(45) });
    await listPage(api);
    expect(api.listCalls().every((c) => !c.query.has('pageSize') && !c.query.has('size') && !c.query.has('limit'))).toBe(true);
  });

  it('REQ-LIST-003 AC-2 NFR-001 AC-2 activating a pagination link moves focus to the list heading', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(45) });
    const { user } = await listPage(api);
    await user.click(within(screen.getByRole('navigation', { name: 'Pagination' })).getByText(/next/i));
    await waitFor(() => expect(titles()[0]).toBe('Todo 25'));
    expect(screen.getByRole('heading', { level: 1, name: 'My to-dos' })).toHaveFocus();
  });
});

describe('S3 list: mark done and not done (REQ-TODO-005)', () => {
  it('REQ-TODO-005 AC-1 marking a to-do done sends done true with the client header and shows it Completed', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(2) });
    const { user } = await listPage(api);
    await user.click(within(rowOf('Todo 2')).getByRole('checkbox'));
    await waitFor(() => expect(within(rowOf('Todo 2')).getByRole('checkbox')).toBeChecked());
    const call = api.callsTo('PUT', /\/done$/)[0]!;
    expect(call.body).toEqual({ done: true });
    expect(call.headers.get('X-Todo-Client')).toBe('web');
    expect(await screen.findByText('Marked done: Todo 2.')).toBeInTheDocument();
  });

  it('REQ-TODO-005 AC-2 marking a Completed to-do not done sends done false', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(2, (n) => ({ done: n === 2 })) });
    const { user } = await listPage(api);
    await user.click(within(rowOf('Todo 2')).getByRole('checkbox'));
    await waitFor(() => expect(within(rowOf('Todo 2')).getByRole('checkbox')).not.toBeChecked());
    expect(api.callsTo('PUT', /\/done$/)[0]!.body).toEqual({ done: false });
    expect(await screen.findByText('Marked not done: Todo 2.')).toBeInTheDocument();
  });

  it('REQ-TODO-005 AC-3 under the Active filter a to-do marked done leaves the list immediately, with the list re-queried and no reload', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(3) });
    const { user } = await listPage(api, '/todos?status=active');
    const before = api.listCalls().length;
    await user.click(within(rowOf('Todo 2')).getByRole('checkbox'));
    await waitFor(() => expect(screen.queryByRole('link', { name: 'Todo 2' })).not.toBeInTheDocument());
    expect(api.listCalls().length).toBeGreaterThan(before);
    expect(lastList(api).query.get('status')).toBe('active');
    expect(titles()).toEqual(['Todo 3', 'Todo 1']);
    expect(screen.getByRole('heading', { level: 1, name: 'My to-dos' })).toHaveFocus();
  });
});

describe('S3 list: delete (REQ-TODO-006) and D1', () => {
  const openDelete = async (api: FakeApi, title: string, entry = '/todos') => {
    const r = await listPage(api, entry);
    await r.user.click(within(rowOf(title)).getByRole('button', { name: /delete/i }));
    const dialog = await screen.findByRole('dialog', { name: 'Delete this to-do?' });
    return { ...r, dialog };
  };

  it('REQ-TODO-006 AC-1 Delete asks for confirmation naming the to-do before anything is deleted', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(2) });
    const { dialog } = await openDelete(api, 'Todo 2');
    expect(within(dialog).getByText(/Todo 2/)).toBeInTheDocument();
    expect(dialog).toHaveTextContent('This permanently deletes the to-do. This cannot be undone.');
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
    expect(api.callsTo('DELETE', /.*/)).toHaveLength(0);
  });

  it('REQ-TODO-006 AC-2 Cancel leaves the to-do unchanged and sends no delete', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(2) });
    const { user, dialog } = await openDelete(api, 'Todo 2');
    await user.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(titles()).toEqual(['Todo 2', 'Todo 1']);
    expect(api.callsTo('DELETE', /.*/)).toHaveLength(0);
  });

  it('REQ-TODO-006 AC-3 confirming deletes the to-do, re-queries the list and announces it', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(2) });
    const { user, dialog } = await openDelete(api, 'Todo 2');
    await user.click(within(dialog).getByRole('button', { name: 'Delete' }));
    await waitFor(() => expect(screen.queryByRole('link', { name: 'Todo 2' })).not.toBeInTheDocument());
    expect(api.callsTo('DELETE', /^\/api\/v1\/todos\/[0-9a-f-]{36}$/)[0]!.headers.get('X-Todo-Client')).toBe('web');
    expect(titles()).toEqual(['Todo 1']);
    expect(await screen.findByText('Deleted: Todo 2.')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: 'My to-dos' })).toHaveFocus();
  });

  it('REQ-TODO-006 AC-5 deleting the only to-do on the last page shows the last valid page, not an error', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(21) });
    const { user, dialog, location } = await openDelete(api, 'Todo 1', '/todos?page=2');
    await user.click(within(dialog).getByRole('button', { name: 'Delete' }));
    await waitFor(() => expect(titles()).toHaveLength(20));
    expect(location.search).not.toContain('page=2');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('REQ-TODO-006 AC-4 NFR-006 AC-1 a delete of a to-do that no longer exists re-queries the list and says so, with no Back link', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(2) });
    const { user, dialog } = await openDelete(api, 'Todo 2');
    api.todos = api.todos.filter((t) => t.title !== 'Todo 2'); // deleted elsewhere
    const before = api.listCalls().length;
    await user.click(within(dialog).getByRole('button', { name: 'Delete' }));
    expect(await screen.findByText('That to-do no longer exists, and the list has been refreshed.')).toBeInTheDocument();
    expect(api.listCalls().length).toBeGreaterThan(before);
    expect(screen.queryByRole('link', { name: 'Back to my to-dos' })).not.toBeInTheDocument();
    expect(titles()).toEqual(['Todo 1']);
  });
});

describe('S3 list: clear all completed (REQ-TODO-007) and D2', () => {
  const PLURAL = (n: number) =>
    `Delete all ${n} completed to-dos? This deletes all your completed to-dos, including any not shown because of the current page, filter or search. This cannot be undone.`;
  const SINGULAR =
    'Delete 1 completed to-do? This deletes your only completed to-do, even if it is not shown because of the current page, filter or search. This cannot be undone.';
  const withCompleted = (n: number) => seed(n + 2, (i) => ({ done: i <= n }));

  it('REQ-TODO-007 AC-4 the control is disabled when there are no completed to-dos', async () => {
    await listPage(new FakeApi({ signedIn: true, todos: seed(3) }));
    expect(clearButton()).toBeDisabled();
  });

  it.each([
    [1, SINGULAR],
    [2, PLURAL(2)],
    [21, PLURAL(21)],
  ])('REQ-TODO-007 AC-1 with %i completed to-dos the confirmation states the number, the scope and permanence in the fixed sentence', async (n, sentence) => {
    const api = new FakeApi({ signedIn: true, todos: withCompleted(n) });
    const { user } = await listPage(api);
    expect(clearButton()).toBeEnabled();
    const before = api.listCalls().length;
    await user.click(clearButton());
    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveTextContent(sentence);
    expect(api.listCalls().length).toBeGreaterThan(before); // re-fetch before opening (sdd §8)
    expect(api.callsTo('DELETE', /.*/)).toHaveLength(0);
  });

  it('REQ-TODO-007 AC-3 Cancel deletes nothing', async () => {
    const api = new FakeApi({ signedIn: true, todos: withCompleted(2) });
    const { user } = await listPage(api);
    await user.click(clearButton());
    const dialog = await screen.findByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(api.callsTo('DELETE', /.*/)).toHaveLength(0);
    expect(api.todos).toHaveLength(4);
  });

  it('REQ-TODO-007 AC-2 confirming deletes all completed to-dos regardless of the filter, keeps the active ones and re-queries', async () => {
    const api = new FakeApi({ signedIn: true, todos: withCompleted(2) });
    const { user } = await listPage(api, '/todos?status=active');
    await user.click(clearButton());
    const dialog = await screen.findByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: /^delete 2 completed$/i }));
    await waitFor(() => expect(api.callsTo('DELETE', '/api/v1/todos/completed')).toHaveLength(1));
    expect(api.callsTo('DELETE', '/api/v1/todos/completed')[0]!.headers.get('X-Todo-Client')).toBe('web');
    expect(api.todos.every((t) => !t.done)).toBe(true);
    await waitFor(() => expect(clearButton()).toBeDisabled());
    expect(await screen.findByText('Deleted 2 completed to-dos.')).toBeInTheDocument();
  });

  it('REQ-TODO-007 AC-4 when the re-fetch finds none completed, no dialog opens, no delete is sent, the control is disabled and the live region says so', async () => {
    const api = new FakeApi({ signedIn: true, todos: withCompleted(1) });
    const { user } = await listPage(api);
    expect(clearButton()).toBeEnabled();
    api.todos = api.todos.map((t) => ({ ...t, done: false })); // cleared in another tab
    await user.click(clearButton());
    expect(await screen.findByText('There are no completed to-dos to clear.')).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(api.callsTo('DELETE', /.*/)).toHaveLength(0);
    expect(clearButton()).toBeDisabled();
  });
});
