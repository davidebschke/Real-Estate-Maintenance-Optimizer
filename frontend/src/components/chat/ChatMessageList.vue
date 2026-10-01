<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { ChatMessage } from '@/types/chat'

defineProps<{ messages: ChatMessage[]; isSending: boolean; hasError: boolean }>()

const { t } = useI18n()
</script>

<template>
  <ul class="chat-message-list" aria-live="polite">
    <li v-if="messages.length === 0" class="chat-message-list__empty">{{ t('chat.empty') }}</li>
    <li
      v-for="message in messages"
      :key="message.id"
      class="chat-message-list__message"
      :class="`chat-message-list__message--${message.author}`"
    >
      <span v-if="message.category" class="chat-message-list__category">
        {{ t(`chat.categories.${message.category}`, message.category) }}
      </span>
      {{ message.text }}
    </li>
    <li v-if="isSending" class="chat-message-list__status">{{ t('chat.sending') }}</li>
    <li v-if="hasError" class="chat-message-list__status chat-message-list__status--error" role="alert">
      {{ t('chat.error') }}
    </li>
  </ul>
</template>

<style scoped src="@/styles/chat/chat-message-list.css"></style>
