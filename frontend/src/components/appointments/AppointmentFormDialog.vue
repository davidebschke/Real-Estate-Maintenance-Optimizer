<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
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
import AppointmentConflictNotice from '@/components/appointments/AppointmentConflictNotice.vue'
import RequiredFieldLabel from '@/components/forms/RequiredFieldLabel.vue'
import DemoQuotaHint from '@/components/auth/DemoQuotaHint.vue'
import { useDemoQuota } from '@/composables/useDemoQuota'
import { useAppointmentsStore } from '@/stores/appointments'
import { usePropertiesStore } from '@/stores/properties'
import { useLocale } from '@/composables/useLocale'
import { useDurationOptions } from '@/composables/useDurationOptions'
import {
  DURATION_OPTIONS,
  RECURRENCE_INTERVAL_OPTIONS,
  combineDayAndTime,
  formatDayOption,
  generateTimeSlotOptions,
  generateUpcomingDayOptions,
  getDurationMinutes,
  includeTimeOption,
  toIsoDate,
  toTimeString,
} from '@/utils/appointmentSchedulingOptions'

const TITLE_MAX_LENGTH = 50
const DESCRIPTION_MAX_LENGTH = 2000

const visible = defineModel<boolean>('visible', { required: true })

const { t } = useI18n()
const { currentLocale } = useLocale()
const { durationOptions } = useDurationOptions()
const store = useAppointmentsStore()
const propertiesStore = usePropertiesStore()
const { isExhausted: isQuotaExhausted } = useDemoQuota('appointments')

/** Whether the dialog is currently editing an existing appointment rather than creating a new one. */
const isEditMode = computed(() => store.editingAppointment !== null)

/** Whether a demo account already used up its appointment creations, which blocks creating but never editing. */
const isBlockedByQuota = computed(() => !isEditMode.value && isQuotaExhausted.value)

const timeSlotOptions = generateTimeSlotOptions()
const dayOptions = computed(() => {
  const upcomingOptions = generateUpcomingDayOptions(new Date(), currentLocale.value)
  const editingStart = store.editingAppointment?.start
  if (!editingStart) return upcomingOptions

  const editingDayValue = toIsoDate(editingStart)
  if (upcomingOptions.some((option) => option.value === editingDayValue)) return upcomingOptions
  return [formatDayOption(editingStart, currentLocale.value), ...upcomingOptions]
})
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

const selectableTimeOptions = computed(() => includeTimeOption(timeSlotOptions, form.time))

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

/** Whether the title has been left (blurred) at least once, so its required-field hint may be shown. */
const isTitleTouched = ref(false)

/** Whether the title has been left empty after being touched, i.e. its required-field hint must be shown. */
const isTitleMissing = computed(() => isTitleTouched.value && form.title.trim().length === 0)

watch(visible, (isVisible) => {
  if (isVisible) resetForm()
})

watch(
  () => [form.day, form.time, form.durationMinutes],
  () => store.clearAppointmentConflict(),
)

/** Fills every field from the appointment being edited. */
function fillFormFromEditingAppointment(appointment: NonNullable<typeof store.editingAppointment>) {
  form.title = appointment.title
  form.propertyId = appointment.propertyId
  form.description = appointment.description
  form.day = toIsoDate(appointment.start)
  form.time = toTimeString(appointment.start)
  form.durationMinutes = getDurationMinutes(appointment.start, appointment.end)
  form.locked = appointment.locked
  form.recurring = appointment.recurring
  form.recurrenceIntervalMonths = appointment.recurrenceIntervalMonths
  form.materials = [...appointment.materials]
}

/** Resets every field to its default, or fills them from the appointment being edited, called each time the dialog is opened. */
function resetForm() {
  isTitleTouched.value = false
  store.clearAppointmentConflict()

  if (store.editingAppointment) {
    fillFormFromEditingAppointment(store.editingAppointment)
    return
  }

  form.title = ''
  form.propertyId = null
  form.description = ''
  form.day = dayOptions.value[0]?.value ?? ''
  form.time = timeSlotOptions[0]?.value ?? ''
  form.durationMinutes = DURATION_OPTIONS[0]?.minutes ?? 60
  form.locked = false
  form.recurring = false
  form.recurrenceIntervalMonths = null
  form.materials = []
}

