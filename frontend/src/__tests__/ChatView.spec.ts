import { beforeEach, describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { i18n } from '@/i18n'
import ChatView from '@/views/ChatView.vue'
import * as chatService from '@/services/chatService'

vi.mock('@/services/chatService')

beforeEach(() => {
  vi.resetAllMocks()
  i18n.global.locale.value = 'de'
})

describe('ChatView', () => {
  it('renders the localized heading', () => {
    const wrapper = mount(ChatView, { global: { plugins: [i18n] } })

    expect(wrapper.find('h1').text()).toBe('Chat-Assistent')
  })

  it('sends the typed message and shows the assistant reply', async () => {
    vi.mocked(chatService.sendChatMessage).mockResolvedValue({
      reply: 'Wird erledigt',
      category: 'appointment_request',
    })
    const wrapper = mount(ChatView, { global: { plugins: [i18n] } })

    await wrapper.find('input').setValue('Neuer Termin bitte')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(chatService.sendChatMessage).toHaveBeenCalledWith('Neuer Termin bitte')
    expect(wrapper.text()).toContain('Neuer Termin bitte')
    expect(wrapper.text()).toContain('Wird erledigt')
    expect(wrapper.text()).toContain('Terminanfrage')
  })
})
