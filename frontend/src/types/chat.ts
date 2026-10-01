/** One message of the chat conversation, written either by the user or by the assistant. */
export interface ChatMessage {
  id: number
  author: 'user' | 'assistant'
  text: string
  category?: string
}
