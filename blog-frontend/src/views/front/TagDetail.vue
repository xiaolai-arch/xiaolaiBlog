<template>
  <div class="tag-detail">
    <div class="container">
      <h1 class="page-title">Tag: #{{ tag?.name || route.params.slug }}</h1>

      <div v-if="loading" class="loading">Loading...</div>
      <template v-else>
        <article v-for="article in articles" :key="article.id" class="article-card">
          <div class="card-meta">
            <span class="date">{{ formatDate(article.createTime) }}</span>
            <span v-if="article.categoryName">{{ article.categoryName }}</span>
          </div>
          <h2 class="card-title">
            <router-link :to="`/articles/${article.slug}`">{{ article.title }}</router-link>
          </h2>
          <p class="card-summary">{{ article.summary || stripHtml(article.contentHtml).substring(0, 200) }}</p>
        </article>

        <div class="pagination" v-if="total > size">
          <el-pagination
            v-model:current-page="page"
            :page-size="size"
            :total="total"
            layout="prev, pager, next"
            @current-change="onPageChange"
            background small
          />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { articleApi, tagApi } from '@/api'
import dayjs from 'dayjs'

const route = useRoute()
const articles = ref<any[]>([])
const tag = ref<any>(null)
const loading = ref(true)
const page = ref(1)
const size = ref(10)
const total = ref(0)

onMounted(async () => { await loadTag(); await loadArticles() })
watch(() => route.params.slug, () => { page.value = 1; loadTag(); loadArticles() })

async function loadTag() {
  try { const res = await tagApi.getList()
    if (res.data.code === 200) tag.value = res.data.data.find((t: any) => t.slug === route.params.slug)
  } catch (_) {}
}

async function loadArticles() {
  loading.value = true
  try {
    const res = await articleApi.getList({ page: page.value, size: size.value, tag: route.params.slug })
    if (res.data.code === 200) { articles.value = res.data.data.list; total.value = res.data.data.total }
  } catch (_) {}
  loading.value = false
}

function onPageChange(p: number) { page.value = p; loadArticles(); window.scrollTo(0, 0) }
function formatDate(d: string) { return dayjs(d).format('YYYY-MM-DD') }
function stripHtml(html: string) {
  if (!html) return ''
  const div = document.createElement('div'); div.innerHTML = html
  return div.textContent || div.innerText || ''
}
</script>

<style scoped>
.page-title { font-size: 28px; padding: 40px 0; }
.card-meta { font-size: 13px; color: var(--text-muted); margin-bottom: 8px; }
.card-title { font-size: 20px; margin-bottom: 8px; }
.card-title a { color: var(--text-primary); }
.card-title a:hover { color: var(--accent-color); }
.card-summary { color: var(--text-secondary); font-size: 14px; }
.pagination { display: flex; justify-content: center; padding: 40px 0; }
.loading { text-align: center; padding: 80px; color: var(--text-muted); }
.article-card { padding: 24px; border: 1px solid var(--border-color); border-radius: 12px; margin-bottom: 16px; }
</style>