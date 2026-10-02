<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import Dialog from 'primevue/dialog'
import ProfileUsernameSection from '@/components/profile/ProfileUsernameSection.vue'
import ProfilePasswordSection from '@/components/profile/ProfilePasswordSection.vue'
import ProfileAppointmentBufferSection from '@/components/profile/ProfileAppointmentBufferSection.vue'

const { t } = useI18n()
const visible = defineModel<boolean>('visible', { required: true })
</script>

<template>
  <Dialog v-model:visible="visible" modal :header="t('profile.title')" class="profile-settings-dialog">
    <ProfileUsernameSection />
    <ProfilePasswordSection />
    <ProfileAppointmentBufferSection />

    <p class="profile-settings-dialog__required-legend">{{ t('profile.requiredFieldsLegend') }}</p>

    <div class="profile-settings-dialog__actions">
      <button type="button" class="profile-settings-dialog__close" @click="visible = false">
        {{ t('profile.close') }}
      </button>
    </div>
  </Dialog>
</template>

<style src="@/styles/profile/profile-settings-dialog.css"></style>
