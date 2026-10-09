/** The planning rules the backend's AI optimization applies by default (remo.optimization.* in application.yml), shown to the user; must stay in sync with the backend defaults. */
export const OPTIMIZATION_RULES = {
  minLeadDays: 28,
  horizonDays: 365,
  maxRecurringShiftDays: 14,
} as const
