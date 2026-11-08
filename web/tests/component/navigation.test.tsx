import { screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { FakeApi, OWNER_IDENTIFIER, OWNER_PASSWORD, seed } from '../helpers/fakeApi';
import { renderApp } from '../helpers/renderApp';

const calls = (api: FakeApi) => api.calls.map((c) => `${c.method} ${c.path}`);
const UNKNOWN_ADDRESSES = ['/nope', '/todos/abc/def', '/login/extra'];

describe('REQ-NAV-001 home route', () => {
  it('REQ-NAV-001 AC-1 a signed-in user opening / (with a query string) is sent to /todos, history replaced, filter All, no search, page 1', async () => {
    const { location } = await renderApp('/?status=active&q=milk&page=3', new FakeApi({ signedIn: true, todos: seed(3) }));
    await screen.findByRole('heading', { level: 1, name: 'My to-dos' });
    expect(location.pathname).toBe('/todos');
    expect(location.search).toBe('');
    expect(screen.getByRole('radio', { name: 'All' })).toBeChecked();
    expect(screen.getByRole('searchbox', { name: /search to-dos/i })).toHaveValue('');
  });

  it('REQ-NAV-001 AC-1 the / entry is replaced in history, not pushed', async () => {
    const { location } = await renderApp('/', new FakeApi({ signedIn: true }));
    await waitFor(() => expect(location.pathname).toBe('/todos'));
    expect(location.navigationType).toBe('REPLACE');
  });

  it('REQ-NAV-001 AC-2 a signed-out visitor opening / is sent to log in with no message and sees the list after logging in', async () => {
    const api = new FakeApi({ todos: seed(1) });
    const { user, location } = await renderApp('/', api);
    await waitFor(() => expect(location.pathname).toBe('/login'));
    expect(new URLSearchParams(location.search).get('returnTo')).toBe('/todos');
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    await user.type(screen.getByLabelText(/email address or username/i), OWNER_IDENTIFIER);
    await user.type(screen.getByLabelText(/^password/i), OWNER_PASSWORD);
    await user.click(screen.getByRole('button', { name: /^log in$/i }));
    await waitFor(() => expect(location.pathname).toBe('/todos'));
    expect(await screen.findByRole('heading', { level: 1, name: 'My to-dos' })).toBeInTheDocument();
  });
});

describe('REQ-NAV-001 not-found page', () => {
  it.each(UNKNOWN_ADDRESSES)('REQ-NAV-001 AC-3 a signed-in user opening %s sees the not-found page with a link home, no redirect and no API call but the probe', async (address) => {
    const api = new FakeApi({ signedIn: true, todos: seed(2) });
    const { location } = await renderApp(address, api);
    const heading = await screen.findByRole('heading', { level: 1, name: /not found/i });
    expect(heading).toBeInTheDocument();
    expect(location.pathname).toBe(address);
    expect(screen.getByRole('link', { name: /home/i })).toHaveAttribute('href', '/');
    expect(calls(api)).toEqual(['GET /api/v1/auth/session']);
  });

  it('REQ-NAV-001 AC-3 AC-7 the not-found page sets the document title and moves focus to its heading', async () => {
    await renderApp('/nope', new FakeApi({ signedIn: true }));
    const heading = await screen.findByRole('heading', { level: 1, name: /not found/i });
    await waitFor(() => expect(heading).toHaveFocus());
    expect(document.title).toMatch(/not found/i);
    expect(document.title).toMatch(/To-do list/);
  });

  it('REQ-NAV-001 AC-3 while the probe is in flight an unknown address renders nothing and does not redirect', async () => {
    const api = new FakeApi({ signedIn: true });
    api.on('GET', '/api/v1/auth/session', () => new Promise<Response>(() => {}));
    const { location } = await renderApp('/nope', api);
    await new Promise((r) => setTimeout(r, 50));
    expect(screen.queryByRole('heading', { name: /not found/i })).not.toBeInTheDocument();
    expect(location.pathname).toBe('/nope');
  });

  it('REQ-NAV-001 AC-3 after a failed probe an unknown address shows the server alert, not the not-found page', async () => {
    const api = new FakeApi({ signedIn: true });
    api.on('GET', '/api/v1/auth/session', () => 'network');
    const { location } = await renderApp('/nope', api);
    expect(await screen.findByRole('alert')).toHaveTextContent('The app could not reach the server.');
    expect(screen.queryByRole('heading', { name: /not found/i })).not.toBeInTheDocument();
    expect(location.pathname).toBe('/nope');
  });

  it.each(UNKNOWN_ADDRESSES)('REQ-NAV-001 AC-8 a signed-out visitor opening %s is sent to log in with no message, never sees the not-found page, and lands on /todos after logging in', async (address) => {
    const api = new FakeApi({ todos: seed(1) });
    const { user, location } = await renderApp(address, api);
    await waitFor(() => expect(location.pathname).toBe('/login'));
    expect(new URLSearchParams(location.search).get('returnTo')).toBe(address);
    expect(screen.queryByRole('heading', { name: /not found/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    await user.type(screen.getByLabelText(/email address or username/i), OWNER_IDENTIFIER);
    await user.type(screen.getByLabelText(/^password/i), OWNER_PASSWORD);
    await user.click(screen.getByRole('button', { name: /^log in$/i }));
    await waitFor(() => expect(location.pathname).toBe('/todos'));
    expect(screen.queryByRole('heading', { name: /not found/i })).not.toBeInTheDocument();
  });

  it('REQ-NAV-001 AC-8 a signed-out visitor opening /todos/not-a-uuid is sent to log in and lands on /todos after logging in', async () => {
    const { user, location } = await renderApp('/todos/not-a-uuid', new FakeApi());
    await waitFor(() => expect(location.pathname).toBe('/login'));
    expect(new URLSearchParams(location.search).get('returnTo')).toBe('/todos/not-a-uuid');
    await user.type(screen.getByLabelText(/email address or username/i), OWNER_IDENTIFIER);
    await user.type(screen.getByLabelText(/^password/i), OWNER_PASSWORD);
    await user.click(screen.getByRole('button', { name: /^log in$/i }));
    await waitFor(() => expect(location.pathname).toBe('/todos'));
  });

  it('REQ-NAV-001 AC-5 the link back to home goes to / and on to the to-do list', async () => {
    const { user, location } = await renderApp('/nope', new FakeApi({ signedIn: true, todos: seed(1) }));
    await user.click(await screen.findByRole('link', { name: /home/i }));
    expect(await screen.findByRole('heading', { level: 1, name: 'My to-dos' })).toBeInTheDocument();
    expect(location.pathname).toBe('/todos');
  });

  it('REQ-NAV-001 AC-6 the animation is decorative: hidden from assistive technology, with the heading, sentence and link present at first render', async () => {
    const { container } = await renderApp('/nope', new FakeApi({ signedIn: true }));
    const heading = await screen.findByRole('heading', { level: 1, name: /not found/i });
    const main = screen.getByRole('main');
    expect(within(main).getByRole('link', { name: /home/i })).toBeVisible();
    expect(heading).toBeVisible();
    expect(main.querySelectorAll('p').length).toBeGreaterThan(0);
    const decorative = container.querySelectorAll('[aria-hidden="true"]');
    expect(decorative.length).toBeGreaterThan(0);
    // nothing the user needs sits inside the hidden element
    decorative.forEach((el) => {
      expect(el.contains(heading)).toBe(false);
      expect(el.querySelector('a, button, h1')).toBeNull();
    });
  });
});

describe('REQ-NAV-001 AC-4 to-do ids in the URL', () => {
  const BAD_IDS = ['not-a-uuid', 'a;b', 'a%2Fb', '%'];

  it.each(BAD_IDS.flatMap((id) => [`/todos/${id}`, `/todos/${id}/edit`]))(
    'REQ-NAV-001 AC-4 signed in, %s shows the to-do not-found state, not the page-not-found page, and calls no getTodo',
    async (address) => {
      const api = new FakeApi({ signedIn: true, todos: seed(1) });
      await renderApp(address, api);
      expect(await screen.findByRole('heading', { level: 1, name: 'To-do not found' })).toBeInTheDocument();
      expect(screen.getByText('This to-do does not exist or has been deleted.')).toBeInTheDocument();
      expect(screen.getByRole('link', { name: 'Back to my to-dos' })).toHaveAttribute('href', '/todos');
      expect(api.callsTo('GET', /^\/api\/v1\/todos\/.+/)).toHaveLength(0);
    },
  );

  it.each(['', '/edit'])(
    'REQ-NAV-001 AC-4 REQ-ISO-001 AC-2 a well-formed id that does not name one of my to-dos (404) shows the same to-do not-found state (%j)',
    async (suffix) => {
      const api = new FakeApi({ signedIn: true, todos: seed(1) });
      await renderApp(`/todos/3f2b8c1e-5a4d-4e6f-9a1b-0c2d3e4f5a6b${suffix}`, api);
      expect(await screen.findByRole('heading', { level: 1, name: 'To-do not found' })).toBeInTheDocument();
      expect(screen.getByText('This to-do does not exist or has been deleted.')).toBeInTheDocument();
      expect(api.callsTo('GET', '/api/v1/todos/3f2b8c1e-5a4d-4e6f-9a1b-0c2d3e4f5a6b')).toHaveLength(1);
    },
  );
});
