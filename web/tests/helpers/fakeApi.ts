import { vi } from 'vitest';

// In-memory stand-in for the API, written from contracts/identity-api-v1.yaml and contracts/todo-api-v1.yaml.
// It replaces global fetch for component tests. It is test support, not the unit under test.

export type Priority = 'LOW' | 'MEDIUM' | 'HIGH';
export interface TodoRecord {
  id: string;
  title: string;
  description: string | null;
  dueDate: string;
  priority: Priority;
  done: boolean;
  createdAt: string;
  updatedAt: string;
}
export interface ApiCall {
  method: string;
  path: string;
  query: URLSearchParams;
  headers: Headers;
  body: unknown;
}
export type Responder = (call: ApiCall) => Response | 'network' | Promise<Response | 'network'>;
interface Override {
  match: (call: ApiCall) => boolean;
  respond: Responder;
  once: boolean;
}

export const SENTINEL = 'SENTINEL-INTERNALS-7f3a';
export const OWNER_IDENTIFIER = 'Alice@Example.com';
export const OWNER_PASSWORD = 'correct-password';

export function problem(
  status: number,
  code: string,
  errors: { field: string; code: string; message: string }[] = [],
): Response {
  return new Response(
    JSON.stringify({
      type: `urn:todo:problem:${code.toLowerCase().replace(/_/g, '-')}`,
      title: `${SENTINEL} title`,
      status,
      detail: `${SENTINEL} detail`,
      code,
      errors,
    }),
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  );
}

export function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

const UUID = (n: number) => `00000000-0000-4000-8000-${n.toString(16).padStart(12, '0')}`;

export function makeTodo(n: number, patch: Partial<TodoRecord> = {}): TodoRecord {
  const created = new Date(Date.UTC(2026, 0, 1, 0, 0, 0) + n * 60_000).toISOString();
  return {
    id: UUID(n),
    title: `Todo ${n}`,
    description: null,
    dueDate: '2026-11-06',
    priority: 'MEDIUM',
    done: false,
    createdAt: created,
    updatedAt: created,
    ...patch,
  };
}

export class FakeApi {
  signedIn: boolean;
  todos: TodoRecord[] = [];
  calls: ApiCall[] = [];
  private overrides: Override[] = [];
  private nextId = 1000;

  constructor(opts: { signedIn?: boolean; todos?: TodoRecord[] } = {}) {
    this.signedIn = opts.signedIn ?? false;
    this.todos = opts.todos ?? [];
  }

  install(): this {
    vi.stubGlobal('fetch', (input: RequestInfo | URL, init?: RequestInit) => this.handle(input, init));
    return this;
  }

  /** Answer the first (or every) matching call with `respond` instead of the default behaviour. */
  on(method: string, path: string | RegExp, respond: Responder, once = false): this {
    this.overrides.push({
      match: (c) => c.method === method && (typeof path === 'string' ? c.path === path : path.test(c.path)),
      respond,
      once,
    });
    return this;
  }

  /** Calls made to `method path` so far. */
  callsTo(method: string, path: string | RegExp): ApiCall[] {
    return this.calls.filter(
      (c) => c.method === method && (typeof path === 'string' ? c.path === path : path.test(c.path)),
    );
  }

  listCalls(): ApiCall[] {
    return this.callsTo('GET', '/api/v1/todos');
  }

  private async handle(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
    const url = new URL(
      typeof input === 'string' ? input : input instanceof URL ? input.href : input.url,
      'http://localhost',
    );
    const req = new Request(url, init ?? (input instanceof Request ? input : undefined));
    const text = req.method === 'GET' || req.method === 'HEAD' ? '' : await req.text();
    let body: unknown = undefined;
    if (text) {
      try {
        body = JSON.parse(text);
      } catch {
        body = text;
      }
    }
    const call: ApiCall = { method: req.method, path: url.pathname, query: url.searchParams, headers: req.headers, body };
    this.calls.push(call);

    const idx = this.overrides.findIndex((o) => o.match(call));
    if (idx >= 0) {
      const o = this.overrides[idx]!;
      if (o.once) this.overrides.splice(idx, 1);
      const r = await o.respond(call);
      if (r === 'network') throw new TypeError('Failed to fetch');
      return r;
    }
    return this.route(call);
  }

  private find(id: string): TodoRecord | undefined {
    return this.todos.find((t) => t.id === id);
  }

