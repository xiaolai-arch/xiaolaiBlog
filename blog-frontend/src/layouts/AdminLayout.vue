<template>
  <div class="admin-layout" data-theme="light" :class="{ 'sidebar-collapsed': isCollapsed }">
    <router-view v-if="isLogin" :key="route.fullPath" />
    <template v-else>
      <a class="admin-skip-link" href="#admin-content">跳至主要内容</a>
      <aside class="admin-sidebar" aria-label="后台侧栏">
        <router-link to="/admin/dashboard" class="sidebar-brand" :aria-label="isCollapsed ? '咲来管理 · 概览' : undefined">
          <span class="brand-mark" aria-hidden="true">咲</span><span v-if="!isCollapsed" class="brand-copy">咲来 <span>内容管理</span></span>
        </router-link>
        <AdminNavigation :collapsed="isCollapsed" />
        <div class="sidebar-bottom">
          <router-link to="/" class="site-return"><el-icon><ArrowLeft /></el-icon><span v-if="!isCollapsed">访问博客</span></router-link>
          <button class="sidebar-toggle rounded-full font-medium transition-all duration-300" @click="isCollapsed = !isCollapsed" :aria-label="isCollapsed ? '展开侧栏' : '收起侧栏'" :aria-expanded="!isCollapsed">
            <el-icon><component :is="isCollapsed ? 'Expand' : 'Fold'" /></el-icon><span v-if="!isCollapsed">收起侧栏</span>
          </button>
        </div>
      </aside>
      <div class="admin-workspace">
        <header class="admin-header">
          <div class="header-location">
            <button class="mobile-nav-button rounded-full font-medium transition-all duration-300" @click="mobileOpen = true" aria-label="打开后台导航" :aria-expanded="mobileOpen"><el-icon><Menu /></el-icon></button>
            <span class="workspace-label">工作空间</span><span class="header-separator" aria-hidden="true">/</span><span class="current-page">{{ pageName }}</span>
          </div>
          <div class="admin-header-actions">
            <router-link to="/" class="header-site-link">查看网站 <el-icon><TopRight /></el-icon></router-link>
            <el-dropdown trigger="click" @command="accountCommand">
              <button class="account-button rounded-full font-medium transition-all duration-300" aria-label="账号菜单">
                <span class="account-avatar">{{ userName.slice(0, 1) }}</span><span class="account-name">{{ userName }}</span><el-icon><ArrowDown /></el-icon>
              </button>
              <template #dropdown><el-dropdown-menu><el-dropdown-item command="account">账号设置</el-dropdown-item><el-dropdown-item command="logout" divided>退出登录</el-dropdown-item></el-dropdown-menu></template>
            </el-dropdown>
          </div>
        </header>
        <main id="admin-content" class="admin-main" tabindex="-1"><div class="admin-content"><router-view :key="route.fullPath" /></div><footer class="admin-footer">咲来 · 内容管理<span>专注创作，记录每一步。</span></footer></main>
      </div>
      <el-drawer v-model="mobileOpen" direction="ltr" size="min(300px, 88vw)" title="咲来 · 内容管理" class="admin-mobile-drawer">
        <AdminNavigation @navigate="mobileOpen = false" /><router-link to="/" class="site-return" @click="mobileOpen = false">访问博客 →</router-link>
      </el-drawer>
    </template>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, TopRight } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import AdminNavigation from '@/components/AdminNavigation.vue'
import '@/assets/styles/admin.scss'
const route = useRoute(), router = useRouter(), auth = useAuthStore()
const isLogin = computed(() => route.name === 'AdminLogin')
const isCollapsed = ref(false), mobileOpen = ref(false)
const userName = computed(() => auth.user?.nickname || auth.user?.username || '管理员')
const pageNames: Record<string, string> = { dashboard: '概览', articles: '文章', categories: '分类', tags: '标签', comments: '评论', media: '媒体库', gallery: '画廊映射', music: '音乐', settings: '站点设置' }
const pageName = computed(() => pageNames[String(route.path.split('/')[2])] || '内容管理')
watch(() => route.fullPath, () => { mobileOpen.value = false })
onMounted(() => document.body.classList.add('admin-surface'))
onBeforeUnmount(() => document.body.classList.remove('admin-surface'))
async function accountCommand(command: string) { if (command === 'logout') { await auth.logout(); await router.replace('/admin/login') } else await router.push('/account') }
</script>
