import { expect, test } from '@playwright/test';
import { apiCreateTodo, apiLogIn, apiSignUp, listHeading, newAccount, rowFor, uiLogIn } from './support/app';
import { restartApi } from './support/api-process';
import { mainLaunch } from './support/launch';

// Runs alone, in its own configuration (e2e/playwright.restart.config.ts, run by `pnpm -C web e2e` after the main
// one), because it stops and starts the API process. A separate run, not a project depending on the others, so a
// failure elsewhere can never make Playwright skip it. Needs the API process started by e2e/global-setup.ts
// (not E2E_EXTERNAL_SERVERS=1).
test('REQ-ACC-001 AC-3 REQ-DATA-001 AC-1 after the API is restarted the first user logs in again and their to-dos, with all fields, are still there', async ({ page }) => {
  test.setTimeout(10 * 60_000);
  const account = newAccount();
  await apiSignUp(page.request, account);
  await apiLogIn(page.request, account);
  const created = await apiCreateTodo(page.request, {
    title: 'Outlives the restart',
    description: 'kept in SQLite',
    dueDate: '2027-02-03',
    priority: 'HIGH',
  });
  await page.request.put(`/api/v1/todos/${created.id}/done`, { headers: { 'X-Todo-Client': 'web' }, data: { done: true } });

  await restartApi(mainLaunch());

  await page.context().clearCookies();
  await page.goto('/login');
  await uiLogIn(page, account);
  await expect(listHeading(page)).toBeVisible();
  const row = rowFor(page, 'Outlives the restart');
  await expect(row).toBeVisible();
  await expect(row.getByRole('checkbox')).toBeChecked();
  await expect(row).toContainText(/due 3 feb 2027/i);
  await expect(row).toContainText(/priority: high/i);
  await row.getByRole('link', { name: 'Outlives the restart' }).click();
  await expect(page.getByText('kept in SQLite')).toBeVisible();
});
