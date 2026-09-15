/** Category of maintenance work an appointment covers, used to color-code its calendar block. */
export type AppointmentCategory = 'maintenance' | 'inspection' | 'cleaning' | 'technical'

/** A single scheduled maintenance appointment shown on the calendar. */
export interface Appointment {
  id: string
  title: string
  propertyName: string
  category: AppointmentCategory
  start: Date
  end: Date
  travelDistanceKm: number
}
