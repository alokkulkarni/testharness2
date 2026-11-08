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

  for (let i = 0; i < RUNS; i++) {
    await timed('list', async () => {
      await page.goto('/todos');
      await expect(rows(page)).toHaveCount(20);
    });
  }

  await page.goto('/todos');
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
  await page.goto('/todos');

  for (let i = 0; i < RUNS; i++) {
    const target = (i % 25) + 1;
    await timed('page', async () => {
      await page.goto(`/todos?page=${target}`);
      await expect(page.getByText(new RegExp(`Showing ${(target - 1) * 20 + 1}-`))).toBeVisible();
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
    await rows(page).first().getByRole('link', { name: 'Edit' }).click();
    await page.getByLabel(/^title/i).fill(title);
    await timed('edit', async () => {
      await page.getByRole('button', { name: 'Save changes' }).click();
      await expect(page.getByRole('heading', { level: 1, name: title })).toBeVisible();
    });
  }

  await page.goto('/todos');
  for (let i = 0; i < RUNS; i++) {
    const box = rows(page).nth(i).getByRole('checkbox');
    await timed('complete', async () => {
      await box.check();
      await expect(box).toBeChecked();
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
