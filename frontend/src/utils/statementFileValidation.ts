/** Largest bank statement the backend accepts in bytes; must stay in sync with the backend's upload limit. */
export const MAX_STATEMENT_BYTES = 5 * 1024 * 1024

/** Why a chosen file cannot be sent as a bank statement. */
export type StatementFileProblem = 'empty' | 'notPdf' | 'tooLarge'

/** Checks a chosen file for being a non-empty PDF of at most the allowed size, returning the first problem found or null if it is fine. */
export function findStatementFileProblem(file: File): StatementFileProblem | null {
  const looksLikePdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf')
  if (!looksLikePdf) return 'notPdf'
  if (file.size === 0) return 'empty'
  if (file.size > MAX_STATEMENT_BYTES) return 'tooLarge'
  return null
}
