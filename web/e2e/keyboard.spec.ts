import { expect, test } from '@playwright/test';
import { apiCreateTodo, apiSeedMany, listHeading, newAccount, pressTab, rowFor, rows, signedInUser } from './support/app';

// NFR-001 AC-2: every action is reachable and operable with the keyboard alone, focus is visible, and focus moves
// into and out of confirmation dialogs. These tests use page.keyboard only (no clicks) after the page has loaded.

// Limit derived from the page size: a full page is 20 rows (sdd.md) at about 4 tab stops each (checkbox, title link,
// Edit link, Delete button) = 80, plus about 20 stops of page chrome (header, filters, search, pagination links),
// plus a margin: 150. It only bounds a failing search; a reachable control returns as soon as it is focused.
const MAX_TAB_STOPS = 150;

async function tabTo(page: import('@playwright/test').Page, target: import('@playwright/test').Locator, max = MAX_TAB_STOPS) {
  for (let i = 0; i < max; i++) {
    if (await target.evaluate((el) => el === document.activeElement)) return;
    await pressTab(page);
  }
  throw new Error('Could not reach the control with Tab');
}

test('NFR-001 AC-2 sign up and log in with the keyboard only', async ({ page }) => {
  const account = newAccount();
  await page.goto('/signup');
  await expect(page.getByRole('heading', { level: 1, name: 'Sign up' })).toBeFocused();
  await tabTo(page, page.getByLabel(/email address or username/i));
  await page.keyboard.type(account.identifier);
  await page.keyboard.press('Tab');
  await page.keyboard.type(account.password);
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole('heading', { level: 1, name: 'Log in' })).toBeFocused();
  await tabTo(page, page.getByLabel(/email address or username/i));
  await page.keyboard.type(account.identifier);
  await page.keyboard.press('Tab');
  await page.keyboard.type(account.password);
  await page.keyboard.press('Enter');
  await expect(listHeading(page)).toBeVisible();
});

test('NFR-001 AC-2 create, search, filter, complete and page with the keyboard only', async ({ page }) => {
  await signedInUser(page);
  await apiSeedMany(page.request, 22, (n) => `Key ${String(n).padStart(2, '0')}`);
  await page.goto('/todos/new');
  await tabTo(page, page.getByLabel(/^title/i));
  await page.keyboard.type('Typed by keyboard');
  await tabTo(page, page.getByLabel(/^due date/i));
  // typing digits into a date input depends on the browser engine and locale, and is not the point here
  await page.getByLabel(/^due date/i).fill('2026-11-06');
  await page.keyboard.press('Enter');
  await expect(listHeading(page)).toBeVisible();
  await expect(rows(page).first()).toContainText('Typed by keyboard');

  await tabTo(page, page.getByRole('searchbox', { name: /search to-dos/i }));
  await page.keyboard.type('keyboard');
  await page.keyboard.press('Enter');
  await expect(rows(page)).toHaveCount(1);
  await page.getByRole('searchbox', { name: /search to-dos/i }).fill('');
  await page.keyboard.press('Enter');
  await expect(rows(page)).toHaveCount(20);

  await tabTo(page, page.getByRole('radio', { name: 'All' }));
  await page.keyboard.press('ArrowRight'); // radio group by arrow keys: All -> Active
  await expect(page.getByRole('radio', { name: 'Active' })).toBeChecked();
  await expect(page.getByRole('radio', { name: 'Active' })).toBeFocused();

  await tabTo(page, rows(page).first().getByRole('checkbox'));
  await page.keyboard.press('Space');
  await expect(rows(page)).toHaveCount(20); // the marked row left the Active view and the next one filled in
  await expect(page.getByText(/^Marked done: /)).toBeVisible();

  await tabTo(page, page.getByRole('navigation', { name: 'Pagination' }).getByText(/next/i));
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/page=2/);
  await expect(listHeading(page)).toBeFocused();
});