  private route(c: ApiCall): Response {
    const { method, path } = c;
    if (path === '/api/v1/auth/session' && method === 'GET') {
      return this.signedIn
        ? json(200, { user: { identifier: OWNER_IDENTIFIER }, expiresAt: '2026-11-08T18:00:00.000Z' })
        : problem(401, 'UNAUTHENTICATED');
    }
    if (path === '/api/v1/auth/login' && method === 'POST') {
      const b = (c.body ?? {}) as { identifier?: string; password?: string };
      if (b.identifier?.trim().toLowerCase() === OWNER_IDENTIFIER.toLowerCase() && b.password === OWNER_PASSWORD) {
        this.signedIn = true;
        return json(200, { user: { identifier: OWNER_IDENTIFIER }, expiresAt: '2026-11-08T18:00:00.000Z' });
      }
      return problem(401, 'INVALID_CREDENTIALS');
    }
    if (path === '/api/v1/auth/signup' && method === 'POST') {
      const b = (c.body ?? {}) as { identifier?: string };
      if (b.identifier?.trim().toLowerCase() === OWNER_IDENTIFIER.toLowerCase()) {
        return problem(409, 'IDENTIFIER_TAKEN', [
          {
            field: 'identifier',
            code: 'TAKEN',
            message: 'This email address or username is already taken. Choose a different one, or log in.',
          },
        ]);
      }
      return json(201, { user: { identifier: b.identifier?.trim() ?? '' } });
    }
    if (path === '/api/v1/auth/logout' && method === 'POST') {
      this.signedIn = false;
      return new Response(null, { status: 204 });
    }
    if (!path.startsWith('/api/v1/todos')) return problem(404, 'NOT_FOUND');
    if (!this.signedIn) return problem(401, 'UNAUTHENTICATED');

    if (path === '/api/v1/todos' && method === 'GET') return this.list(c);
    if (path === '/api/v1/todos' && method === 'POST') {
      const b = c.body as Partial<TodoRecord>;
      const now = new Date(Date.UTC(2026, 5, 1) + this.nextId * 60_000).toISOString();
      const t: TodoRecord = {
        id: UUID(this.nextId++),
        title: (b.title ?? '').trim(),
        description: b.description ? b.description : null,
        dueDate: b.dueDate ?? '',
        priority: b.priority ?? 'MEDIUM',
        done: false,
        createdAt: now,
        updatedAt: now,
      };
      this.todos.push(t);
      return json(201, t);
    }
    if (path === '/api/v1/todos/completed' && method === 'DELETE') {
      const before = this.todos.length;
      this.todos = this.todos.filter((t) => !t.done);
      return json(200, { deletedCount: before - this.todos.length });
    }
    const m = /^\/api\/v1\/todos\/([^/]+)(\/done)?$/.exec(path);
    if (m) {
      const t = this.find(decodeURIComponent(m[1]!));
      if (!t) return problem(404, 'NOT_FOUND');
      if (m[2] && method === 'PUT') {
        t.done = (c.body as { done: boolean }).done;
        t.updatedAt = new Date(Date.parse(t.updatedAt) + 1000).toISOString();
        return json(200, t);
      }
      if (!m[2] && method === 'GET') return json(200, t);
      if (!m[2] && method === 'PUT') {
        const b = c.body as Partial<TodoRecord>;
        t.title = (b.title ?? '').trim();
        t.description = b.description ? b.description : null;
        t.dueDate = b.dueDate ?? t.dueDate;
        t.priority = b.priority ?? t.priority;
        t.updatedAt = new Date(Date.parse(t.updatedAt) + 1000).toISOString();
        return json(200, t);
      }
      if (!m[2] && method === 'DELETE') {
        this.todos = this.todos.filter((x) => x !== t);
        return new Response(null, { status: 204 });
      }
    }
    return problem(404, 'NOT_FOUND');
  }

  private list(c: ApiCall): Response {
    const status = c.query.get('status') ?? 'all';
    if (!['all', 'active', 'completed'].includes(status)) {
      return problem(400, 'VALIDATION_FAILED', [
        { field: 'status', code: 'INVALID_VALUE', message: 'Choose All, Active or Completed.' },
      ]);
    }
    const pageParam = c.query.get('page') ?? '1';
    if (!/^0*[1-9][0-9]*$/.test(pageParam)) {
      return problem(400, 'VALIDATION_FAILED', [
        { field: 'page', code: 'INVALID_VALUE', message: 'Enter a page number of 1 or more.' },
      ]);
    }
    const q = (c.query.get('q') ?? '').trim().toLowerCase();
    const sorted = [...this.todos].sort((a, b) => (a.createdAt < b.createdAt ? 1 : a.createdAt > b.createdAt ? -1 : 0));
    const matching = sorted.filter(
      (t) =>
        (status === 'all' || (status === 'active' ? !t.done : t.done)) &&
        (q === '' || t.title.toLowerCase().includes(q) || (t.description ?? '').toLowerCase().includes(q)),
    );
    const totalPages = Math.ceil(matching.length / 20);
    const requested = Number(pageParam);
    const page = totalPages === 0 ? 1 : Math.min(requested, totalPages);
    return json(200, {
      items: matching.slice((page - 1) * 20, page * 20),
      page,
      pageSize: 20,
      totalItems: matching.length,
      totalPages,
      totals: {
        all: this.todos.length,
        active: this.todos.filter((t) => !t.done).length,
        completed: this.todos.filter((t) => t.done).length,
      },
    });
  }
}

export function seed(count: number, patch: (n: number) => Partial<TodoRecord> = () => ({})): TodoRecord[] {
  return Array.from({ length: count }, (_, i) => makeTodo(i + 1, patch(i + 1)));
}
