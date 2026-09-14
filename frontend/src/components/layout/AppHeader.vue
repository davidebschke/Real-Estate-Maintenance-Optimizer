<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import remoLogo from '@/assets/icons/remo-logo.svg'
import NavigationMenu from '@/components/layout/NavigationMenu.vue'
import LanguageSwitch from '@/components/layout/LanguageSwitch.vue'
import UserAccountDropdown from '@/components/layout/UserAccountDropdown.vue'
import AppointmentButton from '@/components/layout/AppointmentButton.vue'

const { t } = useI18n()
</script>

<template>
  <header class="app-header">
    <RouterLink :to="{ name: 'overview' }" class="app-header__brand">
      <img :src="remoLogo" alt="" class="app-header__logo" />
      <span class="app-header__brand-name">{{ t('header.brand') }}</span>
    </RouterLink>

    <NavigationMenu class="app-header__navigation" />

    <div class="app-header__actions">
      <LanguageSwitch />
      <UserAccountDropdown />
      <AppointmentButton />
    </div>
  </header>
</template>

<style scoped src="@/styles/layout/app-header.css"></style>
