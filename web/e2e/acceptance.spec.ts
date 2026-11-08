import { expect, test, type Locator, type Page } from '@playwright/test';
import { apiCreateTodo, apiSeedMany, listHeading, newAccount, apiLogIn, apiSignUp, rowFor, rows, signedInUser } from './support/app';

/** True when the Previous/Next control is not an enabled link or button (disabled, aria-disabled or absent as a control). */
async function pagerControlDisabled(label: Locator): Promise<boolean> {
  return label.evaluate((el) => {
    const control = el.closest('a, button');
    return !control || control.hasAttribute('disabled') || control.getAttribute('aria-disabled') === 'true';
  });
}

async function addTodo(page: Page, title: string, opts: { description?: string; dueDate?: string; priority?: 'Low' | 'Medium' | 'High' } = {}) {
  await page.goto('/todos/new');
  await page.getByLabel(/^title/i).fill(title);
  if (opts.description) await page.getByLabel(/^description/i).fill(opts.description);
  await page.getByLabel(/^due date/i).fill(opts.dueDate ?? '2026-11-06');
  if (opts.priority) await page.getByLabel(/^priority/i).selectOption({ label: opts.priority });
  await page.getByRole('button', { name: 'Add to-do' }).click();
  await expect(listHeading(page)).toBeVisible();
}

