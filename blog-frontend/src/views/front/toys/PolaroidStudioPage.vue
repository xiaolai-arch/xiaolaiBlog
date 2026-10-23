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

<style scoped>
/* 原版页面在 body 上挂 Michelle 字体（--default-font-family）；本站 body 字体不同，
   在岛容器上补齐。岛内是 React 渲染的子树（无 Vue scoped 属性），需 :deep() 穿透；
   button/input 强制继承，避免落到主站或 UA 默认字体。线上版标题的 SF_Pro 任意值
   字体类并未生成（实际渲染 Michelle），这里中和该类保持一致。 */
.polaroid-island-host {
  font-family: 'Michelle', sans-serif;
}

.polaroid-island-host :deep(button),
.polaroid-island-host :deep(input) {
  font-family: inherit;
}

.polaroid-island-host :deep([class*='SF_Pro']) {
  font-family: 'Michelle', sans-serif;
}
</style>
