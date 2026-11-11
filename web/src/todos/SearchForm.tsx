import { useEffect, useRef, useState, type FormEvent } from 'react';
import { trimWhitespace } from '../validation/whitespace';

/** REQ-LIST-002: submitted with Enter or the Search button; the SPA trims `q` and submits an empty one as no search. */
export function SearchForm({ q, onSearch }: { q: string; onSearch: (q: string) => void }) {
  const [value, setValue] = useState(q);
  // the q the box last matched: a q that this form submitted leaves the text as typed (ux-design S3); another q (Back) replaces it
  const synced = useRef(q);
  useEffect(() => {
    if (q === synced.current) return;
    synced.current = q;
    setValue(q);
  }, [q]);

  function submit(event: FormEvent) {
    event.preventDefault();
    const trimmed = trimWhitespace(value);
    synced.current = trimmed;
    onSearch(trimmed);
  }

  return (
    <form role="search" onSubmit={submit} className="search">
      <label htmlFor="search-todos">Search to-dos</label>
      <div className="search-row">
        <input id="search-todos" type="search" value={value} onChange={(e) => setValue(e.target.value)} />
        <button type="submit" className="btn secondary">
          Search
        </button>
      </div>
    </form>
  );
}
