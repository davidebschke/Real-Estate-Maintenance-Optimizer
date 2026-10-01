import { afterEach, describe, it, expect, vi } from 'vitest'
import axios from 'axios'
import { sendChatMessage } from '@/services/chatService'

vi.mock('axios')

afterEach(() => {
  vi.unstubAllEnvs()
  vi.resetAllMocks()
})

describe('chatService', () => {
  it('posts the message to the configured webhook without credentials and returns the reply', async () => {
    vi.stubEnv('VITE_N8N_WEBHOOK_URL', 'http://n8n.test/webhook/remo-chat')
    vi.mocked(axios.post).mockResolvedValue({ data: { reply: 'Hallo', category: 'other' } })

    const reply = await sendChatMessage('Hi')

    expect(axios.post).toHaveBeenCalledWith(
      'http://n8n.test/webhook/remo-chat',
      { message: 'Hi' },
      { withCredentials: false, withXSRFToken: false },
    )
    expect(reply).toEqual({ reply: 'Hallo', category: 'other' })
  })

  it('fails fast when the webhook url is not configured', async () => {
    vi.stubEnv('VITE_N8N_WEBHOOK_URL', '')

    await expect(sendChatMessage('Hi')).rejects.toThrow('VITE_N8N_WEBHOOK_URL')
    expect(axios.post).not.toHaveBeenCalled()
  })
})
