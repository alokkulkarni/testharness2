import { useEffect, useState } from 'react';
import type { StatusFilter } from '../api/client';

const OPTIONS: { value: StatusFilter; label: string }[] = [
  { value: 'all', label: 'All' },
  { value: 'active', label: 'Active' },
  { value: 'completed', label: 'Completed' },
];

/** REQ-LIST-001: a fieldset of three radios; a change applies at once (no apply button). */
export function FilterGroup({ status, onChange }: { status: StatusFilter; onChange: (status: StatusFilter) => void }) {
  // the chosen radio shows at once; the address (and so `status`) follows when the router has navigated
  const [chosen, setChosen] = useState<StatusFilter | null>(null);
  useEffect(() => setChosen(null), [status]);
  const shown = chosen ?? status;
  return (
    <fieldset className="filter">
      <legend>Show</legend>
      {OPTIONS.map((option) => (
        <label key={option.value} className="filter-option">
          <input
            type="radio"
            name="status"
            value={option.value}
            checked={shown === option.value}
            onChange={() => {
              setChosen(option.value);
              onChange(option.value);
            }}
          />
          <span>{option.label}</span>
        </label>
      ))}
    </fieldset>
  );
}
