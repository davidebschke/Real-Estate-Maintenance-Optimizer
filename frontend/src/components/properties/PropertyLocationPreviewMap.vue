<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import L from 'leaflet'
import type { GeocodedPosition } from '@/services/geocodingService'

const props = defineProps<{
  position: GeocodedPosition | null
}>()

const mapContainer = ref<HTMLDivElement | null>(null)
let map: L.Map | null = null
let marker: L.Marker | null = null

const defaultCenter: L.LatLngTuple = [51.1657, 10.4515]
const defaultZoom = 6
const focusedZoom = 15

/** Places (or clears) the single preview marker and re-centers the map on it. */
function renderPosition(position: GeocodedPosition | null) {
  if (!map) {
    return
  }

  marker?.remove()
  marker = null

  if (position) {
    marker = L.marker([position.lat, position.lng]).addTo(map)
    map.setView([position.lat, position.lng], focusedZoom)
  } else {
    map.setView(defaultCenter, defaultZoom)
  }
}

onMounted(() => {
  if (!mapContainer.value) {
    return
  }

  map = L.map(mapContainer.value, {
    zoomControl: false,
    scrollWheelZoom: false,
    dragging: false,
  }).setView(defaultCenter, defaultZoom)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors',
  }).addTo(map)

  renderPosition(props.position)
})

watch(
  () => props.position,
  (position) => renderPosition(position),
)

onUnmounted(() => {
  map?.remove()
  map = null
  marker = null
})
</script>

<template>
  <div ref="mapContainer" class="property-location-preview-map"></div>
</template>

<style scoped src="@/styles/properties/property-location-preview-map.css"></style>
