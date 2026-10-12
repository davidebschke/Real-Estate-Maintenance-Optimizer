<script setup lang="ts">
import InputText from 'primevue/inputtext'
import { useI18n } from 'vue-i18n'
import RequiredFieldLabel from '@/components/forms/RequiredFieldLabel.vue'
import { TENANT_NAME_MAX_LENGTH } from '@/utils/apartmentFieldValidation'

withDefaults(
  defineProps<{
    idPrefix: string
    required?: boolean
    showFirstNameError?: boolean
    showLastNameError?: boolean
  }>(),
  { required: true, showFirstNameError: false, showLastNameError: false },
)

const firstName = defineModel<string>('firstName', { required: true })
const lastName = defineModel<string>('lastName', { required: true })

defineEmits<{
  'blur-first-name': []
  'blur-last-name': []
}>()

const { t } = useI18n()
</script>

<template>
  <div class="tenant-name-fields">
    <div class="tenant-name-fields__field">
      <RequiredFieldLabel :field-id="`${idPrefix}-first-name`" :label="t('tenants.form.firstNameLabel')" :required="required" />
      <InputText
        :id="`${idPrefix}-first-name`"
        v-model="firstName"
        :maxlength="TENANT_NAME_MAX_LENGTH"
        :invalid="showFirstNameError"
        :aria-required="required ? 'true' : undefined"
        :aria-describedby="showFirstNameError ? `${idPrefix}-first-name-error` : undefined"
        :placeholder="t('tenants.form.firstNamePlaceholder')"
        @blur="$emit('blur-first-name')"
      />
      <p v-if="showFirstNameError" :id="`${idPrefix}-first-name-error`" class="tenant-name-fields__error">
        {{ t('tenants.form.firstNameRequiredError') }}
      </p>
    </div>

    <div class="tenant-name-fields__field">
      <RequiredFieldLabel :field-id="`${idPrefix}-last-name`" :label="t('tenants.form.lastNameLabel')" :required="required" />
      <InputText
        :id="`${idPrefix}-last-name`"
        v-model="lastName"
        :maxlength="TENANT_NAME_MAX_LENGTH"
        :invalid="showLastNameError"
        :aria-required="required ? 'true' : undefined"
        :aria-describedby="showLastNameError ? `${idPrefix}-last-name-error` : undefined"
        :placeholder="t('tenants.form.lastNamePlaceholder')"
        @blur="$emit('blur-last-name')"
      />
      <p v-if="showLastNameError" :id="`${idPrefix}-last-name-error`" class="tenant-name-fields__error">
        {{ t('tenants.form.lastNameRequiredError') }}
      </p>
    </div>
  </div>
</template>

<style src="@/styles/tenants/tenant-name-fields.css"></style>
