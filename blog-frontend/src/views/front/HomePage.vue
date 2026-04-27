<template>
  <div class="home-page">
    <div class="container">
      <!-- Hero section -->
      <section class="hero">
        <h1 class="hero-title">{{ siteTitle }}</h1>
        <p class="hero-subtitle">{{ siteSubtitle }}</p>
      </section>

      <!-- Article list -->
      <section class="article-list">
        <div v-if="loading" class="loading">加载中...</div>
        <template v-else>
          <article v-for="article in articles" :key="article.id" class="article-card">
            <div class="card-content">
              <div class="card-meta">
                <span v-if="article.isTop" class="top-badge">置顶</span>
                <span v-if="article.categoryName" class="category-link">
                  {{ article.categoryName }}
                </span>
                <span class="date">{{ formatDate(article.createTime) }}</span>
              </div>
              <h2 class="card-title">
                <router-link :to="`/articles/${article.slug}`">{{ article.title }}</router-link>
              </h2>
              <p class="card-summary">{{ article.summary || stripHtml(article.contentHtml).substring(0, 200) }}</p>
              <div class="card-tags" v-if="article.tags?.length">
                <router-link
                  v-for="tag in article.tags"
                  :key="tag.id"
                  :to="`/tags/${tag.slug}`"
                  class="tag-chip"
                >
                  #{{ tag.name }}
                </router-link>
              </div>
            </div>
          </article>

          <!-- Pagination -->
          <div v-if="total > size" class="pagination">
            <el-pagination
              :current-page="page"
              :page-size="size"
              :total="total"
              layout="prev, pager, next"
              @current-change="onPageChange"
              background
              small
            />
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articleApi, siteApi } from '@/api'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

const articles = ref<any[]>([])
const loading = ref(true)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const siteTitle = ref('咲来的博客')
const siteSubtitle = ref('')

onMounted(async () => {
  await loadConfig()
  await loadArticles()
})

watch(() => route.query, () => { page.value = 1; loadArticles() })

async function loadConfig() {
  try {
    const res = await siteApi.getConfig()
    if (res.data.code === 200) {
      const config = res.data.data
      if (config.site_title) siteTitle.value = config.site_title
      if (config.site_subtitle) siteSubtitle.value = config.site_subtitle
    }
  } catch (_) {}
}

async function loadArticles() {
  loading.value = true
  try {
    const res = await articleApi.getList({ page: page.value, size: size.value })
    if (res.data.code === 200) {
      articles.value = res.data.data.list
      total.value = res.data.data.total
    }
  } catch (_) { articles.value = [] }
  loading.value = false
}

function onPageChange(p: number) {
  page.value = p
  loadArticles()
  window.scrollTo(0, 0)
}

function formatDate(d: string) {
  return dayjs(d).format('YYYY-MM-DD')
}

function stripHtml(html: string) {
  if (!html) return ''
  const div = document.createElement('div')
  div.innerHTML = html
  return div.textContent || div.innerText || ''
}
</script>

<style scoped>
.hero {
  text-align: center;
  padding: 60px 0 40px;
}

.hero-title {
  font-size: 32px;
  font-weight: 800;
  margin-bottom: 12px;
  letter-spacing: -1px;
}

.hero-subtitle {
  color: var(--text-secondary);
  font-size: 16px;
}

.top-badge {
  background: #e74c3c;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  margin-right: 8px;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--text-muted);
}

.category-link {
  color: var(--accent-color);
}

.card-title {
  font-size: 22px;
  margin-bottom: 12px;
  line-height: 1.4;
}

.card-title a {
  color: var(--text-primary);
}

.card-title a:hover {
  color: var(--accent-color);
}

.card-summary {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
  margin-bottom: 16px;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-chip {
  color: var(--accent-color) !important;
  font-size: 13px;
}

.pagination {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

.loading {
  text-align: center;
  padding: 80px;
  color: var(--text-muted);
}
</style>