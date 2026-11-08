import { expect, test, type Route } from '@playwright/test';
import { apiSeedMany, listHeading, rows, signedInUser } from './support/app';

const isListRequest = (url: URL, method: string) => url.pathname === '/api/v1/todos' && method === 'GET';

test('NFR-006 AC-1 with the list request aborted, the list screen shows a friendly message with its next step; after the interception is removed, trying again loads the list', async ({ page }) => {
  await signedInUser(page);
  await apiSeedMany(page.request, 2, (n) => `State ${n}`);
  const abort = (route: Route) => route.abort('connectionrefused');
  await page.route((url) => url.pathname === '/api/v1/todos', async (route) => {
    if (isListRequest(new URL(route.request().url()), route.request().method())) await abort(route);
    else await route.continue();
  });

  await page.goto('/todos');
  const alert = page.getByRole('alert');
  await expect(alert).toBeVisible();
  await expect(alert).toContainText(/could not be loaded/i);
  await expect(alert).toContainText(/try again/i);
  await expect(alert).not.toContainText(/failed to fetch|networkerror|TypeError/i);

  await page.unroute((url) => url.pathname === '/api/v1/todos');
  await page.getByRole('button', { name: 'Retry' }).click();
  await expect(rows(page)).toHaveCount(2);
  await expect(page.getByRole('alert')).toHaveCount(0);
});

test('NFR-006 AC-2 a 500 problem response with internals in its body shows none of them to the user', async ({ page }) => {
  await signedInUser(page);
  await page.route(
    (url) => url.pathname === '/api/v1/todos',
    (route) =>
      route.request().method() === 'GET'
        ? route.fulfill({
            status: 500,
            contentType: 'application/problem+json',
            json: {
              type: 'urn:todo:problem:internal-error',
              title: 'SENTINEL-TITLE java.lang.NullPointerException',
              status: 500,
              detail: 'SENTINEL-DETAIL select * from todos',
              code: 'INTERNAL_ERROR',
              errors: [],
            },
          })
        : route.continue(),
  );
  await page.goto('/todos');
  await expect(page.getByRole('alert')).toContainText(/could not be loaded/i);
  const body = await page.locator('body').innerText();
  for (const leak of ['SENTINEL', 'NullPointer', 'select *', 'INTERNAL_ERROR', '500']) expect(body).not.toContain(leak);
});

test('REQ-LIST-003 AC-6 a page number past the last in the URL shows the last valid page without an error', async ({ page }) => {
  await signedInUser(page);
  await apiSeedMany(page.request, 25, (n) => `Row ${n}`);
  await page.goto('/todos?page=99');
  await expect(rows(page)).toHaveCount(5);
  await expect(page).toHaveURL(/page=2/);
  await expect(page.getByRole('alert')).toHaveCount(0);
});

test('REQ-LIST-001 AC-5 an unknown status in the URL is removed and the list shows All', async ({ page }) => {
  await signedInUser(page);
  await apiSeedMany(page.request, 2, (n) => `Any ${n}`);
  await page.goto('/todos?status=bogus');
  await expect(listHeading(page)).toBeVisible();
  await expect(rows(page)).toHaveCount(2);
  await expect(page).not.toHaveURL(/bogus/);
  await expect(page.getByRole('radio', { name: 'All' })).toBeChecked();
});

test('REQ-TODO-003 AC-2 a user with no to-dos sees the first-use empty state', async ({ page }) => {
  await signedInUser(page);
  await page.goto('/todos');
  await expect(page.getByRole('heading', { level: 2, name: 'You have no to-dos yet' })).toBeVisible();
  await expect(page.getByRole('button', { name: /clear all completed/i })).toBeDisabled();
});
