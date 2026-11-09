import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { createTodo, getTodo, isUnauthorized, toFailure, updateTodo } from '../api/client';
import type { Priority, Todo, TodoCreate, TodoUpdate } from '../api/todos';
import { BackToList, ErrorSummary, FormField, LoadingText, Message } from '../app/ui';
import { usePageChrome } from '../app/usePageFocus';
import { shownFieldErrors, userMessageFor, type Failure, type UserMessage } from '../errors/userMessage';
import { descriptionMessage, dueDateMessage, titleMessage } from '../validation/rules';
import { isBlank, trimWhitespace } from '../validation/whitespace';
import { PRIORITY_LABEL } from './format';
import { isTodoId } from './todoId';

type Field = 'title' | 'description' | 'dueDate' | 'priority';
type Errors = Partial<Record<Field, string>>;
const FIELD_ORDER: Field[] = ['title', 'description', 'dueDate', 'priority'];

interface Values {
  title: string;
  description: string;
  dueDate: string;
  priority: Priority;
}

/** S4 / S6: one form, two modes (ux-design S4, S6; REQ-TODO-001, REQ-TODO-002, REQ-TODO-004). */
function TodoForm({ mode, id, initial }: { mode: 'create' | 'edit'; id: string; initial: Values }) {
  const navigate = useNavigate();
  const [values, setValues] = useState<Values>(initial);
  const [errors, setErrors] = useState<Errors>({});
  const [summaryKey, setSummaryKey] = useState(0);
  const [alert, setAlert] = useState<{ message: UserMessage; back: boolean } | null>(null);
  const [busy, setBusy] = useState(false);
  const action = mode === 'create' ? 'create' : 'save';

  function refuse(next: Errors) {
    setErrors(next);
    setAlert(null);
    setSummaryKey((n) => n + 1);
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    const local: Errors = {};
    const t = titleMessage(values.title);
    const d = descriptionMessage(values.description);
    const due = dueDateMessage(values.dueDate);
    if (t) local.title = t;
    if (d) local.description = d;
    if (due) local.dueDate = due;
    if (t || d || due) return refuse(local);

    const title = trimWhitespace(values.title);
    const blank = isBlank(values.description);
    setBusy(true);
    setErrors({});
    setAlert(null);
    try {
      if (mode === 'create') {
        const body: TodoCreate = { title, dueDate: values.dueDate, priority: values.priority };
        if (!blank) body.description = values.description;
        await createTodo(body);
        navigate('/todos');
      } else {
        const body: TodoUpdate = {
          title,
          description: blank ? null : values.description,
          dueDate: values.dueDate,
          priority: values.priority,
        };
        await updateTodo(id, body);
        navigate(`/todos/${id}`);
      }
    } catch (error) {
      if (isUnauthorized(error)) return;
      const failure: Failure = toFailure(error);
      const shown = shownFieldErrors(failure, action);
      if (shown.length > 0) {
        const next: Errors = {};
        for (const e of shown) {
          const field = e.field as Field;
          next[field] ??= e.message;
        }
        refuse(next);
      } else {
        const message = userMessageFor(failure, action);
        setAlert({ message, back: message.nextStep === 'back-to-list' });
      }
    } finally {
      setBusy(false);
    }
  }

  function set<K extends keyof Values>(key: K, value: Values[K]) {
    setValues((previous) => ({ ...previous, [key]: value }));
  }

  const summary = FIELD_ORDER.filter((f) => errors[f]).map((f) => ({ fieldId: f, message: errors[f]! }));
  const cancelTo = mode === 'create' ? '/todos' : `/todos/${id}`;

  return (
    <>
      {alert ? (
        <Message message={alert.message}>{alert.back ? <BackToList /> : null}</Message>
      ) : null}
      {summary.length > 0 ? <ErrorSummary errors={summary} focusKey={summaryKey} /> : null}
      <form onSubmit={onSubmit} noValidate>
        <FormField id="title" label="Title" requirement="required" help="Up to 200 characters." error={errors.title}>
          {(props) => <input {...props} type="text" value={values.title} onChange={(e) => set('title', e.target.value)} />}
        </FormField>
        <FormField
          id="description"
          label="Description"
          requirement="optional"
          help="Up to 2000 characters."
          error={errors.description}
        >
          {(props) => (
            <textarea {...props} rows={5} value={values.description} onChange={(e) => set('description', e.target.value)} />
          )}
        </FormField>
        <FormField id="dueDate" label="Due date" requirement="required" help="Format: year-month-day." error={errors.dueDate}>
          {(props) => <input {...props} type="date" value={values.dueDate} onChange={(e) => set('dueDate', e.target.value)} />}
        </FormField>
        <FormField id="priority" label="Priority" error={errors.priority}>
          {(props) => (
            <select {...props} value={values.priority} onChange={(e) => set('priority', e.target.value as Priority)}>
              {(['LOW', 'MEDIUM', 'HIGH'] as const).map((p) => (
                <option key={p} value={p}>
                  {PRIORITY_LABEL[p]}
                </option>
              ))}
            </select>
          )}
        </FormField>
        <div className="action-row">
          <button type="submit" className="btn primary" aria-disabled={busy}>
            {busy ? 'Saving...' : mode === 'create' ? 'Add to-do' : 'Save changes'}
          </button>
          <Link className="btn secondary" to={cancelTo}>
            Cancel
          </Link>
        </div>
      </form>
    </>
  );
}

