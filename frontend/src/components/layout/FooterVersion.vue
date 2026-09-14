<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAppVersion } from '@/composables/useAppVersion'

const { t } = useI18n()
const { version, loadVersion } = useAppVersion()

onMounted(loadVersion)
</script>

<template>
  <span v-if="version" class="footer-version">{{ t('footer.version', { version }) }}</span>
</template>

<style scoped src="@/styles/layout/footer-version.css"></style>
