<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import RequiredFieldLabel from '@/components/forms/RequiredFieldLabel.vue'
import { useAuthStore } from '@/stores/auth'
import { useSettingsSubmission } from '@/composables/useSettingsSubmission'
import { useTouchedFields } from '@/composables/useTouchedFields'
import { isValidUsername } from '@/utils/accountFieldValidation'

const { t } = useI18n()
const authStore = useAuthStore()
const username = ref(authStore.currentUser?.username ?? '')
const { touched, markTouched } = useTouchedFields(['username'])
const { isSaving, isSaved, failureReason, submit, clearFeedback } = useSettingsSubmission(
  (name: string) => authStore.changeUsername(name),
  'usernameTaken',
)

const trimmedUsername = computed(() => username.value.trim())
const isInvalid = computed(() => !isValidUsername(trimmedUsername.value))
const isUnchanged = computed(() => trimmedUsername.value.toLowerCase() === authStore.currentUser?.username)
const isUsernameErrorShown = computed(() => touched.username && isInvalid.value)

/** Saves the entered username and shows the normalized one the backend stored. */
async function save(): Promise<void> {
  if (isInvalid.value || isUnchanged.value) return
  if (await submit(trimmedUsername.value)) {
    username.value = authStore.currentUser?.username ?? username.value
  }
}
</script>

<template>
  <section class="profile-settings-dialog__section" aria-labelledby="profile-username-heading">
    <h3 id="profile-username-heading" class="profile-settings-dialog__heading">{{ t('profile.username.heading') }}</h3>
    <p class="profile-settings-dialog__description">{{ t('profile.username.description') }}</p>

    <div class="profile-settings-dialog__field">
      <RequiredFieldLabel field-id="profile-username" :label="t('profile.username.label')" />
      <InputText
        id="profile-username"
        v-model="username"
        maxlength="50"
        autocomplete="username"
        :invalid="isUsernameErrorShown"
        aria-required="true"
        :aria-describedby="isUsernameErrorShown ? 'profile-username-error' : undefined"
        @blur="markTouched('username')"
        @input="clearFeedback"
        @keydown.enter.prevent="save"
      />
      <p v-if="isUsernameErrorShown" id="profile-username-error" class="profile-settings-dialog__field-error">
        {{ t('profile.username.invalidError') }}
      </p>
    </div>

    <p v-if="failureReason" class="profile-settings-dialog__field-error" role="alert">
      {{ t(`profile.errors.${failureReason}`) }}
    </p>
    <p v-if="isSaved" class="profile-settings-dialog__success" role="status">{{ t('profile.username.success') }}</p>

    <Button
      class="profile-settings-dialog__submit"
      :label="isSaving ? t('profile.saving') : t('profile.username.submit')"
      :disabled="isInvalid || isUnchanged || isSaving"
      @click="save"
    />
  </section>
</template>
