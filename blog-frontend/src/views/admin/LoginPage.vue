<template>
  <main class="admin-login-page"><router-link to="/" class="login-home">咲来 Blog</router-link>
    <section class="admin-login-card bg-white rounded-2xl shadow-sm"><span class="login-brand-mark" aria-hidden="true">咲</span><h1>欢迎回来。</h1><p class="login-description">登录内容管理，继续你的创作。</p>
      <el-form :model="form" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="用户名"><el-input v-model.trim="form.username" name="username" autocomplete="username" placeholder="输入管理员用户名" size="large" class="bg-[#f5f5f7] rounded-xl text-[#1d1d1f] focus:outline-none focus:ring-2 focus:ring-[#0071e3]/30 transition-all " /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" name="password" type="password" autocomplete="current-password" placeholder="输入密码" size="large" show-password class="bg-[#f5f5f7] rounded-xl text-[#1d1d1f] focus:outline-none focus:ring-2 focus:ring-[#0071e3]/30 transition-all " /></el-form-item>
        <p v-if="error" class="login-error" role="alert">{{ error }}</p>
        <el-button type="primary" size="large" native-type="submit" :loading="loading" class="login-submit rounded-full font-medium transition-all duration-300 active:scale-[0.98]">{{ loading ? '正在登录…' : '登录管理后台' }}</el-button>
      </el-form><router-link to="/" class="login-back">返回博客 <span aria-hidden="true">↗</span></router-link>
    </section><p class="login-footer">专注创作，记录每一步。</p>
  </main>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
const router = useRouter(), authStore = useAuthStore()
const form = ref({ username: '', password: '' }), loading = ref(false), error = ref('')
async function handleLogin() {
  if (loading.value) return
  if (!form.value.username || !form.value.password) { error.value = '请输入用户名和密码'; return }
  loading.value = true; error.value = ''
  try {
    const success = await authStore.login(form.value.username, form.value.password)
    if (success && authStore.isAdmin) await router.replace('/admin/dashboard')
    else if (success) { await authStore.logout(); error.value = '此账号没有后台管理权限，请使用管理员账号登录' }
    else error.value = '用户名或密码不正确'
  } catch (e: any) { error.value = e.response?.data?.message || '登录失败，请稍后重试' }
  finally { loading.value = false }
}
</script>
