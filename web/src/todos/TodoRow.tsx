import { useId } from 'react';
import { Link } from 'react-router';
import type { Todo } from '../api/todos';
import { formatDate, PRIORITY_LABEL } from './format';

/** One to-do of the list (REQ-TODO-003 AC-1): checkbox, title link, status, due date, priority, Edit and Delete. */
export function TodoRow({
  todo,
  toggling,
  requestedDone,
  onToggle,
  onDelete,
}: {
  todo: Todo;
  /** True while any row's toggle is in flight: every checkbox is aria-disabled, so a click that is ignored has a cue. */
  toggling: boolean;
  /** The state asked for by a toggle whose result the list has not shown yet; null when none. */
  requestedDone: boolean | null;
  onToggle: (todo: Todo) => void;
  onDelete: (todo: Todo) => void;
}) {
  const titleId = useId();
  return (
    <li className={`todo-row${todo.done ? ' done' : ''}`}>
      <div className="todo-main">
        {/* aria-disabled, not disabled, so the checkbox keeps focus while the request is in flight; it shows the
            requested state at once, until the list has it (reverted if the request fails), so a click takes effect */}
        <input
          type="checkbox"
          className="todo-check"
          checked={requestedDone ?? todo.done}
          aria-labelledby={titleId}
          aria-disabled={toggling}
          onChange={() => {
            if (!toggling) onToggle(todo);
          }}
        />
        <Link id={titleId} to={`/todos/${todo.id}`} className="todo-title">
          {todo.title}
        </Link>
      </div>
      <div className="todo-meta">
        {todo.done ? <span className="badge">Completed</span> : null}
        <span>Due {formatDate(todo.dueDate)}</span>
        <span className={`priority priority-${todo.priority.toLowerCase()}`}>Priority: {PRIORITY_LABEL[todo.priority]}</span>
      </div>
      <div className="todo-actions">
        <Link className="btn secondary" to={`/todos/${todo.id}/edit`}>
          Edit<span className="visually-hidden"> {todo.title}</span>
        </Link>
        <button type="button" className="btn danger-outline" onClick={() => onDelete(todo)}>
          Delete<span className="visually-hidden"> {todo.title}</span>
        </button>
      </div>
    </li>
  );
}
