<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import L from 'leaflet'
import type { AppointmentMapMarker } from '@/composables/useAppointmentMapMarkers'

const props = defineProps<{
  markers: AppointmentMapMarker[]
}>()

const mapContainer = ref<HTMLDivElement | null>(null)
let map: L.Map | null = null
let markerLayer: L.LayerGroup | null = null
let routeLine: L.Polyline | null = null
let resizeObserver: ResizeObserver | null = null

const defaultCenter: L.LatLngTuple = [51.1657, 10.4515]
const defaultZoom = 6

/** Redraws every marker and the connecting route line for the current appointments, then fits the view to them. */
function renderMarkers(markers: AppointmentMapMarker[]) {
  if (!map || !markerLayer) {
    return
  }

  markerLayer.clearLayers()
  routeLine?.remove()
  routeLine = null

  const latLngs: L.LatLngTuple[] = markers.map((marker) => [marker.lat, marker.lng])

  markers.forEach((marker) => {
    L.marker([marker.lat, marker.lng])
      .bindPopup(`${marker.position}. ${marker.appointment.title}`)
      .addTo(markerLayer as L.LayerGroup)
  })

  if (latLngs.length > 1) {
    routeLine = L.polyline(latLngs, { color: '#3b82f6' }).addTo(map)
  }

  if (latLngs.length > 0) {
    map.fitBounds(L.latLngBounds(latLngs), { padding: [32, 32] })
  } else {
    map.setView(defaultCenter, defaultZoom)
  }
}

onMounted(() => {
  if (!mapContainer.value) {
    return
  }

  map = L.map(mapContainer.value, { zoomControl: true, scrollWheelZoom: true }).setView(
    defaultCenter,
    defaultZoom,
  )
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors',
  }).addTo(map)
  markerLayer = L.layerGroup().addTo(map)

  renderMarkers(props.markers)

  resizeObserver = new ResizeObserver(() => map?.invalidateSize())
  resizeObserver.observe(mapContainer.value)
})

watch(
  () => props.markers,
  (markers) => renderMarkers(markers),
)

onUnmounted(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
  map?.remove()
  map = null
  markerLayer = null
  routeLine = null
})
</script>

<template>
  <div ref="mapContainer" class="appointment-map"></div>
</template>

<style scoped src="@/styles/map/appointment-map.css"></style>
