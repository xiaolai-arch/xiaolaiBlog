<template>
  <button type="button" class="ghost-btn" aria-label="关于这个画廊">
    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.5" />
      <path d="M12 11V16" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
      <circle cx="12" cy="8" r="0.9" fill="currentColor" />
    </svg>
  </button>
</template>

<script setup lang="ts">
// Placeholder — replaced by the ported info panel next.
</script>

<style scoped>
.ghost-btn {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 0;
  border-radius: 9999px;
  background: none;
  color: #a1a1aa;
  cursor: pointer;
}
.ghost-btn svg { width: 20px; height: 20px; }
.ghost-btn:hover { color: #52525b; }
</style>
