<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import ChatMessageList from '@/components/chat/ChatMessageList.vue'
import ChatMessageInput from '@/components/chat/ChatMessageInput.vue'
import { useChat } from '@/composables/useChat'

const { t } = useI18n()
const { messages, isSending, hasError, send } = useChat()
</script>

<template>
  <div class="chat-view">
    <h1 class="chat-view__heading">
      <i class="pi pi-comments" aria-hidden="true"></i>
      {{ t('chat.heading') }}
    </h1>
    <ChatMessageList :messages="messages" :is-sending="isSending" :has-error="hasError" />
    <ChatMessageInput :disabled="isSending" @send="send" />
  </div>
</template>

<style scoped src="@/styles/chat/chat-view.css"></style>
