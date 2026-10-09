<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'

/** One statistics section reachable from the statistics page. */
interface StatisticsMenuEntry {
  routeName: string
  icon: string
  titleKey: string
  descriptionKey: string
}

const menuEntries: StatisticsMenuEntry[] = [
  {
    routeName: 'statistics-properties',
    icon: 'pi-building',
    titleKey: 'statistics.menu.properties.title',
    descriptionKey: 'statistics.menu.properties.description',
  },
]

const { t } = useI18n()
</script>

<template>
  <nav :aria-label="t('statistics.menu.label')">
    <ul class="statistics-menu">
      <li v-for="entry in menuEntries" :key="entry.routeName">
        <RouterLink :to="{ name: entry.routeName }" class="statistics-menu__entry">
          <i class="pi statistics-menu__icon" :class="entry.icon" aria-hidden="true"></i>
          <span class="statistics-menu__text">
            <span class="statistics-menu__title">{{ t(entry.titleKey) }}</span>
            <span class="statistics-menu__description">{{ t(entry.descriptionKey) }}</span>
          </span>
          <i class="pi pi-chevron-right statistics-menu__chevron" aria-hidden="true"></i>
        </RouterLink>
      </li>
    </ul>
  </nav>
</template>

<style scoped src="@/styles/statistics/statistics-menu.css"></style>
