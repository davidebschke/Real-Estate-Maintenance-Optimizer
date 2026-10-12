import { useI18n } from 'vue-i18n'
import type { MonthPaymentStatus, PaymentAlert, PaymentMatch } from '@/types/payment'

/** How a payment state is drawn: its PrimeIcons class and its severity, which the styles map to a color. */
export type PaymentSeverity = 'success' | 'warn' | 'danger' | 'info' | 'muted'

/** Visual presentation of each month status. */
const STATUS_PRESENTATION: Record<MonthPaymentStatus, { icon: string; severity: PaymentSeverity }> = {
  PAID: { icon: 'pi-check-circle', severity: 'success' },
  PARTIAL: { icon: 'pi-exclamation-circle', severity: 'warn' },
  PENDING: { icon: 'pi-clock', severity: 'info' },
  OVERDUE: { icon: 'pi-times-circle', severity: 'danger' },
  UPCOMING: { icon: 'pi-calendar', severity: 'muted' },
  UNTRACKED: { icon: 'pi-minus', severity: 'muted' },
}

/** Visual presentation of each scan match quality. */
const MATCH_SEVERITY: Record<PaymentMatch, PaymentSeverity> = {
  MATCH: 'success',
  POSSIBLE: 'warn',
  NONE: 'danger',
}

/** Every month status in the order the legend lists them. */
export const MONTH_STATUSES: MonthPaymentStatus[] = ['PAID', 'PARTIAL', 'PENDING', 'OVERDUE', 'UPCOMING', 'UNTRACKED']

/** Provides localized labels, icons and severities for month statuses, payment alerts and scan matches. */
export function usePaymentStatus() {
  const { t } = useI18n()

  /** Returns the localized label of a month status. */
  function statusLabel(status: MonthPaymentStatus): string {
    return t(`payments.status.${status}`)
  }

  /** Returns the PrimeIcons class of a month status. */
  function statusIcon(status: MonthPaymentStatus): string {
    return STATUS_PRESENTATION[status].icon
  }

  /** Returns the severity of a month status. */
  function statusSeverity(status: MonthPaymentStatus): PaymentSeverity {
    return STATUS_PRESENTATION[status].severity
  }

  /** Returns the localized label of a payment alert, e.g. "Zahlung überfällig". */
  function alertLabel(alert: PaymentAlert): string {
    return t(`payments.alert.${alert}`)
  }

  /** Returns the severity of a payment alert: overdue is danger, everything else warn. */
  function alertSeverity(alert: PaymentAlert): PaymentSeverity {
    return alert === 'OVERDUE' ? 'danger' : 'warn'
  }

  /** Returns the localized label of how confidently a scanned payment was assigned. */
  function matchLabel(match: PaymentMatch): string {
    return t(`payments.match.${match}`)
  }

  /** Returns the severity of a scan match quality. */
  function matchSeverity(match: PaymentMatch): PaymentSeverity {
    return MATCH_SEVERITY[match]
  }

  return { statusLabel, statusIcon, statusSeverity, alertLabel, alertSeverity, matchLabel, matchSeverity }
}
