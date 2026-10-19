<template>
  <div class="front-layout" :data-theme="themeStore.isDark ? 'dark' : 'light'">
    <!-- ============ 内页精简头（首页 bare 模式不渲染） ============ -->
    <header v-if="!isBare" class="slim-header">
      <div class="slim-inner">
        <router-link to="/" class="site-logo">{{ siteTitle }}</router-link>

        <nav class="pill-nav" :class="{ 'nav-open': menuOpen }">
          <router-link to="/" exact-active-class="active">Work</router-link>
          <router-link to="/gallery" active-class="active">Art</router-link>
          <router-link to="/about" active-class="active">About</router-link>
          <router-link to="/archives" active-class="active">Blog</router-link>
          <el-dropdown trigger="hover" @command="onMoreNav">
            <span class="more-trigger" :class="{ active: moreActive }">More</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/study">学习</el-dropdown-item>
                <el-dropdown-item command="/categories">分类</el-dropdown-item>
                <el-dropdown-item command="/tags">标签</el-dropdown-item>
                <el-dropdown-item command="/leetcode">刷题</el-dropdown-item>
                <el-dropdown-item command="/cet4">四级</el-dropdown-item>
                <el-dropdown-item command="/agent">Agent</el-dropdown-item>
                <el-dropdown-item command="/tools">工具箱</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </nav>

        <div class="header-actions">
          <button class="icon-btn" @click="showSearch = true" aria-label="搜索">
            <el-icon><Search /></el-icon>
          </button>
          <button class="icon-btn" @click="themeStore.toggle()" aria-label="切换主题">
            <el-icon><component :is="themeStore.isDark ? 'Sunny' : 'Moon'" /></el-icon>
          </button>
          <button class="icon-btn menu-btn" @click="menuOpen = !menuOpen" aria-label="菜单">
            <el-icon><Menu /></el-icon>
          </button>
        </div>
      </div>
      <div class="slim-divider"></div>
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

    <!-- ============ 内容 ============ -->
    <main class="site-main">
      <router-view />
    </main>

    <!-- ============ Footer（对标原站：分隔线 + 四栏 + Built with 行） ============ -->
    <footer class="site-footer">
      <div class="footer-inner">
        <div class="footer-rule"></div>

        <div class="footer-grid">
          <!-- 列 1：logo + 当地时间 -->
          <div class="footer-col footer-brand">
            <router-link to="/" class="footer-logo">{{ siteTitle }}</router-link>
            <p class="footer-time">
              {{ localTime }} <span class="blink">•</span> {{ city }}
            </p>
          </div>

          <!-- 列 3：导航 -->
          <nav class="footer-col footer-nav">
            <router-link to="/">Work</router-link>
            <router-link to="/gallery">Art</router-link>
            <router-link to="/about">About</router-link>
            <router-link to="/archives">Blog</router-link>
          </nav>

          <!-- 列 4：联系 -->
          <div class="footer-col footer-contact">
            <p class="contact-label">Let's chat!</p>
            <p class="contact-email">
              <a v-if="social.email" :href="'mailto:' + social.email" class="email-link">
                {{ social.email }} <span class="email-arrow">↗</span>
              </a>
              <a v-else-if="social.github" :href="social.github" target="_blank" rel="noopener" class="email-link">
                GitHub <span class="email-arrow">↗</span>
              </a>
            </p>
            <div class="footer-social">
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
            </div>
          </div>
        </div>

        <!-- 底部行 -->
        <div class="footer-bottom">
          <p>
            <span>Built with Vue 3 &amp; </span>
            <span class="matcha">一杯好茶</span>
            <span> · © {{ year }} {{ siteTitle }}</span>
          </p>
          <router-link to="/changelog" class="changelog-link">CHANGELOG: {{ changelogDate }}</router-link>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useThemeStore } from '@/stores/theme'
import { siteApi } from '@/api'

const themeStore = useThemeStore()
const router = useRouter()
const route = useRoute()

const menuOpen = ref(false)
const showSearch = ref(false)
const searchKeyword = ref('')
const siteTitle = ref('咲来的 Blog')
const description = ref('')
const social = ref<Record<string, string>>({})
const localTime = ref('')
const city = ref('中国')

const changelogDate = '10-19-26'
const year = new Date().getFullYear()

// 首页 bare 模式：页面自带渐变 PageHeader，不渲染精简头
const isBare = computed(() => route.meta.bare === true)

const moreActive = computed(() =>
  ['/study', '/categories', '/tags', '/leetcode', '/cet4', '/agent', '/tools'].some((p) =>
    route.path === p || route.path.startsWith(p + '/')))

let timer: number | undefined

function tick() {
  localTime.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
}

onMounted(async () => {
  tick()
  timer = window.setInterval(tick, 1000)
  try {
    const res = await siteApi.getConfig()
    if (res.data.code === 200) {
      const config = res.data.data
      if (config.site_title) siteTitle.value = config.site_title
      description.value = config.site_description || ''
      social.value = {
        github: config.social_github || '',
        email: config.social_email || '',
        bilibili: config.social_bilibili || '',
      }
    }
  } catch (e) {
    // Use defaults
  }
})

