import { expect, test } from '@playwright/test';
import { openFromQueue, personPage, searchQueue, signIn, submitRequest, unique } from './support';

test('E-02 staff only see their own categories and the manager sees everything', async ({ context }) => {
  const itRequester = await personPage(context, 'req1@civicconnect.test');
  const lostRequester = await personPage(context, 'req5@civicconnect.test');
  const itRequest = await submitRequest(itRequester, 'IT support', unique('E-02 the wifi in the meeting room drops'));
  const lostRequest = await submitRequest(lostRequester, 'Lost property', unique('E-02 a blue umbrella was left in the hall'));

  const staffIt = await personPage(context, 'staff1@civicconnect.test');
  await searchQueue(staffIt, itRequest.reference);
  await expect(staffIt.getByRole('link').filter({ hasText: itRequest.reference })).toBeVisible();
  await staffIt.locator('#filter-search').fill(lostRequest.reference);
  await expect(staffIt.getByRole('link').filter({ hasText: itRequest.reference })).toHaveCount(0);
  await expect(staffIt.getByRole('link').filter({ hasText: lostRequest.reference })).toHaveCount(0);
  await staffIt.goto(`/staff/requests/${lostRequest.id}`);
  await expect(staffIt.getByText('This request could not be shown')).toBeVisible();
  await expect(staffIt.getByText('Accept responsibility')).toHaveCount(0);

  const staffLost = await personPage(context, 'staff3@civicconnect.test');
  await openFromQueue(staffLost, lostRequest.reference);
  await expect(staffLost.getByRole('button', { name: 'Accept responsibility' })).toBeVisible();

  const manager = await personPage(context, 'manager@civicconnect.test');
  await expect(manager.getByRole('heading', { name: 'Service activity' })).toBeVisible();
  await expect(manager.getByRole('rowheader', { name: itRequest.reference })).toBeVisible();
  await expect(manager.getByRole('rowheader', { name: lostRequest.reference })).toBeVisible();
  await expect(manager.locator('body')).not.toContainText('@civicconnect.test');
});

test('E-03 pages are closed to people who are not signed in or have the wrong role', async ({ page }) => {
  await page.goto('/requests');
  await expect(page).toHaveURL(/\/login$/);
  await page.goto('/staff/queue');
  await expect(page).toHaveURL(/\/login$/);

  await page.locator('#login-email').fill('req1@civicconnect.test');
  await page.locator('#login-password').fill('Wrong@1234');
  await page.locator('button[type=submit]').click();
  await expect(page.getByText('Email or password is incorrect.')).toBeVisible();
  await expect(page).toHaveURL(/\/login$/);

  await signIn(page, 'req1@civicconnect.test');
  await page.goto('/staff/queue');
  await expect(page).toHaveURL(/\/requests$/);
  await page.goto('/management/dashboard');
  await expect(page).toHaveURL(/\/requests$/);
});

test('E-05 the submit form explains what is wrong before anything is sent', async ({ page }) => {
  await signIn(page, 'req3@civicconnect.test');
  await page.goto('/submit');
  await page.locator('button[type=submit]').click();
  await expect(page.locator('#request-categoryId-error')).toBeVisible();
  await page.locator('#request-categoryId').selectOption({ label: 'Other' });
  await page.locator('#request-description').fill('too short');
  await page.locator('button[type=submit]').click();
  await expect(page.locator('#request-description-error')).toContainText(/10/);
  await expect(page.locator('#confirmation-title')).toHaveCount(0);
});
