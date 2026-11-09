import type { Priority } from '../api/todos';

// Dates are built from the parts of the YYYY-MM-DD string and never pass through `new Date(string)`, so the
// calendar date is the same in every time zone (REQ-TODO-002 AC-7, sdd §5).
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'] as const;

export function formatDate(value: string): string {
  const [year, month, day] = value.split('-');
  const name = MONTHS[Number(month) - 1];
  if (!year || !day || !name) return value;
  return `${Number(day)} ${name} ${Number(year)}`;
}

/** An instant (UTC ISO string) in the viewer's local time: "6 Nov 2026, 14:01" (sdd §8). */
export function formatDateTime(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  const hh = String(d.getHours()).padStart(2, '0');
  const mm = String(d.getMinutes()).padStart(2, '0');
  return `${d.getDate()} ${MONTHS[d.getMonth()]} ${d.getFullYear()}, ${hh}:${mm}`;
}

export const PRIORITY_LABEL: Record<Priority, string> = { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High' };
