# Project guide

Read the project in this order. Each section says what the files do and what to look for.

## 1. How the app starts

**`index.html`** is the single page the browser loads. It pulls in the two fonts and runs `src/main.tsx`.

**`src/main.tsx`** mounts React and wraps the app in two providers: `BrowserRouter` (page navigation without reloads) and `AuthProvider` (who is logged in). It also loads `styles/index.css`, which turns Tailwind on.

**`src/App.tsx`** is the map of the whole site. Every URL is listed once, grouped by who may see it:

- `/login`, `/register` are open to everyone.
- `/submit`, `/requests`, `/requests/:id` are requester pages.
- `/staff/queue`, `/staff/requests/:id` are staff pages.
- `/management/dashboard` is the management page.

Each group sits inside a `ProtectedRoute` (the role check) and an `AppLayout` (the navbar and sidebar). `Home` sends `/` to the right start page for the logged-in role.

## 2. Who is logged in

**`src/context/AuthContext.tsx`** holds the logged-in user. It exposes `user`, `login`, `register` and `logout`. Login and register both call the API, save the returned token and user in `sessionStorage`, and update the state. Because the session is kept in `sessionStorage`, a page refresh keeps you logged in but closing the tab does not.

**`src/hooks/useAuth.ts`** is a one-line shortcut so components write `const { user } = useAuth()`.

**`src/components/common/ProtectedRoute.tsx`** sends visitors who are not logged in to `/login`, and sends logged-in users who open another role's page back to their own home page. This is a convenience only. The backend must enforce permissions itself.

## 3. Talking to the backend

**`src/utils/api.ts`** replaces the old `services/` folder. It contains:

- `getSession`, `saveSession`, `clearSession` for the stored login.
- `api.get(path, params)` and `api.post(path, body)`, which add the login token to each request and turn failures into an `ApiError` with a readable `message`.
- A 401 rule: if the server says the session is invalid, the login is cleared and you are sent to `/login`.
- `USE_MOCK`, which decides whether requests go to the real server or to the demo backend.

Pages call it directly, for example `api.post('/requests', payload)`. There are no separate wrapper functions for each endpoint.

**`src/utils/mockApi.ts`** is a fake backend that runs in the browser so you can use the app before the Spring Boot API exists. It stores sample data in `localStorage`, checks the login token, and applies the same rules the real backend should: valid status transitions only, a 409 when the version is stale, staff limited to their categories, resolved requests read-only, and reports for managers only. It is only loaded when `VITE_USE_MOCK=true`. Delete the file and the two lines that import it in `api.ts` when the real backend is ready.

## 4. Loading data

**`src/hooks/useRequests.ts`** has one internal helper, `useResource`, which runs a request and returns `{ data, error, loading, reload, refresh }`. It ignores out-of-date responses and can re-check on a timer. The other hooks wrap it for each screen: `useCategories`, `useMyRequests`, `useQueue`, `useRequestDetail`, `useReport`. The requester detail page polls every 30 seconds so status changes show within the 1 minute required by FR-005.

## 5. Pages

Pages sit in `src/pages`, one folder per role.

- **`auth/LoginPage.tsx`, `auth/RegisterPage.tsx`**: forms with validation. Login shows the demo account buttons in mock mode.
- **`requester/SubmitRequestPage.tsx`**: loads categories, shows `RequestForm`, then shows the reference number after submit.
- **`requester/MyRequestsPage.tsx`**: the list of the requester's requests.
- **`requester/RequestDetailPage.tsx`**: status message, progress tracker, details and history. No staff names are shown.
- **`staff/RequestQueuePage.tsx`**: the filterable queue. Filters live in the URL so the browser back button keeps them.
- **`staff/RequestDetailPage.tsx`**: full detail plus `RequestActions`, the right-hand panel. It offers only the next valid step: Accept, then Start work, then Resolve (note required), then Close. It also handles adding notes, the read-only state and the version conflict message.
- **`management/DashboardPage.tsx`**: date range and category filters, counts by status, the per-category table with average time to resolve, the request list with open / overdue / resolved / closed views, and the CSV download.

## 6. Components

**`components/common`**: small reusable pieces.

- `Button` (variants, loading state), `Alert`, `EmptyState`, `LoadingSpinner`, `BrandMark`.
- `Field` wraps one input with its label, hint and error message and wires up the accessibility attributes.
- `StatusBadge` shows a status with an icon as well as a colour.
- `LifecycleTracker` draws the five-stop progress line.

**`components/layout`**: `AppLayout` (page frame and mobile menu), `Navbar`, `Sidebar` (links per role), `AuthLayout` (the login and register frame).

**`components/requests`**: `RequestCard` (one row in a list), `RequestFilterBar` (queue filters), `RequestForm` (the submit form and its validation), `StatusHistoryTimeline` (the history list).

## 7. Utilities

- **`statusLabels.ts`**: the five statuses in order, their display names, the allowed transitions, the message each status shows a requester, and priority labels. Change wording here, not in the pages.
- **`roles.ts`**: role names, each role's home page, and the sidebar links and notes.
- **`dateFormat.ts`**: all date formatting, always in South African time (SAST).

## 8. Styles (Tailwind)

Styling is written as Tailwind utility classes directly in each component's `className`, for example `className='flex justify-between items-center p-6 shadow-md'`. To change how something looks, edit the classes on that element.

**`src/styles/index.css`** is now small. It turns Tailwind on, defines the project's colours and fonts once in a `@theme` block (`ink`, `signal`, `paper`, `line`, `muted`, and the two font families), sets the base text style, and defines the keyboard focus ring. Use those names in classes, for example `bg-ink` or `text-muted`.

Classes that repeat a lot live in one place instead of being copied: `Button.tsx` holds the button looks (`buttonClass` is also used for links styled as buttons), `Field.tsx` holds the input look (`CONTROL_CLASS`), `StatusBadge.tsx` holds the status colours, and `Panel.tsx` holds the white content box.

Responsive rules use Tailwind prefixes: `md:` and `lg:` apply from that width up. The sidebar turns into a slide-in menu and the progress tracker turns vertical on narrow screens this way.

Tailwind only picks up complete class names, so write `bg-ink` in full. Do not build a class name by joining strings.

## 9. TypeScript

Every component is written as `const Name: React.FC<Props> = () => { ... }` with a typed props interface. **`src/types.ts`** holds the shared shapes: `User`, `Status`, `Role`, `ServiceRequest`, `HistoryEntry`, `Report` and so on. They describe exactly what the API returns, so when the backend contract changes, change them here and the compiler lists everything that needs updating.

`npm run build` type-checks first (`tsc --noEmit`) and then builds, so a type error stops the build. `npm run dev` does not stop on type errors. Your editor shows them as you type.

## What was simplified

- The `services/` folder (six files) became `utils/api.ts` plus `utils/mockApi.ts`.
- Auth no longer re-checks `/users/me` on load or tracks a "session ended" notice. A 401 simply logs you out.
- `Field` takes the input as its child instead of a render function.
- The whole project moved from JavaScript to TypeScript (`.tsx` and `.ts`), and the hand-written stylesheet moved into Tailwind classes on each element.
- The page-title hook was removed. The title is set once in `index.html`.
- The dashboard bar charts were replaced by the tables that already held the same numbers.
