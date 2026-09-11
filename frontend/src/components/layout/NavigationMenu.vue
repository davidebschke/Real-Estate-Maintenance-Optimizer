<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useNavigation, type NavigationKey } from '@/composables/useNavigation'

/** List of top-level navigation entries rendered in a fixed, reusable order. */
const navigationEntries: NavigationKey[] = ['overview', 'calendar', 'statistics', 'properties']

const { t } = useI18n()
const { selectNavigationEntry } = useNavigation()

defineExpose({ navigationEntries })

/** Forwards the click of a navigation entry to the shared navigation composable. */
function handleClick(key: NavigationKey): void {
  selectNavigationEntry(key)
}
</script>

<template>
  <nav class="navigation-menu" aria-label="Main navigation">
    <ul class="navigation-menu__list">
      <li v-for="entry in navigationEntries" :key="entry" class="navigation-menu__item">
        <button type="button" class="navigation-menu__link" @click="handleClick(entry)">
          {{ t(`header.nav.${entry}`) }}
        </button>
      </li>
    </ul>
  </nav>
</template>

<style scoped src="@/styles/layout/navigation-menu.css"></style>
