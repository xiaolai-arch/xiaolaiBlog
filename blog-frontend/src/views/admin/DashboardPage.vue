<template>
  <div class="dashboard">
    <h2 class="page-title">Dashboard</h2>
    <div class="stats-grid">
      <el-card class="stat-card">
        <div class="stat-value">{{ stats.articles }}</div>
        <div class="stat-label">Articles</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value">{{ stats.categories }}</div>
        <div class="stat-label">Categories</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value">{{ stats.tags }}</div>
        <div class="stat-label">Tags</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value">{{ stats.comments }}</div>
        <div class="stat-label">Comments</div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { articleApi, categoryApi, tagApi, commentApi } from '@/api'

const stats = ref({ articles: 0, categories: 0, tags: 0, comments: 0 })

onMounted(async () => {
  try {
    const [aRes, cRes, tRes, comRes] = await Promise.all([
      articleApi.getAdminList({ page: 1, size: 1 }),
      categoryApi.getAdminList(),
      tagApi.getAdminList(),
      commentApi.getAdminList({ page: 1, size: 1 }),
    ])
    stats.value.articles = aRes.data.data?.total || 0
    stats.value.categories = cRes.data.data?.length || 0
    stats.value.tags = tRes.data.data?.length || 0
    stats.value.comments = comRes.data.data?.total || 0
  } catch (_) {}
})
</script>

<style scoped>
.page-title { font-size: 24px; margin-bottom: 24px; }
.stats-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 20px; }
.stat-card { text-align: center; }
.stat-value { font-size: 36px; font-weight: 800; color: var(--accent-color); }
.stat-label { font-size: 14px; color: var(--text-secondary); margin-top: 8px; }
</style>