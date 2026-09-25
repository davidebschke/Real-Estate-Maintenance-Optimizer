import { test, expect, type Page } from '@playwright/test'

const BACKEND_BASE_URL = 'http://localhost:8080'

/** Opens the creation form, fills in the given title and property, and submits it. */
async function createAppointment(
  page: Page,
  options: { title: string; property: string; locked?: boolean; recurring?: boolean },
) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/api/appointments') && response.request().method() === 'POST',
  )

  await page.getByRole('button', { name: '+ Termin' }).click()
  await page.getByLabel('Titel').fill(options.title)
  await page.locator('#appointment-property').click()
  await page.getByRole('option', { name: options.property }).click()

  if (options.locked) {
    await page.getByText('Unverschiebbar (Termin mit Dritten, z. B. TÜV)').click()
  }
  if (options.recurring) {
    await page.getByText('Dauerauftrag — wiederholen').click()
    await page.locator('#appointment-recurrence-interval').click()
    await page.getByRole('option', { name: 'alle 3 Monate' }).click()
  }

  await page.getByRole('button', { name: 'Termin anlegen', exact: true }).click()

  const response = await responsePromise
  const body = (await response.json()) as { id: string }
  return body.id
}

/** Deletes the appointment with the given id directly via the API, so tests don't leave data behind. */
async function deleteAppointment(page: Page, id: string, scope: 'single' | 'series' = 'single') {
  await page.request.delete(`${BACKEND_BASE_URL}/api/appointments/${id}`, { params: { scope } })
}

/** Drags the calendar card with the given title 200px down, past the creation dialog's still-closing mask so the low-level mouse events actually reach the grid. */
async function dragAppointmentCardDown(page: Page, title: string) {
  await page
    .locator('.p-dialog-mask')
    .waitFor({ state: 'detached' })
    .catch(() => {})
  const box = await page.getByText(title).boundingBox()
  if (!box) throw new Error(`appointment card "${title}" not found`)

  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
  await page.mouse.down()
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2 + 200, { steps: 10 })
  await page.mouse.up()
}

test.describe('appointments', () => {
  test.use({ viewport: { width: 1440, height: 900 } })

  test('creating an appointment shows it on the calendar and in its detail view', async ({
    page,
  }) => {
    await page.goto('/calendar')
    const title = `E2E Kellerreinigung ${Date.now()}`

    const id = await createAppointment(page, { title, property: 'Wohnanlage Sonnenhof' })

    try {
      await expect(page.getByText(title)).toBeVisible()

      await page.getByText(title).click()
      await expect(page.getByRole('heading', { name: title })).toBeVisible()
      await expect(page.locator('.appointment-detail-drawer__property')).toContainText(
        'Wohnanlage Sonnenhof',
      )
    } finally {
      await deleteAppointment(page, id)
    }
  })

  test('reschedules an unlocked appointment from its detail view', async ({ page }) => {
    await page.goto('/calendar')
    const title = `E2E Verschiebbar ${Date.now()}`

    const id = await createAppointment(page, { title, property: 'Wohnanlage Sonnenhof' })

    try {
      await page.getByText(title).click()
      await page.getByRole('button', { name: 'Verschieben' }).click()

      await page.locator('#appointment-detail-duration').click()
      await page.getByRole('option', { name: '4 Std' }).click()
      await page.getByRole('button', { name: 'Speichern' }).click()

      await expect(page.getByRole('button', { name: 'Verschieben' })).toBeVisible()
    } finally {
      await deleteAppointment(page, id)
    }
  })

  test('reschedules an unlocked appointment by dragging it onto a new time slot', async ({
    page,
  }) => {
    test.skip(new Date().getDay() === 0, 'no appointments are scheduled on Sundays')
    await page.goto('/calendar')
    const title = `E2E Drag ${Date.now()}`

    const id = await createAppointment(page, { title, property: 'Wohnanlage Sonnenhof' })

    try {
      await dragAppointmentCardDown(page, title)

      await page.getByText(title).click()
      await expect(page.locator('.appointment-detail-drawer__badge')).not.toContainText('07:00')
    } finally {
      await deleteAppointment(page, id)
    }
  })

  test('a locked appointment cannot be rescheduled by dragging it', async ({ page }) => {
    test.skip(new Date().getDay() === 0, 'no appointments are scheduled on Sundays')
    await page.goto('/calendar')
    const title = `E2E Drag Unverschiebbar ${Date.now()}`

    const id = await createAppointment(page, {
      title,
      property: 'Wohnanlage Sonnenhof',
      locked: true,
    })

    try {
      await dragAppointmentCardDown(page, title)

      await page.getByText(title).click()
      await expect(
        page.locator(
          '.appointment-detail-drawer__badge:not(.appointment-detail-drawer__badge--locked)',
        ),
      ).toContainText('07:00')
    } finally {
      await deleteAppointment(page, id)
    }
  })

  test('an unverschiebbar appointment cannot be rescheduled', async ({ page }) => {
    await page.goto('/calendar')
    const title = `E2E Unverschiebbar ${Date.now()}`

    const id = await createAppointment(page, {
      title,
      property: 'Wohnanlage Sonnenhof',
      locked: true,
    })

    try {
      await page.getByText(title).click()
      await expect(page.locator('.appointment-detail-drawer__badge--locked')).toBeVisible()
      await expect(page.getByRole('button', { name: 'Verschieben' })).toHaveCount(0)
    } finally {
      await deleteAppointment(page, id)
    }
  })

  test('deletes a non-recurring appointment after confirming', async ({ page }) => {
    await page.goto('/calendar')
    const title = `E2E Löschen ${Date.now()}`

    await createAppointment(page, { title, property: 'Wohnanlage Sonnenhof' })

    await page.getByText(title).click()
    await page.getByRole('button', { name: 'Termin löschen' }).click()
    await page.getByRole('button', { name: 'Löschen', exact: true }).click()

    await expect(page.getByText(title)).toHaveCount(0)
  })

  test('an appointment created for today opens its detail view when clicked on the overview page', async ({
    page,
  }) => {
    test.skip(new Date().getDay() === 0, 'no appointments are scheduled on Sundays')

    await page.goto('/calendar')
    const title = `E2E Übersicht ${Date.now()}`

    const id = await createAppointment(page, { title, property: 'Wohnanlage Sonnenhof' })

    try {
      await page.goto('/')
      await page.getByText(title).click()

      await expect(page.getByRole('heading', { name: title })).toBeVisible()
    } finally {
      await deleteAppointment(page, id)
    }
  })

  test('deleting a recurring appointment asks whether to delete all following occurrences too', async ({
    page,
  }) => {
    await page.goto('/calendar')
    const title = `E2E Dauerauftrag ${Date.now()}`

    const id = await createAppointment(page, {
      title,
      property: 'Wohnanlage Sonnenhof',
      recurring: true,
    })

    try {
      await page.getByText(title).click()
      await page.getByRole('button', { name: 'Termin löschen' }).click()

      await expect(
        page.getByText('Sollen auch alle weiteren Termine dieser Serie gelöscht werden?'),
      ).toBeVisible()
      await page.getByRole('button', { name: 'Auch alle weiteren löschen' }).click()

      await expect(page.getByText(title)).toHaveCount(0)
    } finally {
      await deleteAppointment(page, id, 'series')
    }
  })
})
