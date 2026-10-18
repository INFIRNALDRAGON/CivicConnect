import { BrowserContext, Page, expect } from '@playwright/test';

export const PASSWORD = 'Test@1234';

export const unique = (prefix: string): string => `${prefix} ${Date.now().toString(36)}${Math.floor(Math.random() * 1000)}`;

export async function signIn(page: Page, email: string): Promise<void> {
  await page.goto('/login');
  await page.locator('#login-email').fill(email);
  await page.locator('#login-password').fill(PASSWORD);
  await page.locator('button[type=submit]').click();
  await expect(page).toHaveURL(/\/(requests|staff\/queue|management\/dashboard)$/);
}

export async function personPage(context: BrowserContext, email: string): Promise<Page> {
  const page = await context.newPage();
  await signIn(page, email);
  return page;
}

export interface Submitted {
  reference: string;
  id: string;
}

export async function submitRequest(page: Page, category: string, description: string): Promise<Submitted> {
  await page.goto('/submit');
  await page.locator('#request-categoryId').selectOption({ label: category });
  await page.locator('#request-description').fill(description);
  await page.locator('button[type=submit]').click();
  await expect(page.locator('#confirmation-title')).toBeVisible();
  const reference = (await page.locator('p.font-display').first().innerText()).trim();
  await page.getByRole('link', { name: 'View this request' }).click();
  await expect(page.locator('#status-heading')).toBeVisible();
  const id = page.url().split('/').pop() as string;
  return { reference, id };
}

export async function searchQueue(page: Page, reference: string): Promise<void> {
  await page.goto('/staff/queue');
  await page.locator('#filter-search').fill(reference);
  await expect(page.locator('#filter-search')).toHaveValue(reference);
}

export async function openFromQueue(page: Page, reference: string): Promise<void> {
  await searchQueue(page, reference);
  const cards = page.getByRole('list', { name: 'Requests' }).getByRole('listitem');
  await expect(cards).toHaveCount(1);
  await expect(cards.first()).toContainText(reference);
  await cards.first().getByRole('link').click();
  await expect(page.getByRole('heading', { name: `Request ${reference}` })).toBeAttached();
}

export async function acceptRequest(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Accept responsibility' }).click();
  await expect(page.getByText('You are now responsible for this request.')).toBeVisible();
}

export async function startWork(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Start work' }).click();
  await expect(page.getByText('Status changed to In progress.')).toBeVisible();
}

export async function resolveWith(page: Page, note: string): Promise<void> {
  await page.getByLabel('Resolution note').fill(note);
  await page.getByRole('button', { name: 'Resolve request' }).click();
  await expect(page.getByText('Status changed to Resolved.')).toBeVisible();
}
