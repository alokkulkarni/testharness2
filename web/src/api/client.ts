import type { Failure } from '../errors/userMessage';
import type { LoginRequest, Session, SignupRequest, SignupResult } from './identity';
import type { ClearCompletedResult, Problem, StatusFilter, Todo, TodoCreate, TodoPage, TodoUpdate } from './todos';

// The one fetch wrapper (sdd §2, §6.1, ADR-0001). It turns every failed call into a `Failure` and never hands a
// raw Response, TypeError or exception text to a screen. Relative /api paths only; the dev server proxies them.

/** Thrown by every operation below; carries the Failure a screen maps with userMessageFor. */
export class ApiFailure extends Error {
  readonly failure: Failure;
  constructor(failure: Failure) {
    super(failure.kind);
    this.name = 'ApiFailure';
    this.failure = failure;
  }
}

/** Any thrown value as a Failure: an ApiFailure keeps its own, anything else is an error inside the SPA. */
export function toFailure(error: unknown): Failure {
  return error instanceof ApiFailure ? error.failure : { kind: 'unexpected' };
}

/** True for a 401 that the session layer has taken over (the session-ended flow); the call site shows nothing. */
export function isUnauthorized(error: unknown): boolean {
  return error instanceof ApiFailure && error.failure.kind === 'http' && error.failure.status === 401;
}

type UnauthorizedHandler = () => void;
let onUnauthorized: UnauthorizedHandler | null = null;

/** SessionProvider registers here to be told of every 401 except the probe's and logIn's (sdd §4.1). */
export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  onUnauthorized = handler;
}

function asProblem(value: unknown): Problem | undefined {
  if (typeof value !== 'object' || value === null) return undefined;
  const v = value as Record<string, unknown>;
  if (typeof v.code !== 'string' || typeof v.status !== 'number') return undefined;
  const errors = Array.isArray(v.errors)
    ? v.errors.filter(
        (e): e is Problem['errors'][number] =>
          typeof e === 'object' && e !== null && typeof (e as { field?: unknown }).field === 'string' && typeof (e as { message?: unknown }).message === 'string',
      )
    : [];
  return {
    type: typeof v.type === 'string' ? v.type : '',
    title: typeof v.title === 'string' ? v.title : '',
    status: v.status,
    code: v.code as Problem['code'],
    errors,
  };
}

interface RequestOptions {
  body?: unknown;
  /** Read and return the JSON body of a 2xx response. */
  json?: boolean;
  /** This call's 401 is not a session-ended signal (the probe, and logIn whose 401 means invalid credentials). */
  own401?: boolean;
}

async function request<T>(method: string, path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {};
  if (method !== 'GET') headers['X-Todo-Client'] = 'web';
  if (options.body !== undefined) headers['Content-Type'] = 'application/json';

  let response: Response;
  try {
    response = await fetch(path, {
      method,
      headers,
      ...(options.body !== undefined ? { body: JSON.stringify(options.body) } : {}),
    });
  } catch {
    throw new ApiFailure({ kind: 'network' });
  }

  if (!response.ok) {
    let problem: Problem | undefined;
    try {
      problem = asProblem(await response.json());
    } catch {
      problem = undefined;
    }
    if (response.status === 401 && !options.own401) onUnauthorized?.();
    throw new ApiFailure({ kind: 'http', status: response.status, ...(problem ? { problem } : {}) });
  }

  if (!options.json) return undefined as T;
  try {
    return (await response.json()) as T;
  } catch {
    // the response arrived but could not be read: an error inside the SPA, not a network failure
    throw new ApiFailure({ kind: 'unexpected' });
  }
}

// identity-api-v1.yaml
export const getSession = () => request<Session>('GET', '/api/v1/auth/session', { json: true, own401: true });
export const logIn = (body: LoginRequest) => request<Session>('POST', '/api/v1/auth/login', { body, json: true, own401: true });
export const signUp = (body: SignupRequest) => request<SignupResult>('POST', '/api/v1/auth/signup', { body, json: true });
export const logOut = () => request<void>('POST', '/api/v1/auth/logout');

// todo-api-v1.yaml
export interface ListQuery {
  status: StatusFilter;
  q: string;
  page: number;
}

export function listTodos(query: ListQuery): Promise<TodoPage> {
  const params = new URLSearchParams();
  if (query.status !== 'all') params.set('status', query.status);
  if (query.q !== '') params.set('q', query.q);
  if (query.page > 1) params.set('page', String(query.page));
  const qs = params.toString();
  return request<TodoPage>('GET', `/api/v1/todos${qs ? `?${qs}` : ''}`, { json: true });
}

const todoPath = (id: string) => `/api/v1/todos/${encodeURIComponent(id)}`;
export const getTodo = (id: string) => request<Todo>('GET', todoPath(id), { json: true });
export const createTodo = (body: TodoCreate) => request<Todo>('POST', '/api/v1/todos', { body, json: true });
export const updateTodo = (id: string, body: TodoUpdate) => request<Todo>('PUT', todoPath(id), { body, json: true });
export const setTodoDone = (id: string, done: boolean) => request<Todo>('PUT', `${todoPath(id)}/done`, { body: { done }, json: true });
export const deleteTodo = (id: string) => request<void>('DELETE', todoPath(id));
export const clearCompletedTodos = () => request<ClearCompletedResult>('DELETE', '/api/v1/todos/completed', { json: true });