onUnmounted(() => window.clearInterval(timer))

function onMoreNav(path: string) {
  menuOpen.value = false
  router.push(path)
}

function doSearch() {
  if (searchKeyword.value.trim()) {
    router.push({ name: 'Search', query: { q: searchKeyword.value.trim() } })
    showSearch.value = false
    searchKeyword.value = ''
  }
}
</script>

<style scoped>
/* ============ 内页精简头 ============ */
.slim-header {
  position: relative;
  z-index: 100;
  background: var(--bg-primary);
}

.slim-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.site-logo {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 0.0125em;
  color: var(--text-primary) !important;
  white-space: nowrap;
}

.pill-nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pill-nav > a,
.more-trigger {
  padding: 5px 14px 4px;
  border-radius: 999px;
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 500;
  letter-spacing: 0.005em;
  color: var(--text-muted);
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  transition: color 0.2s ease-out;
  outline: none;
}

.pill-nav > a:hover,
.more-trigger:hover {
  color: var(--text-secondary);
}

.pill-nav > a.active {
  color: var(--text-secondary);
  position: relative;
}

.pill-nav > a.active::before,
.more-trigger.active::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  background: rgba(228, 228, 234, 0.6);
  backdrop-filter: blur(8px);
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.03),
    0 4px 14px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
  z-index: -1;
}

[data-theme='dark'] .pill-nav > a.active::before,
[data-theme='dark'] .more-trigger.active::before {
  background: rgba(44, 44, 58, 0.7);
  border-color: rgba(255, 255, 255, 0.08);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.icon-btn {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  padding: 8px;
  border-radius: 999px;
  font-size: 17px;
  display: flex;
  align-items: center;
  transition: background 0.2s, color 0.2s;
}

.icon-btn:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.menu-btn {
  display: none;
}

.slim-divider {
  max-width: 1280px;
  margin: 0 auto;
  height: 1px;
  background: var(--border-color);
}

.search-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  padding-top: 120px;
}

.search-box {
  width: 560px;
  max-width: 90vw;
}

/* ============ 内容 ============ */
.site-main {
  min-height: calc(100vh - 420px);
}

/* ============ Footer ============ */
.site-footer {
  padding: 32px 0 40px;
}

.footer-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 64px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.footer-rule {
  height: 1px;
  background: var(--border-color);
}

.footer-grid {
  display: grid;
  grid-template-columns: 1.6fr 0.7fr 1.4fr;
  gap: 20px;
  padding-top: 20px;
}

.footer-logo {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 0.0125em;
  color: var(--text-primary) !important;
}

.footer-time {
  margin-top: 12px;
  font-size: 16px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}

.blink {
  animation: blink 2s steps(1) infinite;
  color: var(--accent-color);
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

.footer-col {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
}

.footer-nav {
  gap: 6px;
}

.footer-nav a {
  font-family: var(--font-display);
  font-size: 16px;
  color: var(--text-muted);
  transition: color 0.2s;
}

.footer-nav a:hover {
  color: var(--text-secondary);
}

.contact-label {
  font-size: 16px;
  color: var(--text-muted);
}

.contact-email {
  margin-top: 4px;
}

.email-link {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-secondary);
  word-break: break-all;
}

.email-link:hover {
  color: #3b82f6;
}

.email-arrow {
  display: inline-block;
  opacity: 0;
  margin-left: 4px;
  transition: opacity 0.15s ease-out;
}

.email-link:hover .email-arrow {
  opacity: 1;
}

.footer-social {
  display: flex;
  gap: 16px;
  margin-top: 8px;
}

.footer-social a {
  color: var(--text-muted);
  display: inline-flex;
  transition: color 0.2s, transform 0.2s;
}

.footer-social a:hover {
  color: var(--text-secondary);
  transform: translateY(-2px);
}

.footer-bottom {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-muted);
  text-align: center;
}

.matcha {
  font-weight: 500;
  color: #059669;
  transition: color 0.2s;
  cursor: default;
}

.matcha:hover {
  color: #10b981;
}

.changelog-link {
  font-size: 13px;
  letter-spacing: 0.02em;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}

.changelog-link:hover {
  color: #3b82f6;
}

@media (max-width: 768px) {
  .slim-inner {
    padding: 16px 24px;
  }

  .pill-nav {
    display: none;
    position: absolute;
    top: 64px;
    left: 12px;
    right: 12px;
    background: var(--card-bg);
    flex-direction: column;
    align-items: stretch;
    padding: 10px;
    border-radius: 20px;
    box-shadow: var(--shadow-hover);
    gap: 2px;
    z-index: 99;
  }

  .pill-nav.nav-open {
    display: flex;
  }

  .pill-nav > a,
  .more-trigger {
    justify-content: center;
  }

  .menu-btn {
    display: flex;
  }

  .footer-inner {
    padding: 0 24px;
  }

  .footer-grid {
    grid-template-columns: 1fr;
    gap: 32px;
  }
}
</style>
