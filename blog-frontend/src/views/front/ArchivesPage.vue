<template>
  <div class="archives-page">
    <div class="container">
      <h1 class="page-title">归档 <span>{{ count }} 篇</span></h1>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else class="archive-list">
        <div v-for="(articles, ym) in archives" :key="ym" class="archive-month">
          <h2 class="month-title">{{ ym }} <span>{{ articles.length }}</span></h2>
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
import { computed, ref, onMounted } from 'vue'
import { articleApi } from '@/api'
import dayjs from 'dayjs'

const archives = ref<Record<string, any[]>>({})
const loading = ref(true)
const count = computed(() => Object.values(archives.value).reduce((sum, list) => sum + list.length, 0))

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
.page-title { font-size: 28px; padding: 40px 0 8px; }
.page-title span, .month-title span { font-size: 14px; color: var(--text-muted); font-weight: 500; margin-left: 8px; }
.archive-list { border-left: 2px solid var(--border-color); margin: 24px 0 40px 8px; padding-left: 20px; }
.month-title { font-size: 18px; margin: 28px 0 8px; color: var(--accent-color); }
.archive-item { display: flex; align-items: center; gap: 16px; padding: 10px 0; }
.archive-date { font-size: 14px; color: var(--text-muted); min-width: 40px; }
.archive-title { color: var(--text-primary); font-size: 15px; }
.archive-title:hover { color: var(--accent-color); }
.loading { text-align: center; padding: 80px; color: var(--text-muted); }
</style>