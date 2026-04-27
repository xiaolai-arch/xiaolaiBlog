import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig } from 'axios'

const api: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (window.location.pathname.startsWith('/admin') && window.location.pathname !== '/admin/login') {
        window.location.href = '/admin/login'
      }
    }
    return Promise.reject(error)
  }
)

export default api

// API modules
export const articleApi = {
  getList: (params?: any) => api.get('/articles', { params }),
  getTop: () => api.get('/articles/top'),
  getBySlug: (slug: string) => api.get(`/articles/${slug}`),
  getAdminList: (params?: any) => api.get('/admin/articles', { params }),
  getAdminDetail: (id: number) => api.get(`/admin/articles/${id}`),
  create: (data: any) => api.post('/admin/articles', data),
  update: (id: number, data: any) => api.put(`/admin/articles/${id}`, data),
  delete: (id: number) => api.delete(`/admin/articles/${id}`),
  restore: (id: number) => api.put(`/admin/articles/${id}/restore`),
  permanentDelete: (id: number) => api.delete(`/admin/articles/${id}/permanent`),
  getArchives: () => api.get('/archives'),
}

export const categoryApi = {
  getList: () => api.get('/categories'),
  getAdminList: () => api.get('/admin/categories'),
  create: (data: any) => api.post('/admin/categories', data),
  update: (id: number, data: any) => api.put(`/admin/categories/${id}`, data),
  delete: (id: number) => api.delete(`/admin/categories/${id}`),
}

export const tagApi = {
  getList: () => api.get('/tags'),
  getAdminList: () => api.get('/admin/tags'),
  create: (data: any) => api.post('/admin/tags', data),
  update: (id: number, data: any) => api.put(`/admin/tags/${id}`, data),
  delete: (id: number) => api.delete(`/admin/tags/${id}`),
}

export const commentApi = {
  getByArticleSlug: (slug: string) => api.get(`/articles/${slug}/comments`),
  create: (slug: string, data: any) => api.post(`/articles/${slug}/comments`, data),
  getAdminList: (params?: any) => api.get('/admin/comments', { params }),
  approve: (id: number) => api.put(`/admin/comments/${id}/approve`),
  reject: (id: number) => api.put(`/admin/comments/${id}/reject`),
  markSpam: (id: number) => api.put(`/admin/comments/${id}/spam`),
  reply: (id: number, content: string) => api.post(`/admin/comments/${id}/reply`, { content }),
  delete: (id: number) => api.delete(`/admin/comments/${id}`),
}

export const mediaApi = {
  upload: (file: File, title?: string, categoryId?: number) => {
    const formData = new FormData()
    formData.append('file', file)
    if (title) formData.append('title', title)
    if (categoryId) formData.append('categoryId', String(categoryId))
    return api.post('/admin/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  getList: (categoryId?: number) => api.get('/admin/files', { params: categoryId ? { categoryId } : {} }),
  update: (id: number, data: { title?: string; categoryId?: number }) => api.put(`/admin/files/${id}`, data),
  delete: (id: number) => api.delete(`/admin/files/${id}`),
}

export const mediaCategoryApi = {
  getList: () => api.get('/admin/media-categories'),
  create: (data: { name: string; slug?: string; description?: string }) => api.post('/admin/media-categories', data),
  update: (id: number, data: { name: string; slug?: string; description?: string }) => api.put(`/admin/media-categories/${id}`, data),
  delete: (id: number) => api.delete(`/admin/media-categories/${id}`),
}

export const galleryApi = {
  getCategories: () => api.get('/gallery/categories'),
  getImages: (params?: { categoryId?: number; page?: number; size?: number }) => api.get('/gallery/images', { params }),
}

export const siteApi = {
  getConfig: () => api.get('/site/config'),
  getAbout: () => api.get('/site/about'),
  getAdminConfig: () => api.get('/admin/config'),
  updateConfig: (data: any) => api.put('/admin/config', data),
}

export const authApi = {
  login: (data: any) => api.post('/admin/auth/login', data),
  logout: () => api.post('/admin/auth/logout'),
}