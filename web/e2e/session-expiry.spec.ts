import { expect, test } from '@playwright/test';
import { writeFileSync } from 'node:fs';
import { listHeading, signedInUser } from './support/app';
import { CLOCK_OFFSET_FILE } from './support/launch';

// Runs only under e2e/playwright.session-expiry.config.ts (sdd §11, ADR-0013): the API there uses the test clock.
test.afterEach(() => {
  writeFileSync(CLOCK_OFFSET_FILE, ''); // an empty file means no offset
});

test('REQ-AUTH-004 AC-4 REQ-AUTH-002 AC-4 after the 8-hour session ends, the next request sends the user to log in with the session-ended message', async ({ page }) => {
  await signedInUser(page);
  await page.goto('/todos');
  await expect(listHeading(page)).toBeVisible();

  writeFileSync(CLOCK_OFFSET_FILE, 'PT8H1M'); // the API's clock moves 8 h 1 min forward
  await page.getByRole('radio', { name: 'Active' }).check();

  await expect(page).toHaveURL(/\/login\?returnTo=/);
  await expect(page.getByRole('status').filter({ hasText: 'Your session has ended. Please log in again.' })).toBeVisible();
});

test('REQ-AUTH-002 AC-4 a session that has not reached 8 hours is still valid', async ({ page }) => {
  await signedInUser(page);
  writeFileSync(CLOCK_OFFSET_FILE, 'PT7H59M');
  await page.goto('/todos');
  await expect(listHeading(page)).toBeVisible();
});
