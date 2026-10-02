<script setup lang="ts">
defineProps<{
  headingId: string
  heading: string
  description: string
  failureMessage: string | null
  successMessage: string | null
}>()
</script>

<template>
  <section class="profile-settings-dialog__section" :aria-labelledby="headingId">
    <h3 :id="headingId" class="profile-settings-dialog__heading">{{ heading }}</h3>
    <p class="profile-settings-dialog__description">{{ description }}</p>

    <slot />

    <p v-if="failureMessage" class="profile-settings-dialog__field-error" role="alert">{{ failureMessage }}</p>
    <p v-if="successMessage" class="profile-settings-dialog__success" role="status">{{ successMessage }}</p>

    <slot name="actions" />
  </section>
</template>
