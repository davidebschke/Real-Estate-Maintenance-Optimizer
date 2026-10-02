<script setup lang="ts">
import { computed, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import RequiredFieldLabel from '@/components/forms/RequiredFieldLabel.vue'
import { useAuthStore } from '@/stores/auth'
import { useSettingsSubmission } from '@/composables/useSettingsSubmission'
import { useTouchedFields } from '@/composables/useTouchedFields'
import { findNewPasswordProblem } from '@/utils/accountFieldValidation'

const { t } = useI18n()
const authStore = useAuthStore()
const form = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const { touched, markTouched, resetTouched } = useTouchedFields(['newPassword', 'confirmPassword'])
const { isSaving, isSaved, failureReason, submit, clearFeedback } = useSettingsSubmission(
  (currentPassword: string, newPassword: string) => authStore.changePassword(currentPassword, newPassword),
)

const newPasswordProblem = computed(() => findNewPasswordProblem(form.newPassword, form.currentPassword))
const isMismatching = computed(() => form.confirmPassword !== form.newPassword)
const isNewPasswordErrorShown = computed(() => touched.newPassword && newPasswordProblem.value !== null)
const isConfirmErrorShown = computed(() => touched.confirmPassword && isMismatching.value)
const canSubmit = computed(
  () => form.currentPassword.length > 0 && newPasswordProblem.value === null && !isMismatching.value,
)

/** Changes the password and clears every field again once it succeeded. */
async function save(): Promise<void> {
  if (!canSubmit.value) return
  if (await submit(form.currentPassword, form.newPassword)) {
    form.currentPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
    resetTouched()
  }
}
</script>

<template>
  <section class="profile-settings-dialog__section" aria-labelledby="profile-password-heading">
    <h3 id="profile-password-heading" class="profile-settings-dialog__heading">{{ t('profile.password.heading') }}</h3>
    <p class="profile-settings-dialog__description">{{ t('profile.password.description') }}</p>

    <div class="profile-settings-dialog__field">
      <RequiredFieldLabel field-id="profile-current-password" :label="t('profile.password.currentLabel')" />
      <InputText
        id="profile-current-password"
        v-model="form.currentPassword"
        type="password"
        maxlength="128"
        autocomplete="current-password"
        aria-required="true"
        @input="clearFeedback"
      />
    </div>

    <div class="profile-settings-dialog__grid">
      <div class="profile-settings-dialog__field">
        <RequiredFieldLabel field-id="profile-new-password" :label="t('profile.password.newLabel')" />
        <InputText
          id="profile-new-password"
          v-model="form.newPassword"
          type="password"
          maxlength="128"
          autocomplete="new-password"
          :invalid="isNewPasswordErrorShown"
          aria-required="true"
          :aria-describedby="isNewPasswordErrorShown ? 'profile-new-password-error' : undefined"
          @blur="markTouched('newPassword')"
          @input="clearFeedback"
        />
        <p
          v-if="isNewPasswordErrorShown && newPasswordProblem"
          id="profile-new-password-error"
          class="profile-settings-dialog__field-error"
        >
          {{ t(`profile.password.problems.${newPasswordProblem}`) }}
        </p>
      </div>

      <div class="profile-settings-dialog__field">
        <RequiredFieldLabel field-id="profile-confirm-password" :label="t('profile.password.confirmLabel')" />
        <InputText
          id="profile-confirm-password"
          v-model="form.confirmPassword"
          type="password"
          maxlength="128"
          autocomplete="new-password"
          :invalid="isConfirmErrorShown"
          aria-required="true"
          :aria-describedby="isConfirmErrorShown ? 'profile-confirm-password-error' : undefined"
          @blur="markTouched('confirmPassword')"
          @input="clearFeedback"
          @keydown.enter.prevent="save"
        />
        <p v-if="isConfirmErrorShown" id="profile-confirm-password-error" class="profile-settings-dialog__field-error">
          {{ t('profile.password.problems.mismatch') }}
        </p>
      </div>
    </div>

    <p v-if="failureReason" class="profile-settings-dialog__field-error" role="alert">
      {{ t(`profile.errors.${failureReason}`) }}
    </p>
    <p v-if="isSaved" class="profile-settings-dialog__success" role="status">{{ t('profile.password.success') }}</p>

    <Button
      class="profile-settings-dialog__submit"
      :label="isSaving ? t('profile.saving') : t('profile.password.submit')"
      :disabled="!canSubmit || isSaving"
      @click="save"
    />
  </section>
</template>
