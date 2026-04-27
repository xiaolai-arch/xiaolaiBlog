<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="login-title">Xiaolai Blog</h1>
      <el-form :model="form" label-position="top" @keyup.enter="handleLogin">
        <el-form-item label="Username">
          <el-input v-model="form.username" placeholder="Username" size="large" />
        </el-form-item>
        <el-form-item label="Password">
          <el-input v-model="form.password" type="password" placeholder="Password" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" @click="handleLogin" style="width: 100%">
            {{ loading ? 'Logging in...' : 'Login' }}
          </el-button>
        </el-form-item>
        <p v-if="error" class="error">{{ error }}</p>
      </el-form>
      <router-link to="/" class="back-link">这里不是你该来的地方，快回去吧！</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const form = ref({ username: '', password: '' })
const loading = ref(false)
const error = ref('')

async function handleLogin() {
  if (!form.value.username || !form.value.password) {
    error.value = 'Please enter username and password'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const success = await authStore.login(form.value.username, form.value.password)
    if (success) {
      router.push('/admin/dashboard')
    } else {
      error.value = 'Invalid username or password'
    }
  } catch (e: any) {
    error.value = e.response?.data?.message || 'Login failed'
  }
  loading.value = false
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--bg-secondary);
}

.login-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 40px;
  width: 400px;
  max-width: 90vw;
}

.login-title {
  text-align: center;
  margin-bottom: 32px;
  font-size: 24px;
}

.error {
  color: #e74c3c;
  text-align: center;
  font-size: 14px;
}

.back-link {
  display: block;
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
}
</style>