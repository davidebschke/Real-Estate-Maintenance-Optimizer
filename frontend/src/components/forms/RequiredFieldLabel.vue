<script setup lang="ts">
withDefaults(defineProps<{ fieldId: string; label: string; required?: boolean }>(), {
  required: true,
})
</script>

<template>
  <label :for="fieldId"
    >{{ label }}<span v-if="required" class="required-field-label__marker" aria-hidden="true"> *</span></label
  >
</template>

<style src="@/styles/forms/required-field-label.css"></style>
