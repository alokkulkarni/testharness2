import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { flushSync } from 'react-dom';
import { Link, useSearchParams } from 'react-router';
import { clearCompletedTodos, deleteTodo, isUnauthorized, listTodos, setTodoDone, toFailure } from '../api/client';
import type { StatusFilter } from '../api/client';
import type { Todo, TodoPage } from '../api/todos';
import { Message, LoadingText } from '../app/ui';
import { useAnnounce, usePageChrome } from '../app/usePageFocus';
import { REFRESHED_TEXT, userMessageFor, type Failure, type UserMessage } from '../errors/userMessage';
import { trimWhitespace } from '../validation/whitespace';
import { ConfirmDialog } from './ConfirmDialog';
import { FilterGroup } from './FilterGroup';
import { Pagination } from './Pagination';
import { SearchForm } from './SearchForm';
import { TodoRow } from './TodoRow';

type View = { kind: 'loading' } | { kind: 'loaded'; data: TodoPage } | { kind: 'error'; failure: Failure };
type PageAlert = { message?: UserMessage; text?: string };
type DialogState = { kind: 'delete'; todo: Todo } | { kind: 'clear'; count: number };
interface ReloadOptions {
  /** Show the "no longer exists, and the list has been refreshed" notice once this re-query succeeds. */
  notice?: boolean;
  /** Runs with the data of the re-query that this call asked for. */
  after?: (data: TodoPage) => void;
}

const STATUSES: readonly string[] = ['all', 'active', 'completed'];
const PAGE_PATTERN = /^0*[1-9][0-9]*$/;
const isStatus = (value: string | null): value is StatusFilter => value !== null && STATUSES.includes(value);

