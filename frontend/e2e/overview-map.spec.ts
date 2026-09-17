import { test, expect, type Page } from '@playwright/test'

const BACKEND_BASE_URL = 'http://localhost:8080'

/** Opens the creation form, fills in the given title and property, and submits it. */
async function createAppointment(page: Page, options: { title: string; property: string }) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/api/appointments') && response.request().method() === 'POST',
  )

  await page.getByRole('button', { name: '+ Termin' }).click()
  await page.getByLabel('Titel').fill(options.title)
  await page.locator('#appointment-property').click()
  await page.getByRole('option', { name: options.property }).click()
  await page.getByRole('button', { name: 'Termin anlegen', exact: true }).click()

  const response = await responsePromise
  const body = (await response.json()) as { id: string }
  return body.id
}

/** Deletes the appointment with the given id directly via the API, so tests don't leave data behind. */
async function deleteAppointment(page: Page, id: string) {
  await page.request.delete(`${BACKEND_BASE_URL}/api/appointments/${id}`, {
    params: { scope: 'single' },
  })
}

test.describe('overview map', () => {
  test.use({ viewport: { width: 1440, height: 900 } })
  test.skip(new Date().getDay() === 0, 'An Sonntagen werden keine Termine vergeben')

  test('shows a marker for a newly created appointment next to the daily appointment list', async ({
    page,
  }) => {
    await page.route('https://nominatim.openstreetmap.org/**', async (route) => {
      await route.fulfill({ json: [{ lat: '50.9333', lon: '6.9333' }] })
    })

    await page.goto('/calendar')
    const title = `E2E Kartentermin ${Date.now()}`
    const id = await createAppointment(page, { title, property: 'Wohnanlage Sonnenhof' })

    try {
      await page.goto('/')

      await expect(page.locator('.appointment-map-card')).toBeVisible()
      await expect(page.locator('.appointment-map')).toBeVisible()
      const todaysAppointmentCount = await page.locator('.daily-appointment-card').count()
      await expect(page.locator('.leaflet-marker-icon')).toHaveCount(todaysAppointmentCount, {
        timeout: 10000,
      })
    } finally {
      await deleteAppointment(page, id)
    }
  })
})
