<template>
  <div class="front-layout" :data-theme="themeStore.isDark ? 'dark' : 'light'">
    <!-- Header / Navigation -->
    <header class="site-header">
      <div class="header-inner">
        <router-link to="/" class="site-logo">{{ siteTitle }}</router-link>
        <nav class="main-nav" :class="{ 'nav-open': menuOpen }">
          <router-link to="/" @click="menuOpen = false">首页</router-link>
          <router-link to="/archives" @click="menuOpen = false">文件</router-link>
          <router-link to="/categories" @click="menuOpen = false">类别</router-link>
          <router-link to="/tags" @click="menuOpen = false">标签</router-link>
          <router-link to="/gallery" @click="menuOpen = false">图库</router-link>
          <router-link to="/about" @click="menuOpen = false">关于我</router-link>
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
          placeholder="Search articles..."
          size="large"
          clearable
          @keyup.enter="doSearch"
          autofocus
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
    </div>

    <!-- Main content -->
    <main class="main-content">
      <router-view />
    </main>

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
import { siteApi } from '@/api'

const themeStore = useThemeStore()
const router = useRouter()

const menuOpen = ref(false)
const showSearch = ref(false)
const searchKeyword = ref('')
const siteTitle = ref('Xiaolai Blog')
const copyright = ref('')
const social = ref<Record<string, string>>({})

onMounted(async () => {
  try {
    const res = await siteApi.getConfig()
    if (res.data.code === 200) {
      const config = res.data.data
      if (config.site_title) siteTitle.value = config.site_title
      if (config.copyright) copyright.value = config.copyright
      social.value = {
        github: config.social_github || '',
        email: config.social_email || '',
      }
    }
  } catch (e) {
    // Use defaults
  }
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
  max-width: 800px;
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
  display: flex;
  gap: 24px;
}

.main-nav a {
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  transition: color 0.2s;
}

.main-nav a:hover,
.main-nav a.router-link-exact-active {
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

.main-content {
  min-height: calc(100vh - 160px);
  padding-top: 80px;
}

.site-footer {
  border-top: 1px solid var(--border-color);
  padding: 24px 0;
  margin-top: 60px;
}

.footer-inner {
  max-width: 800px;
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

@media (max-width: 640px) {
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