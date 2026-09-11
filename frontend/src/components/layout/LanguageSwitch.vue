<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useLocale, type AppLocale } from '@/composables/useLocale'

/** List of locales offered by the language switch, in display order. */
const availableLocales: AppLocale[] = ['de', 'en']

const { t } = useI18n()
const { isActive, setLocale } = useLocale()
</script>

<template>
  <div class="language-switch" role="group" aria-label="Language selection">
    <button
      v-for="localeOption in availableLocales"
      :key="localeOption"
      type="button"
      class="language-switch__option"
      :class="{ 'language-switch__option--active': isActive(localeOption) }"
      :aria-pressed="isActive(localeOption)"
      @click="setLocale(localeOption)"
    >
      {{ t(`header.language.${localeOption}`) }}
    </button>
  </div>
</template>

<style scoped>
.language-switch {
  display: flex;
  gap: 0.3rem;
}

.language-switch__option {
  background: transparent;
  border: none;
  border-radius: 6px;
  color: var(--remo-header-text-muted-color, #9ca3af);
  cursor: pointer;
  font-size: 1.02rem;
  font-weight: 600;
  padding: 0.42rem 0.6rem;
  transition:
    background-color 0.15s ease-in-out,
    color 0.15s ease-in-out;
}

.language-switch__option:hover {
  background-color: var(--remo-header-hover-color, #374151);
}

.language-switch__option--active {
  color: var(--remo-header-active-color, #ffffff);
}
</style>
