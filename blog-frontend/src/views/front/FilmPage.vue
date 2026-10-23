<template>
  <div ref="hostEl" class="film-island-host"></div>
</template>

<script setup lang="ts">
/* /film 页挂载壳：承载原版 React 组件岛（FilmPage.tsx 来自 michelletliu/michelle-liu，MIT），
   图片数据由后端 /api/film-photos 提供（图库照片） */
import { ref, onMounted, onBeforeUnmount } from 'vue'

const hostEl = ref<HTMLElement | null>(null)
let unmount: (() => void) | null = null

onMounted(async () => {
  const { mountFilmPage } = await import('@/film-island/mount')
  if (hostEl.value) {
    unmount = mountFilmPage(hostEl.value)
  }
})

onBeforeUnmount(() => {
  unmount?.()
  unmount = null
})
</script>

<style scoped>
.film-island-host {
  min-height: 100vh;
}
</style>
