// sdd §8: the id part of the §4.2 returnTo pattern (lower-case UUID).
const TODO_ID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;

export function isTodoId(value: string): boolean {
  return TODO_ID.test(value);
}
