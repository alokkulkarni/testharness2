import { expect, test } from '@playwright/test';
import { apiSignUp, listHeading, newAccount, signedInUser, uiLogIn } from './support/app';

test.describe('sign up, log in, log out', () => {
  test('REQ-AUTH-001 AC-1 AC-4 a fresh browser opens /signup directly, signs up with no verification step, then logs in', async ({ page }) => {
    const account = newAccount();
    await page.goto('/signup');
    await expect(page.getByRole('heading', { level: 1, name: 'Sign up' })).toBeVisible();
    await expect(page.getByRole('alert')).toHaveCount(0);
    await expect(page).toHaveURL(/\/signup$/);
    await page.getByLabel(/email address or username/i).fill(account.identifier);
    await page.getByLabel(/^password/i).fill(account.password);
    await page.getByRole('button', { name: 'Sign up' }).click();
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('status').filter({ hasText: /account created/i })).toBeVisible();
    await uiLogIn(page, account);
    await expect(listHeading(page)).toBeVisible();
    await expect(page).toHaveURL(/\/todos$/);
  });

  test('REQ-AUTH-001 AC-2 AC-6 signing up again with another letter case of a taken identifier is refused', async ({ page }) => {
    const account = { identifier: `Case${newAccount().identifier}`, password: 'e2e-Password-123' };
    await apiSignUp(page.request, account);
    await page.goto('/signup');
    await page.getByLabel(/email address or username/i).fill(account.identifier.toLowerCase());
    await page.getByLabel(/^password/i).fill(account.password);
    await page.getByRole('button', { name: 'Sign up' }).click();
    await expect(page.getByText(/already taken/i).first()).toBeVisible();
    await expect(page).toHaveURL(/\/signup$/);
  });

  test('REQ-AUTH-002 AC-5 logging in with a different letter case of the identifier works', async ({ page }) => {
    const account = newAccount();
    await apiSignUp(page.request, account);
    await page.goto('/login');
    await uiLogIn(page, { ...account, identifier: account.identifier.toUpperCase() });
    await expect(listHeading(page)).toBeVisible();
  });

  test('REQ-AUTH-002 AC-2 AC-3 NFR-006 AC-4 a wrong password and an unknown identifier show the same message', async ({ page }) => {
    const account = newAccount();
    await apiSignUp(page.request, account);
    await page.goto('/login');
    await uiLogIn(page, { ...account, password: 'not-the-password' });
    const first = await page.getByRole('alert').textContent();
    expect(first).toContain('Invalid identifier or password. Check them and try again.');
    await uiLogIn(page, { identifier: newAccount().identifier, password: 'not-the-password' });
    await expect(page.getByRole('alert')).toHaveText(first ?? '');
    await expect(page).toHaveURL(/\/login/);
  });

  test('REQ-AUTH-002 AC-4 reloading the page keeps the user signed in', async ({ page }) => {
    await signedInUser(page);
    await page.goto('/todos');
    await expect(listHeading(page)).toBeVisible();
    await page.reload();
    await expect(listHeading(page)).toBeVisible();
    await expect(page).toHaveURL(/\/todos$/);
  });

  test('REQ-AUTH-004 AC-2 a fresh browser opening /todos lands on log in with no message, logs in and returns to /todos', async ({ page }) => {
    const account = newAccount();
    await apiSignUp(page.request, account);
    await page.goto('/todos');
    await expect(page).toHaveURL(/\/login\?returnTo=/);
    await expect(page.getByRole('heading', { level: 1, name: 'Log in' })).toBeVisible();
    await expect(page.getByText(/session has ended|account created/i)).toHaveCount(0);
    await expect(page.getByRole('alert')).toHaveCount(0);
    await uiLogIn(page, account);
    await expect(page).toHaveURL(/\/todos$/);
    await expect(listHeading(page)).toBeVisible();
  });

  test('REQ-AUTH-004 AC-3 logging out ends the session; going back to a to-do page asks to log in again', async ({ page }) => {
    await signedInUser(page);
    await page.goto('/todos');
    await page.getByRole('button', { name: 'Log out' }).click();
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByText(/session has ended|account created/i)).toHaveCount(0);
    const afterLogout = await page.request.get('/api/v1/todos');
    expect(afterLogout.status()).toBe(401);
    await page.goBack();
    await expect(page.getByRole('heading', { level: 1, name: 'Log in' })).toBeVisible();
    await expect(listHeading(page)).toHaveCount(0);
  });

  test('REQ-AUTH-004 AC-1 to-do API calls without a session are refused as unauthenticated', async ({ request }) => {
    for (const [method, url] of [
      ['get', '/api/v1/todos'],
      ['post', '/api/v1/todos'],
      ['delete', '/api/v1/todos/completed'],
    ] as const) {
      const res = await request[method](url, { headers: { 'X-Todo-Client': 'web' } });
      expect(res.status(), `${method} ${url}`).toBe(401);
    }
  });
});
