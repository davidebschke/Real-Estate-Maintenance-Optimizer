import { test, expect } from '@playwright/test'
import { mockPendingProposals, proposalJson } from './support/optimizationMocks'

test.describe('AI appointment optimization', () => {
  test.use({ viewport: { width: 1440, height: 900 } })

  test('opens the optimization page from the main navigation and explains the planning rules', async ({ page }) => {
    await mockPendingProposals(page, [])
    await page.goto('/')

    await page.getByRole('link', { name: 'Optimierung', exact: true }).click()

    await expect(page).toHaveURL('/optimization')
    await expect(page.getByRole('heading', { name: 'KI-Terminoptimierung' })).toBeVisible()
    await expect(page.getByText('Wiederkehrende Termine werden höchstens 14 Tage verschoben.')).toBeVisible()
    await expect(page.getByText('Keine offenen Vorschläge.', { exact: false })).toBeVisible()
  })

  test('starts a run and shows its proposals awaiting confirmation', async ({ page }) => {
    await mockPendingProposals(page, [])
    await page.route('**/api/optimizations', async (route) => {
      await route.fulfill({
        json: {
          runId: 'run-1',
          createdAt: '2026-10-09T08:00:00Z',
          analyzedAppointmentCount: 12,
          candidateCount: 4,
          proposals: [proposalJson('proposal-1', 'Heizungswartung'), proposalJson('proposal-2', 'Gartenpflege')],
        },
      })
    })
    await page.goto('/optimization')

    await page.getByRole('button', { name: 'Termine optimieren' }).click()

    await expect(page.locator('.optimization-proposal-card')).toHaveCount(2)
    await expect(page.getByText('Letzter Lauf: 12 Termine geprüft, 4 mögliche Verschiebungen bewertet, 2 Vorschläge.')).toBeVisible()
    await expect(page.locator('.optimization-proposal-card').first()).toContainText('Ersparnis: 10 km · 15 Min')
  })

  test('applying a proposal sends the confirmation and removes it from the list', async ({ page }) => {
    await mockPendingProposals(page, [proposalJson('proposal-1', 'Heizungswartung'), proposalJson('proposal-2', 'Gartenpflege')])
    let acceptedUrl = ''
    await page.route('**/api/optimizations/proposals/*/accept', async (route) => {
      acceptedUrl = route.request().url()
      await route.fulfill({ json: { ...proposalJson('proposal-1', 'Heizungswartung'), status: 'ACCEPTED' } })
    })
    await page.goto('/optimization')

    await page.locator('.optimization-proposal-card').first().getByRole('button', { name: 'Übernehmen' }).click()

    await expect(page.locator('.optimization-proposal-card')).toHaveCount(1)
    await expect(page.locator('.optimization-proposal-card')).toContainText('Gartenpflege')
    expect(acceptedUrl).toContain('/api/optimizations/proposals/proposal-1/accept')
  })

  test('shows the backend message when an outdated proposal cannot be applied', async ({ page }) => {
    await mockPendingProposals(page, [proposalJson('proposal-1', 'Heizungswartung')])
    await page.route('**/api/optimizations/proposals/*/accept', async (route) => {
      await route.fulfill({ status: 409, json: { message: 'Dieser Vorschlag ist veraltet.' } })
    })
    await page.goto('/optimization')

    await page.getByRole('button', { name: 'Übernehmen' }).click()

    await expect(page.getByRole('alert')).toHaveText('Dieser Vorschlag ist veraltet.')
    await expect(page.locator('.optimization-proposal-card')).toHaveCount(0)
  })

  test('shows the backend message when the run fails', async ({ page }) => {
    await mockPendingProposals(page, [])
    await page.route('**/api/optimizations', async (route) => {
      await route.fulfill({ status: 501, json: { message: 'Die KI-Optimierung ist auf diesem Server nicht aktiviert.' } })
    })
    await page.goto('/optimization')

    await page.getByRole('button', { name: 'Termine optimieren' }).click()

    await expect(page.getByRole('alert')).toHaveText('Die KI-Optimierung ist auf diesem Server nicht aktiviert.')
  })

  test('the overview banner counts the pending proposals and leads to them', async ({ page }) => {
    await mockPendingProposals(page, [proposalJson('proposal-1', 'Heizungswartung'), proposalJson('proposal-2', 'Gartenpflege')])
    await page.goto('/')

    await expect(page.getByText('Die KI hat 2 Vorschläge, die Fahrzeit sparen.')).toBeVisible()
    await page.getByRole('link', { name: 'Vorschläge ansehen' }).click()

    await expect(page).toHaveURL('/optimization')
    await expect(page.locator('.optimization-proposal-card')).toHaveCount(2)
  })

  test('the optimization page fits a phone screen without horizontal scrolling', async ({ page }) => {
    await mockPendingProposals(page, [proposalJson('proposal-1', 'Heizungswartung mit sehr langem Titel')])
    await page.setViewportSize({ width: 360, height: 740 })
    await page.goto('/optimization')

    const card = page.locator('.optimization-proposal-card')
    await expect(card).toBeVisible()
    const accept = card.getByRole('button', { name: 'Übernehmen' })
    await accept.scrollIntoViewIfNeeded()
    await expect(accept).toBeInViewport({ ratio: 1 })
    const overflows = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth)
    expect(overflows).toBe(false)
  })
})
