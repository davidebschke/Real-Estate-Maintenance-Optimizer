<script setup lang="ts">
import { computed, ref } from 'vue'
import Popover from 'primevue/popover'
import { useI18n } from 'vue-i18n'

/** Example account entry shown for demo purposes only, no backend integration. */
interface ExampleAccount {
  id: string
  name: string
  role: string
  email: string
  initials: string
  color: string
}

/** Static example accounts, standing in for real account switching until auth is implemented. */
const exampleAccounts: ExampleAccount[] = [
  {
    id: 'marco-keller',
    name: 'Marco Keller',
    role: 'Haustechnik · Außendienst',
    email: 'm.keller@rmo-koeln.de',
    initials: 'MK',
    color: '#f5a623',
  },
  {
    id: 'anna-braun',
    name: 'Anna Braun',
    role: 'Disposition · Büro',
    email: 'a.braun@rmo-koeln.de',
    initials: 'AB',
    color: '#3fae8f',
  },
  {
    id: 'thomas-wagner',
    name: 'Thomas Wagner',
    role: 'Eigentümer · Portfolio',
    email: 't.wagner@rmo-koeln.de',
    initials: 'TW',
    color: '#4a7fd6',
  },
]

const defaultAccount = exampleAccounts[0]!

const { t } = useI18n()
const popoverRef = ref<InstanceType<typeof Popover> | null>(null)
const activeAccountId = ref<string>(defaultAccount.id)

const activeAccount = computed<ExampleAccount>(
  () => exampleAccounts.find((account) => account.id === activeAccountId.value) ?? defaultAccount,
)

/** Opens or closes the account switcher popover from the trigger button. */
function toggle(event: MouseEvent): void {
  popoverRef.value?.toggle(event)
}

/** Switches the active example account and closes the popover. */
function selectAccount(accountId: string): void {
  activeAccountId.value = accountId
  popoverRef.value?.hide()
}
</script>

<template>
  <div class="user-account-dropdown">
    <button type="button" class="user-account-dropdown__trigger" @click="toggle">
      <span class="user-account-dropdown__avatar" :style="{ backgroundColor: activeAccount.color }">
        {{ activeAccount.initials }}
      </span>
      <span class="user-account-dropdown__identity">
        <span class="user-account-dropdown__name">{{ activeAccount.name }}</span>
        <span class="user-account-dropdown__role">{{ activeAccount.role }}</span>
      </span>
      <i class="pi pi-chevron-down user-account-dropdown__chevron" aria-hidden="true"></i>
    </button>

    <Popover ref="popoverRef" class="user-account-dropdown__popover">
      <div class="user-account-dropdown__header">
        <span class="user-account-dropdown__avatar" :style="{ backgroundColor: activeAccount.color }">
          {{ activeAccount.initials }}
        </span>
        <span class="user-account-dropdown__header-text">
          <span class="user-account-dropdown__name">{{ activeAccount.name }}</span>
          <span class="user-account-dropdown__email">{{ activeAccount.email }}</span>
        </span>
      </div>

      <div class="user-account-dropdown__divider"></div>

      <span class="user-account-dropdown__section-label">{{ t('header.account.switchAccount') }}</span>
      <ul class="user-account-dropdown__account-list">
        <li v-for="account in exampleAccounts" :key="account.id">
          <button
            type="button"
            class="user-account-dropdown__account-option"
            :class="{ 'user-account-dropdown__account-option--active': account.id === activeAccountId }"
            @click="selectAccount(account.id)"
          >
            <span class="user-account-dropdown__avatar" :style="{ backgroundColor: account.color }">
              {{ account.initials }}
            </span>
            <span class="user-account-dropdown__identity">
              <span class="user-account-dropdown__name">{{ account.name }}</span>
              <span class="user-account-dropdown__role">{{ account.role }}</span>
            </span>
            <i
              v-if="account.id === activeAccountId"
              class="pi pi-check user-account-dropdown__check"
              aria-hidden="true"
            ></i>
          </button>
        </li>
      </ul>

      <div class="user-account-dropdown__divider"></div>

      <ul class="user-account-dropdown__menu">
        <li>
          <button type="button" class="user-account-dropdown__menu-item">
            {{ t('header.account.menu.profile') }}
          </button>
        </li>
        <li>
          <button type="button" class="user-account-dropdown__menu-item">
            {{ t('header.account.menu.vehicle') }}
          </button>
        </li>
        <li>
          <button type="button" class="user-account-dropdown__menu-item">
            {{ t('header.account.menu.notifications') }}
          </button>
        </li>
      </ul>

      <div class="user-account-dropdown__divider"></div>

      <button type="button" class="user-account-dropdown__logout">
        {{ t('header.account.menu.logout') }}
      </button>
    </Popover>
  </div>
</template>

<style scoped src="@/styles/layout/user-account-dropdown.css"></style>
<style src="@/styles/layout/user-account-dropdown-popover.css"></style>
