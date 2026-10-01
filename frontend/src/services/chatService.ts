import axios from 'axios'

/** Answer returned by the n8n chat webhook. */
export interface ChatReply {
  reply: string
  category?: string
}

/** Sends a user message to the n8n webhook and returns the AI-generated answer. */
export async function sendChatMessage(message: string): Promise<ChatReply> {
  const webhookUrl: string = import.meta.env.VITE_N8N_WEBHOOK_URL ?? ''
  if (!webhookUrl) {
    throw new Error('VITE_N8N_WEBHOOK_URL is not configured')
  }
  const { data } = await axios.post<ChatReply>(
    webhookUrl,
    { message },
    { withCredentials: false, withXSRFToken: false },
  )
  if (typeof data?.reply !== 'string' || !data.reply) {
    throw new Error('Chat webhook returned no reply')
  }
  return data
}
