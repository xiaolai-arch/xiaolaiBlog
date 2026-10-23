import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/FrontLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/front/HomePage.vue'), meta: { bare: true } },
      { path: 'articles/:slug', name: 'ArticleDetail', component: () => import('@/views/front/ArticleDetail.vue') },
      { path: 'categories', name: 'Categories', component: () => import('@/views/front/CategoriesPage.vue') },
      { path: 'categories/:slug', name: 'CategoryDetail', component: () => import('@/views/front/CategoryDetail.vue') },
      { path: 'tags', name: 'Tags', component: () => import('@/views/front/TagsPage.vue') },
      { path: 'tags/:slug', name: 'TagDetail', component: () => import('@/views/front/TagDetail.vue') },
      { path: 'archives', name: 'Archives', component: () => import('@/views/front/ArchivesPage.vue') },
      { path: 'search', name: 'Search', component: () => import('@/views/front/SearchPage.vue') },
      { path: 'gallery', name: 'Gallery', component: () => import('@/views/front/GalleryPage.vue') },
      { path: 'projects', name: 'Projects', component: () => import('@/views/front/ProjectsPage.vue') },
      { path: 'tools', name: 'Tools', component: () => import('@/views/front/ToolsPage.vue') },
      { path: 'tools/markdown', name: 'ToolMarkdown', component: () => import('@/views/front/MarkdownToolPage.vue') },
      { path: 'leetcode', name: 'LeetCode', component: () => import('@/views/front/LeetCodePage.vue') },
      { path: 'cet4', name: 'CET4', component: () => import('@/views/front/CET4Page.vue') },
      { path: 'agent', name: 'AgentSprint', component: () => import('@/views/front/AgentSprintPage.vue') },
      { path: 'study', name: 'Study', component: () => import('@/views/front/StudyPage.vue') },
      { path: 'about', name: 'About', component: () => import('@/views/front/AboutPage.vue') },
      { path: 'changelog', name: 'Changelog', component: () => import('@/views/front/ChangelogPage.vue') },
      { path: 'toys/polaroid', name: 'ToyPolaroid', component: () => import('@/views/front/toys/PolaroidStudioPage.vue') },
      { path: 'toys/sketchbook', name: 'ToySketchbook', component: () => import('@/views/front/toys/SketchbookPage.vue') },
    ]
  },
  {
    // 沉浸式胶片页：整页滚动驱动换片，独立于站点布局（同 3D 画廊模式）
    path: '/film',
    name: 'Film',
    component: () => import('@/views/front/FilmPage.vue'),
  },
  {
    // 音乐书架页（参考站 library 形态）：专辑封面书墙 + 底部播放条
    path: '/music',
    name: 'MusicLibrary',
    component: () => import('@/views/front/MusicLibraryPage.vue'),
  },
  {
    // Fully immersive: the room is its own fullscreen shell, so it sits outside
    // FrontLayout rather than fighting the site header's stacking order.
    path: '/gallery/3d',
    name: 'Gallery3D',
    component: () => import('@/views/front/GalleryRoomPage.vue'),
  },
  {
    path: '/login',
    name: 'Auth',
    component: () => import('@/views/AuthPage.vue'),
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    children: [
      { path: 'login', name: 'AdminLogin', component: () => import('@/views/admin/LoginPage.vue') },
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/admin/DashboardPage.vue'), meta: { requiresAuth: true } },
      { path: 'articles', name: 'ArticleList', component: () => import('@/views/admin/ArticleListPage.vue'), meta: { requiresAuth: true } },
      { path: 'articles/create', name: 'ArticleCreate', component: () => import('@/views/admin/ArticleEditPage.vue'), meta: { requiresAuth: true } },
      { path: 'articles/:id/edit', name: 'ArticleEdit', component: () => import('@/views/admin/ArticleEditPage.vue'), meta: { requiresAuth: true } },
      { path: 'categories', name: 'AdminCategories', component: () => import('@/views/admin/CategoryManagePage.vue'), meta: { requiresAuth: true } },
      { path: 'tags', name: 'AdminTags', component: () => import('@/views/admin/TagManagePage.vue'), meta: { requiresAuth: true } },
      { path: 'comments', name: 'CommentManage', component: () => import('@/views/admin/CommentManagePage.vue'), meta: { requiresAuth: true } },
      { path: 'media', name: 'MediaManage', component: () => import('@/views/admin/MediaManagePage.vue'), meta: { requiresAuth: true } },
      { path: 'music', name: 'MusicManage', component: () => import('@/views/admin/MusicManagePage.vue'), meta: { requiresAuth: true } },
      { path: 'settings', name: 'SiteSettings', component: () => import('@/views/admin/SiteSettingsPage.vue'), meta: { requiresAuth: true } },
    ]
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) || 'Xiaolai Blog'
  const token = localStorage.getItem('token')
  let user: any = null
  try {
    user = JSON.parse(localStorage.getItem('user') || 'null')
  } catch (_) {}

  if (to.meta.requiresAuth) {
    // 管理后台：需要管理员角色；前台工具页：登录即可
    if (to.path.startsWith('/admin')) {
      if (!token || user?.role !== 'admin') {
        next({ name: 'AdminLogin' })
        return
      }
    } else if (!token) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }
  next()
})

export default router