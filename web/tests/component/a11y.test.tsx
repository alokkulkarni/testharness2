import { fireEvent, screen, within } from '@testing-library/react';
import type { ReactNode } from 'react';
import { axe } from 'vitest-axe';
import { describe, expect, it, vi } from 'vitest';
import { FakeApi, makeTodo, seed } from '../helpers/fakeApi';
import { renderApp } from '../helpers/renderApp';

// WCAG 2.1 Level AA (NFR-001, Q44). jsdom has no layout engine or computed colours, so the colour-contrast rule
// cannot run here; it runs in the real browser in e2e/a11y.spec.ts, where it is enabled.
const AA = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'];
async function violations(container: HTMLElement): Promise<string[]> {
  const results = await axe(container, {
    runOnly: { type: 'tag', values: AA },
    rules: { 'color-contrast': { enabled: false } },
  });
  return results.violations.map((v) => `${v.id}: ${v.help} (${v.nodes.map((n) => n.target.join(' ')).join(', ')})`);
}

describe('NFR-001 AC-1 axe on every screen (component level)', () => {
  it('NFR-001 AC-1 sign-up has no WCAG 2.1 AA violations', async () => {
    const { container } = await renderApp('/signup', new FakeApi());
    await screen.findByRole('heading', { level: 1, name: 'Sign up' });
    expect(await violations(container)).toEqual([]);
  });

  it('NFR-001 AC-1 sign-up with an error summary has no violations', async () => {
    const { container, user } = await renderApp('/signup', new FakeApi());
    await screen.findByRole('heading', { level: 1, name: 'Sign up' });
    await user.click(screen.getByRole('button', { name: /^sign up$/i }));
    await screen.findByRole('alert');
    expect(await violations(container)).toEqual([]);
  });

  it('NFR-001 AC-1 log in has no violations', async () => {
    const { container } = await renderApp('/login', new FakeApi());
    await screen.findByRole('heading', { level: 1, name: 'Log in' });
    expect(await violations(container)).toEqual([]);
  });

  it('NFR-001 AC-1 log in after a refused attempt has no violations', async () => {
    const { container, user } = await renderApp('/login', new FakeApi());
    await screen.findByRole('heading', { level: 1, name: 'Log in' });
    await user.type(screen.getByLabelText(/email address or username/i), 'nobody');
    await user.type(screen.getByLabelText(/^password/i), 'wrong-password');
    await user.click(screen.getByRole('button', { name: /^log in$/i }));
    await screen.findByRole('alert');
    expect(await violations(container)).toEqual([]);
  });

  it('NFR-001 AC-1 the list (populated, with pagination) has no violations', async () => {
    const { container } = await renderApp('/todos', new FakeApi({ signedIn: true, todos: seed(45, (n) => ({ done: n % 3 === 0 })) }));
    await screen.findByRole('heading', { level: 1, name: 'My to-dos' });
    await screen.findByRole('navigation', { name: 'Pagination' });
    expect(await violations(container)).toEqual([]);
  });

  it('NFR-001 AC-1 the list in both empty states has no violations', async () => {
    const first = await renderApp('/todos', new FakeApi({ signedIn: true, todos: [] }));
    await screen.findByRole('heading', { level: 2, name: 'You have no to-dos yet' });
    expect(await violations(first.container)).toEqual([]);
    first.unmount();
    const none = await renderApp('/todos?status=completed', new FakeApi({ signedIn: true, todos: seed(2) }));
    await screen.findByRole('heading', { level: 2, name: 'No matching to-dos' });
    expect(await violations(none.container)).toEqual([]);
  });

  it('NFR-001 AC-1 the create form, with and without errors, has no violations', async () => {
    const { container, user } = await renderApp('/todos/new', new FakeApi({ signedIn: true }));
    await screen.findByRole('heading', { level: 1, name: 'Add a to-do' });
    expect(await violations(container)).toEqual([]);
    await user.click(screen.getByRole('button', { name: /^add to-do$/i }));
    await screen.findByRole('alert');
    expect(await violations(container)).toEqual([]);
  });

  it('NFR-001 AC-1 the edit form has no violations', async () => {
    const api = new FakeApi({ signedIn: true, todos: [makeTodo(1, { title: 'Original', description: 'Notes' })] });
    const { container } = await renderApp(`/todos/${api.todos[0]!.id}/edit`, api);
    await screen.findByDisplayValue('Original');
    expect(await violations(container)).toEqual([]);
  });

  it('NFR-001 AC-1 the to-do detail page and its not-found state have no violations', async () => {
    const api = new FakeApi({ signedIn: true, todos: [makeTodo(1, { title: 'Detail me', description: 'Notes' })] });
    const found = await renderApp(`/todos/${api.todos[0]!.id}`, api);
    await screen.findByRole('heading', { level: 1, name: 'Detail me' });
    expect(await violations(found.container)).toEqual([]);
    found.unmount();
    const missing = await renderApp('/todos/not-a-uuid', api);
    await screen.findByRole('heading', { level: 1, name: 'To-do not found' });
    expect(await violations(missing.container)).toEqual([]);
  });

  it('NFR-001 AC-1 the delete confirmation dialog has no violations', async () => {
    const { container, user } = await renderApp('/todos', new FakeApi({ signedIn: true, todos: seed(2) }));
    await screen.findByRole('heading', { level: 1, name: 'My to-dos' });
    const row = screen.getByRole('link', { name: 'Todo 2' }).closest('li')!;
    await user.click(within(row).getByRole('button', { name: /delete/i }));
    await screen.findByRole('dialog', { name: 'Delete this to-do?' });
    expect(await violations(container)).toEqual([]);
  });

  it('NFR-001 AC-1 the clear-completed confirmation dialog has no violations', async () => {
    const { container, user } = await renderApp('/todos', new FakeApi({ signedIn: true, todos: seed(3, (n) => ({ done: n < 3 })) }));
    await screen.findByRole('heading', { level: 1, name: 'My to-dos' });
    await user.click(screen.getByRole('button', { name: /clear all completed/i }));
    await screen.findByRole('dialog');
    expect(await violations(container)).toEqual([]);
  });

  it('REQ-NAV-001 AC-7 NFR-001 AC-1 the page-not-found page has no violations', async () => {
    const { container } = await renderApp('/nope', new FakeApi({ signedIn: true }));
    await screen.findByRole('heading', { level: 1, name: /not found/i });
    expect(await violations(container)).toEqual([]);
  });

  it('NFR-001 AC-1 the application error page has no violations', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    function Thrower(): ReactNode {
      throw new Error('boom');
    }
    const { container } = await renderApp('/todos', new FakeApi({ signedIn: true }), () => <Thrower />);
    await screen.findByRole('heading', { level: 1 });
    expect(await violations(container)).toEqual([]);
  });
});

