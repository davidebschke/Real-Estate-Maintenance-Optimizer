<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
</script>

<template>
  <AppHeader />
  <main class="app-content">
    <RouterView />
  </main>
</template>

<style scoped>
.app-content {
  padding: 1.5rem;
}
</style>
