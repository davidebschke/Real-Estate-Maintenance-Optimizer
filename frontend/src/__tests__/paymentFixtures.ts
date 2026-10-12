import type {
  FormerTenantPayment,
  MonthPayment,
  MonthPaymentStatus,
  PaymentAlertEntry,
  PaymentYearOverview,
  ScannedPayment,
  TenantPaymentYear,
} from '@/types/payment'

/** Builds a scanned payment of 950 euros by Max Mustermann, confidently assigned to a tenant, with overridable fields. */
export function createScannedPayment(overrides: Partial<ScannedPayment> = {}): ScannedPayment {
  return {
    paymentDate: '2026-10-02',
    amount: 950,
    payerName: 'Max Mustermann',
    reference: 'Miete Oktober 2026',
    periodYear: 2026,
    periodMonth: 10,
    tenantId: 'tenant-1',
    match: 'MATCH',
    alreadyRecorded: false,
    ...overrides,
  }
}

/** Builds the twelve months of a year, all with the given status except for explicitly overridden months. */
export function createMonths(status: MonthPaymentStatus, overrides: Record<number, Partial<MonthPayment>> = {}): MonthPayment[] {
  return Array.from({ length: 12 }, (_, index) => ({
    month: index + 1,
    status,
    paidAmount: status === 'PAID' ? 950 : 0,
    expectedAmount: 950,
    ...overrides[index + 1],
  }))
}

/** Builds the year table row of Erika Mustermann with all months paid, with overridable fields. */
export function createTenantPaymentYear(overrides: Partial<TenantPaymentYear> = {}): TenantPaymentYear {
  return {
    tenantId: 'tenant-1',
    apartmentId: 'apartment-1',
    firstName: 'Erika',
    lastName: 'Mustermann',
    floor: 2,
    monthlyRent: 950,
    alert: 'NONE',
    months: createMonths('PAID'),
    ...overrides,
  }
}

/** Builds the year table of a property with one tenant, with overridable fields. */
export function createPaymentYearOverview(overrides: Partial<PaymentYearOverview> = {}): PaymentYearOverview {
  return { year: 2026, trackingStart: '2026-01', tenants: [createTenantPaymentYear()], formerTenantPayments: [], ...overrides }
}

/** Builds a payment of the former tenant Mustermann, Erika for the apartment on the 2nd floor, with overridable fields. */
export function createFormerTenantPayment(overrides: Partial<FormerTenantPayment> = {}): FormerTenantPayment {
  return {
    apartmentId: 'apartment-1',
    floor: 2,
    formerTenantName: 'Mustermann, Erika',
    paymentDate: '2026-03-02',
    amount: 950,
    reference: 'Miete März 2026',
    periodYear: 2026,
    periodMonth: 3,
    ...overrides,
  }
}

/** Builds a payment alert entry of a tenant in a property, with overridable fields. */
export function createAlertEntry(overrides: Partial<PaymentAlertEntry> = {}): PaymentAlertEntry {
  return {
    propertyId: 'property-1',
    apartmentId: 'apartment-1',
    tenantId: 'tenant-1',
    alert: 'OVERDUE',
    ...overrides,
  }
}
