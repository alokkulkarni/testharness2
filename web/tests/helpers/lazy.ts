/**
 * Loads an application module at test run time instead of at collection time.
 *
 * Why: while the SPA does not exist (and whenever a module is missing), a static import makes Vitest fail the whole
 * file at collection, so the JUnit report holds one failure per file and the acceptance criteria named in the tests
 * cannot be mapped to results. Loading inside each test makes every named test fail on its own, for the right
 * reason (the module under test is missing), and pass or fail on its assertions once the module exists.
 *
 * `path` is a root-relative module path such as '/src/auth/returnTo'. Usage:
 *   const m = await load<typeof import('../../src/auth/returnTo')>('/src/auth/returnTo');
 */
export function load<T>(path: string): Promise<T> {
  return import(/* @vite-ignore */ path) as Promise<T>;
}
