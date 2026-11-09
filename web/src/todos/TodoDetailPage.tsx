import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { deleteTodo, getTodo, isUnauthorized, toFailure } from '../api/client';
import type { Todo } from '../api/todos';
import { BackToList, LoadingText, Message } from '../app/ui';
import { useAnnounce, usePageChrome } from '../app/usePageFocus';
import { userMessageFor, type Failure, type UserMessage } from '../errors/userMessage';
import { ConfirmDialog } from './ConfirmDialog';
import { formatDate, formatDateTime, PRIORITY_LABEL } from './format';
import { isTodoId } from './todoId';

type State =
  | { kind: 'loading' }
  | { kind: 'loaded'; todo: Todo }
  | { kind: 'gone'; todo: Todo }
  | { kind: 'not-found' }
  | { kind: 'error'; failure: Failure };

/** S5 To-do detail (ux-design S5, REQ-TODO-003 AC-3, REQ-NAV-001 AC-4, REQ-ISO-001 AC-2). */
export function TodoDetailPage() {
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const announce = useAnnounce();
  const [state, setState] = useState<State>({ kind: 'loading' });
  const [reloadToken, setReloadToken] = useState(0);
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);
  const [dialogError, setDialogError] = useState<UserMessage | null>(null);

  const headingText =
    state.kind === 'loaded' || state.kind === 'gone' ? state.todo.title : state.kind === 'not-found' ? 'To-do not found' : 'To-do';
  const heading = usePageChrome(headingText);

  useEffect(() => {
    if (!isTodoId(id)) {
      setState({ kind: 'not-found' }); // an id that is not a to-do identifier makes no API call (sdd §8)
      return;
    }
    let cancelled = false;
    setState({ kind: 'loading' });
    getTodo(id).then(
      (todo) => {
        if (!cancelled) setState({ kind: 'loaded', todo });
      },
      (error: unknown) => {
        if (cancelled || isUnauthorized(error)) return;
        const failure = toFailure(error);
        setState(failure.kind === 'http' && failure.status === 404 ? { kind: 'not-found' } : { kind: 'error', failure });
      },
    );
    return () => {
      cancelled = true;
    };
  }, [id, reloadToken]);

  async function confirmDelete(todo: Todo) {
    setBusy(true);
    setDialogError(null);
    try {
      await deleteTodo(todo.id);
      announce(`Deleted: ${todo.title}.`);
      navigate('/todos');
    } catch (error) {
      if (isUnauthorized(error)) return;
      const message = userMessageFor(toFailure(error), 'delete-from-detail');
      if (message.nextStep === 'back-to-list') {
        setConfirming(false);
        setState({ kind: 'gone', todo });
      } else {
        setDialogError(message);
      }
    } finally {
      setBusy(false);
    }
  }

  let body;
  if (state.kind === 'loading') {
    body = <LoadingText>Loading...</LoadingText>;
  } else if (state.kind === 'not-found') {
    body = (
      <>
        <p>This to-do does not exist or has been deleted.</p>
        <BackToList />
      </>
    );
  } else if (state.kind === 'error') {
    body = (
      <Message message={userMessageFor(state.failure, 'load-todo')}>
        <button type="button" className="btn secondary" onClick={() => setReloadToken((n) => n + 1)}>
          Retry
        </button>
      </Message>
    );
  } else if (state.kind === 'gone') {
    body = (
      <Message text="That to-do no longer exists.">
        <BackToList />
      </Message>
    );
  } else {
    const { todo } = state;
    body = (
      <>
        <dl className="detail">
          <dt>Status</dt>
          <dd>{todo.done ? 'Completed' : 'Active'}</dd>
          <dt>Due date</dt>
          <dd>{formatDate(todo.dueDate)}</dd>
          <dt>Priority</dt>
          <dd>{PRIORITY_LABEL[todo.priority]}</dd>
          <dt>Description</dt>
          <dd className="preserve-lines">{todo.description ?? 'No description'}</dd>
          <dt>Created</dt>
          <dd>{formatDateTime(todo.createdAt)}</dd>
          <dt>Updated</dt>
          <dd>{formatDateTime(todo.updatedAt)}</dd>
        </dl>
        <div className="action-row">
          <Link className="btn primary" to={`/todos/${todo.id}/edit`}>
            Edit
          </Link>
          <button
            type="button"
            className="btn danger-outline"
            onClick={() => {
              setDialogError(null);
              setConfirming(true);
            }}
          >
            Delete
          </button>
        </div>
        <p>
          <BackToList />
        </p>
        {confirming ? (
          <ConfirmDialog
            heading="Delete this to-do?"
            confirmLabel="Delete"
            busyLabel="Deleting..."
            busy={busy}
            error={dialogError ? <Message message={dialogError} /> : null}
            onConfirm={() => confirmDelete(todo)}
            onCancel={() => setConfirming(false)}
          >
            <p className="dialog-todo">{`“${todo.title}”`}</p>
            <p>This permanently deletes the to-do. This cannot be undone.</p>
          </ConfirmDialog>
        ) : null}
      </>
    );
  }

  return (
    <>
      <h1 ref={heading} tabIndex={-1}>
        {headingText}
      </h1>
      {body}
    </>
  );
}
