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
      if (window.location.pathname !== '/login') {
        if (window.location.pathname.startsWith('/admin')) {
          if (window.location.pathname !== '/admin/login') {
            window.location.href = '/admin/login'
          }
        } else {
          window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname)
        }
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

export const galleryFrameApi = {
  /** 公开：3D 展厅读取画框映射（frameId -> {imageUrl,label}） */
  getFrames: () => api.get('/gallery-frames'),
  adminList: () => api.get('/admin/gallery-frames'),
  adminSave: (frames: Array<{ frameId: string; imageUrl: string; label?: string | null }>) =>
    api.put('/admin/gallery-frames', frames),
}

export const siteApi = {
  getConfig: () => api.get('/site/config'),
  getAbout: () => api.get('/site/about'),
  getAdminConfig: () => api.get('/admin/config'),
  updateConfig: (data: any) => api.put('/admin/config', data),
}

export const noteApi = {
  list: () => api.get('/notes'),
  detail: (id: number) => api.get(`/notes/${id}`),
  create: (data: { title: string; content: string }) => api.post('/notes', data),
  update: (id: number, data: { title?: string; content?: string }) => api.put(`/notes/${id}`, data),
  remove: (id: number) => api.delete(`/notes/${id}`),
}

export const progressApi = {
  get: (key: string) => api.get(`/progress/${key}`),
  put: (key: string, value: string) => api.put(`/progress/${key}`, { value }),
}

export const musicApi = {
  getCurrent: () => api.get('/music/current'),
  getAdminList: () => api.get('/admin/music'),
  upload: (payload: { audio: File; cover?: File; title: string; artist?: string; album?: string; lyrics?: string; duration?: number }) => {
    const formData = new FormData()
    formData.append('audio', payload.audio)
    if (payload.cover) formData.append('cover', payload.cover)
    formData.append('title', payload.title)
    if (payload.artist) formData.append('artist', payload.artist)
    if (payload.album) formData.append('album', payload.album)
    if (payload.lyrics) formData.append('lyrics', payload.lyrics)
    if (payload.duration) formData.append('duration', String(payload.duration))
    return api.post('/admin/music/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000,
    })
  },
  update: (id: number, data: { title?: string; artist?: string; album?: string; lyrics?: string }) =>
    api.put(`/admin/music/${id}`, data),
  delete: (id: number) => api.delete(`/admin/music/${id}`),
}

export const authApi = {
  login: (data: any) => api.post('/admin/auth/login', data),
  logout: () => api.post('/admin/auth/logout'),
  changePassword: (data: { oldPassword: string; newPassword: string }) => api.put('/admin/profile/password', data),
}