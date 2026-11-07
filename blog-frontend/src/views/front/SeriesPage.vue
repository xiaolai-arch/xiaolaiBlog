<template>
  <main class="series-page"><h1>文章系列</h1><p>按顺序阅读同一主题的文章。阅读记录保存在当前浏览器。</p>
    <p v-if="error" role="alert">{{ error }} <button @click="load">重试</button></p>
    <el-select v-model="selected" placeholder="选择系列" @change="selectSeries"><el-option v-for="name in names" :key="name" :value="name" :label="name" /></el-select>
    <p v-if="loading">加载中…</p><p v-else-if="!names.length">还没有公开的文章系列，可以先去 <router-link to="/archives">文章归档</router-link> 看看。</p>
    <p v-if="articles.length">已读 {{ articles.filter(a => read[a.slug]).length }} / {{ articles.length }}</p>
    <ol><li v-for="a in articles" :key="a.id"><router-link :to="`/articles/${a.slug}`">{{ a.title }}</router-link> <span v-if="read[a.slug]">· 已读</span></li></ol>
  </main>
</template>
<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articleApi } from '@/api'
import { accountKey } from '@/utils/accountStorage'
const route = useRoute(), router = useRouter(), names = ref<string[]>([]), articles = ref<any[]>([]), selected = ref(''), error = ref(''), loading = ref(false)
const read = ref<Record<string, boolean>>({})
try { read.value = JSON.parse(localStorage.getItem(accountKey('articles:read')) || '{}') } catch {}
let generation = 0
async function loadArticles() { const current = ++generation; articles.value = []; if (!selected.value) return; loading.value = true; error.value = ''; try { const res = await articleApi.seriesArticles(selected.value); if (current === generation) articles.value = res.data.data || [] } catch { if (current === generation) error.value = '系列加载失败' } finally { if (current === generation) loading.value = false } }
function selectSeries() { router.replace({ query: { name: selected.value } }) }
async function load() { try { names.value = (await articleApi.series()).data.data || []; selected.value = typeof route.query.name === 'string' ? route.query.name : names.value[0] || ''; await loadArticles() } catch { error.value = '系列加载失败' } }
watch(() => route.query.name, () => { selected.value = String(route.query.name || ''); void loadArticles() })
onMounted(load)
</script>
<style scoped>.series-page { max-width: 800px; padding: 40px 16px; margin: auto; } p { margin: 20px 0; } li { padding: 14px; border-bottom: 1px solid var(--border-color); }</style>
