// sdd §4.2: a returnTo value is honoured only when it is one of the app's own to-do routes.
const ALLOWED =
  /^\/todos(\/new|\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(\/edit)?)?(\?[^#\\]*)?$/;

export function safeReturnTo(raw: string | null): string {
  if (raw !== null && ALLOWED.test(raw)) return raw;
  return '/todos';
}
