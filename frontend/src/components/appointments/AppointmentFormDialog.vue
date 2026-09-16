<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import ToggleSwitch from 'primevue/toggleswitch'
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'
import AppointmentPropertySelect from '@/components/appointments/AppointmentPropertySelect.vue'
import AppointmentMaterialInput from '@/components/appointments/AppointmentMaterialInput.vue'
import AppointmentAiSuggestionBanner from '@/components/appointments/AppointmentAiSuggestionBanner.vue'
import { useAppointmentsStore } from '@/stores/appointments'
import { usePropertiesStore } from '@/stores/properties'
import { useLocale } from '@/composables/useLocale'
import {
  DURATION_OPTIONS,
  RECURRENCE_INTERVAL_OPTIONS,
  combineDayAndTime,
  generateTimeSlotOptions,
  generateUpcomingDayOptions,
} from '@/utils/appointmentSchedulingOptions'

const TITLE_MAX_LENGTH = 50
const DESCRIPTION_MAX_LENGTH = 2000

const visible = defineModel<boolean>('visible', { required: true })

const { t } = useI18n()
const { currentLocale } = useLocale()
const store = useAppointmentsStore()
const propertiesStore = usePropertiesStore()

const timeOptions = generateTimeSlotOptions()
const dayOptions = computed(() => generateUpcomingDayOptions(new Date(), currentLocale.value))
const durationOptions = computed(() =>
  DURATION_OPTIONS.map((option) => ({
    ...option,
    label: t(`appointments.form.duration.options.${option.key}`),
  })),
)
const recurrenceIntervalOptions = computed(() =>
  RECURRENCE_INTERVAL_OPTIONS.map((option) => ({
    ...option,
    label: t(`appointments.form.recurrence.options.${option.key}`),
  })),
)

const form = reactive({
  title: '',
  propertyId: null as string | null,
  description: '',
  day: '',
  time: '',
  durationMinutes: 60,
  locked: false,
  recurring: false,
  recurrenceIntervalMonths: null as number | null,
  materials: [] as string[],
})

const isValid = computed(
  () =>
    form.title.trim().length > 0 &&
    form.title.length <= TITLE_MAX_LENGTH &&
    form.propertyId !== null &&
    form.description.length <= DESCRIPTION_MAX_LENGTH &&
    form.day !== '' &&
    form.time !== '' &&
    (!form.recurring || form.recurrenceIntervalMonths !== null),
)

watch(visible, (isVisible) => {
  if (isVisible) resetForm()
})

/** Resets every field to its default, called each time the dialog is opened. */
function resetForm() {
  form.title = ''
  form.propertyId = null
  form.description = ''
  form.day = dayOptions.value[0]?.value ?? ''
  form.time = timeOptions[0]?.value ?? ''
  form.durationMinutes = DURATION_OPTIONS[0]?.minutes ?? 60
  form.locked = false
  form.recurring = false
  form.recurrenceIntervalMonths = null
  form.materials = []
}

/** Creates the appointment from the current form state, then closes the dialog. */
async function submit() {
  if (!isValid.value || form.propertyId === null) return

  const property = propertiesStore.properties.find((candidate) => candidate.id === form.propertyId)
  if (!property) return

  await store.createAppointment({
    title: form.title,
    propertyId: property.id,
    propertyName: property.name,
    propertyAddress: property.address,
    description: form.description,
    start: combineDayAndTime(form.day, form.time),
    durationMinutes: form.durationMinutes,
    locked: form.locked,
    recurring: form.recurring,
    recurrenceIntervalMonths: form.recurring ? form.recurrenceIntervalMonths : null,
    materials: form.materials,
  })

  visible.value = false
}

/** Closes the dialog without creating an appointment. */
function cancel() {
  visible.value = false
}
</script>

<template>
  <Dialog
    v-model:visible="visible"
    modal
    :header="t('appointments.form.title')"
    class="appointment-form-dialog"
  >
    <div class="appointment-form-dialog__grid">
      <div class="appointment-form-dialog__field">
        <label for="appointment-title">{{ t('appointments.form.titleLabel') }}</label>
        <InputText
          id="appointment-title"
          v-model="form.title"
          :maxlength="TITLE_MAX_LENGTH"
          :placeholder="t('appointments.form.titlePlaceholder')"
        />
      </div>

      <AppointmentPropertySelect v-model="form.propertyId" class="appointment-form-dialog__field" />
    </div>

    <div class="appointment-form-dialog__field">
      <label for="appointment-description">{{ t('appointments.form.descriptionLabel') }}</label>
      <Textarea
        id="appointment-description"
        v-model="form.description"
        :maxlength="DESCRIPTION_MAX_LENGTH"
        rows="4"
        :placeholder="t('appointments.form.descriptionPlaceholder')"
      />
    </div>

    <div class="appointment-form-dialog__grid appointment-form-dialog__grid--schedule">
      <div class="appointment-form-dialog__field">
        <label for="appointment-day">{{ t('appointments.form.dayLabel') }}</label>
        <Select
          input-id="appointment-day"
          v-model="form.day"
          :options="dayOptions"
          option-label="label"
          option-value="value"
        />
      </div>
      <div class="appointment-form-dialog__field">
        <label for="appointment-time">{{ t('appointments.form.timeLabel') }}</label>
        <Select
          input-id="appointment-time"
          v-model="form.time"
          :options="timeOptions"
          option-label="label"
          option-value="value"
        />
      </div>
      <div class="appointment-form-dialog__field">
        <label for="appointment-duration">{{ t('appointments.form.durationLabel') }}</label>
        <Select
          input-id="appointment-duration"
          v-model="form.durationMinutes"
          :options="durationOptions"
          option-label="label"
          option-value="minutes"
        />
      </div>
    </div>

    <div class="appointment-form-dialog__toggles">
      <label class="appointment-form-dialog__toggle">
        <ToggleSwitch v-model="form.locked" />
        <span>{{ t('appointments.form.lockedLabel') }}</span>
      </label>
      <label class="appointment-form-dialog__toggle">
        <ToggleSwitch v-model="form.recurring" />
        <span>{{ t('appointments.form.recurringLabel') }}</span>
      </label>
      <Select
        v-if="form.recurring"
        v-model="form.recurrenceIntervalMonths"
        input-id="appointment-recurrence-interval"
        :aria-label="t('appointments.form.recurringLabel')"
        class="appointment-form-dialog__recurrence-interval"
        :options="recurrenceIntervalOptions"
        option-label="label"
        option-value="months"
        :placeholder="t('appointments.form.recurrenceIntervalPlaceholder')"
      />
    </div>

    <AppointmentMaterialInput v-model="form.materials" />

    <AppointmentAiSuggestionBanner />

    <div class="appointment-form-dialog__actions">
      <Button :label="t('appointments.form.submit')" :disabled="!isValid" @click="submit" />
      <button type="button" class="appointment-form-dialog__cancel" @click="cancel">
        {{ t('appointments.form.cancel') }}
      </button>
    </div>
  </Dialog>
</template>

<style src="@/styles/appointments/appointment-form-dialog.css"></style>
