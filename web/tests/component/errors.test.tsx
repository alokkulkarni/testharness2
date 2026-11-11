import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import type { ReactNode } from 'react';
import { useLocation } from 'react-router';
import { describe, expect, it, vi } from 'vitest';
import { FakeApi, SENTINEL, json, problem, seed, throwingResponse } from '../helpers/fakeApi';
import { renderApp } from '../helpers/renderApp';

const REFRESHED = 'That to-do no longer exists, and the list has been refreshed.';
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
const noInternals = () => {
  expect(document.body).not.toHaveTextContent(SENTINEL);
  expect(document.body).not.toHaveTextContent(/INTERNAL_ERROR|Failed to fetch|Exception|stack trace/);
};

describe('NFR-006 list screen failures', () => {
  it('NFR-006 AC-1 AC-2 a 500 on the list shows a friendly message with Retry and no internals; retrying loads the list', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(2) });
    api.on('GET', '/api/v1/todos', () => problem(500, 'INTERNAL_ERROR'), true);
    const { user } = await renderApp('/todos', api);
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(/could not be loaded/i);
    expect(alert).toHaveTextContent(/try again/i);
    noInternals();
    await user.click(screen.getByRole('button', { name: /^retry$/i }));
    await waitFor(() => expect(titles()).toEqual(['Todo 2', 'Todo 1']));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('NFR-006 AC-1 AC-2 a network error on the list shows the same friendly message without browser error text', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(2) });
    api.on('GET', '/api/v1/todos', () => 'network', true);
    await renderApp('/todos', api);
    expect(await screen.findByRole('alert')).toHaveTextContent(/could not be loaded/i);
    noInternals();
  });

  it('NFR-006 AC-1 a 404 on a done toggle re-queries the list, then says the to-do is gone and the list is refreshed, with no Back link (Q48)', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(3) });
    const { user } = await listPage(api);
    const gone = api.todos.find((t) => t.title === 'Todo 2')!;
    api.todos = api.todos.filter((t) => t !== gone);
    const before = api.listCalls().length;
    await user.click(within(rowOf('Todo 2')).getByRole('checkbox'));
    expect(await screen.findByText(REFRESHED)).toBeInTheDocument();
    expect(api.listCalls().length).toBe(before + 1);
    expect(screen.queryByRole('link', { name: 'Back to my to-dos' })).not.toBeInTheDocument();
    expect(titles()).toEqual(['Todo 3', 'Todo 1']);
    noInternals();
  });

  it('NFR-006 AC-1 a 404 on a done toggle whose re-query fails shows the load message, never "refreshed", and a later retry shows the list without it (Q48)', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(3) });
    const { user } = await listPage(api);
    api.todos = api.todos.filter((t) => t.title !== 'Todo 2');
    api.on('GET', '/api/v1/todos', () => 'network', true);
    await user.click(within(rowOf('Todo 2')).getByRole('checkbox'));
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(/could not be loaded/i);
    expect(document.body).not.toHaveTextContent(/list has been refreshed/i);
    await user.click(screen.getByRole('button', { name: /^retry$/i }));
    await waitFor(() => expect(titles()).toEqual(['Todo 3', 'Todo 1']));
    expect(document.body).not.toHaveTextContent(/list has been refreshed/i);
  });

  it('NFR-006 AC-1 a 404 on a row Delete whose re-query fails shows the load message, never "refreshed" (Q48)', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(2) });
    const { user } = await listPage(api);
    await user.click(within(rowOf('Todo 2')).getByRole('button', { name: /delete/i }));
    const dialog = await screen.findByRole('dialog');
    api.todos = api.todos.filter((t) => t.title !== 'Todo 2');
    api.on('GET', '/api/v1/todos', () => 'network', true);
    await user.click(within(dialog).getByRole('button', { name: 'Delete' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/could not be loaded/i);
    expect(document.body).not.toHaveTextContent(/list has been refreshed/i);
    await user.click(screen.getByRole('button', { name: /^retry$/i }));
    await waitFor(() => expect(titles()).toEqual(['Todo 1']));
    expect(document.body).not.toHaveTextContent(/list has been refreshed/i);
  });

  it('NFR-006 AC-1 a toggle that fails with a 500 shows an update message, reverts the checkbox and keeps focus on it', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(2) });
    const { user } = await listPage(api);
    api.on('PUT', /\/done$/, () => problem(500, 'INTERNAL_ERROR'), true);
    const box = within(rowOf('Todo 2')).getByRole('checkbox');
    await user.click(box);
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(/try again/i);
    expect(within(rowOf('Todo 2')).getByRole('checkbox')).not.toBeChecked();
    expect(within(rowOf('Todo 2')).getByRole('checkbox')).toHaveFocus();
    noInternals();
  });

  it('NFR-006 AC-1 a delete that fails with a 500 keeps the dialog open with an inline alert and a try-again message', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(2) });
    const { user } = await listPage(api);
    await user.click(within(rowOf('Todo 2')).getByRole('button', { name: /delete/i }));
    const dialog = await screen.findByRole('dialog');
    api.on('DELETE', /.*/, () => problem(500, 'INTERNAL_ERROR'), true);
    await user.click(within(dialog).getByRole('button', { name: 'Delete' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent(/could not be deleted.*try again/i);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    noInternals();
  });

  it('NFR-006 AC-1 a delete that fails with a network error keeps the dialog open with an inline try-again alert and no browser error text', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(2) });
    const { user } = await listPage(api);
    await user.click(within(rowOf('Todo 2')).getByRole('button', { name: /delete/i }));
    const dialog = await screen.findByRole('dialog');
    api.on('DELETE', /.*/, () => 'network', true);
    await user.click(within(dialog).getByRole('button', { name: 'Delete' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent(/could not be deleted.*try again/i);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    noInternals();
  });

  it('NFR-006 AC-1 a clear-completed that fails with a network error keeps the dialog open with an inline try-again alert', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(2, () => ({ done: true })) });
    const { user } = await listPage(api);
    await user.click(screen.getByRole('button', { name: /clear all completed/i }));
    const dialog = await screen.findByRole('dialog');
    api.on('DELETE', '/api/v1/todos/completed', () => 'network', true);
    await user.click(within(dialog).getByRole('button', { name: /^delete 2 completed$/i }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent(/try again/i);
    noInternals();
  });

  it('NFR-006 AC-1 a clear-completed that fails with a 500 keeps the dialog open with an inline alert and a try-again message', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(2, () => ({ done: true })) });
    const { user } = await listPage(api);
    await user.click(screen.getByRole('button', { name: /clear all completed/i }));
    const dialog = await screen.findByRole('dialog');
    api.on('DELETE', '/api/v1/todos/completed', () => problem(500, 'INTERNAL_ERROR'), true);
    await user.click(within(dialog).getByRole('button', { name: /^delete 2 completed$/i }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent(/try again/i);
    noInternals();
  });

  it('NFR-006 AC-1 when the re-fetch before the clear-completed dialog fails, no dialog opens and a load message shows (D2)', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(2, () => ({ done: true })) });
    const { user } = await listPage(api);
    api.on('GET', '/api/v1/todos', () => problem(500, 'INTERNAL_ERROR'), true);
    await user.click(screen.getByRole('button', { name: /clear all completed/i }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/could not be loaded.*try again/i);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(api.callsTo('DELETE', /.*/)).toHaveLength(0);
    expect(screen.getByRole('button', { name: /clear all completed/i })).toHaveFocus();
  });
});

