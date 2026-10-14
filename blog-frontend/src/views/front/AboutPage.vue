<template>
  <div class="about-page">
    <div class="container">
      <section class="profile">
        <div class="avatar"><img :src="avatarUrl" alt="头像" /></div>
        <div>
          <h1>{{ title }}</h1>
          <p>{{ description || '记录学习笔记、项目实践和一些还没整理完的想法。' }}</p>
          <div class="links">
            <a v-if="github" :href="github" target="_blank" rel="noopener">GitHub</a>
            <a v-if="email" :href="'mailto:' + email">邮箱</a>
            <router-link to="/archives">归档</router-link>
            <router-link to="/gallery">图库</router-link>
          </div>
        </div>
      </section>
      <div v-if="categories.length" class="topics">
        <h2>最近在写</h2>
        <router-link v-for="category in categories" :key="category.id" :to="`/categories/${category.slug}`" class="topic">
          <span>{{ category.name }}</span>
          <span>{{ category.articleCount || 0 }} 篇</span>
        </router-link>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else class="about-content markdown-body" v-html="content"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { siteApi, categoryApi } from '@/api'
import avatarUrl from '@/assets/avatar.png'

const content = ref('')
const loading = ref(true)
const title = ref('关于我')
const description = ref('')
const github = ref('')
const email = ref('')
const categories = ref<any[]>([])

onMounted(async () => {
  try {
    const [aboutRes, configRes, categoryRes] = await Promise.all([
      siteApi.getAbout(),
      siteApi.getConfig(),
      categoryApi.getList(),
    ])
    if (aboutRes.data.code === 200) {
      content.value = aboutRes.data.data.content || '<p>这里还没有介绍，可以在后台的站点设置里补充。</p>'
    }
    if (configRes.data.code === 200) {
      const config = configRes.data.data
      if (config.site_title) title.value = config.site_title
      description.value = config.site_description || config.site_subtitle || ''
      github.value = config.social_github || ''
      email.value = config.social_email || ''
    }
    if (categoryRes.data.code === 200) categories.value = categoryRes.data.data || []
  } catch (_) {
    content.value = '<p>介绍加载失败。</p>'
  }
  loading.value = false
})
</script>

<style scoped>
.profile {
  display: flex;
  gap: 20px;
  align-items: center;
  margin: 36px 0 28px;
  padding: 24px;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  background: var(--card-bg);
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  overflow: hidden;
  flex: none;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.profile h1 {
  font-size: 26px;
  margin-bottom: 6px;
}

.profile p {
  color: var(--text-secondary);
  font-size: 14px;
}

.links {
  display: flex;
  gap: 14px;
  margin-top: 10px;
  font-size: 14px;
}

.topics {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 10px;
  margin-bottom: 28px;
}

.topics h2 {
  grid-column: 1 / -1;
  font-size: 16px;
}

.topic {
  display: flex;
  justify-content: space-between;
  padding: 12px 14px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-primary);
  background: var(--card-bg);
}

.topic span:last-child {
  color: var(--text-muted);
  font-size: 12px;
}

.about-content {
  padding-bottom: 40px;
}

.loading {
  text-align: center;
  padding: 80px;
  color: var(--text-muted);
}
</style>
