<script setup lang="ts">
import { ref } from 'vue'
import Chip from 'primevue/chip'
import InputText from 'primevue/inputtext'
import { useI18n } from 'vue-i18n'

const materials = defineModel<string[]>({ required: true })

const { t } = useI18n()
const draftMaterial = ref('')

/** Adds the current draft as a new material tag, unless it is blank or already present. */
function addMaterial() {
  const trimmed = draftMaterial.value.trim()
  if (trimmed.length === 0 || materials.value.includes(trimmed)) {
    draftMaterial.value = ''
    return
  }

  materials.value = [...materials.value, trimmed]
  draftMaterial.value = ''
}

/** Removes the material tag at the given index. */
function removeMaterial(index: number) {
  materials.value = materials.value.filter((_, materialIndex) => materialIndex !== index)
}
</script>

<template>
  <div class="appointment-material-input">
    <label class="appointment-material-input__label" for="appointment-material-draft">
      {{ t('appointments.form.materials.label') }}
    </label>
    <div class="appointment-material-input__tags">
      <Chip
        v-for="(material, index) in materials"
        :key="material"
        class="appointment-material-input__chip"
        :label="material"
        removable
        @remove="removeMaterial(index)"
      />
      <InputText
        id="appointment-material-draft"
        v-model="draftMaterial"
        class="appointment-material-input__field"
        :placeholder="t('appointments.form.materials.placeholder')"
        @keydown.enter.prevent="addMaterial"
      />
    </div>
  </div>
</template>

<style scoped src="@/styles/appointments/appointment-material-input.css"></style>
