<template>
  <div class="bar-placeholder">artwork bar</div>
</template>

<script setup lang="ts">
// Placeholder — replaced by the artwork picker next.
defineProps<{ focusedId: string; canDownload: boolean; openSignal: number }>()
defineEmits<{ (e: 'download'): void; (e: 'expanded-change', open: boolean): void }>()
</script>

<style scoped>
.bar-placeholder {
  pointer-events: auto;
  padding: 8px 16px;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  color: #71717a;
}
</style>
