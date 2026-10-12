/** Decision state of an AI optimization proposal. */
export type OptimizationProposalStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED'

/** An AI-suggested move of one appointment, awaiting or carrying the user's decision. */
export interface OptimizationProposal {
  id: string
  appointmentId: string | null
  appointmentTitle: string
  propertyName: string
  recurring: boolean
  originalStart: Date
  originalEnd: Date
  proposedStart: Date
  proposedEnd: Date
  savedDistanceMeters: number
  savedDurationSeconds: number
  reason: string
  status: OptimizationProposalStatus
  decidedAt: Date | null
}

/** Why the AI optimization cannot run on this server. */
export type OptimizationUnavailableReason = 'AI_DISABLED' | 'ROUTING_DISABLED'

/** Whether the server can run an AI optimization at all, and if not, why. */
export interface OptimizationAvailability {
  available: boolean
  reason: OptimizationUnavailableReason | null
}

/** The outcome of one AI optimization run. */
export interface OptimizationRun {
  runId: string
  createdAt: Date
  analyzedAppointmentCount: number
  candidateCount: number
  proposals: OptimizationProposal[]
}

/** Length of one point of the savings line chart. */
export type SavingsGranularity = 'WEEK' | 'MONTH'

/** Distance and driving time saved by the proposals accepted within one period. */
export interface SavingsPeriod {
  periodStart: Date
  savedDistanceMeters: number
  savedDurationSeconds: number
  acceptedProposalCount: number
}

/** Everything the AI optimization saved the account so far, in total and per period. */
export interface SavingsStatistics {
  granularity: SavingsGranularity
  totalSavedDistanceMeters: number
  totalSavedDurationSeconds: number
  acceptedProposalCount: number
  periods: SavingsPeriod[]
}
