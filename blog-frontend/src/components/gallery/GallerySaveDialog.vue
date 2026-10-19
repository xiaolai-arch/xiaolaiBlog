<template>
  <div v-if="open" class="save-placeholder">save dialog</div>
</template>

<script setup lang="ts">
// Placeholder — replaced by the ported share dialog next.
defineProps<{ open: boolean; hangs: unknown[]; anchor: HTMLElement | null }>()
defineEmits<{ (e: 'close'): void }>()
</script>

<style scoped>
.save-placeholder {
  position: fixed;
  top: 80px;
  right: 64px;
  z-index: 50;
  padding: 12px;
  border-radius: 12px;
  background: #fff;
  font-size: 13px;
}
</style>
