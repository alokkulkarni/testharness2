import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { FakeApi, SENTINEL, makeTodo, problem, seed } from '../helpers/fakeApi';
import { renderApp } from '../helpers/renderApp';
import { liveRegionText } from '../helpers/status';

const title = () => screen.getByLabelText(/^title/i);
const description = () => screen.getByLabelText(/^description/i);
const dueDate = () => screen.getByLabelText(/^due date/i);
const priority = () => screen.getByLabelText(/^priority/i);
const setValue = (el: HTMLElement, value: string) => fireEvent.change(el, { target: { value } });
const openNew = async (api: FakeApi) => {
  const r = await renderApp('/todos/new', api);
  await screen.findByRole('heading', { level: 1, name: 'Add a to-do' });
  return r;
};
const submitNew = (r: { user: { click: (e: Element) => Promise<void> } }) =>
  r.user.click(screen.getByRole('button', { name: /^add to-do$/i }));
const posted = (api: FakeApi) => api.callsTo('POST', '/api/v1/todos');

describe('S4 Add a to-do', () => {
  it('REQ-TODO-001 AC-1 AC-2 a valid to-do is created with the client header and the default priority Medium, then the list shows it first', async () => {
    const api = new FakeApi({ signedIn: true, todos: seed(2) });
    const r = await openNew(api);
    expect(priority()).toHaveValue('MEDIUM');
    await r.user.type(title(), 'Write the report');
    setValue(dueDate(), '2026-11-06');
    await submitNew(r);
    await waitFor(() => expect(r.location.pathname).toBe('/todos'));
    expect(r.location.search).toBe('');
    const call = posted(api)[0]!;
    expect(call.headers.get('X-Todo-Client')).toBe('web');
    expect(call.body).toMatchObject({ title: 'Write the report', dueDate: '2026-11-06', priority: 'MEDIUM' });
    const first = (await screen.findAllByRole('checkbox'))[0]!.closest('li')!;
    expect(within(first).getByRole('link', { name: 'Write the report' })).toBeInTheDocument();
  });

  it('REQ-TODO-002 AC-8 the priority control offers only Low, Medium and High', async () => {
    await openNew(new FakeApi({ signedIn: true }));
    const options = within(priority()).getAllByRole('option').map((o) => o.textContent);
    expect(options).toEqual(['Low', 'Medium', 'High']);
  });

  it('REQ-TODO-001 AC-3 a description is sent; an omitted description is not sent', async () => {
    const api = new FakeApi({ signedIn: true });
    const r = await openNew(api);
    await r.user.type(title(), 'With notes');
    await r.user.type(description(), 'Some notes');
    setValue(dueDate(), '2026-11-06');
    await submitNew(r);
    await waitFor(() => expect(posted(api)).toHaveLength(1));
    expect(posted(api)[0]!.body).toMatchObject({ description: 'Some notes' });
    r.unmount();

    const api2 = new FakeApi({ signedIn: true });
    const r2 = await openNew(api2);
    await r2.user.type(title(), 'No notes');
    setValue(dueDate(), '2026-11-06');
    await submitNew(r2);
    await waitFor(() => expect(posted(api2)).toHaveLength(1));
    expect((posted(api2)[0]!.body as { description?: string | null }).description ?? null).toBeNull();
  });

  it('REQ-TODO-002 AC-1 a title padded with non-breaking spaces is saved trimmed (the SPA may trim it or leave it to the server, sdd §5)', async () => {
    const api = new FakeApi({ signedIn: true });
    const r = await openNew(api);
    setValue(title(), '  Pay rent　');
    setValue(dueDate(), '2026-11-06');
    await submitNew(r);
    await waitFor(() => expect(posted(api)).toHaveLength(1));
    expect((posted(api)[0]!.body as { title: string }).title.trim()).toBe('Pay rent');
  });

  it.each([
    ['empty', ''],
    ['spaces only', '   '],
    ['non-breaking spaces only', '  '],
    ['U+2007 and U+202F only', '  '],
    ['ideographic space only', '　'],
  ])('REQ-TODO-002 AC-2 a title that is %s is refused with "Enter a title." and nothing is sent', async (_label, value) => {
    const api = new FakeApi({ signedIn: true });
    const r = await openNew(api);
    if (value) setValue(title(), value);
    setValue(dueDate(), '2026-11-06');
    await submitNew(r);
    expect((await screen.findAllByText('Enter a title.')).length).toBeGreaterThan(0);
    expect(posted(api)).toHaveLength(0);
  });

  it('REQ-TODO-002 AC-3 a title of 201 characters is refused, 200 is accepted', async () => {
    const api = new FakeApi({ signedIn: true });
    const r = await openNew(api);
    setValue(title(), 'x'.repeat(201));
    setValue(dueDate(), '2026-11-06');
    await submitNew(r);
    expect((await screen.findAllByText('The title must be 200 characters or fewer.')).length).toBeGreaterThan(0);
    expect(posted(api)).toHaveLength(0);
    setValue(title(), 'x'.repeat(200));
    await submitNew(r);
    await waitFor(() => expect(posted(api)).toHaveLength(1));
  });

  it('REQ-TODO-002 AC-3 characters are counted as code points, not UTF-16 units', async () => {
    const api = new FakeApi({ signedIn: true });
    const r = await openNew(api);
    setValue(title(), '\u{1F600}'.repeat(200));
    setValue(dueDate(), '2026-11-06');
    await submitNew(r);
    await waitFor(() => expect(posted(api)).toHaveLength(1));
  });

  it('REQ-TODO-002 AC-4 a description of 2001 characters is refused, 2000 is accepted', async () => {
    const api = new FakeApi({ signedIn: true });
    const r = await openNew(api);
    setValue(title(), 'Long notes');
    setValue(dueDate(), '2026-11-06');
    setValue(description(), 'd'.repeat(2001));
    await submitNew(r);
    expect((await screen.findAllByText('The description must be 2000 characters or fewer.')).length).toBeGreaterThan(0);
    expect(posted(api)).toHaveLength(0);
    setValue(description(), 'd'.repeat(2000));
    await submitNew(r);
    await waitFor(() => expect(posted(api)).toHaveLength(1));
  });

  it('REQ-TODO-002 AC-5 a to-do without a due date is refused with "Enter a due date."', async () => {
    const api = new FakeApi({ signedIn: true });
    const r = await openNew(api);
    setValue(title(), 'No date');
    await submitNew(r);
    expect((await screen.findAllByText('Enter a due date.')).length).toBeGreaterThan(0);
    expect(dueDate()).toHaveAttribute('aria-invalid', 'true');
    expect(posted(api)).toHaveLength(0);
  });

  it('REQ-TODO-002 AC-9 a due date in the past is accepted without a warning', async () => {
    const api = new FakeApi({ signedIn: true });
    const r = await openNew(api);
    setValue(title(), 'Backdated');
    setValue(dueDate(), '2020-01-01');
    await submitNew(r);
    await waitFor(() => expect(posted(api)).toHaveLength(1));
    expect((posted(api)[0]!.body as { dueDate: string }).dueDate).toBe('2020-01-01');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('REQ-TODO-002 AC-6 AC-10 NFR-001 AC-3 a 400 from the server shows its message beside the named field only, with the field tied to the message', async () => {
    const api = new FakeApi({ signedIn: true });
    api.on('POST', '/api/v1/todos', () =>
      problem(400, 'VALIDATION_FAILED', [
        { field: 'dueDate', code: 'INVALID_DATE', message: 'Enter a real date, for example 2026-11-06.' },
        { field: 'owner', code: 'UNKNOWN_FIELD', message: 'OWNER-FIELD-MESSAGE' },
      ]),
    );
    const r = await openNew(api);
    setValue(title(), 'Bad date');
    setValue(dueDate(), '2026-11-06');
    await submitNew(r);
    const message = (await screen.findAllByText(/Enter a real date, for example 2026-11-06\./))[0]!;
    expect(dueDate()).toHaveAttribute('aria-invalid', 'true');
    const describedBy = dueDate().getAttribute('aria-describedby') ?? '';
    expect(describedBy.split(' ').some((id) => document.getElementById(id)?.textContent?.includes('Enter a real date'))).toBe(true);
    expect(message).toBeInTheDocument();
    expect(document.body).not.toHaveTextContent('OWNER-FIELD-MESSAGE');
    expect(document.body).not.toHaveTextContent(SENTINEL);
  });

  it('NFR-001 AC-3 a refused submit shows an error summary that receives focus and links to the field', async () => {
    const r = await openNew(new FakeApi({ signedIn: true }));
    await submitNew(r);
    const summary = await screen.findByRole('alert');
    expect(within(summary).getByText('There is a problem')).toBeInTheDocument();
    await waitFor(() => expect(summary.contains(document.activeElement) || summary === document.activeElement).toBe(true));
    const link = within(summary).getByRole('link', { name: 'Enter a title.' });
    await r.user.click(link);
    expect(title()).toHaveFocus();
    expect(title()).toHaveAttribute('aria-invalid', 'true');
  });

  it('NFR-006 AC-1 NFR-006 AC-2 a failed create (500) keeps the input and shows a form-level message with a next step and no internals', async () => {
    const api = new FakeApi({ signedIn: true });
    api.on('POST', '/api/v1/todos', () => problem(500, 'INTERNAL_ERROR'));
    const r = await openNew(api);
    setValue(title(), 'Keep me');
    setValue(dueDate(), '2026-11-06');
    await submitNew(r);
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(/try again/i);
    expect(document.body).not.toHaveTextContent(SENTINEL);
    expect(document.body).not.toHaveTextContent(/INTERNAL_ERROR/);
    expect(title()).toHaveValue('Keep me');
  });

  it('NFR-006 AC-1 a network failure on create keeps the input and shows a try-again message without browser error text', async () => {
    const api = new FakeApi({ signedIn: true });
    api.on('POST', '/api/v1/todos', () => 'network');
    const r = await openNew(api);
    setValue(title(), 'Keep me');
    setValue(dueDate(), '2026-11-06');
    await submitNew(r);
    expect(await screen.findByRole('alert')).toHaveTextContent(/try again/i);
    expect(document.body).not.toHaveTextContent(/failed to fetch/i);
    expect(title()).toHaveValue('Keep me');
  });
});

describe('S6 Edit a to-do', () => {
  const existing = () => [makeTodo(1, { title: 'Original', description: 'Old notes', dueDate: '2026-03-04', priority: 'HIGH' })];
  const openEdit = async (api: FakeApi) => {
    const r = await renderApp(`/todos/${api.todos[0]!.id}/edit`, api);
    await screen.findByRole('heading', { level: 1, name: 'Edit to-do' });
    await screen.findByDisplayValue('Original');
    return r;
  };
  const save = (r: Awaited<ReturnType<typeof openEdit>>) => r.user.click(screen.getByRole('button', { name: /^save changes$/i }));

  it('REQ-TODO-004 AC-1 the form is prefilled and saving sends all four content fields, then shows the detail page with the new values', async () => {
    const api = new FakeApi({ signedIn: true, todos: existing() });
    const r = await openEdit(api);
    expect(description()).toHaveValue('Old notes');
    expect(dueDate()).toHaveValue('2026-03-04');
    expect(priority()).toHaveValue('HIGH');
    setValue(title(), 'Renamed');
    setValue(priority(), 'LOW');
    setValue(dueDate(), '2026-12-25');
    await save(r);
    await waitFor(() => expect(r.location.pathname).toBe(`/todos/${api.todos[0]!.id}`));
    const put = api.callsTo('PUT', /^\/api\/v1\/todos\/[0-9a-f-]{36}$/)[0]!;
    expect(put.headers.get('X-Todo-Client')).toBe('web');
    expect(put.body).toEqual({ title: 'Renamed', description: 'Old notes', dueDate: '2026-12-25', priority: 'LOW' });
    expect(await screen.findByRole('heading', { level: 1, name: 'Renamed' })).toBeInTheDocument();
    expect(screen.getByText('25 Dec 2026')).toBeInTheDocument();
  });

  it('REQ-TODO-004 AC-2 an invalid edit is refused and nothing is sent', async () => {
    const api = new FakeApi({ signedIn: true, todos: existing() });
    const r = await openEdit(api);
    setValue(title(), ' ');
    await save(r);
    expect((await screen.findAllByText('Enter a title.')).length).toBeGreaterThan(0);
    expect(api.callsTo('PUT', /.*/)).toHaveLength(0);
    expect(api.todos[0]!.title).toBe('Original');
  });

  it('REQ-TODO-004 AC-3 saving an edit of a to-do that no longer exists shows a not-found message with a way back and creates nothing', async () => {
    const api = new FakeApi({ signedIn: true, todos: existing() });
    const r = await openEdit(api);
    api.todos = [];
    await save(r);
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('This to-do no longer exists, so your changes could not be saved.');
    expect(within(alert).getByRole('link', { name: 'Back to my to-dos' })).toHaveAttribute('href', '/todos');
    expect(api.todos).toHaveLength(0);
    expect(api.callsTo('POST', /.*/)).toHaveLength(0);
  });

  it('REQ-TODO-004 AC-4 clearing the description saves no description; the due date cannot be cleared', async () => {
    const api = new FakeApi({ signedIn: true, todos: existing() });
    const r = await openEdit(api);
    setValue(dueDate(), '');
    await save(r);
    expect((await screen.findAllByText('Enter a due date.')).length).toBeGreaterThan(0);
    expect(api.callsTo('PUT', /.*/)).toHaveLength(0);
    setValue(dueDate(), '2026-03-04');
    setValue(description(), '');
    await save(r);
    await waitFor(() => expect(api.callsTo('PUT', /.*/)).toHaveLength(1));
    const body = api.callsTo('PUT', /.*/)[0]!.body as { description?: string | null };
    expect(body.description === undefined || body.description === null || body.description === '').toBe(true);
  });

  it('ux-design S6 loading state: the edit page shows "Loading..." in a status instead of a form until the to-do is loaded', async () => {
    const api = new FakeApi({ signedIn: true, todos: existing() });
    api.on('GET', /^\/api\/v1\/todos\/[0-9a-f-]{36}$/, () => new Promise<Response>(() => {}));
    await renderApp(`/todos/${api.todos[0]!.id}/edit`, api);
    await screen.findByRole('heading', { level: 1, name: 'Edit to-do' });
    expect(liveRegionText()).toContain('Loading...');
    expect(screen.queryByLabelText(/^title/i)).not.toBeInTheDocument();
  });

  it('NFR-006 AC-1 a failed load of the to-do shows a message with Retry', async () => {
    const api = new FakeApi({ signedIn: true, todos: existing() });
    api.on('GET', /^\/api\/v1\/todos\/[0-9a-f-]{36}$/, () => problem(500, 'INTERNAL_ERROR'), true);
    const r = await renderApp(`/todos/${api.todos[0]!.id}/edit`, api);
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(/could not be loaded/i);
    expect(document.body).not.toHaveTextContent(SENTINEL);
    await r.user.click(screen.getByRole('button', { name: /^retry$/i }));
    expect(await screen.findByDisplayValue('Original')).toBeInTheDocument();
  });
});

describe('S5 To-do detail', () => {
  const withNotes = () => [
    makeTodo(1, { title: 'Detail me', description: 'Line one\nLine two', dueDate: '2026-11-06', priority: 'LOW', done: true }),
    makeTodo(2, { title: 'Plain', description: null, dueDate: '2026-12-20' }),
  ];

  it('REQ-TODO-003 AC-3 the detail page shows description, created and updated timestamps, status, due date and priority', async () => {
    const api = new FakeApi({ signedIn: true, todos: withNotes() });
    await renderApp(`/todos/${api.todos[0]!.id}`, api);
    expect(await screen.findByRole('heading', { level: 1, name: 'Detail me' })).toBeInTheDocument();
    const dl = document.querySelector('dl')!;
    const terms = within(dl).getAllByRole('term').map((t) => t.textContent);
    expect(terms).toEqual(['Status', 'Due date', 'Priority', 'Description', 'Created', 'Updated']);
    expect(dl).toHaveTextContent('Completed');
    expect(dl).toHaveTextContent('6 Nov 2026');
    expect(dl).toHaveTextContent('Low');
    expect(dl).toHaveTextContent(/Line one\s*Line two/);
  });

  it('REQ-TODO-003 AC-3 a to-do without a description shows "No description"', async () => {
    const api = new FakeApi({ signedIn: true, todos: withNotes() });
    await renderApp(`/todos/${api.todos[1]!.id}`, api);
    await screen.findByRole('heading', { level: 1, name: 'Plain' });
    expect(screen.getByText('No description')).toBeInTheDocument();
  });

  it('REQ-TODO-003 AC-3 created and updated times are shown in the viewer\'s local time', async () => {
    const original = process.env.TZ;
    try {
      process.env.TZ = 'Pacific/Kiritimati'; // UTC+14
      expect(new Date(Date.UTC(2026, 0, 1)).getTimezoneOffset()).toBe(-840); // the zone took effect in this worker
      const api = new FakeApi({ signedIn: true, todos: withNotes() });
      await renderApp(`/todos/${api.todos[0]!.id}`, api);
      await screen.findByRole('heading', { level: 1, name: 'Detail me' });
      // makeTodo(1) is created 2026-01-01T00:01:00Z, which is 2026-01-01 14:01 at UTC+14
      expect(document.querySelector('dl')).toHaveTextContent(/1 Jan 2026, 14:01/);
    } finally {
      if (original === undefined) delete process.env.TZ;
      else process.env.TZ = original;
    }
  });

  it.each(['Pacific/Kiritimati', 'Pacific/Pago_Pago'])(
    'REQ-TODO-002 AC-7 the due date shows as the same calendar date under TZ=%s, in the list and on the detail page',
    async (tz) => {
      const original = process.env.TZ;
      try {
        process.env.TZ = tz;
        // the zone took effect in this worker: Kiritimati is UTC+14, Pago Pago UTC-11 (no daylight saving in November)
        expect(new Date(Date.UTC(2026, 10, 6)).getTimezoneOffset()).toBe(tz === 'Pacific/Kiritimati' ? -840 : 660);
        const api = new FakeApi({ signedIn: true, todos: withNotes() });
        const r = await renderApp('/todos', api);
        await screen.findByRole('heading', { level: 1, name: 'My to-dos' });
        expect(await screen.findByText(/due 6 nov 2026/i)).toBeInTheDocument();
        r.unmount();
        await renderApp(`/todos/${api.todos[0]!.id}`, api);
        await screen.findByRole('heading', { level: 1, name: 'Detail me' });
        expect(document.querySelector('dl')).toHaveTextContent('6 Nov 2026');
      } finally {
        if (original === undefined) delete process.env.TZ;
        else process.env.TZ = original;
      }
    },
  );

  it('REQ-TODO-004 AC-1 the detail page offers Edit, Delete and a link back to the list', async () => {
    const api = new FakeApi({ signedIn: true, todos: withNotes() });
    await renderApp(`/todos/${api.todos[0]!.id}`, api);
    await screen.findByRole('heading', { level: 1, name: 'Detail me' });
    expect(screen.getByRole('link', { name: 'Edit' })).toHaveAttribute('href', `/todos/${api.todos[0]!.id}/edit`);
    expect(screen.getByRole('button', { name: /^delete/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to my to-dos' })).toHaveAttribute('href', '/todos');
  });

  it('REQ-TODO-006 AC-3 deleting from the detail page confirms, deletes and goes to the list', async () => {
    const api = new FakeApi({ signedIn: true, todos: withNotes() });
    const r = await renderApp(`/todos/${api.todos[0]!.id}`, api);
    await screen.findByRole('heading', { level: 1, name: 'Detail me' });
    await r.user.click(screen.getByRole('button', { name: /^delete/i }));
    const dialog = await screen.findByRole('dialog', { name: 'Delete this to-do?' });
    expect(api.callsTo('DELETE', /.*/)).toHaveLength(0);
    await r.user.click(within(dialog).getByRole('button', { name: 'Delete' }));
    await waitFor(() => expect(r.location.pathname).toBe('/todos'));
    expect(api.callsTo('DELETE', /.*/)).toHaveLength(1);
  });

  it('REQ-TODO-006 AC-4 NFR-006 AC-1 a delete from the detail page of a to-do that is gone says so and keeps the Back link', async () => {
    const api = new FakeApi({ signedIn: true, todos: withNotes() });
    const r = await renderApp(`/todos/${api.todos[0]!.id}`, api);
    await screen.findByRole('heading', { level: 1, name: 'Detail me' });
    await r.user.click(screen.getByRole('button', { name: /^delete/i }));
    const dialog = await screen.findByRole('dialog');
    api.todos = [];
    await r.user.click(within(dialog).getByRole('button', { name: 'Delete' }));
    const alert = await screen.findByText('That to-do no longer exists.');
    expect(alert).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to my to-dos' })).toHaveAttribute('href', '/todos');
    // ux-design section 6: D1 closing on a 404 from S5 moves focus to the h1 (NFR-001 AC-2); the dialog is gone
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole('heading', { level: 1 })).toHaveFocus());
  });
});
