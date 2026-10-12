import { describe, expect, it } from 'vitest'
import { findStatementFileProblem, MAX_STATEMENT_BYTES } from '@/utils/statementFileValidation'

/** Builds a file of the given size, name and MIME type. */
function createFile(size: number, name = 'statement.pdf', type = 'application/pdf'): File {
  return new File([new Uint8Array(size)], name, { type })
}

describe('findStatementFileProblem', () => {
  it('accepts a PDF within the size limit', () => {
    expect(findStatementFileProblem(createFile(1024))).toBeNull()
  })

  it('accepts a file of exactly the size limit', () => {
    expect(findStatementFileProblem(createFile(MAX_STATEMENT_BYTES))).toBeNull()
  })

  it('accepts a PDF whose MIME type the browser left empty', () => {
    expect(findStatementFileProblem(createFile(10, 'Kontoauszug.PDF', ''))).toBeNull()
  })

  it('rejects a file that is neither typed nor named like a PDF', () => {
    expect(findStatementFileProblem(createFile(10, 'photo.png', 'image/png'))).toBe('notPdf')
  })

  it('rejects an empty file', () => {
    expect(findStatementFileProblem(createFile(0))).toBe('empty')
  })

  it('rejects a file above the size limit', () => {
    expect(findStatementFileProblem(createFile(MAX_STATEMENT_BYTES + 1))).toBe('tooLarge')
  })
})