/** S3 My to-dos (ux-design S3, D1, D2): list, filter, search, pagination, mark done, delete, clear completed. */
export function TodoListPage() {
  const heading = usePageChrome('My to-dos', true);
  const announce = useAnnounce();
  const [params, setParams] = useSearchParams();

  // The query as the screen uses it: a bad `status` or `page` in the URL counts as All / page 1 (sdd §8) and is
  // replaced in the address below, so the API is never called with it.
  const rawStatus = params.get('status');
  const rawPage = params.get('page');
  const statusOk = rawStatus === null || isStatus(rawStatus);
  const pageOk = rawPage === null || PAGE_PATTERN.test(rawPage);
  const status: StatusFilter = isStatus(rawStatus) ? rawStatus : 'all';
  const page = rawPage !== null && pageOk ? Number(rawPage) : 1;
  const searchText = params.get('q') ?? '';
  const q = trimWhitespace(searchText);
  const key = `${status}|${q}|${page}`;

  useEffect(() => {
    if (statusOk && pageOk) return;
    setParams(
      (previous) => {
        const next = new URLSearchParams(previous);
        if (!statusOk) next.delete('status');
        if (!pageOk) next.delete('page');
        return next;
      },
      { replace: true },
    );
  }, [statusOk, pageOk, setParams]);

  const [view, setView] = useState<View>({ kind: 'loading' });
  const [refreshing, setRefreshing] = useState(false);
  const [reloadToken, setReloadToken] = useState(0);
  const [pageAlert, setPageAlert] = useState<PageAlert | null>(null);
  const [toggling, setToggling] = useState<string | null>(null);
  // the state a toggle asked for, shown on its row until the re-queried list carries it (or the row has left)
  const [requested, setRequested] = useState<{ id: string; done: boolean } | null>(null);
  const [clearing, setClearing] = useState(false);
  const [dialog, setDialog] = useState<DialogState | null>(null);
  const [dialogBusy, setDialogBusy] = useState(false);
  const [dialogError, setDialogError] = useState<UserMessage | null>(null);
  const [headingFocus, setHeadingFocus] = useState(0);

  const viewKind = useRef(view.kind);
  viewKind.current = view.kind;
  const skipKey = useRef<string | null>(null);
  const pendingNotice = useRef(false);
  const afterLoad = useRef<((data: TodoPage) => void) | null>(null);

  const focusHeading = () => heading.current?.focus();
  /** For after a dialog has closed: the dialog is modal until it unmounts, so focus moves in an effect. */
  const focusHeadingAfterRender = () => setHeadingFocus((n) => n + 1);
  useEffect(() => {
    if (headingFocus > 0) heading.current?.focus();
  }, [headingFocus, heading]);

  function reload(options: ReloadOptions = {}) {
    pendingNotice.current = options.notice ?? false;
    afterLoad.current = options.after ?? null;
    setReloadToken((n) => n + 1);
  }

  /** Shows a list response; when the API returned another page than the address names, replaces the address. */
  function show(data: TodoPage, requestedPage: number, notice: boolean) {
    // rendered in the task that received the response, so the rows are on screen as soon as the data is
    flushSync(() => {
      setView({ kind: 'loaded', data });
      setRefreshing(false);
      setPageAlert(notice ? { text: REFRESHED_TEXT } : null);
    });
    if (data.page !== requestedPage) {
      skipKey.current = `${status}|${q}|${data.page}`;
      setParams(
        (previous) => {
          const next = new URLSearchParams(previous);
          if (data.page > 1) next.set('page', String(data.page));
          else next.delete('page');
          return next;
        },
        { replace: true },
      );
    }
  }

  // A layout effect so the request starts in the commit that shows the screen, not one scheduling hop later.
  useLayoutEffect(() => {
    if (skipKey.current === key) {
      skipKey.current = null; // the address was replaced to match data already shown
      return;
    }
    let cancelled = false;
    const notice = pendingNotice.current;
    const after = afterLoad.current;
    pendingNotice.current = false;
    afterLoad.current = null;
    if (viewKind.current === 'loaded') setRefreshing(true);
    else setView({ kind: 'loading' });
    listTodos({ status, q, page }).then(
      (data) => {
        if (cancelled) return;
        show(data, page, notice);
        after?.(data);
      },
      (error: unknown) => {
        if (cancelled || isUnauthorized(error)) return;
        setView({ kind: 'error', failure: toFailure(error) });
        setRefreshing(false);
        setPageAlert(null);
      },
    );
    return () => {
      cancelled = true;
    };
    // `show` and the query read only values that are part of `key`
  }, [key, reloadToken]);

  function changeParams(change: (next: URLSearchParams) => void) {
    setParams(
      (previous) => {
        const next = new URLSearchParams(previous);
        change(next);
        next.delete('page'); // a filter or search change returns to page 1 (REQ-LIST-003 AC-5)
        return next;
      },
      {},
    );
  }

  function searchFor(target: number): string {
    const next = new URLSearchParams(params);
    if (target > 1) next.set('page', String(target));
    else next.delete('page');
    const text = next.toString();
    return text ? `?${text}` : '';
  }

  async function toggle(todo: Todo) {
    if (toggling) return;
    setToggling(todo.id);
    setRequested({ id: todo.id, done: !todo.done });
    setPageAlert(null);
    try {
      await setTodoDone(todo.id, !todo.done);
      announce(todo.done ? `Marked not done: ${todo.title}.` : `Marked done: ${todo.title}.`);
      reload({
        after: (data) => {
          if (!data.items.some((t) => t.id === todo.id)) focusHeading(); // the row left this view
        },
      });
    } catch (error) {
      setRequested(null);
      if (isUnauthorized(error)) return;
      const message = userMessageFor(toFailure(error), 'toggle-done');
      if (message.nextStep === 'list-refreshed') reload({ notice: true, after: focusHeading });
      else setPageAlert({ message });
    } finally {
      setToggling(null);
    }
  }

  async function startClear() {
    if (clearing) return;
    setClearing(true);
    setPageAlert(null);
    try {
      // re-fetch first: the dialog states the number from this response (sdd §8)
      const data = await listTodos({ status, q, page });
      show(data, page, false);
      if (data.totals.completed === 0) {
        announce('There are no completed to-dos to clear.');
        focusHeading(); // the trigger became disabled
      } else {
        setDialogError(null);
        setDialog({ kind: 'clear', count: data.totals.completed });
      }
    } catch (error) {
      if (isUnauthorized(error)) return;
      setPageAlert({ message: userMessageFor(toFailure(error), 'load-list') });
    } finally {
      setClearing(false);
    }
  }

  function closeDialog() {
    setDialog(null);
    setDialogError(null);
  }

  async function confirmDelete(todo: Todo) {
    setDialogBusy(true);
    setDialogError(null);
    try {
      await deleteTodo(todo.id);
      closeDialog();
      announce(`Deleted: ${todo.title}.`);
      focusHeadingAfterRender();
      reload();
    } catch (error) {
      if (isUnauthorized(error)) return;
      const message = userMessageFor(toFailure(error), 'delete-from-list');
      if (message.nextStep === 'list-refreshed') {
        closeDialog();
        focusHeadingAfterRender();
        reload({ notice: true });
      } else {
        setDialogError(message);
      }
    } finally {
      setDialogBusy(false);
    }
  }

  async function confirmClear() {
    setDialogBusy(true);
    setDialogError(null);
    try {
      const { deletedCount } = await clearCompletedTodos();
      closeDialog();
      announce(deletedCount === 1 ? 'Deleted 1 completed to-do.' : `Deleted ${deletedCount} completed to-dos.`);
      focusHeadingAfterRender();
      reload();
    } catch (error) {
      if (isUnauthorized(error)) return;
      setDialogError(userMessageFor(toFailure(error), 'clear-completed'));
    } finally {
      setDialogBusy(false);
    }
  }

  const data = view.kind === 'loaded' ? view.data : null;
  useEffect(() => {
    if (requested === null || toggling !== null) return;
    if (view.kind !== 'loaded') return setRequested(null);
    const row = view.data.items.find((t) => t.id === requested.id);
    if (!row || row.done === requested.done) setRequested(null);
  }, [view, requested, toggling]);
  const canClear = data !== null && data.totals.completed > 0;

  let body;
  if (view.kind === 'loading') {
    body = <LoadingText>Loading your to-dos...</LoadingText>;
  } else if (view.kind === 'error') {
    body = (
      <Message message={userMessageFor(view.failure, 'load-list')}>
        <button type="button" className="btn secondary" onClick={() => reload()}>
          Retry
        </button>
      </Message>
    );
  } else if (view.data.totals.all === 0 && status === 'all' && q === '') {
    body = (
      <section className="empty-state" aria-labelledby="empty-heading">
        <h2 id="empty-heading">You have no to-dos yet</h2>
        <p>Add your first to-do to start keeping track of your tasks.</p>
        <Link className="btn primary" to="/todos/new">
          Add a to-do
        </Link>
      </section>
    );
  } else if (view.data.totalItems === 0) {
    body = (
      <section className="empty-state" aria-labelledby="empty-heading">
        <h2 id="empty-heading">No matching to-dos</h2>
        <p>Try a different search or filter.</p>
      </section>
    );
  } else {
    const d = view.data;
    const from = (d.page - 1) * d.pageSize + 1;
    body = (
      <div aria-busy={refreshing}>
        <p role="status" className="result-summary">{`Showing ${from}-${from + d.items.length - 1} of ${d.totalItems}`}</p>
        <ul className="todo-list">
          {d.items.map((todo) => (
            <TodoRow
              key={todo.id}
              todo={todo}
              toggling={toggling === todo.id}
              requestedDone={requested?.id === todo.id ? requested.done : null}
              onToggle={toggle}
              onDelete={(t) => {
                setDialogError(null);
                setDialog({ kind: 'delete', todo: t });
              }}
            />
          ))}
        </ul>
        <Pagination page={d.page} totalPages={d.totalPages} searchFor={searchFor} onActivate={focusHeading} />
      </div>
    );
  }

  return (
    <>
      <h1 ref={heading} tabIndex={-1}>
        My to-dos
      </h1>
      <div className="action-row">
        <Link className="btn primary" to="/todos/new">
          Add a to-do
        </Link>
        <button
          type="button"
          className="btn danger-outline"
          disabled={!canClear}
          aria-disabled={clearing || undefined}
          onClick={startClear}
        >
          Clear all completed
        </button>
      </div>
      {pageAlert ? <Message {...pageAlert} /> : null}
      <section aria-label="Find to-dos" className="controls">
        <SearchForm
          q={searchText}
          onSearch={(text) =>
            changeParams((next) => {
              if (text === '') next.delete('q');
              else next.set('q', text);
            })
          }
        />
        <FilterGroup
          status={status}
          onChange={(value) =>
            changeParams((next) => {
              if (value === 'all') next.delete('status');
              else next.set('status', value);
            })
          }
        />
      </section>
      {body}
      {dialog?.kind === 'delete' ? (
        <ConfirmDialog
          heading="Delete this to-do?"
          confirmLabel="Delete"
          busyLabel="Deleting..."
          busy={dialogBusy}
          error={dialogError ? <Message message={dialogError} /> : null}
          onConfirm={() => confirmDelete(dialog.todo)}
          onCancel={closeDialog}
        >
          <p className="dialog-todo">{`“${dialog.todo.title}”`}</p>
          <p>This permanently deletes the to-do. This cannot be undone.</p>
        </ConfirmDialog>
      ) : null}
      {dialog?.kind === 'clear' ? (
        <ConfirmDialog
          heading="Clear completed to-dos?"
          confirmLabel={`Delete ${dialog.count} completed`}
          busyLabel="Deleting..."
          busy={dialogBusy}
          error={dialogError ? <Message message={dialogError} /> : null}
          onConfirm={confirmClear}
          onCancel={closeDialog}
        >
          <p>
            {dialog.count === 1
              ? 'Delete 1 completed to-do? This deletes your only completed to-do, even if it is not shown because of the current page, filter or search. This cannot be undone.'
              : `Delete all ${dialog.count} completed to-dos? This deletes all your completed to-dos, including any not shown because of the current page, filter or search. This cannot be undone.`}
          </p>
        </ConfirmDialog>
      ) : null}
    </>
  );
}
