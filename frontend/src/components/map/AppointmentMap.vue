<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import L from 'leaflet'
import type { AppointmentMapMarker } from '@/composables/useAppointmentMapMarkers'

const props = defineProps<{
  markers: AppointmentMapMarker[]
  route?: [number, number][] | null
}>()

const mapContainer = ref<HTMLDivElement | null>(null)
let map: L.Map | null = null
let markerLayer: L.LayerGroup | null = null
let routeLine: L.Polyline | null = null
let resizeObserver: ResizeObserver | null = null

const defaultCenter: L.LatLngTuple = [51.1657, 10.4515]
const defaultZoom = 6

/** Redraws the line connecting the markers along the given road route, or as a dashed straight line while there is none. */
function renderRouteLine(markers: AppointmentMapMarker[], route: [number, number][] | null) {
  if (!map) {
    return
  }

  routeLine?.remove()
  routeLine = null

  if (route && route.length > 1) {
    routeLine = L.polyline(route, { color: '#3b82f6' }).addTo(map)
  } else if (markers.length > 1) {
    const latLngs: L.LatLngTuple[] = markers.map((marker) => [marker.lat, marker.lng])
    routeLine = L.polyline(latLngs, { color: '#3b82f6', dashArray: '6 8' }).addTo(map)
  }
}

/** Groups the markers by position, so appointments at the same property share one marker instead of covering each other. */
function groupByPosition(markers: AppointmentMapMarker[]): AppointmentMapMarker[][] {
  const groups = new Map<string, AppointmentMapMarker[]>()
  markers.forEach((marker) => {
    const key = `${marker.lat},${marker.lng}`
    groups.set(key, [...(groups.get(key) ?? []), marker])
  })
  return [...groups.values()]
}

/** Creates the marker icon showing the stop numbers of the given group, muted once all of its appointments are completed. */
function createNumberedIcon(group: AppointmentMapMarker[]): L.DivIcon {
  const isCompleted = group.every((marker) => marker.appointment.completed)
  return L.divIcon({
    className: `appointment-map__marker${isCompleted ? ' appointment-map__marker--completed' : ''}`,
    html: `<span>${group.map((marker) => marker.position).join(', ')}</span>`,
    iconSize: [28, 28],
    iconAnchor: [14, 14],
    popupAnchor: [0, -14],
  })
}

/** Creates the popup content listing the number and title of each appointment of the given group as plain text. */
function createPopupContent(group: AppointmentMapMarker[]): HTMLElement {
  const content = document.createElement('div')
  group.forEach((marker) => {
    const line = document.createElement('div')
    line.textContent = `${marker.position}. ${marker.appointment.title}`
    content.appendChild(line)
  })
  return content
}

/** Redraws every marker and the connecting line for the current appointments, then fits the view to them and the route. */
function renderMarkers(markers: AppointmentMapMarker[], route: [number, number][] | null) {
  if (!map || !markerLayer) {
    return
  }

  markerLayer.clearLayers()
  renderRouteLine(markers, route)

  const latLngs: L.LatLngTuple[] = markers.map((marker) => [marker.lat, marker.lng])

  groupByPosition(markers).forEach(([first, ...others]) => {
    if (!first) return
    const group = [first, ...others]
    L.marker([first.lat, first.lng], { icon: createNumberedIcon(group) })
      .bindPopup(createPopupContent(group))
      .addTo(markerLayer as L.LayerGroup)
  })

  if (latLngs.length > 0) {
    map.fitBounds(L.latLngBounds([...latLngs, ...(route ?? [])]), { padding: [32, 32] })
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

  renderMarkers(props.markers, props.route ?? null)

  resizeObserver = new ResizeObserver(() => map?.invalidateSize())
  resizeObserver.observe(mapContainer.value)
})

watch(
  () => props.markers,
  (markers) => renderMarkers(markers, props.route ?? null),
)

watch(
  () => props.route,
  (route) => {
    renderRouteLine(props.markers, route ?? null)
    if (route && map && !map.getBounds().contains(L.latLngBounds(route))) {
      map.fitBounds(
        L.latLngBounds([
          ...props.markers.map((marker): L.LatLngTuple => [marker.lat, marker.lng]),
          ...route,
        ]),
        {
          padding: [32, 32],
        },
      )
    }
  },
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
