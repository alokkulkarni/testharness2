import js from '@eslint/js';
import tseslint from 'typescript-eslint';

export default tseslint.config(
  { ignores: ['node_modules', 'dist', 'test-results', 'playwright-report', 'playwright-report-session-expiry', 'playwright-report-restart', 'blob-report', 'src/api/**'] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
);
