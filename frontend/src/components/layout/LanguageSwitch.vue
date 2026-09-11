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

<style scoped src="@/styles/layout/language-switch.css"></style>