/** Creates or updates the appointment from the current form state, then closes the dialog on success. */
async function submit() {
  if (!isValid.value || isBlockedByQuota.value || form.propertyId === null) return

  const property = propertiesStore.properties.find((candidate) => candidate.id === form.propertyId)
  if (!property) return

  const payload = {
    title: form.title,
    propertyId: property.id,
    description: form.description,
    start: combineDayAndTime(form.day, form.time),
    durationMinutes: form.durationMinutes,
    locked: form.locked,
    recurring: form.recurring,
    recurrenceIntervalMonths: form.recurring ? form.recurrenceIntervalMonths : null,
    materials: form.materials,
  }

  if (store.editingAppointment) {
    const succeeded = await store.updateAppointment(store.editingAppointment.id, payload)
    if (succeeded) visible.value = false
    return
  }

  const created = await store.createAppointment(payload)
  if (created) visible.value = false
}

/** Applies the account's suggested next free slot to the day and time fields without any further manual input. */
function acceptSuggestedSlot() {
  const conflict = store.appointmentConflict
  if (!conflict) return

  form.day = toIsoDate(conflict.suggestedStart)
  form.time = toTimeString(conflict.suggestedStart)
  store.clearAppointmentConflict()
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
    :header="isEditMode ? t('appointments.edit.title') : t('appointments.form.title')"
    class="appointment-form-dialog"
  >
    <div class="appointment-form-dialog__grid">
      <div class="appointment-form-dialog__field">
        <RequiredFieldLabel field-id="appointment-title" :label="t('appointments.form.titleLabel')" />
        <InputText
          id="appointment-title"
          v-model="form.title"
          :maxlength="TITLE_MAX_LENGTH"
          :invalid="isTitleMissing"
          aria-required="true"
          :aria-describedby="isTitleMissing ? 'appointment-title-error' : undefined"
          :placeholder="t('appointments.form.titlePlaceholder')"
          @blur="isTitleTouched = true"
        />
        <p v-if="isTitleMissing" id="appointment-title-error" class="appointment-form-dialog__field-error">
          {{ t('appointments.form.titleRequiredError') }}
        </p>
      </div>

      <AppointmentPropertySelect v-model="form.propertyId" required class="appointment-form-dialog__field" />
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
        <RequiredFieldLabel field-id="appointment-day" :label="t('appointments.form.dayLabel')" />
        <Select
          input-id="appointment-day"
          v-model="form.day"
          :options="dayOptions"
          option-label="label"
          option-value="value"
          aria-required="true"
        />
      </div>
      <div class="appointment-form-dialog__field">
        <RequiredFieldLabel field-id="appointment-time" :label="t('appointments.form.timeLabel')" />
        <Select
          input-id="appointment-time"
          v-model="form.time"
          :options="selectableTimeOptions"
          option-label="label"
          option-value="value"
          aria-required="true"
        />
      </div>
      <div class="appointment-form-dialog__field">
        <RequiredFieldLabel field-id="appointment-duration" :label="t('appointments.form.durationLabel')" />
        <Select
          input-id="appointment-duration"
          v-model="form.durationMinutes"
          :options="durationOptions"
          option-label="label"
          option-value="minutes"
          aria-required="true"
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

    <AppointmentAiSuggestionBanner v-if="!isEditMode" />

    <AppointmentConflictNotice
      v-if="!isEditMode && store.appointmentConflict"
      :conflict="store.appointmentConflict"
      @accept="acceptSuggestedSlot"
    />

    <p v-if="isEditMode && store.hasUpdateError" class="appointment-form-dialog__error">
      {{ t('appointments.edit.error') }}
    </p>

    <DemoQuotaHint v-if="!isEditMode" resource="appointments" />

    <p class="appointment-form-dialog__required-legend">{{ t('appointments.form.requiredFieldsLegend') }}</p>

    <div class="appointment-form-dialog__actions">
      <Button
        :label="isEditMode ? t('appointments.edit.submit') : t('appointments.form.submit')"
        :disabled="!isValid || isBlockedByQuota"
        @click="submit"
      />
      <button type="button" class="appointment-form-dialog__cancel" @click="cancel">
        {{ t('appointments.form.cancel') }}
      </button>
    </div>
  </Dialog>
</template>

<style src="@/styles/appointments/appointment-form-dialog.css"></style>
