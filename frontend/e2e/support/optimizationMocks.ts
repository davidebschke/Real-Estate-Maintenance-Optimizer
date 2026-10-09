import type { Page } from '@playwright/test'

/** A pending proposal as the backend returns it, moving a heating service next to another visit of the same property. */
export function proposalJson(id: string, appointmentTitle: string) {
  return {
    id,
    appointmentId: `appointment-${id}`,
    appointmentTitle,
    propertyName: 'Rheinhaus Deutz',
    recurring: false,
    originalStart: '2026-11-03T10:00:00',
    originalEnd: '2026-11-03T11:00:00',
    proposedStart: '2026-11-04T09:15:00',
    proposedEnd: '2026-11-04T10:15:00',
    savedDistanceMeters: 10000,
    savedDurationSeconds: 900,
    reason: 'Direkt nach dem Termin im selben Objekt.',
    status: 'PENDING',
    decidedAt: null,
  }
}

/** Answers the pending-proposals request with the given proposals instead of the real backend, so no AI run is needed. */
export async function mockPendingProposals(page: Page, proposals: ReturnType<typeof proposalJson>[]) {
  await page.route('**/api/optimizations/proposals', async (route) => {
    await route.fulfill({ json: proposals })
  })
}

/** Answers the savings request with weekly or monthly statistics of two accepted proposals, or none when empty. */
export async function mockSavings(page: Page, empty = false) {
  await page.route('**/api/optimizations/savings**', async (route) => {
    const granularity = new URL(route.request().url()).searchParams.get('granularity') ?? 'WEEK'
    await route.fulfill({
      json: empty
        ? { granularity, totalSavedDistanceMeters: 0, totalSavedDurationSeconds: 0, acceptedProposalCount: 0, periods: [] }
        : {
            granularity,
            totalSavedDistanceMeters: 12500,
            totalSavedDurationSeconds: 1800,
            acceptedProposalCount: 3,
            periods: [
              { periodStart: '2026-09-28', savedDistanceMeters: 10000, savedDurationSeconds: 1200, acceptedProposalCount: 2 },
              { periodStart: '2026-10-05', savedDistanceMeters: 2500, savedDurationSeconds: 600, acceptedProposalCount: 1 },
            ],
          },
    })
  })
}
