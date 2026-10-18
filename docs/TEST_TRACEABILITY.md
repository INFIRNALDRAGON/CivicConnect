# Test traceability (supports PED v3.0 Sections 30 to 33 and 38)

Execution evidence stays "Pending CI" until a GitHub Actions run exists for the release-candidate commit. Do not copy results from a local run into the PED without keeping the report (`backend/target/site/jacoco`, `backend/target/surefire-reports`, `backend/target/failsafe-reports`, `frontend/playwright-report`).

## Unit and component tests (T-01 to T-24)

All run with `./mvnw test`, need no database, and use the in-memory test doubles in `backend/src/test/java/com/civicconnect/backend/support`.

| ID | Test | Basis |
|---|---|---|
| T-01 | `StatusTransitionServiceTest.cannotSkipStages` | FR-010 |
| T-02 | `StatusTransitionServiceTest.fullLifecycleIsAuditedInOrder` | FR-010, NFR-006 |
| T-03 | `StatusTransitionServiceTest.closedIsTerminal` | FR-012, CR-05 |
| T-04 | `StatusTransitionServiceTest.resolvingNeedsANote` | FR-012 |
| T-05 | `StatusTransitionServiceTest.onlyTheOwnerOrAManagerStartsWork` | FR-009, FR-010 |
| T-06 | `StatusTransitionServiceTest.staffOutsideTheirCategoryAreRefused` | FR-006, ASR-02 |
| T-07 | `StatusTransitionServiceTest.failedAuditWriteSurfacesAndNobodyIsNotified` (renamed). The PostgreSQL version is `RollbackIT` | ASR-01, NFR-006 |
| T-08 | `StatusTransitionServiceTest.simultaneousUpdatesGiveOneWinnerAndOneConflict`. The PostgreSQL version is `ConcurrencyIT` | ASR-03 |
| T-09 | `StatusTransitionServiceTest.staffCannotAssignToSomeoneElseButManagersCan` | FR-009 |
| T-10 | `StatusTransitionServiceTest.cannotAssignTwice` | FR-009 |
| T-11 | `StatusTransitionServiceTest.commentsAreRefusedAfterResolved` | FR-011, CR-05 |
| T-12 | `RequestServiceTest.submitStoresRequestWithReferenceAndFirstHistoryRow` | FR-001, NFR-002 |
| T-13 | `RequestServiceTest.submissionIsValidated` plus the exact boundaries in `BoundaryAndAcceptanceTest` (9 and 10 characters, trimming, 2000 and 2001) | FR-001, FR-002 |
| T-14 | `RequestServiceTest.onlyRequestersSubmit` | FR-001, ASR-02 |
| T-15 | `RequestServiceTest.referencesAreUnique` (100,000 references) | NFR-002 |
| T-16 | `RequestServiceTest.requesterSeesOnlyOwnRequests` | FR-004, NFR-003 |
| T-17 | `RequestServiceTest.staffQueueIsScopedToCategories` | FR-006 |
| T-18 | `RequestServiceTest.queueFiltersAndSortsByAge` | FR-007 |
| T-19 | `NotificationAndReportingTest.requesterIsToldAboutEachChange` | FR-005 |
| T-20 | `NotificationAndReportingTest.notificationsCanBeMarkedReadOnlyByTheirOwner` | FR-005, NFR-003 |
| T-21 | `NotificationAndReportingTest.statusCountsUseTheDefaultSevenDayRange` | FR-013 |
| T-22 | `NotificationAndReportingTest.overdueUsesTheCategoryThresholdAndIgnoresClosedOutRequests` plus `BoundaryAndAcceptanceTest` (exactly 72 h, 71 h 59 min, 72 h 1 min, a 24 h category) | FR-014 |
| T-23 | `NotificationAndReportingTest.averageResolutionTimeIsPerCategory` | FR-016 |
| T-24 | `NotificationAndReportingTest.reportingIsManagerOnly` | FR-013, ASR-02 |

New since the PED draft:

| Test | Basis |
|---|---|
| `StatusTransitionServiceTest.rejectingNeedsAReasonAndIsFinal`, `onlySubmittedRequestsCanBeRejected` | CR-03, D-03 |
| `BoundaryAndAcceptanceTest.requesterSeesStageAndLastUpdateOnOwnRequest` | FR-003 (gap in Section 38) |
| `BoundaryAndAcceptanceTest.staffDetailShowsDescriptionHistoryAndNotesInOrder` | FR-008 (gap) |
| `BoundaryAndAcceptanceTest.aStoredNoteCarriesItsAuthorAndTime`, `requestersNeverReceiveInternalNotes` | FR-011 positive case (gap), NFR-003 |
| `BoundaryAndAcceptanceTest.activityCanBeNarrowedToOneCategory` | FR-015 (gap) |
| `ArchitectureTest` (4 rules: no module cycles, allowed module dependencies, controllers are only called from outside, persistence classes stay inside their module) | ADR-M2-02, TD-04, R-010 |
| `JwtServiceTest`, `AuthServiceTest` | CC-06, ASR-02 |
| `RequestAssemblerTest` | API contract the frontend relies on |

