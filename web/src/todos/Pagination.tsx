import { Link } from 'react-router';

/** Page numbers to show: first, last, the current page and two either side, with a gap marked by null (an ellipsis). */
function pageItems(page: number, totalPages: number): (number | null)[] {
  const wanted = new Set<number>([1, totalPages]);
  for (let p = page - 2; p <= page + 2; p++) if (p >= 1 && p <= totalPages) wanted.add(p);
  const sorted = [...wanted].sort((a, b) => a - b);
  const items: (number | null)[] = [];
  sorted.forEach((p, i) => {
    if (i > 0 && p - sorted[i - 1]! > 1) items.push(null);
    items.push(p);
  });
  return items;
}

/**
 * REQ-LIST-003 AC-1..AC-4, ux-design §5: Previous, numbered links, Next. The current page is `aria-current="page"`;
 * a Previous or Next that cannot be used is plain text with aria-disabled (not focusable). `hrefFor` gives the query
 * string for a page; `onActivate` runs when a link is activated (the list moves focus to its heading).
 */
export function Pagination({
  page,
  totalPages,
  searchFor,
  onActivate,
}: {
  page: number;
  totalPages: number;
  searchFor: (page: number) => string;
  onActivate: () => void;
}) {
  if (totalPages <= 1) return null;
  const link = (target: number, content: string, label?: string) => (
    <Link to={{ search: searchFor(target) }} onClick={onActivate} {...(label ? { 'aria-label': label } : {})}>
      {content}
    </Link>
  );
  return (
    <nav aria-label="Pagination" className="pagination">
      <ul>
        <li>{page > 1 ? link(page - 1, 'Previous') : <span aria-disabled="true">Previous</span>}</li>
        {pageItems(page, totalPages).map((p, i) =>
          p === null ? (
            <li key={`gap-${i}`} aria-hidden="true">
              …
            </li>
          ) : (
            <li key={p}>
              {p === page ? (
                <span aria-current="page" className="page-current">
                  <span className="visually-hidden">Page </span>
                  {p}
                </span>
              ) : (
                link(p, String(p), `Page ${p}`)
              )}
            </li>
          ),
        )}
        <li>{page < totalPages ? link(page + 1, 'Next') : <span aria-disabled="true">Next</span>}</li>
      </ul>
    </nav>
  );
}
