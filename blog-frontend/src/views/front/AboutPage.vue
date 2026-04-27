<template>
  <div class="about-page">
    <div class="container">
      <h1 class="page-title">我也不知道这个页面该写什么.....</h1>
      <div v-if="loading" class="loading">Loading...</div>
      <div v-else class="about-content markdown-body" v-html="content"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { siteApi } from '@/api'

const content = ref('')
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await siteApi.getAbout()
    if (res.data.code === 200) {
      content.value = res.data.data.content || '<p>打不死我的，一直在打我....</p>'

    }
  } catch (_) { content.value = '<p>Failed to load.</p>' }
  loading.value = false
})
</script>

<style scoped>
.page-title { font-size: 28px; padding: 40px 0; }
.about-content { padding-bottom: 40px; }
.loading { text-align: center; padding: 80px; color: var(--text-muted); }
</style>