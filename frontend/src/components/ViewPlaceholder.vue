<script setup lang="ts">
defineProps<{ text: string }>()
</script>

<template>
  <div class="view-placeholder card-3d">
    <p class="view-placeholder__text">{{ text }}</p>
  </div>
</template>

<style scoped src="@/styles/view-placeholder.css"></style>
<style scoped src="@/styles/card-3d.css"></style>
