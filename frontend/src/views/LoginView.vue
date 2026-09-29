<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import remoLogo from '@/assets/icons/remo-logo.svg'
import AuthCard from '@/components/auth/AuthCard.vue'
import LanguageSwitch from '@/components/layout/LanguageSwitch.vue'
import { resolveSafeRedirect } from '@/utils/safeRedirect'

/** How long the green success state stays visible before the app opens. */
const SUCCESS_REDIRECT_DELAY_MS = 800

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

/** Opens the originally requested page (or the overview) once the success state was visible for a moment. */
function openApp() {
  setTimeout(() => {
    router.replace(resolveSafeRedirect(route.query.redirect))
  }, SUCCESS_REDIRECT_DELAY_MS)
}
</script>

<template>
  <div class="login-view">
    <div class="login-view__language">
      <LanguageSwitch />
    </div>

    <main class="login-view__content">
      <div class="login-view__brand">
        <img :src="remoLogo" alt="" class="login-view__logo" />
        <span class="login-view__brand-name">{{ t('header.brand') }}</span>
      </div>
      <p class="login-view__tagline">{{ t('auth.tagline') }}</p>

      <AuthCard @authenticated="openApp" />
    </main>
  </div>
</template>

<style scoped src="@/styles/auth/login-view.css"></style>
