<script setup lang="ts">
import { ref } from 'vue'
import Popover from 'primevue/popover'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { useAccountPresentation } from '@/composables/useAccountPresentation'

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()
const { displayName, initials, roleLabel } = useAccountPresentation()
const popoverRef = ref<InstanceType<typeof Popover> | null>(null)
const isLoggingOut = ref(false)

/** Opens or closes the account menu popover from the trigger button. */
function toggle(event: MouseEvent): void {
  popoverRef.value?.toggle(event)
}

/** Ends the session (deleting a demo account) and returns to the login screen, even if the backend could not be reached, since the store forgets the session locally either way. */
async function logout(): Promise<void> {
  if (isLoggingOut.value) return
  isLoggingOut.value = true
  popoverRef.value?.hide()
  await authStore.logout().catch(() => undefined)
  isLoggingOut.value = false
  await router.replace({ name: 'login' })
}
</script>

<template>
  <div class="user-account-dropdown">
    <button type="button" class="user-account-dropdown__trigger" @click="toggle">
      <span class="user-account-dropdown__avatar">{{ initials }}</span>
      <span class="user-account-dropdown__identity">
        <span class="user-account-dropdown__name">{{ displayName }}</span>
        <span class="user-account-dropdown__role">{{ roleLabel }}</span>
      </span>
      <i class="pi pi-chevron-down user-account-dropdown__chevron" aria-hidden="true"></i>
    </button>

    <Popover ref="popoverRef" class="user-account-dropdown__popover">
      <div class="user-account-dropdown__header">
        <span class="user-account-dropdown__avatar">{{ initials }}</span>
        <span class="user-account-dropdown__header-text">
          <span class="user-account-dropdown__name">{{ displayName }}</span>
          <span class="user-account-dropdown__email">{{ roleLabel }}</span>
        </span>
      </div>

      <div class="user-account-dropdown__divider"></div>

      <ul class="user-account-dropdown__menu">
        <li>
          <button type="button" class="user-account-dropdown__menu-item">
            {{ t('header.account.menu.profile') }}
          </button>
        </li>
        <li>
          <button type="button" class="user-account-dropdown__menu-item">
            {{ t('header.account.menu.notifications') }}
          </button>
        </li>
      </ul>

      <div class="user-account-dropdown__divider"></div>

      <button type="button" class="user-account-dropdown__logout" :disabled="isLoggingOut" @click="logout">
        {{ t('header.account.menu.logout') }}
      </button>
    </Popover>
  </div>
</template>

<style scoped src="@/styles/layout/user-account-dropdown.css"></style>
<style src="@/styles/layout/user-account-dropdown-popover.css"></style>
