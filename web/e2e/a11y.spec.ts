import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { apiCreateTodo, apiSeedMany, newAccount, rowFor, signedInUser } from './support/app';

// NFR-001: WCAG 2.1 Level AA (Q44). Real browser, so colour contrast runs here (it cannot in jsdom).
const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'];

async function expectNoViolations(page: Page): Promise<void> {
  const results = await new AxeBuilder({ page }).withTags(TAGS).analyze();
  expect(results.violations.map((v) => `${v.id}: ${v.help} [${v.nodes.map((n) => n.target.join(' ')).join(' | ')}]`)).toEqual([]);
}

test.describe('NFR-001 AC-1 axe on the eight audited screens (WCAG 2.1 A and AA)', () => {
  test('NFR-001 AC-1 sign-up has no violations, with and without errors', async ({ page }) => {
    await page.goto('/signup');
    await expect(page.getByRole('heading', { level: 1, name: 'Sign up' })).toBeVisible();
    await expectNoViolations(page);
    await page.getByRole('button', { name: 'Sign up' }).click();
    await expect(page.getByRole('alert')).toBeVisible();
    await expectNoViolations(page);
  });

  test('NFR-001 AC-1 log in has no violations, with the account-created message and after a refused attempt', async ({ page }) => {
    const account = newAccount();
    await page.goto('/signup');
    await page.getByLabel(/email address or username/i).fill(account.identifier);
    await page.getByLabel(/^password/i).fill(account.password);
    await page.getByRole('button', { name: 'Sign up' }).click();
    await expect(page.getByRole('status').filter({ hasText: /account created/i })).toBeVisible();
    await expectNoViolations(page);
    await page.getByLabel(/email address or username/i).fill(account.identifier);
    await page.getByLabel(/^password/i).fill('wrong-password');
    await page.getByRole('button', { name: 'Log in' }).click();
    await expect(page.getByRole('alert')).toBeVisible();
    await expectNoViolations(page);
  });

  test('NFR-001 AC-1 the list has no violations: empty, populated with pagination, and no matches', async ({ page }) => {
    await signedInUser(page);
    await page.goto('/todos');
    await expect(page.getByRole('heading', { level: 2, name: 'You have no to-dos yet' })).toBeVisible();
    await expectNoViolations(page);
    await apiSeedMany(page.request, 25, (n) => `Axe ${n}`);
    const done = await apiCreateTodo(page.request, { title: 'Axe done', priority: 'HIGH' });
    await page.request.put(`/api/v1/todos/${done.id}/done`, { headers: { 'X-Todo-Client': 'web' }, data: { done: true } });
    await page.goto('/todos');
    await expect(page.getByRole('navigation', { name: 'Pagination' })).toBeVisible();
    await expectNoViolations(page);
    await page.getByRole('searchbox', { name: /search to-dos/i }).fill('no-such-text');
    await page.getByRole('button', { name: 'Search' }).click();
    await expect(page.getByRole('heading', { level: 2, name: 'No matching to-dos' })).toBeVisible();
    await expectNoViolations(page);
  });

  test('NFR-001 AC-1 the create and edit form has no violations, with and without errors', async ({ page }) => {
    await signedInUser(page);
    await page.goto('/todos/new');
    await expect(page.getByRole('heading', { level: 1, name: 'Add a to-do' })).toBeVisible();
    await expectNoViolations(page);
    await page.getByRole('button', { name: 'Add to-do' }).click();
    await expect(page.getByRole('alert')).toBeVisible();
    await expectNoViolations(page);
    const todo = await apiCreateTodo(page.request, { title: 'Edit me', description: 'notes' });
    await page.goto(`/todos/${todo.id}/edit`);
    await expect(page.getByRole('heading', { level: 1, name: 'Edit to-do' })).toBeVisible();
    await expectNoViolations(page);
  });

  test('NFR-001 AC-1 the to-do detail page and its not-found state have no violations', async ({ page }) => {
    await signedInUser(page);
    const todo = await apiCreateTodo(page.request, { title: 'Detail axe', description: 'line one\nline two' });
    await page.goto(`/todos/${todo.id}`);
    await expect(page.getByRole('heading', { level: 1, name: 'Detail axe' })).toBeVisible();
    await expectNoViolations(page);
    await page.goto('/todos/not-a-uuid');
    await expect(page.getByRole('heading', { level: 1, name: 'To-do not found' })).toBeVisible();
    await expectNoViolations(page);
  });

  test('NFR-001 AC-1 the delete confirmation dialog has no violations', async ({ page }) => {
    await signedInUser(page);
    await apiSeedMany(page.request, 2, (n) => `Dialog axe ${n}`);
    await page.goto('/todos');
    await rowFor(page, 'Dialog axe 2').getByRole('button', { name: /delete/i }).click();
    await expect(page.getByRole('dialog', { name: 'Delete this to-do?' })).toBeVisible();
    await expectNoViolations(page);
  });

  test('NFR-001 AC-1 the clear-completed confirmation dialog has no violations', async ({ page }) => {
    await signedInUser(page);
    const todo = await apiCreateTodo(page.request, { title: 'Completed axe' });
    await page.request.put(`/api/v1/todos/${todo.id}/done`, { headers: { 'X-Todo-Client': 'web' }, data: { done: true } });
    await page.goto('/todos');
    await page.getByRole('button', { name: /clear all completed/i }).click();
    await expect(page.getByRole('dialog')).toBeVisible();
    await expectNoViolations(page);
  });

  test('REQ-NAV-001 AC-7 NFR-001 AC-1 the page-not-found page has no violations, while signed in', async ({ page }) => {
    await signedInUser(page);
    await page.goto('/nope');
    await expect(page.getByRole('heading', { level: 1, name: /not found/i })).toBeVisible();
    await expectNoViolations(page);
  });
});

