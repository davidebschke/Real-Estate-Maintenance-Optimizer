<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import remoLogo from '@/assets/icons/remo-logo.svg'
import NavigationMenu from '@/components/layout/NavigationMenu.vue'
import LanguageSwitch from '@/components/layout/LanguageSwitch.vue'
import UserAccountDropdown from '@/components/layout/UserAccountDropdown.vue'
import AppointmentButton from '@/components/layout/AppointmentButton.vue'
import ComingSoonDialog from '@/components/layout/ComingSoonDialog.vue'
import { useNavigation } from '@/composables/useNavigation'

const { t } = useI18n()
const { isPopupVisible } = useNavigation()
</script>

<template>
  <header class="app-header">
    <div class="app-header__brand">
      <img :src="remoLogo" alt="" class="app-header__logo" />
      <span class="app-header__brand-name">{{ t('header.brand') }}</span>
    </div>

    <NavigationMenu class="app-header__navigation" />

    <div class="app-header__actions">
      <AppointmentButton />
      <LanguageSwitch />
      <UserAccountDropdown />
    </div>

    <ComingSoonDialog v-model:visible="isPopupVisible" />
  </header>
</template>

<style scoped src="@/styles/layout/app-header.css"></style>
