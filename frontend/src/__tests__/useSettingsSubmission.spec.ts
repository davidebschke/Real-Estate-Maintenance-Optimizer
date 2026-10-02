import { describe, expect, it, vi } from 'vitest'
import { AxiosError, AxiosHeaders } from 'axios'
import { useSettingsSubmission } from '@/composables/useSettingsSubmission'

/** Builds the axios error a request rejects with for the given HTTP status. */
function httpError(status: number): AxiosError {
  const config = { headers: new AxiosHeaders() }
  return new AxiosError('failed', 'ERR_BAD_REQUEST', config, null, {
    status,
    statusText: '',
    headers: {},
    config,
    data: {},
  })
}

describe('useSettingsSubmission', () => {
  it('reports success after the save action resolved', async () => {
    const save = vi.fn<(value: string) => Promise<void>>().mockResolvedValue(undefined)
    const { isSaved, isSaving, failureReason, submit } = useSettingsSubmission(save)

    const succeeded = await submit('value')

    expect(succeeded).toBe(true)
    expect(save).toHaveBeenCalledWith('value')
    expect(isSaved.value).toBe(true)
    expect(isSaving.value).toBe(false)
    expect(failureReason.value).toBeNull()
  })

  it('maps a rejected save action to a failure reason', async () => {
    const { isSaved, failureReason, submit } = useSettingsSubmission(() => Promise.reject(httpError(409)), 'usernameTaken')

    const succeeded = await submit()

    expect(succeeded).toBe(false)
    expect(isSaved.value).toBe(false)
    expect(failureReason.value).toBe('usernameTaken')
  })

  it('is saving while the action runs and ignores a second submit meanwhile', async () => {
    let finishSaving: () => void = () => undefined
    const save = vi.fn<() => Promise<void>>(() => new Promise<void>((resolve) => (finishSaving = resolve)))
    const { isSaving, submit } = useSettingsSubmission(save)

    const firstSubmit = submit()
    const secondSubmit = await submit()
    finishSaving()
    await firstSubmit

    expect(secondSubmit).toBe(false)
    expect(save).toHaveBeenCalledOnce()
    expect(isSaving.value).toBe(false)
  })

  it('clears a previous success and failure on the next submit and on request', async () => {
    const save = vi.fn<() => Promise<void>>().mockRejectedValueOnce(httpError(500)).mockResolvedValueOnce(undefined)
    const { isSaved, failureReason, submit, clearFeedback } = useSettingsSubmission(save)

    await submit()
    expect(failureReason.value).toBe('generic')
    await submit()
    expect(failureReason.value).toBeNull()
    expect(isSaved.value).toBe(true)

    clearFeedback()

    expect(isSaved.value).toBe(false)
  })
})
