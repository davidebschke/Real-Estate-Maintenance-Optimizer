<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { RouteMode } from '@/types/route'

const routeMode = defineModel<RouteMode>({ required: true })

const { t } = useI18n()

/** Ways of travelling the switch offers, in display order, with their icon. */
const modeOptions: { mode: RouteMode; icon: string }[] = [
  { mode: 'car', icon: 'pi-car' },
  { mode: 'walking', icon: 'pi-user' },
]
</script>

<template>
  <div class="route-mode-switch" role="group" :aria-label="t('overview.map.routeMode.label')">
    <button
      v-for="option in modeOptions"
      :key="option.mode"
      type="button"
      class="route-mode-switch__button"
      :class="{ 'route-mode-switch__button--active': routeMode === option.mode }"
      :aria-pressed="routeMode === option.mode"
      @click="routeMode = option.mode"
    >
      <i class="pi" :class="option.icon" aria-hidden="true"></i>
      {{ t(`overview.map.routeMode.${option.mode}`) }}
    </button>
  </div>
</template>

<style scoped src="@/styles/map/route-mode-switch.css"></style>