describe('NFR-001 AC-2 structure that supports keyboard and screen-reader use', () => {
  it('NFR-001 AC-2 a skip link is the first focusable element and targets the main landmark', async () => {
    const { user } = await renderApp('/todos', new FakeApi({ signedIn: true, todos: seed(1) }));
    await screen.findByRole('heading', { level: 1, name: 'My to-dos' });
    await user.tab();
    const skip = screen.getByRole('link', { name: 'Skip to main content' });
    expect(skip).toHaveFocus();
    expect(skip).toHaveAttribute('href', '#main');
    expect(screen.getByRole('main')).toHaveAttribute('id', 'main');
    expect(screen.getByRole('banner')).toBeInTheDocument();
  });

  it('NFR-001 AC-2 each route sets its own document title and moves focus to its heading', async () => {
    const { user } = await renderApp('/login', new FakeApi());
    const login = await screen.findByRole('heading', { level: 1, name: 'Log in' });
    expect(document.title).toBe('Log in - To-do list');
    await vi.waitFor(() => expect(login).toHaveFocus());
    await user.click(screen.getByRole('link', { name: /sign up/i }));
    const signup = await screen.findByRole('heading', { level: 1, name: 'Sign up' });
    expect(document.title).toBe('Sign up - To-do list');
    await vi.waitFor(() => expect(signup).toHaveFocus());
  });

  it('NFR-001 AC-2 the filter is a labelled fieldset of radios and the search form is a labelled search landmark', async () => {
    await renderApp('/todos', new FakeApi({ signedIn: true, todos: seed(1) }));
    await screen.findByRole('heading', { level: 1, name: 'My to-dos' });
    const group = screen.getByRole('group', { name: 'Show' });
    expect(within(group).getAllByRole('radio').map((r) => r.getAttribute('value') ?? r.id)).toHaveLength(3);
    expect(screen.getByRole('search')).toBeInTheDocument();
    expect(screen.getByRole('searchbox', { name: /search to-dos/i })).toBeInTheDocument();
  });

  it('NFR-001 AC-3 a field error is tied to its field with aria-invalid and aria-describedby, and the message is prefixed "Error:"', async () => {
    const { user } = await renderApp('/todos/new', new FakeApi({ signedIn: true }));
    await screen.findByRole('heading', { level: 1, name: 'Add a to-do' });
    fireEvent.change(screen.getByLabelText(/^due date/i), { target: { value: '2026-11-06' } });
    await user.click(screen.getByRole('button', { name: /^add to-do$/i }));
    const field = screen.getByLabelText(/^title/i);
    expect(field).toHaveAttribute('aria-invalid', 'true');
    const ids = (field.getAttribute('aria-describedby') ?? '').split(' ').filter(Boolean);
    const messages = ids.map((id) => document.getElementById(id)?.textContent ?? '');
    expect(messages.some((m) => /^Error:\s*Enter a title\.$/.test(m.trim()))).toBe(true);
  });
});
