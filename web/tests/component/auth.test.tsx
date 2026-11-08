import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { FakeApi, OWNER_IDENTIFIER, OWNER_PASSWORD, SENTINEL, problem, seed } from '../helpers/fakeApi';
import { renderApp } from '../helpers/renderApp';

const identifierField = () => screen.getByLabelText(/email address or username/i);
const passwordField = () => screen.getByLabelText(/^password/i);
const submit = (name: RegExp) => screen.getByRole('button', { name });

describe('Sign up screen S1', () => {
  it('REQ-AUTH-001 AC-7 the form has one identifier field for an email address or a username', async () => {
    await renderApp('/signup', new FakeApi());
    expect(await screen.findByRole('heading', { level: 1, name: 'Sign up' })).toBeInTheDocument();
    expect(screen.getAllByRole('textbox')).toHaveLength(1);
    expect(identifierField()).toBeRequired();
    expect(passwordField()).toHaveAttribute('type', 'password');
    expect(screen.queryByLabelText(/confirm/i)).not.toBeInTheDocument();
  });

  it('REQ-AUTH-001 AC-1 a signed-out visitor opening /signup sees the form with no message and no redirect', async () => {
    const api = new FakeApi();
    const { location } = await renderApp('/signup', api);
    await screen.findByRole('heading', { level: 1, name: 'Sign up' });
    expect(location.pathname).toBe('/signup');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(api.calls.map((c) => `${c.method} ${c.path}`)).toEqual(['GET /api/v1/auth/session']);
  });

  it('REQ-AUTH-002 AC-4 a signed-in visitor opening /signup is sent to /todos', async () => {
    const { location } = await renderApp('/signup', new FakeApi({ signedIn: true }));
    await waitFor(() => expect(location.pathname).toBe('/todos'));
  });

  it('REQ-AUTH-001 AC-3 empty identifier and password are refused with field messages and nothing is sent', async () => {
    const api = new FakeApi();
    const { user } = await renderApp('/signup', api);
    await screen.findByRole('heading', { level: 1, name: 'Sign up' });
    await user.click(submit(/^sign up$/i));
    const summary = await screen.findByRole('alert');
    expect(within(summary).getByText('There is a problem')).toBeInTheDocument();
    expect(screen.getAllByText('Enter an email address or a username.').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Enter a password.').length).toBeGreaterThan(0);
    expect(api.callsTo('POST', '/api/v1/auth/signup')).toHaveLength(0);
  });

  it.each([
    ['REQ-AUTH-001 AC-5', 'a'.repeat(7), 'The password must be at least 8 characters.'],
    ['REQ-AUTH-001 AC-11', 'a'.repeat(129), 'The password must be 128 characters or fewer.'],
  ])('%s password of the wrong length is refused with its message', async (_ac, password, message) => {
    const api = new FakeApi();
    const { user } = await renderApp('/signup', api);
    await screen.findByRole('heading', { level: 1, name: 'Sign up' });
    await user.type(identifierField(), 'alice');
    fireEvent.change(passwordField(), { target: { value: password } });
    await user.click(submit(/^sign up$/i));
    expect((await screen.findAllByText(message)).length).toBeGreaterThan(0);
    expect(passwordField()).toHaveAttribute('aria-invalid', 'true');
    expect(api.callsTo('POST', '/api/v1/auth/signup')).toHaveLength(0);
  });

  it.each([
    ['REQ-AUTH-001 AC-5', 8],
    ['REQ-AUTH-001 AC-11', 128],
  ])('%s a password of exactly %i characters is accepted and sent unchanged', async (_ac, length) => {
    const api = new FakeApi();
    const { user } = await renderApp('/signup', api);
    await screen.findByRole('heading', { level: 1, name: 'Sign up' });
    await user.type(identifierField(), 'alice');
    const password = ` ${'p'.repeat(length - 2)} `;
    fireEvent.change(passwordField(), { target: { value: password } });
    await user.click(submit(/^sign up$/i));
    await waitFor(() => expect(api.callsTo('POST', '/api/v1/auth/signup')).toHaveLength(1));
    expect((api.callsTo('POST', '/api/v1/auth/signup')[0]!.body as { password: string }).password).toBe(password);
  });

  it.each([
    ['REQ-AUTH-001 AC-8', 'ab', 'The email address or username must be 3 to 254 characters.'],
    ['REQ-AUTH-001 AC-8', `${'a'.repeat(250)}@b.co`, 'The email address or username must be 3 to 254 characters.'],
    ['REQ-AUTH-001 AC-9', 'not@valid', 'Enter a valid email address, for example name@example.com.'],
    ['REQ-AUTH-001 AC-10', 'bad name!', 'A username can contain only letters A to Z, digits 0 to 9, and the characters . _ -'],
  ])('%s identifier %j is refused with its message', async (_ac, identifier, message) => {
    const api = new FakeApi();
    const { user } = await renderApp('/signup', api);
    await screen.findByRole('heading', { level: 1, name: 'Sign up' });
    fireEvent.change(identifierField(), { target: { value: identifier } });
    await user.type(passwordField(), 'long-enough-password');
    await user.click(submit(/^sign up$/i));
    expect((await screen.findAllByText(message)).length).toBeGreaterThan(0);
    expect(identifierField()).toHaveAttribute('aria-invalid', 'true');
    expect(api.callsTo('POST', '/api/v1/auth/signup')).toHaveLength(0);
  });

  it('REQ-AUTH-001 AC-1 AC-4 a valid sign-up sends the trimmed identifier with X-Todo-Client, then shows the log-in page with an account-created message and no automatic login', async () => {
    const api = new FakeApi();
    const { user, location } = await renderApp('/signup', api);
    await screen.findByRole('heading', { level: 1, name: 'Sign up' });
    fireEvent.change(identifierField(), { target: { value: ' 　bob_1　 ' } });
    await user.type(passwordField(), 'long-enough-password');
    await user.click(submit(/^sign up$/i));
    await waitFor(() => expect(location.pathname).toBe('/login'));
    const call = api.callsTo('POST', '/api/v1/auth/signup')[0]!;
    expect(call.headers.get('X-Todo-Client')).toBe('web');
    expect(call.body).toEqual({ identifier: 'bob_1', password: 'long-enough-password' });
    expect(await screen.findByRole('status')).toHaveTextContent(/account created/i);
    expect(api.callsTo('GET', '/api/v1/todos')).toHaveLength(0);
    expect(location.search).not.toMatch(/account/i); // the message travels in router state, never in the URL
  });

  it('REQ-AUTH-001 AC-2 AC-6 a taken identifier shows the server message beside the field and keeps the password', async () => {
    const api = new FakeApi();
    const { user } = await renderApp('/signup', api);
    await screen.findByRole('heading', { level: 1, name: 'Sign up' });
    await user.type(identifierField(), 'ALICE@example.com');
    await user.type(passwordField(), 'long-enough-password');
    await user.click(submit(/^sign up$/i));
    const message = 'This email address or username is already taken. Choose a different one, or log in.';
    expect((await screen.findAllByText(message)).length).toBeGreaterThan(0);
    expect(identifierField()).toHaveAttribute('aria-invalid', 'true');
    expect(passwordField()).toHaveValue('long-enough-password');
    expect(document.body).not.toHaveTextContent(SENTINEL);
  });
});

describe('Log in screen S2', () => {
  it('REQ-AUTH-002 AC-1 correct identifier and password sign the user in and show their own list', async () => {
    const api = new FakeApi({ todos: seed(2) });
    const { user, location } = await renderApp('/login', api);
    await screen.findByRole('heading', { level: 1, name: 'Log in' });
    await user.type(identifierField(), OWNER_IDENTIFIER);
    await user.type(passwordField(), OWNER_PASSWORD);
    await user.click(submit(/^log in$/i));
    await waitFor(() => expect(location.pathname).toBe('/todos'));
    expect(await screen.findByRole('heading', { level: 1, name: 'My to-dos' })).toBeInTheDocument();
    expect(api.callsTo('POST', '/api/v1/auth/login')[0]!.headers.get('X-Todo-Client')).toBe('web');
  });

  it('REQ-AUTH-002 AC-5 a different letter case and padding of the identifier still logs in', async () => {
    const { user, location } = await renderApp('/login', new FakeApi());
    await screen.findByRole('heading', { level: 1, name: 'Log in' });
    fireEvent.change(identifierField(), { target: { value: ' ALICE@example.com ' } });
    await user.type(passwordField(), OWNER_PASSWORD);
    await user.click(submit(/^log in$/i));
    await waitFor(() => expect(location.pathname).toBe('/todos'));
  });

  it('REQ-AUTH-002 AC-2 AC-3 NFR-006 AC-4 a wrong password and an unknown identifier show the same generic alert, never the response text', async () => {
    const texts: string[] = [];
    for (const [identifier, password] of [
      [OWNER_IDENTIFIER, 'wrong-password'],
      ['nobody@example.com', 'whatever-password'],
    ] as const) {
      const api = new FakeApi();
      const { user, unmount } = await renderApp('/login', api);
      await screen.findByRole('heading', { level: 1, name: 'Log in' });
      await user.type(identifierField(), identifier);
      await user.type(passwordField(), password);
      await user.click(submit(/^log in$/i));
      const alert = await screen.findByRole('alert');
      texts.push(alert.textContent ?? '');
      expect(alert).toHaveTextContent('Invalid identifier or password. Check them and try again.');
      expect(document.body).not.toHaveTextContent(SENTINEL);
      expect(passwordField()).toHaveValue('');
      expect(identifierField()).toHaveFocus();
      unmount();
    }
    expect(texts[0]).toBe(texts[1]);
  });

  it('REQ-AUTH-002 AC-2 an empty identifier and password are refused with their messages and nothing is sent', async () => {
    const api = new FakeApi();
    const { user } = await renderApp('/login', api);
    await screen.findByRole('heading', { level: 1, name: 'Log in' });
    await user.click(submit(/^log in$/i));
    expect((await screen.findAllByText('Enter your email address or username.')).length).toBeGreaterThan(0);
    expect(screen.getAllByText('Enter your password.').length).toBeGreaterThan(0);
    expect(api.callsTo('POST', '/api/v1/auth/login')).toHaveLength(0);
  });

  it('REQ-AUTH-002 AC-1 a typed /login shows no session message', async () => {
    await renderApp('/login', new FakeApi());
    await screen.findByRole('heading', { level: 1, name: 'Log in' });
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});

describe('Session state (sdd §4.1)', () => {
  it('REQ-AUTH-002 AC-4 a reload on /todos renders nothing until the probe resolves and does not redirect', async () => {
    let release!: () => void;
    const gate = new Promise<void>((r) => (release = r));
    const api = new FakeApi({ signedIn: true, todos: seed(1) });
    api.on('GET', '/api/v1/auth/session', async () => {
      await gate;
      return new Response(JSON.stringify({ user: { identifier: OWNER_IDENTIFIER }, expiresAt: '2026-11-08T18:00:00.000Z' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      });
    });
    const { location } = await renderApp('/todos', api);
    expect(screen.queryByRole('heading', { level: 1 })).not.toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(location.pathname).toBe('/todos');
    release();
    expect(await screen.findByRole('heading', { level: 1, name: 'My to-dos' })).toBeInTheDocument();
    expect(location.pathname).toBe('/todos');
  });

  it.each([
    ['a network error', 'network' as const],
    ['a 500', 'status500' as const],
  ])('REQ-AUTH-002 AC-4 NFR-006 AC-1 a failed probe (%s) shows the server alert with Retry and never redirects', async (_label, kind) => {
    const api = new FakeApi({ signedIn: true, todos: seed(1) });
    api.on('GET', '/api/v1/auth/session', () => (kind === 'network' ? 'network' : problem(500, 'INTERNAL_ERROR')), true);
    const { user, location } = await renderApp('/todos', api);
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('The app could not reach the server.');
    expect(document.body).not.toHaveTextContent(/failed to fetch|SENTINEL/i);
    expect(location.pathname).toBe('/todos');
    await user.click(screen.getByRole('button', { name: /^retry$/i }));
    expect(await screen.findByRole('heading', { level: 1, name: 'My to-dos' })).toBeInTheDocument();
    expect(api.callsTo('GET', '/api/v1/auth/session')).toHaveLength(2);
  });

  it('REQ-AUTH-004 AC-2 a signed-out visitor on a to-do route is sent to log in with no message and returns there after logging in', async () => {
    const api = new FakeApi({ todos: seed(1) });
    const { user, location } = await renderApp('/todos/new', api);
    await waitFor(() => expect(location.pathname).toBe('/login'));
    expect(new URLSearchParams(location.search).get('returnTo')).toBe('/todos/new');
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    await user.type(identifierField(), OWNER_IDENTIFIER);
    await user.type(passwordField(), OWNER_PASSWORD);
    await user.click(submit(/^log in$/i));
    await waitFor(() => expect(location.pathname).toBe('/todos/new'));
  });

  it('REQ-AUTH-004 AC-2 a returnTo that is not an app route is not followed after login', async () => {
    const { user, location } = await renderApp('/login?returnTo=%2F%2Fevil.example', new FakeApi());
    await screen.findByRole('heading', { level: 1, name: 'Log in' });
    await user.type(identifierField(), OWNER_IDENTIFIER);
    await user.type(passwordField(), OWNER_PASSWORD);
    await user.click(submit(/^log in$/i));
    await waitFor(() => expect(location.pathname).toBe('/todos'));
  });

  it('REQ-AUTH-004 AC-4 a 401 on a request while signed in goes to log in with the session-ended message', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(3) });
    const { user, location } = await renderApp('/todos', api);
    await screen.findByRole('heading', { level: 1, name: 'My to-dos' });
    api.signedIn = false; // the session ends on the server
    await user.click(screen.getByRole('radio', { name: 'Active' }));
    await waitFor(() => expect(location.pathname).toBe('/login'));
    expect(new URLSearchParams(location.search).get('returnTo')).toContain('/todos');
    expect(await screen.findByRole('status')).toHaveTextContent('Your session has ended. Please log in again.');
  });

  it('REQ-AUTH-004 AC-4 a 401 from log in is invalid credentials and never the session-ended message', async () => {
    const { user } = await renderApp('/login', new FakeApi());
    await screen.findByRole('heading', { level: 1, name: 'Log in' });
    await user.type(identifierField(), OWNER_IDENTIFIER);
    await user.type(passwordField(), 'wrong-password');
    await user.click(submit(/^log in$/i));
    await screen.findByRole('alert');
    expect(document.body).not.toHaveTextContent(/session has ended/i);
  });

  it('REQ-AUTH-004 AC-4 a reload after expiry (no cookie) on a to-do route goes to log in with no message', async () => {
    const { location } = await renderApp('/todos', new FakeApi({ signedIn: false }));
    await waitFor(() => expect(location.pathname).toBe('/login'));
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(document.body).not.toHaveTextContent(/session has ended/i);
  });

  it('REQ-AUTH-004 AC-3 Log out ends the session, goes to /login with no message and a to-do page then asks to log in', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(1) });
    const { user, location } = await renderApp('/todos', api);
    await screen.findByRole('heading', { level: 1, name: 'My to-dos' });
    await user.click(screen.getByRole('button', { name: /^log out$/i }));
    await waitFor(() => expect(location.pathname).toBe('/login'));
    expect(api.callsTo('POST', '/api/v1/auth/logout')[0]!.headers.get('X-Todo-Client')).toBe('web');
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^log out$/i })).not.toBeInTheDocument();
    expect(api.signedIn).toBe(false);
  });
});
