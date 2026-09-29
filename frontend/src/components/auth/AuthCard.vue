<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import LoginForm from '@/components/auth/LoginForm.vue'
import DemoAccountPanel from '@/components/auth/DemoAccountPanel.vue'

/** The two ways to get into the app offered by the card. */
type AuthMode = 'login' | 'demo'

const AUTH_MODES: AuthMode[] = ['login', 'demo']

const emit = defineEmits<{ authenticated: [] }>()

const { t } = useI18n()
const activeMode = ref<AuthMode>('login')
const isAuthenticated = ref(false)

/** Switches between login and demo account, unless a session was already started. */
function selectMode(mode: AuthMode) {
  if (isAuthenticated.value) return
  activeMode.value = mode
}

/** Turns the card green for the started session and passes the success on to the parent. */
function handleAuthenticated() {
  isAuthenticated.value = true
  emit('authenticated')
}
</script>

<template>
  <section
    class="auth-card card-3d"
    :class="{ 'auth-card--success': isAuthenticated }"
    aria-labelledby="auth-card-title"
  >
    <h1 id="auth-card-title" class="auth-card__title">{{ t('auth.title') }}</h1>
    <p class="auth-card__subtitle">{{ t('auth.subtitle') }}</p>

    <div class="auth-card__modes" role="tablist" :aria-label="t('auth.modes.label')">
      <button
        v-for="mode in AUTH_MODES"
        :id="`auth-mode-${mode}`"
        :key="mode"
        type="button"
        role="tab"
        class="auth-card__mode"
        :class="{ 'auth-card__mode--active': activeMode === mode }"
        :aria-selected="activeMode === mode"
        :aria-controls="`auth-panel-${mode}`"
        :disabled="isAuthenticated && activeMode !== mode"
        @click="selectMode(mode)"
      >
        {{ t(`auth.modes.${mode}`) }}
      </button>
    </div>

    <div :id="`auth-panel-${activeMode}`" role="tabpanel" :aria-labelledby="`auth-mode-${activeMode}`">
      <LoginForm v-if="activeMode === 'login'" @authenticated="handleAuthenticated" />
      <DemoAccountPanel v-else @authenticated="handleAuthenticated" />
    </div>
  </section>
</template>

<style scoped src="@/styles/card-3d.css"></style>
<style scoped src="@/styles/auth/auth-card.css"></style>
