# Tests

Everything runs without input:

| When | What runs |
|---|---|
| Every push and pull request to `main`, and every night at 02:00 UTC | `.github/workflows/ci.yml`: backend build, unit tests, architecture rules and PostgreSQL API tests; frontend type check, unit tests and browser tests on the mock API; browser tests against the real API and PostgreSQL |
| Every pull request | dependency review and CodeQL |
| Before each `git push` (after `scripts/install-git-hooks.sh`) | fast checks only: backend unit tests, frontend build and unit tests |
| On demand on your machine | `scripts/test-all.sh` (macOS, Linux, Git Bash) or `scripts\test-all.ps1` (Windows PowerShell). It starts its own throwaway PostgreSQL in Docker on port 55432, runs everything and removes the database again. Your own database is never touched |
| On demand on GitHub | the "Performance test (P-01)" workflow |

See `docs/TEST_TRACEABILITY.md` for which test covers which requirement.

Layout:

- `backend/src/test/java/.../*Test.java` unit, component and architecture tests (no database)
- `backend/src/test/java/.../it/*IT.java` API and database tests (PostgreSQL)
- `frontend/src/**/*.test.ts` Vitest
- `frontend/e2e/*.spec.ts` Playwright
- `tests/performance/submit.js` k6
