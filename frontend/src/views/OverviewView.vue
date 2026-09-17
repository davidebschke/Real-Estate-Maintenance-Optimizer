<script setup lang="ts">
import DailyAppointmentList from '@/components/overview/DailyAppointmentList.vue'
import AppointmentMapCard from '@/components/map/AppointmentMapCard.vue'
</script>

<template>
  <div class="overview-view">
    <AppointmentMapCard />
    <DailyAppointmentList />
  </div>
</template>

<style scoped src="@/styles/overview/overview-view.css"></style>
