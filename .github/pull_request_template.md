## What this changes

<!-- One or two sentences. Link the requirement IDs (FR-, NFR-) or defect IDs (D-) it relates to. -->

## How it was checked

- [ ] `scripts/test-all.sh` (or `scripts\test-all.ps1`) passes locally, or the CI run on this branch is green
- [ ] New behaviour has a test, and a bug fix has a test that fails without the fix

## Architecture checklist (ADR-M2-02)

- [ ] No module reaches into another module's entities, repositories or `Jpa*` classes (the ArchUnit test enforces this)
- [ ] Status changes still go through `StatusTransitionService` and write the audit row in the same transaction
- [ ] No new endpoint is reachable without authentication unless it is listed as public in `SecurityConfig`

## Security and data

- [ ] No secrets, passwords or personal data are committed
- [ ] The `dev` profile and the demo accounts are not enabled by default for a hosted environment

## Reviewer notes

<!-- Anything the two approvers should look at first. -->
