<template>
  <div ref="hostEl" class="polaroid-island-host"></div>
</template>

<script setup lang="ts">
// 拍立得工作室：原版 React 岛挂载壳（liumichelle.com/project/polaroid 一比一复刻）
import { ref, onMounted, onBeforeUnmount } from 'vue'

const hostEl = ref<HTMLElement | null>(null)
let unmount: (() => void) | null = null

onMounted(async () => {
  const { mountPolaroidPage } = await import('@/film-island/mountPolaroid')
  if (hostEl.value) unmount = mountPolaroidPage(hostEl.value)
})

onBeforeUnmount(() => {
  unmount?.()
  unmount = null
})
</script>
