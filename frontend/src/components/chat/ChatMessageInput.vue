<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'

defineProps<{ disabled: boolean }>()
const emit = defineEmits<{ send: [text: string] }>()

const { t } = useI18n()
const draft = ref('')

/** Emits the trimmed draft as a message and clears the input field. */
function submit(): void {
  const text = draft.value.trim()
  if (!text) return
  emit('send', text)
  draft.value = ''
}
</script>

<template>
  <form class="chat-message-input" @submit.prevent="submit">
    <input
      v-model="draft"
      class="chat-message-input__field"
      type="text"
      :placeholder="t('chat.placeholder')"
      :aria-label="t('chat.placeholder')"
      :disabled="disabled"
    />
    <button class="chat-message-input__send" type="submit" :disabled="disabled || !draft.trim()">
      <i class="pi pi-send" aria-hidden="true"></i>
      {{ t('chat.send') }}
    </button>
  </form>
</template>

<style scoped src="@/styles/chat/chat-message-input.css"></style>