type Load = { kind: 'loading' } | { kind: 'ready'; todo: Todo } | { kind: 'not-found' } | { kind: 'error'; failure: Failure };

export function TodoFormPage({ mode }: { mode: 'create' | 'edit' }) {
  const { id = '' } = useParams();
  const [load, setLoad] = useState<Load>({ kind: 'loading' });
  const [reloadToken, setReloadToken] = useState(0);

  const headingText = mode === 'create' ? 'Add a to-do' : load.kind === 'not-found' ? 'To-do not found' : 'Edit to-do';
  const heading = usePageChrome(headingText);

  useEffect(() => {
    if (mode === 'create') return;
    if (!isTodoId(id)) {
      setLoad({ kind: 'not-found' }); // makes no API call (sdd §8)
      return;
    }
    let cancelled = false;
    setLoad({ kind: 'loading' });
    getTodo(id).then(
      (todo) => {
        if (!cancelled) setLoad({ kind: 'ready', todo });
      },
      (error: unknown) => {
        if (cancelled || isUnauthorized(error)) return;
        const failure = toFailure(error);
        setLoad(failure.kind === 'http' && failure.status === 404 ? { kind: 'not-found' } : { kind: 'error', failure });
      },
    );
    return () => {
      cancelled = true;
    };
  }, [mode, id, reloadToken]);

  let body;
  if (mode === 'create') {
    body = <TodoForm mode="create" id="" initial={{ title: '', description: '', dueDate: '', priority: 'MEDIUM' }} />;
  } else if (load.kind === 'loading') {
    body = <LoadingText>Loading...</LoadingText>;
  } else if (load.kind === 'not-found') {
    body = (
      <>
        <p>This to-do does not exist or has been deleted.</p>
        <BackToList />
      </>
    );
  } else if (load.kind === 'error') {
    body = (
      <Message message={userMessageFor(load.failure, 'load-todo')}>
        <button type="button" className="btn secondary" onClick={() => setReloadToken((n) => n + 1)}>
          Retry
        </button>
      </Message>
    );
  } else {
    const { todo } = load;
    body = (
      <TodoForm
        mode="edit"
        id={todo.id}
        initial={{
          title: todo.title,
          description: todo.description ?? '',
          dueDate: todo.dueDate,
          priority: todo.priority,
        }}
      />
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