## API and integration tests on PostgreSQL (A-01 to A-05)

Run with `./mvnw verify` (class names end in `IT`). They start the whole application on a random port, run Flyway V1 to V3 plus the dev seed against a real PostgreSQL, and call it over HTTP. When PostgreSQL is not reachable they are skipped on a developer machine and **fail** when the `CI` variable is set, so a pipeline can never go green without them.

| ID | Test | Basis |
|---|---|---|
| A-01 | `RequestApiIT.a01_submissionStoresTheRequestAndItsFirstHistoryRow`, `a01_theSubmittedRequestAppearsInTheRequestersOwnList` | FR-001, NFR-006 |
| A-02 | `RequestApiIT.a02_aMissingOrUnknownCategoryIsRefusedAndNothingIsWritten`, `a02_anInactiveCategoryIsRefusedAndNothingIsWritten`, `a02_theDescriptionLengthBoundariesAreEnforced` | FR-001, FR-002, D-02 |
| A-03 | `RequestApiIT.a03_anIllegalStatusJumpIsRefusedAndLeavesNoTrace`, `a03_aStaleVersionIsAConflict`; `RollbackIT` (forced failure while recording the change); `ConcurrencyIT` | FR-010, ASR-01, ASR-03 |
| A-04 | `RequestApiIT.a04_everyProtectedEndpointRefusesAnonymousCallers`, `a04_aRequesterCannotReadSomeoneElsesRequestOrLearnTheirDetails`, `a04_rolesAreEnforcedOnTheServer`, `a04_theStaffQueueOnlyContainsAuthorisedCategories` | NFR-003, ASR-02, D-05 |
| A-05 | `DatabaseConstraintIT.a05_theDatabaseRefusesTwoRequestsWithTheSameReference` | NFR-002, ASR-03 |

Further database and API checks: `DatabaseConstraintIT` (append-only history and notes, CHECK constraints, least-privilege application account, migrations applied), `AuthApiIT` (every demo account can sign in, so the BCrypt seed hash is proven; no user enumeration; registration is always a requester), `LifecycleIT` (full lifecycle with audit order, notifications, rejection, queue search, manager summary).

Deviations from the PED text, to settle before submission:

1. **A-02 expected result.** The PED expects 404, 404 and 400. The implementation returns 400 for an unknown or inactive category, because the category is part of the request body, and the existing unit tests (T-13) already expect a validation error. The tests assert 400. Either change the PED line or ask for the API to return 404.
2. **A-01 Location header.** The PED expects one. The API did not send it; `POST /api/requests` now returns `Location: /api/requests/{id}`.

## Browser tests (E-01 and E-02, plus three more)

Run with `npm run test:e2e` in `frontend/`. By default they start the Vite dev server against the built-in mock API, so they need no backend. With `E2E_BASE_URL` set they run against a real deployment instead, which is how the `system` job in CI runs them.

| ID | Spec | Basis |
|---|---|---|
| E-01 | `lifecycle.spec.ts` request lifecycle | FR-001, FR-003, FR-005, FR-009, FR-010, FR-012 |
| E-02 | `access.spec.ts` access scope | FR-006, FR-013, NFR-003 |
| E-03 | `access.spec.ts` pages closed to anonymous users and wrong roles | NFR-003, ASR-02 |
| E-04 | `lifecycle.spec.ts` rejection with a reason | CR-03, FR-005 |
| E-05 | `access.spec.ts` form validation messages | FR-001, FR-002 |

## Frontend unit tests

`npm test` in `frontend/` (Vitest): transition table, labels, South African time formatting, role navigation, and the mock API's behaviour against the same rules as the backend (TD-06).

## Performance (P-01)

`tests/performance/submit.js` (k6), run manually from the "Performance test (P-01)" workflow or locally with `k6 run tests/performance/submit.js`. It is not part of the normal pipeline because the PED asks for the environment to be recorded with each run.

## Not covered

- Load behaviour, cold starts and hosting differences (Section 39 residual risks).
- Time handling across midnight SAST (Section 39): the boundary tests use a fixed clock, but there is no test that straddles 00:00 SAST.
- Usability (NFR-005) needs real participants.
