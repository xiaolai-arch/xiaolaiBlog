<template>
  <div class="front-layout" :data-theme="themeStore.isDark ? 'dark' : 'light'">
    <!-- Header / Navigation -->
    <header class="site-header">
      <div class="header-inner">
        <router-link to="/" class="site-logo">{{ siteTitle }}</router-link>
        <nav class="main-nav" :class="{ 'nav-open': menuOpen }">
          <router-link to="/" @click="menuOpen = false">首页</router-link>
          <router-link to="/study" @click="menuOpen = false">学习</router-link>
          <router-link to="/archives" @click="menuOpen = false">归档</router-link>
          <router-link to="/categories" @click="menuOpen = false">分类</router-link>
          <router-link to="/tags" @click="menuOpen = false">标签</router-link>
          <router-link to="/gallery" @click="menuOpen = false">图库</router-link>
          <router-link to="/tools" @click="menuOpen = false">工具</router-link>
          <router-link to="/leetcode" @click="menuOpen = false">刷题</router-link>
          <router-link to="/cet4" @click="menuOpen = false">四级</router-link>
          <router-link to="/agent" @click="menuOpen = false">秋招</router-link>
          <router-link to="/about" @click="menuOpen = false">关于</router-link>
        </nav>
        <div class="header-actions">
          <button class="search-btn" @click="showSearch = true">
            <el-icon><Search /></el-icon>
          </button>
          <button class="theme-btn" @click="themeStore.toggle()">
            <el-icon><component :is="themeStore.isDark ? 'Sunny' : 'Moon'" /></el-icon>
          </button>
          <button class="menu-btn" @click="menuOpen = !menuOpen">
            <el-icon><Menu /></el-icon>
          </button>
        </div>
      </div>
    </header>

    <!-- Search overlay -->
    <div v-if="showSearch" class="search-overlay" @click.self="showSearch = false">
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索文章..."
          size="large"
          clearable
          @keyup.enter="doSearch"
          autofocus
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
    </div>

    <!-- Sidebar + content -->
    <div class="layout-body">
      <aside class="site-sidebar">
        <div class="side-card profile-card">
          <router-link to="/about" class="avatar" aria-label="关于我">
            <img :src="avatarUrl" alt="头像" />
          </router-link>
          <h3 class="profile-name">{{ siteTitle }}</h3>
          <p class="profile-desc">{{ description || '记录学习、项目和生活里值得留下来的东西。' }}</p>
          <div class="social-row">
            <a v-if="social.github" :href="social.github" target="_blank" rel="noopener" title="GitHub">
              <svg viewBox="0 0 16 16" width="18" height="18" fill="currentColor" aria-hidden="true">
                <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8z"/>
              </svg>
            </a>
            <a v-if="social.bilibili" :href="social.bilibili" target="_blank" rel="noopener" title="哔哩哔哩">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <rect x="3" y="8" width="18" height="12" rx="3"/>
                <path d="M8 4l3 4M16 4l-3 4M9.5 13v2M14.5 13v2"/>
              </svg>
            </a>
            <a v-if="social.email" :href="'mailto:' + social.email" title="邮箱">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <rect x="2" y="5" width="20" height="14" rx="2"/>
                <path d="M2 7l10 6L22 7"/>
              </svg>
            </a>
          </div>
        </div>

        <nav class="side-card nav-card">
          <router-link to="/" exact-active-class="active">首页</router-link>
          <router-link to="/study" active-class="active">学习</router-link>
          <router-link to="/archives" active-class="active">归档</router-link>
          <router-link to="/categories" active-class="active">分类</router-link>
          <router-link to="/tags" active-class="active">标签</router-link>
          <router-link to="/gallery" active-class="active">图库</router-link>
          <router-link to="/tools" active-class="active">工具</router-link>
          <router-link to="/leetcode" active-class="active">刷题</router-link>
          <router-link to="/cet4" active-class="active">四级</router-link>
          <router-link to="/agent" active-class="active">秋招</router-link>
          <router-link to="/about" active-class="active">关于</router-link>
        </nav>

        <div class="side-card announce-card" v-if="announcement">
          <h3>🔔 公告</h3>
          <p>{{ announcement }}</p>
        </div>

        <div class="side-card" v-if="categories.length">
          <h3>分类</h3>
          <router-link
            v-for="category in categories"
            :key="category.id"
            :to="`/categories/${category.slug}`"
            class="side-row"
          >
            <span>{{ category.name }}</span>
            <span class="count">{{ category.articleCount || 0 }}</span>
          </router-link>
        </div>

        <div class="side-card" v-if="tags.length">
          <h3>标签</h3>
          <div class="tag-cloud">
            <router-link
              v-for="tag in tags"
              :key="tag.id"
              :to="`/tags/${tag.slug}`"
              class="cloud-tag"
            >
              {{ tag.name }}
              <span v-if="tag.articleCount">{{ tag.articleCount }}</span>
            </router-link>
          </div>
        </div>
      </aside>

      <main class="site-main">
        <router-view />
      </main>
    </div>

    <!-- Footer -->
    <footer class="site-footer">
      <div class="footer-inner">
        <p class="copyright">{{ copyright }}</p>
        <div class="social-links">
          <a v-if="social.github" :href="social.github" target="_blank" rel="noopener">GitHub</a>
          <a v-if="social.email" :href="'mailto:' + social.email">Email</a>
          <router-link to="/admin/login">Admin</router-link>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useThemeStore } from '@/stores/theme'