describe('NFR-006 other screens', () => {
  it('NFR-006 AC-1 AC-2 a 500 on the detail page shows a friendly message with Retry and no internals', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(1) });
    api.on('GET', /^\/api\/v1\/todos\/[0-9a-f-]{36}$/, () => problem(500, 'INTERNAL_ERROR'), true);
    await renderApp(`/todos/${api.todos[0]!.id}`, api);
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(/could not be loaded/i);
    expect(screen.getByRole('button', { name: /^retry$/i })).toBeInTheDocument();
    noInternals();
  });

  it('NFR-006 AC-1 AC-2 a network error on the detail page shows a friendly message with Retry and no browser error text', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(1) });
    api.on('GET', /^\/api\/v1\/todos\/[0-9a-f-]{36}$/, () => 'network', true);
    await renderApp(`/todos/${api.todos[0]!.id}`, api);
    expect(await screen.findByRole('alert')).toHaveTextContent(/could not be loaded/i);
    expect(screen.getByRole('button', { name: /^retry$/i })).toBeInTheDocument();
    noInternals();
  });

  it.each([
    ['a 500', () => problem(500, 'INTERNAL_ERROR')],
    ['a network error', () => 'network' as const],
  ])('NFR-006 AC-1 AC-2 a delete from the detail page failing with %s keeps the dialog open with an inline try-again alert', async (_label, respond) => {
    const api = new FakeApi({ signedIn: true, todos: seed(1) });
    api.on('DELETE', /.*/, respond, true);
    const { user } = await renderApp(`/todos/${api.todos[0]!.id}`, api);
    await user.click(await screen.findByRole('button', { name: /^delete/i }));
    const dialog = await screen.findByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Delete' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent(/could not be deleted.*try again/i);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    noInternals();
  });

  it.each([
    ['a 500', () => problem(500, 'INTERNAL_ERROR')],
    ['a network error', () => 'network' as const],
  ])('NFR-006 AC-1 AC-2 saving an edit failing with %s keeps the input and shows a form-level try-again message with no internals', async (_label, respond) => {
    const api = new FakeApi({ signedIn: true, todos: seed(1, () => ({ title: 'Original' })) });
    api.on('PUT', /^\/api\/v1\/todos\/[0-9a-f-]{36}$/, respond, true);
    const { user } = await renderApp(`/todos/${api.todos[0]!.id}/edit`, api);
    await screen.findByDisplayValue('Original');
    fireEvent.change(screen.getByLabelText(/^title/i), { target: { value: 'Changed' } });
    await user.click(screen.getByRole('button', { name: /^save changes$/i }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/could not be saved.*try again/i);
    expect(screen.getByLabelText(/^title/i)).toHaveValue('Changed');
    noInternals();
  });

  it.each([
    ['a 500', () => problem(500, 'INTERNAL_ERROR')],
    ['a network error', () => 'network' as const],
    ['a non-Problem body', () => json(502, { stack: SENTINEL })],
  ])('NFR-006 AC-1 AC-2 sign-up failing with %s shows a try-again message and keeps the input', async (_label, respond) => {
    const api = new FakeApi();
    api.on('POST', '/api/v1/auth/signup', respond);
    const { user } = await renderApp('/signup', api);
    await screen.findByRole('heading', { level: 1, name: 'Sign up' });
    await user.type(screen.getByLabelText(/email address or username/i), 'alice');
    await user.type(screen.getByLabelText(/^password/i), 'long-enough-password');
    await user.click(screen.getByRole('button', { name: /^sign up$/i }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/try again/i);
    noInternals();
    expect(screen.getByLabelText(/email address or username/i)).toHaveValue('alice');
  });

  it.each([
    ['a 500', () => problem(500, 'INTERNAL_ERROR')],
    ['a network error', () => 'network' as const],
  ])('NFR-006 AC-1 AC-2 log-in failing with %s shows a try-again message and not the invalid-credentials text', async (_label, respond) => {
    const api = new FakeApi();
    api.on('POST', '/api/v1/auth/login', respond);
    const { user } = await renderApp('/login', api);
    await screen.findByRole('heading', { level: 1, name: 'Log in' });
    await user.type(screen.getByLabelText(/email address or username/i), 'alice');
    await user.type(screen.getByLabelText(/^password/i), 'long-enough-password');
    await user.click(screen.getByRole('button', { name: /^log in$/i }));
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(/try again/i);
    expect(alert).not.toHaveTextContent(/invalid identifier or password/i);
    noInternals();
  });
});

