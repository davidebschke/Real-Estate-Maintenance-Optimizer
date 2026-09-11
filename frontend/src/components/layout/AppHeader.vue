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

<style scoped>
.app-header {
  align-items: center;
  background-color: var(--remo-header-bg-color, #000000);
  border-bottom: 1px solid var(--remo-header-border-color, #1f2937);
  box-sizing: border-box;
  display: flex;
  gap: 1.8rem;
  height: 4.8rem;
  justify-content: space-between;
  left: 0;
  padding: 0 1.8rem;
  position: sticky;
  right: 0;
  top: 0;
  width: 100%;
  z-index: 100;
}

.app-header__brand {
  align-items: center;
  display: flex;
  gap: 0.5rem;
}

.app-header__logo {
  height: 2.4rem;
  width: 2.4rem;
}

.app-header__brand-name {
  color: var(--remo-header-text-color, #ffffff);
  font-size: 1.32rem;
  font-weight: 700;
}

.app-header__navigation {
  flex: 1;
  display: flex;
  justify-content: center;
}

.app-header__actions {
  align-items: center;
  display: flex;
  gap: 0.9rem;
}
</style>
