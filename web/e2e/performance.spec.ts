import { expect, test } from '@playwright/test';
import { apiSeedMany, listHeading, rowFor, rows, signedInUser } from './support/app';

// NFR-003 AC-1: one user with 500 to-dos; each of list, filter, search, page, create, edit, complete and delete run
// 20 times; time from the action to the rendered result; 95th percentile at most 1 second (sdd §14).
const RUNS = 20;
const LIMIT_MS = 1000;

const p95 = (samples: number[]): number => {
  const sorted = [...samples].sort((a, b) => a - b);
  return sorted[Math.ceil(sorted.length * 0.95) - 1] ?? Number.NaN;
};

test('NFR-003 AC-1 with 500 to-dos, list, filter, search, page, create, edit, complete and delete each show their result within 1 second at the 95th percentile', async ({ page }) => {
  test.setTimeout(10 * 60_000);
  await signedInUser(page);
  await apiSeedMany(page.request, 500, (n) => `Perf ${String(n).padStart(3, '0')} item`);

  const samples: Record<string, number[]> = {};
  const timed = async (name: string, action: () => Promise<void>) => {
    const start = performance.now();
    await action();
    (samples[name] ??= []).push(performance.now() - start);
  };

  // list: the user action is going to the list from another screen (a client-side navigation, not a Vite dev-server
  // reload): open the first to-do, then follow "Back to my to-dos" and time it until the first page of rows shows
  await page.goto('/todos');
  await expect(rows(page)).toHaveCount(20);
  for (let i = 0; i < RUNS; i++) {
    const first = await rows(page).first().getByRole('link').first().innerText();
    await rows(page).first().getByRole('link', { name: first, exact: true }).click();
    await expect(page.getByRole('heading', { level: 1, name: first })).toBeVisible();
    await timed('list', async () => {
      await page.getByRole('link', { name: 'Back to my to-dos' }).click();
      await expect(rows(page)).toHaveCount(20);
    });
  }

  await expect(rows(page)).toHaveCount(20);
  for (let i = 0; i < RUNS; i++) {
    const status = i % 2 === 0 ? 'Completed' : 'All';
    await timed('filter', async () => {
      await page.getByRole('radio', { name: status }).check();
      // no to-do is completed yet: Completed shows the empty-match state, All shows the first page again
      if (status === 'Completed') await expect(page.getByRole('heading', { level: 2, name: 'No matching to-dos' })).toBeVisible();
      else await expect(rows(page)).toHaveCount(20);
    });
  }

  for (let i = 0; i < RUNS; i++) {
    const term = `Perf ${String(100 + i).padStart(3, '0')}`;
    await page.getByRole('searchbox', { name: /search to-dos/i }).fill(term);
    await timed('search', async () => {
      await page.getByRole('button', { name: 'Search' }).click();
      await expect(rows(page)).toHaveCount(1);
      await expect(rows(page).first()).toContainText(term);
    });
  }
  await page.getByRole('searchbox', { name: /search to-dos/i }).fill('');
  await page.getByRole('button', { name: 'Search' }).click();
  await expect(rows(page)).toHaveCount(20);

  // page: activate the Next and Previous pagination links, alternating between pages 2 and 1
  for (let i = 0; i < RUNS; i++) {
    const forward = i % 2 === 0;
    await timed('page', async () => {
      await page.getByRole('navigation', { name: 'Pagination' }).getByRole('link', { name: forward ? 'Next' : 'Previous' }).click();
      await expect(page.getByText(forward ? /Showing 21-40/ : /Showing 1-20/)).toBeVisible();
    });
  }

  for (let i = 0; i < RUNS; i++) {
    const title = `Created ${i}`;
    await page.goto('/todos/new');
    await page.getByLabel(/^title/i).fill(title);
    await page.getByLabel(/^due date/i).fill('2026-11-06');
    await timed('create', async () => {
      await page.getByRole('button', { name: 'Add to-do' }).click();
      await expect(rows(page).first()).toContainText(title);
    });
  }

  for (let i = 0; i < RUNS; i++) {
    const title = `Edited ${i}`;
    await page.goto('/todos');
    await rows(page).first().getByRole('link', { name: /^Edit / }).click();
    await page.getByLabel(/^title/i).fill(title);
    await timed('edit', async () => {
      await page.getByRole('button', { name: 'Save changes' }).click();
      await expect(page.getByRole('heading', { level: 1, name: title })).toBeVisible();
    });
  }

  await page.goto('/todos');
  for (let i = 0; i < RUNS; i++) {
    const row = rows(page).nth(i);
    const title = await row.getByRole('link').first().innerText();
    await timed('complete', async () => {
      await row.getByRole('checkbox').check();
      // an optimistic checkbox is checked at once, so stop the clock at the confirmation (the live region), after the server answered
      await expect(page.getByText(`Marked done: ${title}.`)).toBeVisible();
    });
  }

  for (let i = 0; i < RUNS; i++) {
    await page.goto('/todos');
    const title = await rows(page).first().getByRole('link').first().innerText();
    await rows(page).first().getByRole('button', { name: /delete/i }).click();
    await timed('delete', async () => {
      await page.getByRole('dialog').getByRole('button', { name: 'Delete' }).click();
      await expect(rowFor(page, title)).toHaveCount(0);
    });
  }
  await expect(listHeading(page)).toBeVisible();

  const report = Object.fromEntries(Object.entries(samples).map(([k, v]) => [k, Math.round(p95(v))]));
  test.info().annotations.push({ type: 'p95-ms', description: JSON.stringify(report) });
  for (const name of ['list', 'filter', 'search', 'page', 'create', 'edit', 'complete', 'delete']) {
    expect(samples[name], `${name} was measured`).toHaveLength(RUNS);
    expect(p95(samples[name] ?? []), `${name} p95 (ms)`).toBeLessThanOrEqual(LIMIT_MS);
  }
});