test.describe('NFR-001 structure checks in a real browser', () => {
  test('NFR-001 AC-1 the page language is set and every route has a distinct title', async ({ page }) => {
    await page.goto('/signup');
    await expect(page.locator('html')).toHaveAttribute('lang', 'en');
    await expect(page).toHaveTitle('Sign up - To-do list');
    await page.goto('/login');
    await expect(page).toHaveTitle('Log in - To-do list');
  });

  test('NFR-001 AC-1 no horizontal scrolling at 320 CSS pixels wide on the list, form and not-found page (reflow, 1.4.10)', async ({ page }) => {
    await page.setViewportSize({ width: 320, height: 640 });
    await signedInUser(page);
    await apiCreateTodo(page.request, {
      title: 'A very long title that has to wrap on a narrow screen without forcing the page to scroll sideways',
      description: 'x'.repeat(300),
    });
    for (const [path, heading] of [
      ['/todos', 'My to-dos'],
      ['/todos/new', 'Add a to-do'],
    ] as const) {
      await page.goto(path);
      await expect(page.getByRole('heading', { level: 1, name: heading })).toBeVisible();
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
    }
    await page.goto('/nope');
    await expect(page.getByRole('heading', { level: 1, name: /not found/i })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
  });

  test('NFR-001 AC-3 a refused submit moves focus to the error summary and its link moves focus to the field', async ({ page }) => {
    await signedInUser(page);
    await page.goto('/todos/new');
    await page.getByRole('button', { name: 'Add to-do' }).click();
    const summary = page.getByRole('alert');
    await expect(summary).toBeVisible();
    await expect(summary.getByText('There is a problem')).toBeVisible();
    await summary.getByRole('link', { name: 'Enter a title.' }).click();
    await expect(page.getByLabel(/^title/i)).toBeFocused();
    await expect(page.getByLabel(/^title/i)).toHaveAttribute('aria-invalid', 'true');
  });

  test('REQ-NAV-001 AC-6 under prefers-reduced-motion the not-found page runs no animation', async ({ page }) => {
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await signedInUser(page);
    await page.goto('/nope');
    await expect(page.getByRole('heading', { level: 1, name: /not found/i })).toBeVisible();
    expect(await page.evaluate(() => document.getAnimations().filter((a) => a.playState === 'running').length)).toBe(0);
  });
});
