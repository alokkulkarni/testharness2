import { expect, test, type Page } from '@playwright/test';
import { apiSignUp, listHeading, newAccount, signedInUser, uiLogIn } from './support/app';

interface RecordedAnimation {
  iterations: number;
  endTime: number;
}
declare global {
  interface Window {
    __notFoundAnimations?: RecordedAnimation[];
  }
}

/** Registered before navigation: records document.getAnimations() at the first `animationstart` (sdd §14). */
async function recordAnimationsAtStart(page: Page): Promise<void> {
  await page.addInitScript(() => {
    document.addEventListener(
      'animationstart',
      () => {
        window.__notFoundAnimations = document.getAnimations().map((a) => {
          const timing = a.effect?.getComputedTiming();
          return { iterations: Number(timing?.iterations ?? NaN), endTime: Number(timing?.endTime ?? NaN) };
        });
      },
      { once: true, capture: true },
    );
  });
}

test.describe('REQ-NAV-001 against the real stack', () => {
  test('REQ-NAV-001 AC-2 a fresh browser opening / lands on log in and, after logging in, on /todos', async ({ page }) => {
    const account = newAccount();
    await apiSignUp(page.request, account);
    await page.goto('/');
    await expect(page).toHaveURL(/\/login/);
    await expect(page.getByText(/session has ended|account created/i)).toHaveCount(0);
    await uiLogIn(page, account);
    await expect(page).toHaveURL(/\/todos$/);
    await expect(listHeading(page)).toBeVisible();
  });

  test('REQ-NAV-001 AC-1 a signed-in browser opening / with a query string lands on /todos with no query', async ({ page }) => {
    await signedInUser(page);
    await page.goto('/?status=completed&q=x&page=4');
    await expect(listHeading(page)).toBeVisible();
    await expect(page).toHaveURL(/\/todos$/);
    await expect(page.getByRole('radio', { name: 'All' })).toBeChecked();
  });

  for (const address of ['/nope', '/todos/abc/def', '/login/extra']) {
    test(`REQ-NAV-001 AC-8 a fresh browser opening ${address} lands on log in with no message and never sees the not-found page; after login it lands on /todos`, async ({ page }) => {
      const account = newAccount();
      await apiSignUp(page.request, account);
      await page.goto(address);
      await expect(page).toHaveURL(/\/login\?returnTo=/);
      await expect(page.getByRole('heading', { level: 1, name: 'Log in' })).toBeVisible();
      await expect(page.getByRole('heading', { name: /not found/i })).toHaveCount(0);
      await expect(page.getByText(/session has ended|account created/i)).toHaveCount(0);
      await uiLogIn(page, account);
      await expect(page).toHaveURL(/\/todos$/);
      await expect(page.getByRole('heading', { name: /not found/i })).toHaveCount(0);
    });
  }

  test('REQ-NAV-001 AC-8 a fresh browser opening /todos/not-a-uuid lands on log in and, after login, on /todos', async ({ page }) => {
    const account = newAccount();
    await apiSignUp(page.request, account);
    await page.goto('/todos/not-a-uuid');
    await expect(page).toHaveURL(/\/login\?returnTo=/);
    await uiLogIn(page, account);
    await expect(page).toHaveURL(/\/todos$/);
  });

  test('REQ-NAV-001 AC-3 AC-5 AC-6 signed in, /nope shows the not-found page whose animation plays once within 5 seconds, and the home link leads to /todos', async ({ page }) => {
    await signedInUser(page);
    await recordAnimationsAtStart(page);
    await page.goto('/nope');
    await expect(page.getByRole('heading', { level: 1, name: /not found/i })).toBeVisible();
    await expect(page).toHaveURL(/\/nope$/);

    await expect.poll(() => page.evaluate(() => window.__notFoundAnimations?.length ?? 0)).toBeGreaterThan(0);
    const animations = await page.evaluate(() => window.__notFoundAnimations ?? []);
    for (const a of animations) {
      expect(a.iterations).toBe(1);
      expect(a.endTime).toBeLessThanOrEqual(5000);
    }
    // the heading, sentence and link are usable from the first render, and the illustration is hidden from AT
    await expect(page.getByRole('link', { name: /home/i })).toBeVisible();

    await page.getByRole('link', { name: /home/i }).click();
    await expect(page).toHaveURL(/\/todos$/);
    await expect(listHeading(page)).toBeVisible();
  });

  test('REQ-NAV-001 AC-4 signed in, /todos/not-a-uuid and its /edit show the to-do not-found state, not the page-not-found page', async ({ page }) => {
    await signedInUser(page);
    for (const address of ['/todos/not-a-uuid', '/todos/not-a-uuid/edit', '/todos/a;b']) {
      await page.goto(address);
      await expect(page.getByRole('heading', { level: 1, name: 'To-do not found' })).toBeVisible();
      await expect(page.getByRole('link', { name: 'Back to my to-dos' })).toBeVisible();
    }
  });

  test('REQ-NAV-001 AC-7 the home link on the not-found page is reachable and operable by keyboard with visible focus', async ({ page }) => {
    await signedInUser(page);
    await page.goto('/nope');
    const home = page.getByRole('link', { name: /home/i });
    await expect(page.getByRole('heading', { level: 1, name: /not found/i })).toBeFocused();
    await page.keyboard.press('Tab');
    await expect(home).toBeFocused();
    const outline = await home.evaluate((el) => {
      const s = getComputedStyle(el);
      return { style: s.outlineStyle, width: parseFloat(s.outlineWidth) };
    });
    expect(outline.style).not.toBe('none');
    expect(outline.width).toBeGreaterThan(0);
    await page.keyboard.press('Enter');
    await expect(page).toHaveURL(/\/todos$/);
  });
});
