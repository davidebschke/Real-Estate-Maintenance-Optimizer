<script setup lang="ts">
defineProps<{ fieldId: string; label: string }>()
</script>

<template>
  <label :for="fieldId"
    >{{ label }}<span class="required-field-label__marker" aria-hidden="true"> *</span></label
  >
</template>

<style src="@/styles/forms/required-field-label.css"></style>
