import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import axios from 'axios'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref<any>(JSON.parse(localStorage.getItem('user') || 'null'))

  const isLoggedIn = ref(!!token.value)
  const isAdmin = computed(() => user.value?.role === 'admin')

  function persist(t: string, u: any) {
    token.value = t
    user.value = u
    localStorage.setItem('token', t)
    localStorage.setItem('user', JSON.stringify(u))
    isLoggedIn.value = true
  }

  async function login(username: string, password: string) {
    const res = await axios.post('/api/v1/admin/auth/login', { username, password })
    if (res.data.code === 200) {
      persist(res.data.data.token, res.data.data.user)
      return true
    }
    return false
  }

  /** 访客注册：即注即登，角色固定 user */
  async function register(username: string, password: string, nickname?: string) {
    const res = await axios.post('/api/v1/auth/register', {
      username,
      password,
      nickname: nickname || undefined,
    })
    if (res.data.code === 200) {
      persist(res.data.data.token, res.data.data.user)
      return true
    }
    throw new Error(res.data.message || '注册失败')
  }

  function logout() {
    token.value = ''
    user.value = null
    isLoggedIn.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, isLoggedIn, isAdmin, login, register, logout }
})