import { siteApi, categoryApi, tagApi } from '@/api'
import avatarUrl from '@/assets/avatar.png'

const themeStore = useThemeStore()
const router = useRouter()

const menuOpen = ref(false)
const showSearch = ref(false)
const searchKeyword = ref('')
const siteTitle = ref('咲来的 Blog')
const copyright = ref('')
const description = ref('')
const announcement = ref('')
const categories = ref<any[]>([])
const tags = ref<any[]>([])
const social = ref<Record<string, string>>({})

onMounted(async () => {
  try {
    const res = await siteApi.getConfig()
    if (res.data.code === 200) {
      const config = res.data.data
      if (config.site_title) siteTitle.value = config.site_title
      if (config.copyright) copyright.value = config.copyright
      description.value = config.site_description || ''
      announcement.value = config.announcement || ''
      social.value = {
        github: config.social_github || '',
        email: config.social_email || '',
        bilibili: config.social_bilibili || '',
      }
    }
  } catch (e) {
    // Use defaults
  }
  try {
    const [categoryRes, tagRes] = await Promise.all([categoryApi.getList(), tagApi.getList()])
    if (categoryRes.data.code === 200) categories.value = categoryRes.data.data || []
    if (tagRes.data.code === 200) tags.value = tagRes.data.data || []
  } catch (_) {}
})

function doSearch() {
  if (searchKeyword.value.trim()) {
    router.push({ name: 'Search', query: { q: searchKeyword.value.trim() } })
    showSearch.value = false
    searchKeyword.value = ''
  }
}
</script>

<style scoped>
.site-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: var(--header-bg);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-color);
}

.header-inner {
  max-width: var(--layout-width);
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.site-logo {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary) !important;
  letter-spacing: -0.5px;
}

.main-nav {
  display: none;
  position: absolute;
  top: 60px;
  left: 0;
  right: 0;
  background: var(--card-bg);
  flex-direction: column;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
  gap: 16px;
}

.main-nav.nav-open {
  display: flex;
}

.main-nav a {
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
}

.main-nav a:hover {
  color: var(--accent-color);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-btn,
.theme-btn,
.menu-btn {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  font-size: 18px;
  display: flex;
  align-items: center;
  transition: background 0.2s;
}

.search-btn:hover,
.theme-btn:hover,
.menu-btn:hover {
  background: var(--bg-secondary);
}

.menu-btn {
  display: none;
}

.search-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  padding-top: 120px;
}

.search-box {
  width: 560px;
  max-width: 90vw;
}

/* ---------- 左侧栏 + 内容流 ---------- */
.layout-body {
  max-width: var(--layout-width);
  margin: 0 auto;
  padding: 84px 20px 0;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
  min-height: calc(100vh - 200px);
}

.site-sidebar {
  position: sticky;
  top: 76px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: calc(100vh - 92px);
  overflow-y: auto;
  padding-bottom: 4px;
  /* 隐藏滚动条但保留滚轮/触控板滚动 */
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.site-sidebar::-webkit-scrollbar {
  display: none;
}

.side-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-soft);
}

.side-card h3 {
  font-size: 14px;
  margin-bottom: 12px;
}

.profile-card {
  text-align: center;
}

.avatar {
  display: inline-block;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  padding: 3px;
  margin-bottom: 12px;
  transition: transform 0.2s;
}

.avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

.avatar:hover {
  transform: scale(1.06);
}

.profile-name {
  font-size: 17px;
  margin-bottom: 6px;
}

.profile-desc {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
  margin-bottom: 14px;
}

.social-row {
  display: flex;
  justify-content: center;
  gap: 10px;
}

.social-row a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  transition: color 0.2s, background 0.2s, transform 0.2s;
}

.social-row a:hover {
  color: #fff;
  background: var(--accent-color);
  transform: translateY(-2px);
}

.nav-card {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px;
}

.nav-card a {
  padding: 9px 14px;
  border-radius: 10px;
  color: var(--text-secondary);
  font-size: 14px;
  transition: background 0.15s, color 0.15s;
}

.nav-card a:hover {
  background: var(--bg-secondary);
  color: var(--accent-color);
}

.nav-card a.active {
  background: var(--accent-color);
  color: #fff;
  font-weight: 600;
}

.announce-card p {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
  margin: 0;
}

.side-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 7px 0;
  border-top: 1px solid var(--border-color);
  color: var(--text-primary);
  font-size: 14px;
}

.side-row:first-of-type {
  border-top: 0;
}

.side-row:hover {
  color: var(--accent-color);
}

.count,
.cloud-tag span {
  color: var(--text-muted);
  font-size: 12px;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.cloud-tag {
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-size: 13px;
}

.cloud-tag:hover {
  color: var(--accent-color);
}

/* ---------- 主内容与页脚 ---------- */
.site-footer {
  border-top: 1px solid var(--border-color);
  padding: 24px 0;
  margin-top: 60px;
}

.footer-inner {
  max-width: var(--layout-width);
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: var(--text-muted);
}

.social-links {
  display: flex;
  gap: 16px;
}

.social-links a {
  color: var(--text-muted);
}

.social-links a:hover {
  color: var(--accent-color);
}

@media (max-width: 960px) {
  .layout-body {
    grid-template-columns: 1fr;
    padding-top: 76px;
  }

  .site-sidebar {
    display: none;
  }

  .menu-btn {
    display: flex;
  }

  .footer-inner {
    flex-direction: column;
    gap: 12px;
    text-align: center;
  }
}
</style>