describe('NFR-006 AC-3 AppErrorBoundary', () => {
  function Thrower(): ReactNode {
    throw new Error(SENTINEL);
  }

  it('NFR-006 AC-3 a component that throws while rendering shows a friendly page with a reload control and a link to home, not a blank screen', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    await renderApp('/todos', new FakeApi({ signedIn: true }), () => <Thrower />);
    expect(await screen.findByRole('heading', { level: 1 })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /reload/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /home/i })).toHaveAttribute('href', '/');
    noInternals();
    expect(document.body).not.toHaveTextContent(/boom/i);
  });

  it('NFR-006 AC-3 following the error page\'s link to home clears the error and renders the normal /todos list without a reload', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    let thrown = false;
    function ThrowsOnFirstTodosRender({ children }: { children: ReactNode }) {
      const { pathname } = useLocation();
      if (!thrown && pathname === '/todos') {
        thrown = true;
        throw new Error(SENTINEL);
      }
      return children;
    }
    const api = new FakeApi({ signedIn: true, todos: seed(2) });
    const { user, location } = await renderApp('/todos', api, (routes) => <ThrowsOnFirstTodosRender>{routes}</ThrowsOnFirstTodosRender>);
    const home = await screen.findByRole('link', { name: /home/i });
    await user.click(home);
    expect(await screen.findByRole('heading', { level: 1, name: 'My to-dos' })).toBeInTheDocument();
    expect(location.pathname).toBe('/todos');
    expect(screen.queryByRole('button', { name: /reload/i })).not.toBeInTheDocument();
    // the session state was kept: the session probe ran once only
    expect(api.callsTo('GET', '/api/v1/auth/session')).toHaveLength(1);
  });
});

