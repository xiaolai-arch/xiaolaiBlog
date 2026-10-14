<template>
  <div class="home-page">
    <div class="container">
      <section class="hero">
        <div class="hero-deco hero-deco-1"></div>
        <div class="hero-deco hero-deco-2"></div>
        <div class="hero-text">
          <p class="hero-eyebrow">👋 欢迎来到</p>
          <h1 class="hero-title">{{ siteTitle }}</h1>
          <p class="hero-subtitle">{{ siteSubtitle || siteDescription || '记录学习、项目和生活里值得留下来的东西' }}</p>
          <div class="hero-actions">
            <button class="hero-cta" @click="scrollToList">开始阅读 →</button>
            <span class="hero-stats">
              {{ total }} 篇文章 · {{ categories.length }} 个分类 · {{ tags.length }} 个标签
            </span>
          </div>
        </div>
      </section>

      <section class="tools-strip">
        <router-link to="/tools" class="tools-entry">
          <span class="tools-icon">🧰</span>
          <div class="tools-info">
            <h3>工具箱</h3>
            <p>Markdown 笔记本：Typora 式即时渲染，登录后云端同步</p>
          </div>
          <span class="tools-arrow">→</span>
        </router-link>
      </section>

      <section id="articles" class="article-list">
        <div v-if="loading" class="loading">加载中...</div>
        <template v-else>
          <article v-for="article in articles" :key="article.id" class="article-card">
            <div class="card-content">
              <div class="card-meta">
                <span v-if="article.isTop" class="top-badge">置顶</span>
                <router-link
                  v-if="article.categoryName"
                  class="category-link"
                  :to="`/categories/${article.categorySlug}`"
                >
                  {{ article.categoryName }}
                </router-link>
                <span class="date">{{ formatDate(article.createTime) }}</span>
                <span>{{ readingTime(article) }} 分钟</span>
                <span>{{ article.views || 0 }} 阅读</span>
              </div>
              <h2 class="card-title">
                <router-link :to="`/articles/${article.slug}`">{{ article.title }}</router-link>
              </h2>
              <p class="card-summary">{{ excerpt(article) }}</p>
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
            <router-link
              v-if="article.coverUrl && !hiddenCovers.has(article.id)"
              :to="`/articles/${article.slug}`"
              class="card-cover"
            >
              <img :src="article.coverUrl" :alt="article.title" loading="lazy" @error="hideCover(article.id)" />
            </router-link>
          </article>

          <div v-if="!articles.length" class="loading">还没有文章</div>

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
import { useRoute } from 'vue-router'
import { articleApi, categoryApi, siteApi, tagApi } from '@/api'
import dayjs from 'dayjs'

const route = useRoute()

const articles = ref<any[]>([])
const categories = ref<any[]>([])
const tags = ref<any[]>([])
const hiddenCovers = ref(new Set<number>())
const loading = ref(true)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const siteTitle = ref('咲来的博客')
const siteSubtitle = ref('')
const siteDescription = ref('')

onMounted(async () => {
  await Promise.all([loadConfig(), loadArticles(), loadCounts()])
})

watch(() => route.query, () => { page.value = 1; loadArticles() })

async function loadConfig() {
  try {
    const res = await siteApi.getConfig()
    if (res.data.code === 200) {
      const config = res.data.data
      if (config.site_title) siteTitle.value = config.site_title
      if (config.site_subtitle) siteSubtitle.value = config.site_subtitle
      siteDescription.value = config.site_description || ''
    }
  } catch (_) {}
}

