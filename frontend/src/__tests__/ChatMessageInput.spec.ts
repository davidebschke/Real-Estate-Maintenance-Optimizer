import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import ChatMessageInput from '@/components/chat/ChatMessageInput.vue'

function mountInput(disabled = false) {
  return mount(ChatMessageInput, { props: { disabled }, global: { plugins: [i18n] } })
}

describe('ChatMessageInput', () => {
  it('emits the trimmed text on submit and clears the field', async () => {
    const wrapper = mountInput()
    await wrapper.find('input').setValue('  Hallo  ')
    await wrapper.find('form').trigger('submit')

    expect(wrapper.emitted('send')).toEqual([['Hallo']])
    expect((wrapper.find('input').element as HTMLInputElement).value).toBe('')
  })

  it('does not emit for a blank draft and disables the send button', async () => {
    const wrapper = mountInput()
    await wrapper.find('input').setValue('   ')
    await wrapper.find('form').trigger('submit')

    expect(wrapper.emitted('send')).toBeUndefined()
    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
  })

  it('disables the field while a message is being sent', () => {
    const wrapper = mountInput(true)

    expect(wrapper.find('input').attributes('disabled')).toBeDefined()
  })
})
