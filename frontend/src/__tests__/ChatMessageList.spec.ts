import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import ChatMessageList from '@/components/chat/ChatMessageList.vue'
import type { ChatMessage } from '@/types/chat'

interface ListProps {
  messages: ChatMessage[]
  isSending: boolean
  hasError: boolean
}

function mountList(props: Partial<ListProps> = {}) {
  return mount(ChatMessageList, {
    props: { messages: [], isSending: false, hasError: false, ...props },
    global: { plugins: [i18n] },
  })
}

describe('ChatMessageList', () => {
  it('shows the empty hint without messages', () => {
    expect(mountList().text()).toContain('Stelle eine Frage')
  })

  it('renders messages with author class and localized category', () => {
    const wrapper = mountList({
      messages: [
        { id: 1, author: 'user', text: 'Rohr leckt' },
        { id: 2, author: 'assistant', text: 'Ist notiert', category: 'damage_report' },
      ],
    })

    const items = wrapper.findAll('.chat-message-list__message')
    expect(items[0]!.classes()).toContain('chat-message-list__message--user')
    expect(items[1]!.classes()).toContain('chat-message-list__message--assistant')
    expect(items[1]!.text()).toContain('Schadensmeldung')
    expect(wrapper.text()).not.toContain('Stelle eine Frage')
  })

  it('shows the sending and error states', () => {
    const wrapper = mountList({ isSending: true, hasError: true })

    expect(wrapper.text()).toContain('Der Assistent antwortet')
    expect(wrapper.find('[role="alert"]').text()).toContain('konnte nicht gesendet werden')
  })
})
