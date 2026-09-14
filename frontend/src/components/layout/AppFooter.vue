<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import FooterVersion from '@/components/layout/FooterVersion.vue'

const { t } = useI18n()
const currentYear = new Date().getFullYear()
</script>

<template>
  <footer class="app-footer">
    <span class="app-footer__copyright">{{ t('footer.copyright', { year: currentYear }) }}</span>
    <FooterVersion class="app-footer__version" />
  </footer>
</template>

<style scoped src="@/styles/layout/app-footer.css"></style>
