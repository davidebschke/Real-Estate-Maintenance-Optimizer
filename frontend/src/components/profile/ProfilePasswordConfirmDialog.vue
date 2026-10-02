<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import RequiredFieldLabel from '@/components/forms/RequiredFieldLabel.vue'

const props = defineProps<{ isSaving: boolean; errorMessage: string | null }>()
const emit = defineEmits<{ confirm: [password: string] }>()

const { t } = useI18n()
const visible = defineModel<boolean>('visible', { required: true })
const password = ref('')

watch(visible, () => {
  password.value = ''
})

/** Hands the entered current password to the caller once something was typed and no save is running. */
function confirm(): void {
  if (password.value.length === 0 || props.isSaving) return
  emit('confirm', password.value)
}
</script>

<template>
  <Dialog
    v-model:visible="visible"
    modal
    :header="t('profile.confirm.title')"
    class="profile-settings-dialog profile-password-confirm-dialog"
  >
    <p class="profile-settings-dialog__description">{{ t('profile.confirm.description') }}</p>

    <div class="profile-settings-dialog__field">
      <RequiredFieldLabel field-id="profile-confirm-current-password" :label="t('profile.confirm.label')" />
      <InputText
        id="profile-confirm-current-password"
        v-model="password"
        type="password"
        maxlength="128"
        autocomplete="current-password"
        aria-required="true"
        :invalid="errorMessage !== null"
        :aria-describedby="errorMessage ? 'profile-confirm-error' : undefined"
        @keydown.enter.prevent="confirm"
      />
      <p v-if="errorMessage" id="profile-confirm-error" class="profile-settings-dialog__field-error" role="alert">
        {{ errorMessage }}
      </p>
    </div>

    <div class="profile-settings-dialog__actions profile-password-confirm-dialog__actions">
      <Button
        class="profile-password-confirm-dialog__submit"
        :label="isSaving ? t('profile.saving') : t('profile.confirm.submit')"
        :disabled="password.length === 0 || isSaving"
        @click="confirm"
      />
      <button type="button" class="profile-settings-dialog__close" @click="visible = false">
        {{ t('profile.confirm.cancel') }}
      </button>
    </div>
  </Dialog>
</template>
