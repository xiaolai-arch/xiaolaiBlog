<template>
  <div class="categories-page">
    <div class="container">
      <h1 class="page-title">类别</h1>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else class="category-list">
        <router-link
          v-for="cat in categories"
          :key="cat.id"
          :to="`/categories/${cat.slug}`"
          class="category-card"
        >
          <h2>{{ cat.name }}</h2>
          <span class="count">{{ cat.articleCount || 0 }} articles</span>
          <p v-if="cat.description" class="desc">{{ cat.description }}</p>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { categoryApi } from '@/api'

const categories = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await categoryApi.getList()
    if (res.data.code === 200) categories.value = res.data.data
  } catch (_) {}
  loading.value = false
})
</script>

<style scoped>
.page-title {
  font-size: 28px;
  padding: 40px 0;
}

.category-list {
  display: grid;
  gap: 16px;
}

.category-card {
  display: block;
  padding: 24px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  transition: transform 0.2s;
  color: var(--text-primary);
}

.category-card:hover {
  transform: translateY(-2px);
  border-color: var(--accent-color);
}

.category-card h2 {
  font-size: 20px;
  margin-bottom: 8px;
}

.count {
  font-size: 14px;
  color: var(--text-muted);
}

.desc {
  margin-top: 8px;
  font-size: 14px;
  color: var(--text-secondary);
}

.loading {
  text-align: center;
  padding: 80px;
  color: var(--text-muted);
}
</style>