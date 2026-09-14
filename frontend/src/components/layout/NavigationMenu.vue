<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useNavigation, type NavigationKey } from '@/composables/useNavigation'

/** List of top-level navigation entries rendered in a fixed, reusable order. */
const navigationEntries: NavigationKey[] = ['overview', 'calendar', 'statistics', 'properties']

const { t } = useI18n()
const { selectNavigationEntry } = useNavigation()
const isMobileMenuOpen = ref(false)

defineExpose({ navigationEntries })

/** Forwards the click of a navigation entry to the shared navigation composable and closes the mobile menu. */
function handleClick(key: NavigationKey): void {
  selectNavigationEntry(key)
  isMobileMenuOpen.value = false
}

/** Opens or closes the collapsed mobile navigation list. */
function toggleMobileMenu(): void {
  isMobileMenuOpen.value = !isMobileMenuOpen.value
}
</script>

<template>
  <nav class="navigation-menu" aria-label="Main navigation">
    <button
      type="button"
      class="navigation-menu__toggle"
      :aria-expanded="isMobileMenuOpen"
      aria-label="Toggle navigation menu"
      @click="toggleMobileMenu"
    >
      <i class="pi" :class="isMobileMenuOpen ? 'pi-times' : 'pi-bars'" aria-hidden="true"></i>
    </button>

    <ul class="navigation-menu__list" :class="{ 'navigation-menu__list--open': isMobileMenuOpen }">
      <li v-for="entry in navigationEntries" :key="entry" class="navigation-menu__item">
        <button type="button" class="navigation-menu__link" @click="handleClick(entry)">
          {{ t(`header.nav.${entry}`) }}
        </button>
      </li>
    </ul>
  </nav>
</template>

<style scoped src="@/styles/layout/navigation-menu.css"></style>
