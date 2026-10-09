<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDemoQuota } from '@/composables/useDemoQuota'
import type { DemoQuotaResource } from '@/types/auth'

const props = defineProps<{ resource: DemoQuotaResource }>()

const { t } = useI18n()
const { remaining, isLimited, isExhausted } = useDemoQuota(props.resource)

const RESOURCE_KEYS: Record<DemoQuotaResource, string> = {
  properties: 'Properties',
  appointments: 'Appointments',
  tenants: 'Tenants',
}

const resourceKey = computed(() => RESOURCE_KEYS[props.resource])

const message = computed(() => {
  if (isExhausted.value) return t(`auth.quota.exhausted${resourceKey.value}`)
  return t(`auth.quota.remaining${resourceKey.value}`, remaining.value ?? 0)
})
</script>

<template>
  <p
    v-if="isLimited"
    class="demo-quota-hint"
    :class="{ 'demo-quota-hint--exhausted': isExhausted }"
    role="status"
  >
    <i class="pi" :class="isExhausted ? 'pi-lock' : 'pi-info-circle'" aria-hidden="true"></i>
    <span>{{ message }}</span>
  </p>
</template>

<style scoped src="@/styles/auth/demo-quota-hint.css"></style>
