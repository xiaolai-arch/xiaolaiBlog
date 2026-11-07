<template>
  <div class="dashboard">
    <header class="dashboard-heading"><div><p class="dashboard-date">{{ today }}</p><h1>让好内容，持续发生。</h1><p class="page-description">欢迎回来，{{ name }}。从这里继续你的创作。</p></div>
      <router-link to="/admin/articles/create" class="admin-primary-link rounded-full font-medium transition-all duration-300"><el-icon><Plus /></el-icon>写一篇文章</router-link>
    </header>
    <div class="dashboard-section-heading"><h2>工作概览</h2><button class="text-action rounded-full font-medium transition-all duration-300" :disabled="loading" @click="load">{{ loading ? '更新中…' : '刷新数据' }}<el-icon><RefreshRight /></el-icon></button></div>
    <div class="stats-grid" aria-live="polite">
      <router-link v-for="item in metrics" :key="item.key" :to="item.to" class="stat-card group bg-white rounded-2xl shadow-sm transition-all duration-300" :aria-label="`${item.label}：${stats[item.key] ?? '暂不可用'}，前往管理`">
        <div class="stat-top"><span>{{ item.label }}</span><el-icon class="stat-icon group-hover:scale-105"><component :is="item.icon" /></el-icon></div>
        <strong class="stat-value">{{ loading && stats[item.key] == null ? '—' : stats[item.key] ?? '—' }}</strong><div class="stat-bottom"><span>{{ item.description }}</span><el-icon><ArrowRight /></el-icon></div>
      </router-link>
    </div>
    <p v-if="error" class="dashboard-error" role="alert">{{ error }} <button class="text-action rounded-full font-medium transition-all duration-300" @click="load">重试</button></p>
    <section class="recent-section bg-white rounded-2xl shadow-sm" aria-labelledby="recent-title">
      <header class="section-toolbar"><div><h2 id="recent-title">最近的创作</h2><p>每一个想法，都值得好好记录。</p></div><router-link to="/admin/articles" class="section-link">全部文章 <el-icon><ArrowRight /></el-icon></router-link></header>
      <div v-if="loading" class="dashboard-empty" role="status">正在整理你的内容…</div>
      <div v-else-if="articlesFailed" class="dashboard-empty">文章加载失败，请点击上方刷新数据重试。</div>
      <div v-else-if="!articles.length" class="dashboard-empty"><el-icon><Document /></el-icon><h3>从第一个想法开始</h3><p>创建一篇文章，记录你的学习与发现。</p><router-link to="/admin/articles/create" class="section-link">开始写作 →</router-link></div>
      <ul v-else class="recent-articles"><li v-for="a in articles" :key="a.id"><router-link :to="`/admin/articles/${a.id}/edit`" class="recent-article transition-all duration-300"><span class="article-document"><el-icon><Document /></el-icon></span><span class="recent-article-copy"><strong>{{ a.title }}</strong><span>{{ formatDate(a.updateTime || a.createTime) }}<span v-if="a.categoryName"> · {{ a.categoryName }}</span></span></span><span class="article-status" :class="{ published: a.status === 1 }">{{ articleStatus(a) }}</span><el-icon class="article-chevron"><ArrowRight /></el-icon></router-link></li></ul>
    </section>
    <section class="dashboard-quick-links" aria-label="快捷入口"><router-link to="/admin/media" class="quick-link group"><el-icon class="group-hover:scale-105"><Picture /></el-icon><span><strong>整理媒体库</strong><span>为下一篇内容准备素材</span></span><el-icon><ArrowRight /></el-icon></router-link><router-link to="/admin/settings" class="quick-link group"><el-icon class="group-hover:scale-105"><Setting /></el-icon><span><strong>打理你的站点</strong><span>更新博客信息与个人偏好</span></span><el-icon><ArrowRight /></el-icon></router-link></section>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, RefreshRight } from '@element-plus/icons-vue'
import { articleApi, categoryApi, tagApi, commentApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import dayjs from 'dayjs'
const auth = useAuthStore(), name = computed(() => auth.user?.nickname || '创作者')
const today = dayjs().format('YYYY 年 M 月 D 日')
type MetricKey = 'articles' | 'categories' | 'tags' | 'comments'
const stats = ref<Record<MetricKey, number | null>>({ articles: null, categories: null, tags: null, comments: null })
const metrics: { key: MetricKey; label: string; icon: string; to: string; description: string }[] = [
  { key: 'articles', label: '文章', icon: 'Document', to: '/admin/articles', description: '记录与表达' },
  { key: 'categories', label: '分类', icon: 'Folder', to: '/admin/categories', description: '让内容井然有序' },
  { key: 'tags', label: '标签', icon: 'PriceTag', to: '/admin/tags', description: '串联每一个主题' },
  { key: 'comments', label: '待审核评论', icon: 'ChatDotSquare', to: '/admin/comments', description: '与读者保持交流' },
]
const loading = ref(false), error = ref(''), articles = ref<any[]>([]), articlesFailed = ref(false)
const formatDate = (date: string) => date ? dayjs(date).format('M 月 D 日 · HH:mm') : '尚未记录时间'
const articleStatus = (a: any) => a.status !== 1 ? '草稿' : a.publishAt && dayjs(a.publishAt).isAfter(dayjs()) ? '定时发布' : '已发布'
async function load() {
  if (loading.value) return
  loading.value = true; error.value = ''
  const results = await Promise.allSettled([articleApi.getAdminList({ page: 1, size: 5 }), categoryApi.getAdminList(), tagApi.getAdminList(), commentApi.getAdminList({ page: 1, size: 1, status: 0 })])
  results.forEach((result, i) => { const key = metrics[i]!.key; if (result.status === 'fulfilled') { const data = result.value.data.data; stats.value[key] = i === 0 || i === 3 ? data?.total ?? 0 : data?.length ?? 0 } else { stats.value[key] = null; error.value = '部分数据暂时无法加载，其他入口仍可正常使用。' } })
  const first = results[0]!
  articlesFailed.value = first.status === 'rejected'
  articles.value = first.status === 'fulfilled' ? first.value.data.data?.list || [] : []
  loading.value = false
}
onMounted(load)
</script>
