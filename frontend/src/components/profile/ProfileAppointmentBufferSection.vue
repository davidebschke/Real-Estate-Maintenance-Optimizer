<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import InputNumber from 'primevue/inputnumber'
import Button from 'primevue/button'
import RequiredFieldLabel from '@/components/forms/RequiredFieldLabel.vue'
import { useAuthStore } from '@/stores/auth'
import { useSettingsSubmission } from '@/composables/useSettingsSubmission'
import { useTouchedFields } from '@/composables/useTouchedFields'
import { isValidAppointmentBuffer, MAX_APPOINTMENT_BUFFER_MINUTES } from '@/utils/accountFieldValidation'

const { t } = useI18n()
const authStore = useAuthStore()
const bufferMinutes = ref<number | null>(authStore.currentUser?.appointmentBufferMinutes ?? null)
const { touched, markTouched } = useTouchedFields(['bufferMinutes'])
const { isSaving, isSaved, failureReason, submit, clearFeedback } = useSettingsSubmission((minutes: number) =>
  authStore.changeAppointmentBuffer(minutes),
)

const isInvalid = computed(() => !isValidAppointmentBuffer(bufferMinutes.value))
const isUnchanged = computed(() => bufferMinutes.value === authStore.currentUser?.appointmentBufferMinutes)
const isBufferErrorShown = computed(() => touched.bufferMinutes && isInvalid.value)

/** Saves the entered buffer time. */
async function save(): Promise<void> {
  if (!isValidAppointmentBuffer(bufferMinutes.value) || isUnchanged.value) return
  await submit(bufferMinutes.value)
}
</script>

<template>
  <section class="profile-settings-dialog__section" aria-labelledby="profile-buffer-heading">
    <h3 id="profile-buffer-heading" class="profile-settings-dialog__heading">{{ t('profile.buffer.heading') }}</h3>
    <p class="profile-settings-dialog__description">{{ t('profile.buffer.description') }}</p>

    <div class="profile-settings-dialog__field profile-settings-dialog__field--narrow">
      <RequiredFieldLabel field-id="profile-buffer" :label="t('profile.buffer.label')" />
      <InputNumber
        v-model="bufferMinutes"
        input-id="profile-buffer"
        :min="0"
        :max="MAX_APPOINTMENT_BUFFER_MINUTES"
        :use-grouping="false"
        :suffix="` ${t('profile.buffer.unit')}`"
        :invalid="isBufferErrorShown"
        aria-required="true"
        @blur="markTouched('bufferMinutes')"
        @input="clearFeedback"
        @keydown.enter.prevent="save"
      />
      <p v-if="isBufferErrorShown" class="profile-settings-dialog__field-error">
        {{ t('profile.buffer.invalidError') }}
      </p>
    </div>

    <p v-if="failureReason" class="profile-settings-dialog__field-error" role="alert">
      {{ t(`profile.errors.${failureReason}`) }}
    </p>
    <p v-if="isSaved" class="profile-settings-dialog__success" role="status">{{ t('profile.buffer.success') }}</p>

    <Button
      class="profile-settings-dialog__submit"
      :label="isSaving ? t('profile.saving') : t('profile.buffer.submit')"
      :disabled="isInvalid || isUnchanged || isSaving"
      @click="save"
    />
  </section>
</template>