test.describe('REQ-ACC-001 overall success scenario', () => {
  test('REQ-ACC-001 AC-1 a signed-in user adds, sees, searches, filters, edits, completes and deletes their own to-dos', async ({ page }) => {
    await signedInUser(page);

    // add (REQ-TODO-001 AC-1, AC-3): the new to-do appears first
    await addTodo(page, 'Buy Milk', { description: 'two litres', priority: 'High' });
    await addTodo(page, 'Walk the dog');
    await expect(rows(page)).toHaveCount(2);
    await expect(rows(page).first()).toContainText('Walk the dog');
    await expect(rowFor(page, 'Buy Milk')).toContainText(/priority: high/i);
    await expect(rowFor(page, 'Buy Milk')).toContainText(/due 6 nov 2026/i);

    // see (REQ-TODO-003 AC-3)
    await page.getByRole('link', { name: 'Buy Milk', exact: true }).click();
    await expect(page.getByRole('heading', { level: 1, name: 'Buy Milk' })).toBeVisible();
    await expect(page.getByText('two litres')).toBeVisible();
    await page.getByRole('link', { name: 'Back to my to-dos' }).click();

    // search (REQ-LIST-002 AC-1): case-insensitive contains
    await page.getByRole('searchbox', { name: /search to-dos/i }).fill('milk');
    await page.getByRole('button', { name: 'Search' }).click();
    await expect(rows(page)).toHaveCount(1);
    await expect(rows(page).first()).toContainText('Buy Milk');
    await page.getByRole('searchbox', { name: /search to-dos/i }).fill('');
    await page.getByRole('button', { name: 'Search' }).click();
    await expect(rows(page)).toHaveCount(2);

    // edit (REQ-TODO-004 AC-1)
    await rowFor(page, 'Buy Milk').getByRole('link', { name: 'Edit' }).click();
    await page.getByLabel(/^title/i).fill('Buy oat milk');
    await page.getByRole('button', { name: 'Save changes' }).click();
    await expect(page.getByRole('heading', { level: 1, name: 'Buy oat milk' })).toBeVisible();
    await page.getByRole('link', { name: 'Back to my to-dos' }).click();

    // complete (REQ-TODO-005 AC-1, AC-3) and filter (REQ-LIST-001)
    await rowFor(page, 'Buy oat milk').getByRole('checkbox').check();
    await expect(rowFor(page, 'Buy oat milk').getByRole('checkbox')).toBeChecked();
    await page.getByRole('radio', { name: 'Active' }).check();
    await expect(rows(page)).toHaveCount(1);
    await expect(rows(page).first()).toContainText('Walk the dog');
    await page.getByRole('radio', { name: 'Completed' }).check();
    await expect(rows(page)).toHaveCount(1);
    await expect(rows(page).first()).toContainText('Buy oat milk');

    // delete (REQ-TODO-006 AC-1..AC-3)
    await rowFor(page, 'Buy oat milk').getByRole('button', { name: /delete/i }).click();
    const dialog = page.getByRole('dialog', { name: 'Delete this to-do?' });
    await expect(dialog).toBeVisible();
    await dialog.getByRole('button', { name: 'Delete' }).click();
    await expect(dialog).toBeHidden();
    await expect(page.getByRole('heading', { level: 2, name: 'No matching to-dos' })).toBeVisible();
    await page.getByRole('radio', { name: 'All' }).check();
    await expect(rows(page)).toHaveCount(1);
  });

  test('REQ-ACC-001 AC-2 REQ-ISO-001 AC-1 AC-2 AC-4 a second user cannot see or change the first user\'s to-dos by list, search or direct URL', async ({ page, browser, baseURL }) => {
    await signedInUser(page);
    const secret = await apiCreateTodo(page.request, { title: 'Quarterly-Secret-Plan' });

    const other = await browser.newContext({ baseURL });
    const otherPage = await other.newPage();
    const account = newAccount();
    await apiSignUp(otherPage.request, account);
    await apiLogIn(otherPage.request, account);
    await apiCreateTodo(otherPage.request, { title: 'Mine' });

    await otherPage.goto('/todos');
    await expect(rows(otherPage)).toHaveCount(1);
    await expect(otherPage.getByText('Quarterly-Secret-Plan')).toHaveCount(0);

    await otherPage.getByRole('searchbox', { name: /search to-dos/i }).fill('Quarterly-Secret');
    await otherPage.getByRole('button', { name: 'Search' }).click();
    await expect(otherPage.getByRole('heading', { level: 2, name: 'No matching to-dos' })).toBeVisible();

    await otherPage.goto(`/todos/${secret.id}`);
    await expect(otherPage.getByRole('heading', { level: 1, name: 'To-do not found' })).toBeVisible();
    await otherPage.goto(`/todos/${secret.id}/edit`);
    await expect(otherPage.getByRole('heading', { level: 1, name: 'To-do not found' })).toBeVisible();

    const headers = { 'X-Todo-Client': 'web' };
    for (const res of [
      await otherPage.request.put(`/api/v1/todos/${secret.id}/done`, { headers, data: { done: true } }),
      await otherPage.request.delete(`/api/v1/todos/${secret.id}`, { headers }),
    ]) {
      expect(res.status()).toBe(404);
    }
    const stillThere = await page.request.get(`/api/v1/todos/${secret.id}`);
    expect(stillThere.status()).toBe(200);
    expect((await stillThere.json()).done).toBe(false);
    await other.close();
  });
});

