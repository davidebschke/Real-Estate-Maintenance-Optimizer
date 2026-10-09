import type { OptimizationProposal, SavingsStatistics } from '@/types/optimization'

/** Builds a pending optimization proposal moving a heating service by a day and saving 10 km and 15 minutes, with overridable fields. */
export function createProposal(overrides: Partial<OptimizationProposal> = {}): OptimizationProposal {
  return {
    id: 'proposal-1',
    appointmentId: 'appointment-1',
    appointmentTitle: 'Heizungswartung',
    propertyName: 'Rheinhaus Deutz',
    recurring: false,
    originalStart: new Date(2026, 10, 3, 10, 0),
    originalEnd: new Date(2026, 10, 3, 11, 0),
    proposedStart: new Date(2026, 10, 4, 9, 15),
    proposedEnd: new Date(2026, 10, 4, 10, 15),
    savedDistanceMeters: 10_000,
    savedDurationSeconds: 900,
    reason: 'Direkt nach dem Termin im selben Objekt.',
    status: 'PENDING',
    decidedAt: null,
    ...overrides,
  }
}

/** Builds weekly savings statistics with two weeks of accepted proposals, with overridable fields. */
export function createSavingsStatistics(overrides: Partial<SavingsStatistics> = {}): SavingsStatistics {
  return {
    granularity: 'WEEK',
    totalSavedDistanceMeters: 12_500,
    totalSavedDurationSeconds: 1_800,
    acceptedProposalCount: 3,
    periods: [
      { periodStart: new Date(2026, 8, 28), savedDistanceMeters: 10_000, savedDurationSeconds: 1_200, acceptedProposalCount: 2 },
      { periodStart: new Date(2026, 9, 5), savedDistanceMeters: 2_500, savedDurationSeconds: 600, acceptedProposalCount: 1 },
    ],
    ...overrides,
  }
}
