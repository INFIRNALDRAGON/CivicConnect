import { expect, test } from '@playwright/test';
import { acceptRequest, openFromQueue, personPage, resolveWith, startWork, submitRequest, unique } from './support';

test('E-01 a request goes from submission to resolution and the requester sees it', async ({ context }) => {
  const requester = await personPage(context, 'req1@civicconnect.test');
  const description = unique('E-01 the shared printer on level two keeps jamming');
  const { reference, id } = await submitRequest(requester, 'IT support', description);
  await expect(requester.locator('#status-heading')).toContainText('We have your request');

  const staff = await personPage(context, 'staff1@civicconnect.test');
  await openFromQueue(staff, reference);
  await expect(staff.getByText(description)).toBeVisible();
  await acceptRequest(staff);
  await startWork(staff);

  await requester.goto(`/requests/${id}`);
  await expect(requester.locator('#status-heading')).toContainText('Work on your request has started');

  await staff.getByLabel('Resolution note').fill('short');
  await staff.getByRole('button', { name: 'Resolve request' }).click();
  await expect(staff.getByText(/at least 10 characters/)).toBeVisible();

  await resolveWith(staff, 'Replaced the feed roller and tested ten pages.');
  await expect(staff.getByText('This request is read-only')).toBeVisible();
  await expect(staff.getByLabel('Add a note')).toHaveCount(0);

  await requester.goto(`/requests/${id}`);
  await expect(requester.locator('#status-heading')).toContainText('Your request has been resolved');
  await expect(requester.getByText('Replaced the feed roller and tested ten pages.')).toBeVisible();
  await expect(requester.getByText('Sam Staff-One')).toHaveCount(0);

  await requester.goto('/notifications');
  const messages = requester.getByRole('list', { name: 'Your notifications' });
  await expect(messages.getByText(reference).first()).toBeVisible();
  await expect(messages.getByText(/has been resolved/).first()).toBeVisible();
});

test('E-04 a rejected request tells the requester why and cannot be changed', async ({ context }) => {
  const requester = await personPage(context, 'req2@civicconnect.test');
  const { reference, id } = await submitRequest(requester, 'Maintenance', unique('E-04 duplicate report about the corridor light'));

  const staff = await personPage(context, 'staff1@civicconnect.test');
  await openFromQueue(staff, reference);
  await staff.locator('#reject-reason').fill('no');
  await staff.getByRole('button', { name: 'Reject request' }).click();
  await expect(staff.getByText(/Give a reason of at least 10 characters/)).toBeVisible();

  await staff.locator('#reject-reason').fill('Duplicate of an earlier report for the same light.');
  await staff.getByRole('button', { name: 'Reject request' }).click();
  await expect(staff.getByText('The request was rejected and the requester has been told why.')).toBeVisible();
  await expect(staff.getByRole('button', { name: 'Accept responsibility' })).toHaveCount(0);
  await expect(staff.getByLabel('Add a note')).toHaveCount(0);

  await requester.goto(`/requests/${id}`);
  await expect(requester.locator('#status-heading')).toContainText('could not accept');
  await expect(requester.getByText('Duplicate of an earlier report for the same light.')).toBeVisible();

  await requester.goto('/notifications');
  await expect(requester.getByRole('list', { name: 'Your notifications' }).getByText(reference).first()).toBeVisible();
});
