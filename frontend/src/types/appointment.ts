/** Category of maintenance work an appointment covers, used to color-code its calendar block. */
export type AppointmentCategory = 'maintenance' | 'inspection' | 'cleaning' | 'technical'

/** A single, localized entry in an appointment's history log. */
export interface AppointmentHistoryEntry {
  timestamp: Date
  message: string
}

/** A single scheduled maintenance appointment shown on the calendar. */
export interface Appointment {
  id: string
  seriesId: string | null
  title: string
  propertyId: string
  propertyName: string
  propertyAddress: string
  description: string
  category: AppointmentCategory
  start: Date
  end: Date
  locked: boolean
  recurring: boolean
  recurrenceIntervalMonths: number | null
  materials: string[]
  history: AppointmentHistoryEntry[]
  travelDistanceKm: number
  actualEnd: Date | null
  completed: boolean
}
