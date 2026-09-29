<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { toAuthFailureReason } from '@/services/authService'
import type { AuthFailureReason } from '@/types/auth'

const USERNAME_MAX_LENGTH = 50
const PASSWORD_MAX_LENGTH = 128

const emit = defineEmits<{ authenticated: [] }>()

const { t } = useI18n()
const authStore = useAuthStore()

const form = reactive({ username: '', password: '' })
const status = ref<'idle' | 'submitting' | 'success'>('idle')
const failureReason = ref<AuthFailureReason | null>(null)

const isValid = computed(() => form.username.trim().length > 0 && form.password.length > 0)

const submitLabel = computed(() => {
  if (status.value === 'success') return t('auth.login.success')
  if (status.value === 'submitting') return t('auth.login.submitting')
  return t('auth.login.submit')
})

/** Logs in with the entered credentials, showing the success state before notifying the parent, or the failure reason. */
async function submit() {
  if (!isValid.value || status.value !== 'idle') return

  status.value = 'submitting'
  failureReason.value = null
  try {
    await authStore.login(form.username.trim(), form.password)
    status.value = 'success'
    emit('authenticated')
  } catch (error) {
    failureReason.value = toAuthFailureReason(error)
    status.value = 'idle'
  } finally {
    form.password = ''
  }
}
</script>

<template>
  <form class="login-form" novalidate @submit.prevent="submit">
    <div class="login-form__field">
      <label for="login-username">{{ t('auth.login.usernameLabel') }}</label>
      <InputText
        id="login-username"
        v-model="form.username"
        autocomplete="username"
        autocapitalize="none"
        spellcheck="false"
        :maxlength="USERNAME_MAX_LENGTH"
        :placeholder="t('auth.login.usernamePlaceholder')"
        :disabled="status !== 'idle'"
        :invalid="failureReason === 'invalidCredentials'"
      />
    </div>

    <div class="login-form__field">
      <label for="login-password">{{ t('auth.login.passwordLabel') }}</label>
      <Password
        v-model="form.password"
        input-id="login-password"
        :input-props="{ autocomplete: 'current-password', maxlength: PASSWORD_MAX_LENGTH }"
        :feedback="false"
        toggle-mask
        fluid
        :disabled="status !== 'idle'"
        :invalid="failureReason === 'invalidCredentials'"
      />
    </div>

    <p v-if="failureReason" class="login-form__error" role="alert">
      {{ t(`auth.login.errors.${failureReason}`) }}
    </p>

    <Button
      type="submit"
      class="login-form__submit"
      :class="{ 'login-form__submit--success': status === 'success' }"
      :label="submitLabel"
      :icon="status === 'success' ? 'pi pi-check' : 'pi pi-sign-in'"
      :loading="status === 'submitting'"
      :disabled="!isValid || status !== 'idle'"
    />
  </form>
</template>

<style src="@/styles/auth/login-form.css"></style>
