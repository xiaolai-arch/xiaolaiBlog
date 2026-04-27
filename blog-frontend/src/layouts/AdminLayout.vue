<template>
  <div class="admin-layout">
    <template v-if="route.name === 'AdminLogin'">
      <router-view />
    </template>
    <template v-else>
      <el-container style="height: 100vh">
        <!-- Sidebar -->
        <el-aside :width="isCollapsed ? '64px' : '220px'" class="admin-sidebar">
          <div class="sidebar-header">
            <router-link to="/" class="sidebar-logo">{{ isCollapsed ? 'XL' : '咲来管理' }}</router-link>
          </div>
          <el-menu
            :default-active="route.path"
            :collapse="isCollapsed"
            background-color="#1a1a2e"
            text-color="#b0b3b8"
            active-text-color="#5e7ce2"
            router
          >
            <el-menu-item index="/admin/dashboard">
              <el-icon><DataAnalysis /></el-icon>
              <span>仪表盘</span>
            </el-menu-item>
            <el-menu-item index="/admin/articles">
              <el-icon><Document /></el-icon>
              <span>文章</span>
            </el-menu-item>
            <el-menu-item index="/admin/categories">
              <el-icon><Folder /></el-icon>
              <span>分类</span>
            </el-menu-item>
            <el-menu-item index="/admin/tags">
              <el-icon><PriceTag /></el-icon>
              <span>标签</span>
            </el-menu-item>
            <el-menu-item index="/admin/comments">
              <el-icon><ChatDotSquare /></el-icon>
              <span>评论</span>
            </el-menu-item>
            <el-menu-item index="/admin/media">
              <el-icon><Picture /></el-icon>
              <span>媒体</span>
            </el-menu-item>
            <el-menu-item index="/admin/settings">
              <el-icon><Setting /></el-icon>
              <span>设置</span>
            </el-menu-item>
          </el-menu>
          <div class="sidebar-collapse" @click="isCollapsed = !isCollapsed">
            <el-icon>
              <component :is="isCollapsed ? 'Expand' : 'Fold'" />
            </el-icon>
          </div>
        </el-aside>

        <el-container>
          <!-- Top bar -->
          <el-header class="admin-header">
            <div class="header-left">
              <el-breadcrumb>
                <el-breadcrumb-item :to="'/'">前台</el-breadcrumb-item>
                <el-breadcrumb-item>Admin</el-breadcrumb-item>
              </el-breadcrumb>
            </div>
            <div class="header-right">
              <el-button @click="themeStore.toggle()" circle size="small">
                <el-icon><component :is="themeStore.isDark ? 'Sunny' : 'Moon'" /></el-icon>
              </el-button>
              <el-dropdown trigger="click">
                <span class="user-info">
                  {{ authStore.user?.nickname || '管理员' }}
                  <el-icon><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-item @click="authStore.logout()">退出登录</el-dropdown-item>
                </template>
              </el-dropdown>
            </div>
          </el-header>

          <!-- Content -->
          <el-main class="admin-main">
            <router-view />
          </el-main>
        </el-container>
      </el-container>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const themeStore = useThemeStore()
const authStore = useAuthStore()

const isCollapsed = ref(false)
</script>

<style scoped>
.admin-sidebar {
  background: #1a1a2e;
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
  overflow: hidden;
}

.sidebar-header {
  padding: 16px;
  text-align: center;
  border-bottom: 1px solid #2d2d44;
}

.sidebar-logo {
  color: #e4e6eb !important;
  font-size: 18px;
  font-weight: 700;
}

.admin-header {
  background: var(--header-bg);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  cursor: pointer;
  color: var(--text-primary);
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.admin-main {
  background: var(--bg-secondary);
  padding: 20px;
  overflow-y: auto;
}

.sidebar-collapse {
  margin-top: auto;
  padding: 12px 16px;
  border-top: 1px solid #2d2d44;
  cursor: pointer;
  color: #b0b3b8;
  text-align: center;
}

.sidebar-collapse:hover {
  color: #5e7ce2;
}
</style>