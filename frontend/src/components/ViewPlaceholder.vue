<script setup lang="ts">
defineProps<{ text: string }>()
</script>

<template>
  <p class="view-placeholder">{{ text }}</p>
</template>

<style scoped src="@/styles/view-placeholder.css"></style>