test('NFR-001 AC-2 the delete dialog takes focus on Cancel, keeps it inside, and Escape returns focus to the Delete button', async ({ page }) => {
  await signedInUser(page);
  await apiSeedMany(page.request, 2, (n) => `Dialog ${n}`);
  await page.goto('/todos');
  const trigger = rowFor(page, 'Dialog 2').getByRole('button', { name: /delete/i });
  await tabTo(page, trigger);
  await page.keyboard.press('Enter');
  const dialog = page.getByRole('dialog', { name: 'Delete this to-do?' });
  await expect(dialog).toBeVisible();
  await expect(dialog.getByRole('button', { name: 'Cancel' })).toBeFocused();
  await page.keyboard.press('Tab');
  await expect(dialog.getByRole('button', { name: 'Delete' })).toBeFocused();
  await page.keyboard.press('Tab');
  await expect(dialog.getByRole('button', { name: 'Cancel' })).toBeFocused(); // focus stays inside the modal
  await page.keyboard.press('Escape');
  await expect(dialog).toBeHidden();
  await expect(trigger).toBeFocused();
  await expect(rows(page)).toHaveCount(2); // Escape deleted nothing (REQ-TODO-006 AC-2)
});

test('NFR-001 AC-2 confirming deletion by keyboard moves focus to the list heading', async ({ page }) => {
  await signedInUser(page);
  await apiSeedMany(page.request, 2, (n) => `Gone ${n}`);
  await page.goto('/todos');
  await tabTo(page, rowFor(page, 'Gone 2').getByRole('button', { name: /delete/i }));
  await page.keyboard.press('Enter');
  await page.keyboard.press('Tab');
  await page.keyboard.press('Enter');
  await expect(rows(page)).toHaveCount(1);
  await expect(listHeading(page)).toBeFocused();
});

test('NFR-001 AC-2 the clear-completed dialog is operated and left by keyboard; focus returns to the control on cancel', async ({ page }) => {
  await signedInUser(page);
  const todo = await apiCreateTodo(page.request, { title: 'Done already' });
  await page.request.put(`/api/v1/todos/${todo.id}/done`, { headers: { 'X-Todo-Client': 'web' }, data: { done: true } });
  await page.goto('/todos');
  const trigger = page.getByRole('button', { name: /clear all completed/i });
  await tabTo(page, trigger);
  await page.keyboard.press('Enter');
  const dialog = page.getByRole('dialog');
  await expect(dialog.getByRole('button', { name: 'Cancel' })).toBeFocused();
  await page.keyboard.press('Escape');
  await expect(trigger).toBeFocused();
});

test('NFR-001 AC-2 the detail page is reached, read and left by keyboard (to edit and back to the list)', async ({ page }) => {
  await signedInUser(page);
  const todo = await apiCreateTodo(page.request, { title: 'Readable', description: 'Details here' });
  await page.goto('/todos');
  await tabTo(page, page.getByRole('link', { name: 'Readable', exact: true }));
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(new RegExp(`/todos/${todo.id}$`));
  await expect(page.getByRole('heading', { level: 1, name: 'Readable' })).toBeFocused();
  await tabTo(page, page.getByRole('link', { name: 'Edit' }));
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/\/edit$/);
  await tabTo(page, page.getByRole('link', { name: 'Cancel' }));
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(new RegExp(`/todos/${todo.id}$`));
  await tabTo(page, page.getByRole('link', { name: 'Back to my to-dos' }));
  await page.keyboard.press('Enter');
  await expect(listHeading(page)).toBeVisible();
});

test('NFR-001 AC-2 a to-do is edited and saved by keyboard; the detail page shows the new title and focus is on its heading', async ({ page }) => {
  await signedInUser(page);
  const todo = await apiCreateTodo(page.request, { title: 'Before keyboard edit' });
  await page.goto(`/todos/${todo.id}/edit`);
  await tabTo(page, page.getByLabel(/^title/i));
  await page.keyboard.press('ControlOrMeta+A');
  await page.keyboard.type('After keyboard edit');
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(new RegExp(`/todos/${todo.id}$`));
  await expect(page.getByRole('heading', { level: 1, name: 'After keyboard edit' })).toBeFocused();
});

test('NFR-001 AC-2 keyboard focus is always visible on the list controls', async ({ page }) => {
  await signedInUser(page);
  await apiSeedMany(page.request, 1, (n) => `Focus ${n}`);
  await page.goto('/todos');
  const focusables = [
    page.getByRole('link', { name: 'Add a to-do' }).first(),
    page.getByRole('searchbox', { name: /search to-dos/i }),
    page.getByRole('button', { name: 'Search' }),
    rows(page).first().getByRole('checkbox'),
    rows(page).first().getByRole('button', { name: /delete/i }),
  ];
  for (const control of focusables) {
    await tabTo(page, control);
    const visible = await control.evaluate((el) => {
      const s = getComputedStyle(el);
      return (s.outlineStyle !== 'none' && parseFloat(s.outlineWidth) > 0) || s.boxShadow !== 'none';
    });
    expect(visible).toBe(true);
  }
});
