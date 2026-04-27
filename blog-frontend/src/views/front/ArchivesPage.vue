<template>
  <div class="archives-page">
    <div class="container">
      <h1 class="page-title">Archives</h1>
      <div v-if="loading" class="loading">Loading...</div>
      <div v-else class="archive-list">
        <div v-for="(articles, ym) in archives" :key="ym" class="archive-month">
          <h2 class="month-title">{{ ym }}</h2>
          <div v-for="article in articles" :key="article.id" class="archive-item">
            <span class="archive-date">{{ formatDate(article.createTime) }}</span>
            <router-link :to="`/articles/${article.slug}`" class="archive-title">{{ article.title }}</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { articleApi } from '@/api'
import dayjs from 'dayjs'

const archives = ref<Record<string, any[]>>({})
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await articleApi.getArchives()
    if (res.data.code === 200) {
      archives.value = res.data.data.archives || {}
    }
  } catch (_) {}
  loading.value = false
})

function formatDate(d: string) { return dayjs(d).format('MM-DD') }
</script>

<style scoped>
.page-title { font-size: 28px; padding: 40px 0; }
.month-title { font-size: 22px; margin: 32px 0 16px; color: var(--accent-color); }
.archive-item { display: flex; align-items: center; gap: 16px; padding: 10px 0; border-bottom: 1px solid var(--border-color); }
.archive-date { font-size: 14px; color: var(--text-muted); min-width: 40px; }
.archive-title { color: var(--text-primary); font-size: 15px; }
.archive-title:hover { color: var(--accent-color); }
.loading { text-align: center; padding: 80px; color: var(--text-muted); }
</style>