// sdd §6.1 / §14 NFR-006 (clarifications Q49, critic note N-2): an error thrown in an event handler or promise callback
// is mapped with userMessageFor({ kind: 'unexpected' }, action) and shown where the screen shows that action's other
// failures; the screen is not replaced by the AppErrorBoundary page and the sentinel message is never shown. The
// "stubbed dependency throwing a plain Error" is a response whose body cannot be read (throwingResponse).
describe('NFR-006 AC-1 AC-2 N-2 unexpected errors in event handlers keep the screen', () => {
  it('NFR-006 AC-1 AC-2 N-2 a done toggle whose handler throws shows the unexpected message in the page-level alert above the list and keeps the list', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    const api = new FakeApi({ signedIn: true, todos: seed(3) });
    const { user } = await listPage(api);
    api.on('PUT', /\/done$/, () => throwingResponse(200), true);
    await user.click(within(rowOf('Todo 2')).getByRole('checkbox'));
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(/could not be updated/i);
    expect(alert).toHaveTextContent(/reload the page/i);
    expect(within(alert).getByRole('link', { name: /home/i })).toHaveAttribute('href', '/');
    expect(alert.compareDocumentPosition(rowOf('Todo 2')) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    // the list screen stays rendered; it is not the AppErrorBoundary page
    expect(screen.getByRole('heading', { level: 1, name: 'My to-dos' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: /something went wrong/i })).not.toBeInTheDocument();
    expect(titles()).toEqual(['Todo 3', 'Todo 2', 'Todo 1']);
    noInternals();
  });

  it.each([
    ['create', '/todos/new', 'POST', '/api/v1/todos', 201, /^add to-do$/i, /could not be added/i],
    ['save', null, 'PUT', /^\/api\/v1\/todos\/[0-9a-f-]{36}$/, 200, /^save changes$/i, /could not be saved/i],
  ] as const)(
    'NFR-006 AC-1 AC-2 N-2 a %s form submit whose handler throws shows the unexpected message in the form-level alert and keeps the form and its input',
    async (_action, entry, method, path, status, submitName, actionText) => {
      vi.spyOn(console, 'error').mockImplementation(() => {});
      const api = new FakeApi({ signedIn: true, todos: seed(1, () => ({ title: 'Original' })) });
      api.on(method, path, () => throwingResponse(status), true);
      const { user } = await renderApp(entry ?? `/todos/${api.todos[0]!.id}/edit`, api);
      const field = await screen.findByLabelText(/^title/i);
      await screen.findByRole('heading', { level: 1, name: entry ? 'Add a to-do' : 'Edit to-do' });
      fireEvent.change(field, { target: { value: 'Keep me' } });
      fireEvent.change(screen.getByLabelText(/^due date/i), { target: { value: '2026-11-06' } });
      await user.click(screen.getByRole('button', { name: submitName }));
      const alert = await screen.findByRole('alert');
      expect(alert).toHaveTextContent(actionText);
      expect(alert).toHaveTextContent(/reload the page/i);
      expect(screen.queryByRole('heading', { name: /something went wrong/i })).not.toBeInTheDocument();
      expect(screen.getByLabelText(/^title/i)).toHaveValue('Keep me');
      noInternals();
    },
  );
});

describe('List re-queries keep the screen in place (sdd §6.1, UX design §6)', () => {
  // the next list request never answers, so the test sees the screen exactly while a re-query is in flight
  const pendingNext = (api: FakeApi) => {
    api.on('GET', '/api/v1/todos', () => new Promise<Response>(() => {}), true);
  };

  it('REQ-LIST-001 AC-2 NFR-001 AC-2 a filter change keeps focus on the radio used and the existing rows until the response arrives, without remounting the list', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(3) });
    const { user } = await listPage(api);
    const heading = screen.getByRole('heading', { level: 1, name: 'My to-dos' });
    const before = titles();
    pendingNext(api);
    await user.click(screen.getByRole('radio', { name: 'Active' }));
    expect(screen.getByRole('radio', { name: 'Active' })).toHaveFocus();
    expect(titles()).toEqual(before);
    expect(screen.getByRole('heading', { level: 1, name: 'My to-dos' })).toBe(heading);
  });

  it('REQ-LIST-003 AC-2 NFR-001 AC-2 a pagination link keeps the existing rows until the response arrives and moves focus to the heading without remounting', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(45) });
    const { user } = await listPage(api);
    const heading = screen.getByRole('heading', { level: 1, name: 'My to-dos' });
    const before = titles();
    pendingNext(api);
    await user.click(within(screen.getByRole('navigation', { name: 'Pagination' })).getByText(/next/i));
    expect(titles()).toEqual(before);
    expect(screen.getByRole('heading', { level: 1, name: 'My to-dos' })).toBe(heading);
    expect(heading).toHaveFocus();
  });
});
