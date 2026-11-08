import { expect, test } from '@playwright/test';
import { listHeading, newAccount, uiLogIn } from './support/app';

// NFR-002 AC-1 (sdd §14): the main configuration starts the API and the SPA with the documented commands and talks
// only to localhost: no cloud, hosted database or email service. The browser's requests for a whole session are
// recorded and every one of them must go to the local machine.
const LOCAL_HOSTS = new Set(['localhost', '127.0.0.1', '[::1]', '::1']);

test('NFR-002 AC-1 sign up, log in, add, edit, complete and delete send every browser request to localhost only', async ({ page }) => {
  const hosts = new Set<string>();
  page.on('request', (request) => {
    const url = new URL(request.url());
    if (url.protocol === 'http:' || url.protocol === 'https:' || url.protocol === 'ws:' || url.protocol === 'wss:') hosts.add(url.hostname);
  });

  const account = newAccount();
  await page.goto('/signup');
  await page.getByLabel(/email address or username/i).fill(account.identifier);
  await page.getByLabel(/^password/i).fill(account.password);
  await page.getByRole('button', { name: 'Sign up' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Log in' })).toBeVisible();
  await uiLogIn(page, account);
  await expect(listHeading(page)).toBeVisible();

  await page.getByRole('link', { name: 'Add a to-do' }).first().click();
  await page.getByLabel(/^title/i).fill('Local only');
  await page.getByLabel(/^due date/i).fill('2026-11-06');
  await page.getByRole('button', { name: 'Add to-do' }).click();
  await expect(page.getByRole('link', { name: 'Local only', exact: true })).toBeVisible();
  await page.getByRole('listitem').filter({ has: page.getByRole('checkbox') }).first().getByRole('checkbox').check();
  await expect(page.getByText('Marked done: Local only.')).toBeVisible();
  await page.getByRole('button', { name: 'Log out' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Log in' })).toBeVisible();

  expect(hosts.size).toBeGreaterThan(0);
  expect([...hosts].filter((host) => !LOCAL_HOSTS.has(host))).toEqual([]);
});
