<template>
  <div class="tags-page">
    <div class="container">
      <h1 class="page-title">标签</h1>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else class="tag-cloud">
        <router-link
          v-for="tag in tags"
          :key="tag.id"
          :to="`/tags/${tag.slug}`"
          class="tag-item"
          :style="{ fontSize: getSize(tag.articleCount) }"
        >
          #{{ tag.name }}
          <span class="count">({{ tag.articleCount || 0 }})</span>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { tagApi } from '@/api'

const tags = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await tagApi.getList()
    if (res.data.code === 200) tags.value = res.data.data
  } catch (_) {}
  loading.value = false
})

function getSize(count: number) {
  if (!count) return '14px'
  return Math.min(14 + count * 2, 32) + 'px'
}
</script>

<style scoped>
.page-title { font-size: 28px; padding: 40px 0; }
.tag-cloud { display: flex; flex-wrap: wrap; gap: 16px; padding: 20px 0; align-items: center; }
.tag-item { color: var(--accent-color) !important; padding: 8px 16px; border: 1px solid var(--border-color); border-radius: 20px; transition: all 0.2s; }
.tag-item:hover { border-color: var(--accent-color); background: var(--accent-color); color: white !important; }
.count { font-size: 12px; opacity: 0.7; }
.loading { text-align: center; padding: 80px; color: var(--text-muted); }
</style>