async function loadCounts() {
  try {
    const [categoryRes, tagRes] = await Promise.all([categoryApi.getList(), tagApi.getList()])
    if (categoryRes.data.code === 200) categories.value = categoryRes.data.data || []
    if (tagRes.data.code === 200) tags.value = tagRes.data.data || []
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

function scrollToList() {
  document.getElementById('articles')?.scrollIntoView({ behavior: 'smooth' })
}

function onPageChange(p: number) {
  page.value = p
  loadArticles()
  window.scrollTo(0, 0)
}

function formatDate(d: string) {
  return dayjs(d).format('YYYY-MM-DD')
}

function plainText(html: string) {
  if (!html) return ''
  const div = document.createElement('div')
  div.innerHTML = html
  return div.textContent || ''
}

function excerpt(article: any) {
  const text = article.summary || plainText(article.contentHtml)
  return text.length > 160 ? text.slice(0, 160) + '…' : text
}

function readingTime(article: any) {
  const text = plainText(article.contentHtml) || article.summary || ''
  return Math.max(1, Math.round(text.length / 400))
}

function hideCover(id: number) {
  hiddenCovers.value.add(id)
}
</script>

<style scoped>
/* ---------- Hero 横幅 ---------- */
.hero {
  position: relative;
  overflow: hidden;
  margin: 28px 0 28px;
  padding: 48px 44px;
  border-radius: 20px;
  background: linear-gradient(130deg, #1d4ed8 0%, #2563eb 50%, #38bdf8 100%);
  color: #fff;
  box-shadow: var(--shadow-hover);
}

.hero-deco {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
  pointer-events: none;
}

.hero-deco-1 {
  width: 300px;
  height: 300px;
  right: -70px;
  top: -110px;
}

.hero-deco-2 {
  width: 170px;
  height: 170px;
  right: 190px;
  bottom: -80px;
  background: rgba(255, 255, 255, 0.08);
}

.hero-eyebrow {
  font-size: 14px;
  opacity: 0.85;
  margin-bottom: 10px;
}

.hero-title {
  font-size: 38px;
  font-weight: 800;
  letter-spacing: -0.5px;
  margin-bottom: 12px;
  line-height: 1.25;
}

.hero-subtitle {
  font-size: 15px;
  opacity: 0.88;
  max-width: 560px;
  margin-bottom: 24px;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.hero-cta {
  border: 0;
  cursor: pointer;
  background: #fff;
  color: #1d4ed8;
  font-size: 14px;
  font-weight: 700;
  padding: 11px 24px;
  border-radius: 999px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.hero-cta:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.22);
}

.hero-stats {
  font-size: 13px;
  opacity: 0.85;
}

/* ---------- 工具箱入口 ---------- */
.tools-strip {
  margin-bottom: 24px;
}

.tools-entry {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--card-bg);
  border: 1px dashed var(--border-color);
  border-radius: var(--radius-lg);
  padding: 14px 20px;
  box-shadow: var(--shadow-soft);
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}

.tools-entry:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-hover);
  border-color: var(--accent-color);
}

.tools-icon {
  font-size: 24px;
  flex: none;
}

.tools-info {
  flex: 1;
  min-width: 0;
}

.tools-info h3 {
  font-size: 15px;
  color: var(--text-primary);
  margin-bottom: 2px;
}

.tools-info p {
  font-size: 13px;
  color: var(--text-muted);
}

.tools-arrow {
  color: var(--text-muted);
  transition: transform 0.2s, color 0.2s;
}

.tools-entry:hover .tools-arrow {
  transform: translateX(3px);
  color: var(--accent-color);
}

/* ---------- 文章流 ---------- */
.article-list {
  min-height: 300px;
}

.article-card {
  display: flex;
  gap: 18px;
  align-items: stretch;
}

.card-content {
  flex: 1;
  min-width: 0;
}

.card-cover {
  flex: 0 0 168px;
  border-radius: var(--radius-md);
  overflow: hidden;
  align-self: center;
}

.card-cover img {
  width: 168px;
  height: 112px;
  object-fit: cover;
  display: block;
}

.top-badge {
  background: #e74c3c;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.card-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
  font-size: 13px;
  color: var(--text-muted);
}

.category-link {
  color: var(--accent-color);
}

.card-title {
  font-size: 20px;
  margin-bottom: 8px;
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
  margin-bottom: 12px;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-chip {
  color: var(--accent-color);
  font-size: 13px;
}

.pagination {
  display: flex;
  justify-content: center;
  padding: 28px 0 8px;
}

.loading {
  text-align: center;
  padding: 80px;
  color: var(--text-muted);
}

@media (max-width: 640px) {
  .hero {
    padding: 34px 24px;
  }

  .hero-title {
    font-size: 28px;
  }

  .card-cover {
    display: none;
  }
}
</style>
