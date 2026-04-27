import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/FrontLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/front/HomePage.vue') },
      { path: 'articles/:slug', name: 'ArticleDetail', component: () => import('@/views/front/ArticleDetail.vue') },
      { path: 'categories', name: 'Categories', component: () => import('@/views/front/CategoriesPage.vue') },
      { path: 'categories/:slug', name: 'CategoryDetail', component: () => import('@/views/front/CategoryDetail.vue') },
      { path: 'tags', name: 'Tags', component: () => import('@/views/front/TagsPage.vue') },
      { path: 'tags/:slug', name: 'TagDetail', component: () => import('@/views/front/TagDetail.vue') },
      { path: 'archives', name: 'Archives', component: () => import('@/views/front/ArchivesPage.vue') },
      { path: 'search', name: 'Search', component: () => import('@/views/front/SearchPage.vue') },
      { path: 'gallery', name: 'Gallery', component: () => import('@/views/front/GalleryPage.vue') },
      { path: 'about', name: 'About', component: () => import('@/views/front/AboutPage.vue') },
    ]
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
  if (to.meta.requiresAuth) {
    const token = localStorage.getItem('token')
    if (!token && to.name !== 'AdminLogin') {
      next({ name: 'AdminLogin' })
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router