// The one whitespace definition shared with the API (sdd §5): exactly the 25 code points of the Unicode
// White_Space property. String.prototype.trim is not used because its set differs.
const WHITESPACE = new Set<number>([
  0x0009, 0x000a, 0x000b, 0x000c, 0x000d, 0x0020, 0x0085, 0x00a0, 0x1680,
  0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005, 0x2006, 0x2007, 0x2008, 0x2009, 0x200a,
  0x2028, 0x2029, 0x202f, 0x205f, 0x3000,
]);

export function isWhitespace(codePoint: number): boolean {
  return WHITESPACE.has(codePoint);
}

export function trimWhitespace(value: string): string {
  const points = Array.from(value);
  let start = 0;
  let end = points.length;
  while (start < end && isWhitespace(points[start]!.codePointAt(0)!)) start++;
  while (end > start && isWhitespace(points[end - 1]!.codePointAt(0)!)) end--;
  return points.slice(start, end).join('');
}

export function isBlank(value: string): boolean {
  return trimWhitespace(value) === '';
}

/** Length in Unicode code points, matching the API (sdd §5). */
export function codePointLength(value: string): number {
  return Array.from(value).length;
}
