import { beforeEach, describe, it, expect, vi } from 'vitest'
import { useChat } from '@/composables/useChat'
import * as chatService from '@/services/chatService'

vi.mock('@/services/chatService')

beforeEach(() => {
  vi.resetAllMocks()
})

describe('useChat', () => {
  it('appends the user message and the assistant reply', async () => {
    vi.mocked(chatService.sendChatMessage).mockResolvedValue({ reply: 'Antwort', category: 'other' })
    const { messages, send } = useChat()

    await send('  Frage  ')

    expect(messages.value).toEqual([
      { id: 1, author: 'user', text: 'Frage' },
      { id: 2, author: 'assistant', text: 'Antwort', category: 'other' },
    ])
    expect(chatService.sendChatMessage).toHaveBeenCalledWith('Frage')
  })

  it('ignores blank messages', async () => {
    const { messages, send } = useChat()

    await send('   ')

    expect(messages.value).toHaveLength(0)
    expect(chatService.sendChatMessage).not.toHaveBeenCalled()
  })

  it('exposes a failed request as error state and resets it on the next success', async () => {
    vi.mocked(chatService.sendChatMessage).mockRejectedValueOnce(new Error('down'))
    const { hasError, isSending, send } = useChat()

    await send('Frage')

    expect(hasError.value).toBe(true)
    expect(isSending.value).toBe(false)

    vi.mocked(chatService.sendChatMessage).mockResolvedValue({ reply: 'ok' })
    await send('Nochmal')

    expect(hasError.value).toBe(false)
  })
})