test.describe('persistence and list flows', () => {
  test('REQ-TODO-001 AC-4 a created to-do is still there after the page is reloaded', async ({ page }) => {
    await signedInUser(page);
    await addTodo(page, 'Survives reload');
    await page.reload();
    await expect(rowFor(page, 'Survives reload')).toBeVisible();
  });

  test('REQ-LIST-003 AC-1 AC-2 AC-3 AC-5 paging 45 to-dos: 20, 20, 5 with no repeat or gap, Previous and Next disabled at the ends', async ({ page }) => {
    await signedInUser(page);
    await apiSeedMany(page.request, 45, (n) => `Item ${String(n).padStart(2, '0')}`);
    await page.goto('/todos');
    const pagination = page.getByRole('navigation', { name: 'Pagination' });
    await expect(rows(page)).toHaveCount(20);
    await expect(page.getByText('Showing 1-20 of 45')).toBeVisible();
    const seen: string[] = [];
    const titlesOnPage = async () =>
      (await rows(page).getByRole('link').evaluateAll((els) => els.map((e) => e.textContent ?? ''))).filter((t) => t.startsWith('Item'));
    seen.push(...(await titlesOnPage()));
    expect(await pagerControlDisabled(pagination.getByText(/previous/i))).toBe(true);
    expect(await pagerControlDisabled(pagination.getByText(/next/i))).toBe(false);
    await pagination.getByText(/next/i).click();
    await expect(page).toHaveURL(/page=2/);
    await expect(rows(page)).toHaveCount(20);
    seen.push(...(await titlesOnPage()));
    await pagination.getByRole('link', { name: '3' }).click();
    await expect(rows(page)).toHaveCount(5);
    seen.push(...(await titlesOnPage()));
    expect(seen).toEqual(Array.from({ length: 45 }, (_, i) => `Item ${String(45 - i).padStart(2, '0')}`));
    expect(await pagerControlDisabled(pagination.getByText(/next/i))).toBe(true);
    expect(await pagerControlDisabled(pagination.getByText(/previous/i))).toBe(false);

    await page.getByRole('radio', { name: 'Active' }).check();
    await expect(page).not.toHaveURL(/page=/);
  });

  test('REQ-TODO-005 AC-3 marking a to-do done under the Active filter removes the row without a reload', async ({ page }) => {
    await signedInUser(page);
    await apiSeedMany(page.request, 3, (n) => `Task ${n}`);
    await page.goto('/todos?status=active');
    await expect(rows(page)).toHaveCount(3);
    await page.evaluate(() => {
      (window as unknown as { __noReload: boolean }).__noReload = true; // lost if the page reloads
    });
    await rowFor(page, 'Task 2').getByRole('checkbox').check();
    await expect(rows(page)).toHaveCount(2);
    await expect(page.getByRole('link', { name: 'Task 2', exact: true })).toHaveCount(0);
    expect(await page.evaluate(() => (window as unknown as { __noReload?: boolean }).__noReload)).toBe(true);
  });

  test('REQ-TODO-006 AC-5 deleting the only to-do on the last page shows the previous page, not an error', async ({ page }) => {
    await signedInUser(page);
    await apiSeedMany(page.request, 21, (n) => `Entry ${String(n).padStart(2, '0')}`);
    await page.goto('/todos?page=2');
    await expect(rows(page)).toHaveCount(1);
    await rows(page).first().getByRole('button', { name: /delete/i }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Delete' }).click();
    await expect(rows(page)).toHaveCount(20);
    await expect(page.getByRole('alert')).toHaveCount(0);
  });

  test('REQ-TODO-007 AC-1 AC-2 clear all completed states the count, deletes completed across pages, keeps active', async ({ page }) => {
    await signedInUser(page);
    await apiSeedMany(page.request, 3, (n) => `Job ${n}`);
    await page.goto('/todos');
    await rowFor(page, 'Job 1').getByRole('checkbox').check();
    await rowFor(page, 'Job 2').getByRole('checkbox').check();
    await page.getByRole('button', { name: /clear all completed/i }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText(
      'Delete all 2 completed to-dos? This deletes all your completed to-dos, including any not shown because of the current page, filter or search. This cannot be undone.',
    );
    await dialog.getByRole('button', { name: 'Cancel' }).click();
    await expect(rows(page)).toHaveCount(3);
    await page.getByRole('button', { name: /clear all completed/i }).click();
    await page.getByRole('dialog').getByRole('button', { name: /delete 2 completed/i }).click();
    await expect(rows(page)).toHaveCount(1);
    await expect(rows(page).first()).toContainText('Job 3');
    await expect(page.getByRole('button', { name: /clear all completed/i })).toBeDisabled();
  });

  test('REQ-TODO-002 AC-7 a due date shows as the same calendar date in any time zone', async ({ browser, baseURL }) => {
    for (const timezoneId of ['Pacific/Kiritimati', 'Pacific/Pago_Pago']) {
      const context = await browser.newContext({ baseURL, timezoneId });
      const page = await context.newPage();
      await signedInUser(page);
      await apiCreateTodo(page.request, { title: 'Zone check', dueDate: '2026-11-06' });
      await page.goto('/todos');
      await expect(rowFor(page, 'Zone check')).toContainText(/due 6 nov 2026/i);
      await context.close();
    }
  });
});
