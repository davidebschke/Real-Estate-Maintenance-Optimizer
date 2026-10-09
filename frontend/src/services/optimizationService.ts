import axios from 'axios'
import type {
  OptimizationProposal,
  OptimizationProposalStatus,
  OptimizationRun,
  SavingsGranularity,
  SavingsStatistics,
} from '@/types/optimization'

/** Shape of an optimization proposal as returned by the backend. */
interface OptimizationProposalDto {
  id: string
  appointmentId: string | null
  appointmentTitle: string
  propertyName: string
  recurring: boolean
  originalStart: string
  originalEnd: string
  proposedStart: string
  proposedEnd: string
  savedDistanceMeters: number
  savedDurationSeconds: number
  reason: string
  status: OptimizationProposalStatus
  decidedAt: string | null
}

/** Shape of an optimization run as returned by the backend. */
interface OptimizationRunDto {
  runId: string
  createdAt: string
  analyzedAppointmentCount: number
  candidateCount: number
  proposals: OptimizationProposalDto[]
}

/** Shape of the savings statistics as returned by the backend. */
interface SavingsStatisticsDto {
  granularity: SavingsGranularity
  totalSavedDistanceMeters: number
  totalSavedDurationSeconds: number
  acceptedProposalCount: number
  periods: {
    periodStart: string
    savedDistanceMeters: number
    savedDurationSeconds: number
    acceptedProposalCount: number
  }[]
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

/** Starts an AI optimization run of the logged-in account, returning the proposals awaiting confirmation. */
export async function startOptimizationRun(): Promise<OptimizationRun> {
  const { data } = await axios.post<OptimizationRunDto>(`${apiBaseUrl}/api/optimizations`)
  return {
    runId: data.runId,
    createdAt: new Date(data.createdAt),
    analyzedAppointmentCount: data.analyzedAppointmentCount,
    candidateCount: data.candidateCount,
    proposals: data.proposals.map(toProposal),
  }
}

/** Fetches every proposal still awaiting a decision, sorted by proposed start. */
export async function fetchPendingProposals(): Promise<OptimizationProposal[]> {
  const { data } = await axios.get<OptimizationProposalDto[]>(`${apiBaseUrl}/api/optimizations/proposals`)
  return data.map(toProposal)
}

/** Accepts a proposal, moving its appointment to the proposed slot. */
export async function acceptProposal(id: string): Promise<OptimizationProposal> {
  const { data } = await axios.post<OptimizationProposalDto>(`${apiBaseUrl}/api/optimizations/proposals/${id}/accept`)
  return toProposal(data)
}

/** Rejects a proposal, leaving its appointment untouched. */
export async function rejectProposal(id: string): Promise<OptimizationProposal> {
  const { data } = await axios.post<OptimizationProposalDto>(`${apiBaseUrl}/api/optimizations/proposals/${id}/reject`)
  return toProposal(data)
}

/** Fetches the distance and driving time the AI optimization saved, per week or month. */
export async function fetchSavingsStatistics(granularity: SavingsGranularity): Promise<SavingsStatistics> {
  const { data } = await axios.get<SavingsStatisticsDto>(`${apiBaseUrl}/api/optimizations/savings`, {
    params: { granularity },
  })
  return {
    granularity: data.granularity,
    totalSavedDistanceMeters: data.totalSavedDistanceMeters,
    totalSavedDurationSeconds: data.totalSavedDurationSeconds,
    acceptedProposalCount: data.acceptedProposalCount,
    periods: data.periods.map((period) => ({
      periodStart: new Date(`${period.periodStart}T00:00:00`),
      savedDistanceMeters: period.savedDistanceMeters,
      savedDurationSeconds: period.savedDurationSeconds,
      acceptedProposalCount: period.acceptedProposalCount,
    })),
  }
}

/** Converts a backend proposal DTO into the frontend's domain shape. */
function toProposal(dto: OptimizationProposalDto): OptimizationProposal {
  return {
    id: dto.id,
    appointmentId: dto.appointmentId,
    appointmentTitle: dto.appointmentTitle,
    propertyName: dto.propertyName,
    recurring: dto.recurring,
    originalStart: new Date(dto.originalStart),
    originalEnd: new Date(dto.originalEnd),
    proposedStart: new Date(dto.proposedStart),
    proposedEnd: new Date(dto.proposedEnd),
    savedDistanceMeters: dto.savedDistanceMeters,
    savedDurationSeconds: dto.savedDurationSeconds,
    reason: dto.reason,
    status: dto.status,
    decidedAt: dto.decidedAt ? new Date(dto.decidedAt) : null,
  }
}
