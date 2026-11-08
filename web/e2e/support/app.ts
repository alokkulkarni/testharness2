import { expect, type APIRequestContext, type Page } from '@playwright/test';
import { randomUUID } from 'node:crypto';

// Helpers shared by the end-to-end specs. Each test makes its own account, so tests are independent and
// parallel-safe (no shared rows, no fixed identifiers).
export interface Account {
  identifier: string;
  password: string;
}
export interface TodoSeed {
  title: string;
  description?: string | null;
  dueDate?: string;
  priority?: 'LOW' | 'MEDIUM' | 'HIGH';
}
export interface TodoJson {
  id: string;
  title: string;
  description: string | null;
  dueDate: string;
  priority: string;
  done: boolean;
  createdAt: string;
  updatedAt: string;
}

const CLIENT = { 'X-Todo-Client': 'web' };

export function newAccount(): Account {
  return { identifier: `e2e${randomUUID().replace(/-/g, '').slice(0, 14)}`, password: 'e2e-Password-123' };
}

export async function apiSignUp(request: APIRequestContext, account: Account): Promise<void> {
  const res = await request.post('/api/v1/auth/signup', { headers: CLIENT, data: account });
  expect(res.status()).toBe(201);
}

export async function apiLogIn(request: APIRequestContext, account: Account): Promise<void> {
  const res = await request.post('/api/v1/auth/login', { headers: CLIENT, data: account });
  expect(res.status()).toBe(200);
}

/** Creates an account and signs the page's browser context in, through the API (fast set-up, not the unit under test). */
export async function signedInUser(page: Page): Promise<Account> {
  const account = newAccount();
  await apiSignUp(page.request, account);
  await apiLogIn(page.request, account);
  return account;
}

export async function apiCreateTodo(request: APIRequestContext, seed: TodoSeed): Promise<TodoJson> {
  const res = await request.post('/api/v1/todos', {
    headers: CLIENT,
    data: { dueDate: '2026-11-06', priority: 'MEDIUM', ...seed },
  });
  expect(res.status()).toBe(201);
  return (await res.json()) as TodoJson;
}

export async function apiMarkDone(request: APIRequestContext, id: string): Promise<void> {
  const res = await request.put(`/api/v1/todos/${id}/done`, { headers: CLIENT, data: { done: true } });
  expect(res.status()).toBe(200);
}

export async function apiSeedMany(request: APIRequestContext, count: number, titleOf: (n: number) => string): Promise<void> {
  for (let n = 1; n <= count; n++) await apiCreateTodo(request, { title: titleOf(n) });
}

export async function uiLogIn(page: Page, account: Account): Promise<void> {
  await page.getByLabel(/email address or username/i).fill(account.identifier);
  await page.getByLabel(/^password/i).fill(account.password);
  await page.getByRole('button', { name: 'Log in' }).click();
}

export const listHeading = (page: Page) => page.getByRole('heading', { level: 1, name: 'My to-dos' });
/** The to-do rows of the list: list items that carry the done checkbox. */
export const rows = (page: Page) => page.getByRole('listitem').filter({ has: page.getByRole('checkbox') });
export const rowFor = (page: Page, title: string) => rows(page).filter({ has: page.getByRole('link', { name: title, exact: true }) });
