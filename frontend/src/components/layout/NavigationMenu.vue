<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import type { NavigationKey } from '@/router'

/** List of top-level navigation entries rendered in a fixed, reusable order. */
const navigationEntries: NavigationKey[] = ['overview', 'calendar', 'optimization', 'statistics', 'properties']

/** PrimeIcons class shown in front of the label of each navigation entry. */
const navigationIcons: Record<NavigationKey, string> = {
  overview: 'pi-objects-column',
  calendar: 'pi-calendar',
  optimization: 'pi-sparkles',
  statistics: 'pi-chart-line',
  properties: 'pi-building',
}

const { t } = useI18n()
const isMobileMenuOpen = ref(false)

defineExpose({ navigationEntries, navigationIcons })

/** Closes the collapsed mobile navigation list after a link was followed. */
function closeMobileMenu(): void {
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
        <RouterLink :to="{ name: entry }" class="navigation-menu__link" @click="closeMobileMenu">
          <i class="pi navigation-menu__icon" :class="navigationIcons[entry]" aria-hidden="true"></i>
          <span class="navigation-menu__label">{{ t(`header.nav.${entry}`) }}</span>
        </RouterLink>
      </li>
    </ul>
  </nav>
</template>

<style scoped src="@/styles/layout/navigation-menu.css"></style>
