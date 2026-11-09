import type { StatusFilter } from '../api/todos';

const OPTIONS: { value: StatusFilter; label: string }[] = [
  { value: 'all', label: 'All' },
  { value: 'active', label: 'Active' },
  { value: 'completed', label: 'Completed' },
];

/** REQ-LIST-001: a fieldset of three radios; a change applies at once (no apply button). */
export function FilterGroup({ status, onChange }: { status: StatusFilter; onChange: (status: StatusFilter) => void }) {
  return (
    <fieldset className="filter">
      <legend>Show</legend>
      {OPTIONS.map((option) => (
        <label key={option.value} className="filter-option">
          <input
            type="radio"
            name="status"
            value={option.value}
            checked={status === option.value}
            onChange={() => onChange(option.value)}
          />
          <span>{option.label}</span>
        </label>
      ))}
    </fieldset>
  );
}
