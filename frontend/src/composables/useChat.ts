import { ref } from 'vue'
import { sendChatMessage } from '@/services/chatService'
import type { ChatMessage } from '@/types/chat'

/** Holds the chat conversation and sends user messages to the n8n webhook, exposing a failure as error state. */
export function useChat() {
  const messages = ref<ChatMessage[]>([])
  const isSending = ref(false)
  const hasError = ref(false)
  let nextMessageId = 1

  /** Appends the user message, waits for the assistant answer and appends it too. */
  async function send(text: string): Promise<void> {
    const trimmedText = text.trim()
    if (!trimmedText || isSending.value) return
    messages.value.push({ id: nextMessageId++, author: 'user', text: trimmedText })
    isSending.value = true
    hasError.value = false
    try {
      const { reply, category } = await sendChatMessage(trimmedText)
      messages.value.push({ id: nextMessageId++, author: 'assistant', text: reply, category })
    } catch {
      hasError.value = true
    } finally {
      isSending.value = false
    }
  }

  return { messages, isSending, hasError, send }
}
