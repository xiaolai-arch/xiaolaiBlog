<template>
  <main class="search-page container"><h1>搜索文章</h1>
    <form class="search-controls" @submit.prevent="apply"><el-input v-model="input" maxlength="256" placeholder="搜索标题和正文" clearable aria-label="搜索词" />
      <el-select v-model="category" clearable placeholder="全部分类" aria-label="分类"><el-option v-for="c in categories" :key="c.id" :value="c.slug" :label="c.name" /></el-select>
      <el-select v-model="tag" clearable placeholder="全部标签" aria-label="标签"><el-option v-for="t in tags" :key="t.id" :value="t.slug" :label="t.name" /></el-select>
      <el-button native-type="submit" type="primary">搜索</el-button>
    </form>
    <p v-if="loading" role="status">搜索中…</p><p v-else-if="error" role="alert">{{ error }} <button @click="search">重试</button></p>
    <p v-else-if="!articles.length">没有找到匹配文章。试试更短的关键词，或清除分类和标签筛选。<router-link to="/archives">浏览文章归档</router-link></p>
    <template v-else><p>共 {{ total }} 篇文章 · 正文也参与搜索</p>
      <article v-for="a in articles" :key="a.id" class="article-card"><p>{{ dayjs(a.createTime).format('YYYY-MM-DD') }} · {{ a.categoryName || '未分类' }}</p>
        <h2><router-link :to="`/articles/${a.slug}`"><template v-for="(part,i) in highlight(a.title, keyword)" :key="i"><mark v-if="part.match">{{ part.text }}</mark><template v-else>{{ part.text }}</template></template></router-link></h2>
        <p><template v-for="(part,i) in highlight(a.summary || '打开文章阅读全文', keyword)" :key="i"><mark v-if="part.match">{{ part.text }}</mark><template v-else>{{ part.text }}</template></template></p>
      </article>
      <el-pagination :current-page="page" :page-size="10" :total="total" layout="prev, pager, next" @current-change="changePage" />
    </template>
  </main>
</template>
<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articleApi, categoryApi, tagApi } from '@/api'
import { highlight } from '@/utils/searchHighlight'
import dayjs from 'dayjs'
const route = useRoute(), router = useRouter(), input = ref(''), keyword = ref(''), category = ref(''), tag = ref(''), page = ref(1)
const articles = ref<any[]>([]), categories = ref<any[]>([]), tags = ref<any[]>([]), loading = ref(false), error = ref(''), total = ref(0)
let generation = 0
function apply() { router.push({ query: { q: input.value.trim(), category: category.value || undefined, tag: tag.value || undefined } }) }
function changePage(p: number) { router.push({ query: { ...route.query, page: p } }) }
async function search() {
  const current = ++generation
  input.value = keyword.value = String(route.query.q || '').trim(); category.value = String(route.query.category || ''); tag.value = String(route.query.tag || '')
  page.value = Math.max(1, Number(route.query.page) || 1); loading.value = true; error.value = ''; articles.value = []
  try { const res = await articleApi.getList({ page: page.value, size: 10, keyword: keyword.value, category: category.value, tag: tag.value }); if (current === generation) { articles.value = res.data.data.list; total.value = res.data.data.total } }
  catch { if (current === generation) error.value = '搜索失败，请稍后重试' }
  finally { if (current === generation) loading.value = false }
}
watch(() => route.fullPath, search, { immediate: true })
onMounted(async () => { const res = await Promise.allSettled([categoryApi.getList(), tagApi.getList()]); if (res[0].status === 'fulfilled') categories.value = res[0].value.data.data; if (res[1].status === 'fulfilled') tags.value = res[1].value.data.data })
</script>
<style scoped>.search-page { max-width: 900px; padding: 40px 16px; } .search-controls { display: flex; gap: 12px; margin: 24px 0; flex-wrap: wrap; } .search-controls .el-input { flex: 1 1 240px; } .search-controls .el-select { width: 160px; } .article-card { padding: 24px; border: 1px solid var(--border-color); border-radius: 12px; margin: 16px 0; } p { margin: 12px 0; } h2 { font-size: 22px; } mark { background: #fef08a; color: #18181b; }</style>
