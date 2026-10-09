import { afterEach, describe, expect, it, vi } from 'vitest'
import axios from 'axios'
import {
  acceptProposal,
  fetchPendingProposals,
  fetchSavingsStatistics,
  rejectProposal,
  startOptimizationRun,
} from '@/services/optimizationService'

vi.mock('axios')

const proposalDto = {
  id: 'proposal-1',
  appointmentId: 'appointment-1',
  appointmentTitle: 'Heizungswartung',
  propertyName: 'Rheinhaus Deutz',
  recurring: true,
  originalStart: '2026-11-03T10:00:00',
  originalEnd: '2026-11-03T11:00:00',
  proposedStart: '2026-11-04T09:15:00',
  proposedEnd: '2026-11-04T10:15:00',
  savedDistanceMeters: 10000,
  savedDurationSeconds: 900,
  reason: 'Spart Fahrzeit.',
  status: 'PENDING',
  decidedAt: null,
}

afterEach(() => {
  vi.mocked(axios.get).mockReset()
  vi.mocked(axios.post).mockReset()
})

describe('optimizationService', () => {
  it('starts a run and converts its proposals into local dates', async () => {
    vi.mocked(axios.post).mockResolvedValue({
      data: {
        runId: 'run-1',
        createdAt: '2026-10-09T08:00:00Z',
        analyzedAppointmentCount: 12,
        candidateCount: 5,
        proposals: [proposalDto],
      },
    })

    const run = await startOptimizationRun()

    expect(axios.post).toHaveBeenCalledWith(expect.stringMatching(/\/api\/optimizations$/))
    expect(run.runId).toBe('run-1')
    expect(run.createdAt).toEqual(new Date('2026-10-09T08:00:00Z'))
    expect(run.analyzedAppointmentCount).toBe(12)
    expect(run.candidateCount).toBe(5)
    expect(run.proposals[0]).toMatchObject({
      id: 'proposal-1',
      recurring: true,
      proposedStart: new Date(2026, 10, 4, 9, 15),
      originalEnd: new Date(2026, 10, 3, 11, 0),
      decidedAt: null,
    })
  })

  it('fetches the pending proposals', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: [proposalDto] })

    const proposals = await fetchPendingProposals()

    expect(axios.get).toHaveBeenCalledWith(expect.stringContaining('/api/optimizations/proposals'))
    expect(proposals).toHaveLength(1)
    expect(proposals[0]!.savedDistanceMeters).toBe(10000)
  })

  it('accepts and rejects a proposal by id', async () => {
    vi.mocked(axios.post).mockResolvedValue({
      data: { ...proposalDto, status: 'ACCEPTED', decidedAt: '2026-10-09T08:05:00Z' },
    })

    const accepted = await acceptProposal('proposal-1')
    await rejectProposal('proposal-2')

    expect(axios.post).toHaveBeenNthCalledWith(1, expect.stringContaining('/api/optimizations/proposals/proposal-1/accept'))
    expect(axios.post).toHaveBeenNthCalledWith(2, expect.stringContaining('/api/optimizations/proposals/proposal-2/reject'))
    expect(accepted.status).toBe('ACCEPTED')
    expect(accepted.decidedAt).toEqual(new Date('2026-10-09T08:05:00Z'))
  })

  it('fetches the savings for the requested granularity with each period starting at local midnight', async () => {
    vi.mocked(axios.get).mockResolvedValue({
      data: {
        granularity: 'MONTH',
        totalSavedDistanceMeters: 3000,
        totalSavedDurationSeconds: 240,
        acceptedProposalCount: 1,
        periods: [{ periodStart: '2026-10-01', savedDistanceMeters: 3000, savedDurationSeconds: 240, acceptedProposalCount: 1 }],
      },
    })

    const statistics = await fetchSavingsStatistics('MONTH')

    expect(axios.get).toHaveBeenCalledWith(expect.stringContaining('/api/optimizations/savings'), {
      params: { granularity: 'MONTH' },
    })
    expect(statistics.granularity).toBe('MONTH')
    expect(statistics.periods[0]!.periodStart).toEqual(new Date(2026, 9, 1))
    expect(statistics.totalSavedDistanceMeters).toBe(3000)
  })

  it('propagates a failed request to the caller', async () => {
    vi.mocked(axios.post).mockRejectedValue(new Error('network error'))

    await expect(startOptimizationRun()).rejects.toThrow('network error')
  })
})
