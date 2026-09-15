<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import Drawer from 'primevue/drawer'
import Select from 'primevue/select'
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'
import { useAppointmentsStore } from '@/stores/appointments'
import { useLocale } from '@/composables/useLocale'
import { useAppointmentDeleteConfirmation } from '@/composables/useAppointmentDeleteConfirmation'
import {
  DURATION_OPTIONS,
  combineDayAndTime,
  generateTimeSlotOptions,
  generateUpcomingDayOptions,
  toIsoDate,
  toTimeString,
} from '@/utils/appointmentSchedulingOptions'

const visible = defineModel<boolean>('visible', { required: true })
const props = defineProps<{ appointmentId: string | null }>()

const { t } = useI18n()
const { currentLocale } = useLocale()
const store = useAppointmentsStore()
const { confirmDelete } = useAppointmentDeleteConfirmation()

const appointment = computed(
  () => store.appointments.find((candidate) => candidate.id === props.appointmentId) ?? null,
)

const scheduleRangeLabel = computed(() => {
  if (!appointment.value) return ''
  const dayFormatter = new Intl.DateTimeFormat(currentLocale.value, {
    weekday: 'short',
    day: '2-digit',
    month: '2-digit',
  })
  const timeFormatter = new Intl.DateTimeFormat(currentLocale.value, {
    hour: '2-digit',
    minute: '2-digit',
  })
  return `${dayFormatter.format(appointment.value.start)} · ${timeFormatter.format(appointment.value.start)}–${timeFormatter.format(appointment.value.end)}`
})

const timeOptions = generateTimeSlotOptions()
const dayOptions = computed(() => generateUpcomingDayOptions(new Date(), currentLocale.value, 120))
const durationOptions = computed(() =>
  DURATION_OPTIONS.map((option) => ({
    ...option,
    label: t(`appointments.form.duration.options.${option.key}`),
  })),
)

const isEditingSchedule = ref(false)
const scheduleForm = reactive({ day: '', time: '', durationMinutes: 60 })

watch(appointment, (current) => {
  isEditingSchedule.value = false
  if (!current) return
  scheduleForm.day = toIsoDate(current.start)
  scheduleForm.time = toTimeString(current.start)
  scheduleForm.durationMinutes = Math.round(
    (current.end.getTime() - current.start.getTime()) / 60000,
  )
})

/** Switches the schedule section into edit mode, pre-filled with the current schedule. */
function startEditingSchedule() {
  isEditingSchedule.value = true
}

/** Cancels rescheduling without saving. */
function cancelEditingSchedule() {
  isEditingSchedule.value = false
}

/** Sends the edited schedule to the backend and returns to the read-only view. */
async function saveSchedule() {
  if (!appointment.value) return

  await store.moveAppointment(appointment.value.id, {
    start: combineDayAndTime(scheduleForm.day, scheduleForm.time),
    durationMinutes: scheduleForm.durationMinutes,
  })
  isEditingSchedule.value = false
}

/** Closes the detail view. */
function close() {
  visible.value = false
}

/** Asks for delete confirmation, then deletes the appointment and closes the detail view. */
function requestDelete() {
  const current = appointment.value
  if (!current) return

  confirmDelete(current, async (scope) => {
    await store.deleteAppointment(current.id, scope)
    close()
  })
}
</script>

<template>
  <Drawer v-model:visible="visible" position="right" class="appointment-detail-drawer">
    <template v-if="appointment">
      <button type="button" class="appointment-detail-drawer__back" @click="close">
        {{ t('appointments.detail.back') }}
      </button>

      <div class="appointment-detail-drawer__badges">
        <span class="appointment-detail-drawer__badge">{{ scheduleRangeLabel }}</span>
        <span
          v-if="appointment.locked"
          class="appointment-detail-drawer__badge appointment-detail-drawer__badge--locked"
        >
          {{ t('appointments.detail.lockedBadge') }}
        </span>
        <span v-if="appointment.recurring" class="appointment-detail-drawer__badge">
          {{
            t('appointments.detail.recurringBadge', {
              months: appointment.recurrenceIntervalMonths,
            })
          }}
        </span>
      </div>

      <h2 class="appointment-detail-drawer__title">{{ appointment.title }}</h2>
      <p class="appointment-detail-drawer__property">
        {{ appointment.propertyName }} · {{ appointment.propertyAddress }}
      </p>

      <section class="appointment-detail-drawer__section">
        <h3>{{ t('appointments.detail.descriptionHeading') }}</h3>
        <p>{{ appointment.description || t('appointments.detail.noDescription') }}</p>
      </section>

      <section v-if="appointment.materials.length > 0" class="appointment-detail-drawer__section">
        <h3>{{ t('appointments.detail.materialsHeading') }}</h3>
        <ul class="appointment-detail-drawer__materials">
          <li v-for="material in appointment.materials" :key="material">{{ material }}</li>
        </ul>
      </section>

      <section class="appointment-detail-drawer__section">
        <h3>{{ t('appointments.detail.scheduleHeading') }}</h3>
        <p v-if="appointment.locked" class="appointment-detail-drawer__locked-hint">
          {{ t('appointments.detail.lockedHint') }}
        </p>
        <template v-else-if="!isEditingSchedule">
          <Button
            :label="t('appointments.detail.moveButton')"
            size="small"
            @click="startEditingSchedule"
          />
        </template>
        <div v-else class="appointment-detail-drawer__schedule-form">
          <label for="appointment-detail-day">{{ t('appointments.form.dayLabel') }}</label>
          <Select
            input-id="appointment-detail-day"
            v-model="scheduleForm.day"
            :options="dayOptions"
            option-label="label"
            option-value="value"
          />
          <label for="appointment-detail-time">{{ t('appointments.form.timeLabel') }}</label>
          <Select
            input-id="appointment-detail-time"
            v-model="scheduleForm.time"
            :options="timeOptions"
            option-label="label"
            option-value="value"
          />
          <label for="appointment-detail-duration">{{
            t('appointments.form.durationLabel')
          }}</label>
          <Select
            input-id="appointment-detail-duration"
            v-model="scheduleForm.durationMinutes"
            :options="durationOptions"
            option-label="label"
            option-value="minutes"
          />
          <div class="appointment-detail-drawer__schedule-actions">
            <Button
              :label="t('appointments.detail.saveScheduleButton')"
              size="small"
              @click="saveSchedule"
            />
            <button type="button" @click="cancelEditingSchedule">
              {{ t('appointments.form.cancel') }}
            </button>
          </div>
        </div>
      </section>

      <section class="appointment-detail-drawer__section">
        <h3>{{ t('appointments.detail.historyHeading') }}</h3>
        <ul class="appointment-detail-drawer__history">
          <li
            v-for="entry in appointment.history"
            :key="entry.timestamp.toISOString() + entry.message"
          >
            {{ entry.message }}
          </li>
        </ul>
      </section>

      <Button
        class="appointment-detail-drawer__delete"
        :label="t('appointments.detail.deleteButton')"
        severity="danger"
        size="small"
        @click="requestDelete"
      />
    </template>
  </Drawer>
</template>

<style scoped src="@/styles/appointments/appointment-detail-drawer.css"></style>
