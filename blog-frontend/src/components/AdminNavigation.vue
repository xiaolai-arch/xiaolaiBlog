<template>
  <nav class="admin-navigation" aria-label="后台导航">
    <div v-for="group in groups" :key="group.title" class="nav-group">
      <p class="nav-group-title" v-if="!collapsed">{{ group.title }}</p>
      <router-link v-for="item in group.items" :key="item.path" :to="item.path"
        class="admin-nav-link rounded-full font-medium transition-all duration-300"
        :class="{ selected: isActive(item.path) }" :aria-current="isActive(item.path) ? 'page' : undefined"
        :aria-label="collapsed ? item.label : undefined" :title="collapsed ? item.label : undefined" @click="$emit('navigate')">
        <el-icon><component :is="item.icon" /></el-icon><span v-if="!collapsed">{{ item.label }}</span>
      </router-link>
    </div>
  </nav>
</template>
<script setup lang="ts">
import { useRoute } from 'vue-router'
defineProps<{ collapsed?: boolean }>()
defineEmits<{ navigate: [] }>()
const route = useRoute()
const isActive = (path: string) => route.path === path || route.path.startsWith(path + '/')
const groups = [
  { title: '工作空间', items: [{ path: '/admin/dashboard', label: '概览', icon: 'DataAnalysis' }, { path: '/admin/articles', label: '文章', icon: 'Document' }, { path: '/admin/comments', label: '评论', icon: 'ChatDotSquare' }] },
  { title: '内容管理', items: [{ path: '/admin/categories', label: '分类', icon: 'Folder' }, { path: '/admin/tags', label: '标签', icon: 'PriceTag' }, { path: '/admin/media', label: '媒体库', icon: 'Picture' }, { path: '/admin/gallery', label: '画廊映射', icon: 'Printer' }, { path: '/admin/music', label: '音乐', icon: 'Headset' }] },
  { title: '偏好设置', items: [{ path: '/admin/settings', label: '站点设置', icon: 'Setting' }] },
]
</script>
