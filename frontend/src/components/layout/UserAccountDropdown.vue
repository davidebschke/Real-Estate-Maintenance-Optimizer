<script setup lang="ts">
import { ref } from 'vue'
import Menu from 'primevue/menu'
import type { MenuItem } from 'primevue/menuitem'
import { useI18n } from 'vue-i18n'

/** Example account entry shown for demo purposes only, no backend integration. */
interface ExampleAccount {
  name: string
  role: string
}

/** Static example account, standing in for the authenticated user until auth is implemented. */
const exampleAccount: ExampleAccount = { name: 'Marco Keller', role: 'Property Manager' }

const { t } = useI18n()
const menuRef = ref<InstanceType<typeof Menu> | null>(null)

/** Example dropdown entries; each action is a non-functional placeholder. */
const menuItems: MenuItem[] = [
  { label: t('header.account.menu.profile') },
  { label: t('header.account.menu.settings') },
  { separator: true },
  { label: t('header.account.menu.logout') },
]

/** Toggles the account dropdown menu on click of the account trigger. */
function toggleMenu(event: MouseEvent): void {
  menuRef.value?.toggle(event)
}
</script>

<template>
  <div class="user-account-dropdown">
    <button type="button" class="user-account-dropdown__trigger" @click="toggleMenu">
      {{ exampleAccount.name }}
    </button>
    <Menu ref="menuRef" :model="menuItems" :popup="true" />
  </div>
</template>

<style scoped src="@/styles/layout/user-account-dropdown.css"></style>
