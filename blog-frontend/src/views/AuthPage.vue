<template>
  <div class="auth-page">
    <div class="auth-card">
      <router-link to="/" class="auth-logo">{{ siteTitle }}</router-link>
      <h1 class="auth-title">{{ mode === 'login' ? '登录' : '注册' }}</h1>
      <p class="auth-sub">学习工具（笔记 / 算法 / 四级 / Agent）注册后即可使用</p>

      <div v-if="recoveryMode" class="auth-form">
        <h2>{{ recoveryMode === 'reset' ? '设置新密码' : recoveryMode === 'verify' ? '验证邮箱' : '找回密码' }}</h2>
        <input v-if="recoveryMode === 'request'" v-model="form.username" placeholder="用户名" autocomplete="username" />
        <template v-if="recoveryMode === 'reset'"><input v-model="form.password" type="password" placeholder="新密码（8–64 位）" autocomplete="new-password" /><input v-model="confirmPassword" type="password" placeholder="再次输入新密码" autocomplete="new-password" /></template>
        <p v-if="notice" role="status">{{ notice }}</p><p v-if="error" role="alert">{{ error }}</p>
        <button class="auth-submit" :disabled="submitting" @click="recover">{{ submitting ? '处理中…' : recoveryMode === 'verify' ? '确认验证邮箱' : recoveryMode === 'reset' ? '重置密码' : '发送找回邮件' }}</button>
        <button class="switch-link" @click="exitRecovery">返回登录</button>
      </div>
      <div v-show="!recoveryMode" class="mode-tabs">
        <button :class="{ on: mode === 'login' }" @click="mode = 'login'">登录</button>
        <button :class="{ on: mode === 'register' }" @click="mode = 'register'">注册</button>
      </div>

      <form v-if="!recoveryMode" class="auth-form" @submit.prevent="submit">
        <label class="field">
          <span>用户名</span>
          <input v-model.trim="form.username" autocomplete="username" placeholder="中文、字母、数字、下划线" required />
        </label>
        <label v-if="mode === 'register'" class="field">
          <span>昵称（可选）</span>
          <input v-model.trim="form.nickname" placeholder="打卡记录里显示的名字" />
        </label>
        <label class="field">
          <span>密码</span>
          <input
            v-model="form.password"
            type="password"
            :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
            :placeholder="mode === 'register' ? '8–64 位' : '密码'"
            required
          />
        </label>

        <p v-if="error" class="auth-error">{{ error }}</p>

        <button class="auth-submit" :disabled="submitting">
          {{ submitting ? '请稍候…' : mode === 'login' ? '登录' : '注册并登录' }}
        </button>
      </form>

      <p v-if="!recoveryMode && recoveryEnabled" class="auth-hint"><button class="switch-link" @click="recoveryMode = 'request'">忘记密码？</button></p>
      <p v-if="!recoveryMode" class="auth-hint">
        {{ mode === 'login' ? '还没有账号？' : '已有账号？' }}
        <button class="switch-link" @click="mode = mode === 'login' ? 'register' : 'login'">
          {{ mode === 'login' ? '去注册' : '去登录' }}
        </button>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { siteApi, accountApi } from '@/api'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const mode = ref<'login' | 'register'>('login')
const siteTitle = ref('Xiaolai Blog')
const form = ref({ username: '', password: '', nickname: '' })
const error = ref('')
const submitting = ref(false)
const recoveryEnabled = ref(false), notice = ref(''), confirmPassword = ref('')
const recoveryMode = ref<'request' | 'reset' | 'verify' | ''>(route.query.reset ? 'reset' : route.query.verify ? 'verify' : '')
const recoveryToken = String(route.query.reset || route.query.verify || '')
if (recoveryToken) { void router.replace({ path: '/login', query: {} }) }
accountApi.recovery().then(res => { recoveryEnabled.value = !!res.data.data.enabled }).catch(() => {})
function exitRecovery() { recoveryMode.value = ''; error.value = ''; notice.value = ''; form.value.password = ''; void router.replace('/login') }
async function recover() {
  if (submitting.value) return
  submitting.value = true; error.value = ''; notice.value = ''
  try {
    if (recoveryMode.value === 'verify') { await accountApi.verify(recoveryToken); notice.value = '邮箱验证成功，可以返回登录或账号设置' }
    else if (recoveryMode.value === 'reset') { if (form.value.password !== confirmPassword.value) throw new Error('两次输入的密码不一致'); await accountApi.confirm(recoveryToken, form.value.password); notice.value = '密码已重置，请返回登录'; form.value.password = ''; confirmPassword.value = '' }
    else { await accountApi.reset(form.value.username); notice.value = '如果账号已绑定验证邮箱，将收到找回邮件，请在 30 分钟内使用' }
  } catch (e: any) { error.value = e.response?.data?.message || e.message || '操作失败' }
  finally { submitting.value = false }
}

siteApi.getConfig().then(res => {
  if (res.data.code === 200 && res.data.data.site_title) siteTitle.value = res.data.data.site_title
}).catch(() => {})

async function submit() {
  if (!form.value.username || !form.value.password) return
  submitting.value = true
  error.value = ''
  try {
    if (mode.value === 'login') {
      const ok = await authStore.login(form.value.username, form.value.password)
      if (!ok) error.value = '用户名或密码错误'
    } else {
      await authStore.register(form.value.username, form.value.password, form.value.nickname)
    }
  } catch (e: any) {
    error.value = e?.message || '操作失败，请稍后再试'
  } finally {
    submitting.value = false
  }
  if (!error.value) {
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.push(redirect.startsWith('/') && !redirect.startsWith('//') && !redirect.includes('\\') ? redirect : '/')
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  background:
    radial-gradient(80% 60% at 20% 0%, rgba(213, 224, 255, 0.7), transparent 60%),
    radial-gradient(80% 60% at 85% 15%, rgba(245, 226, 255, 0.6), transparent 55%),
    var(--bg-primary);
}

.auth-card {
  width: 400px;
  max-width: 100%;
  background: var(--card-bg, #fff);
  border: 1px solid var(--border-color);
  border-radius: 24px;
  padding: 36px 36px 28px;
  box-shadow: 0 20px 60px rgba(24, 24, 27, 0.08);
}

.auth-logo {
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0.02em;
  color: var(--text-secondary, #71717a) !important;
  text-decoration: none;
}

.auth-title {
  font-family: var(--font-display);
  font-size: 26px;
  margin: 14px 0 4px;
  color: var(--text-primary, #18181b);
}

.auth-sub {
  font-size: 13px;
  color: var(--text-muted, #a1a1aa);
  margin-bottom: 20px;
}

.mode-tabs {
  display: flex;
  gap: 6px;
  background: var(--bg-secondary, #f4f4f5);
  border-radius: 999px;
  padding: 4px;
  margin-bottom: 22px;
}

.mode-tabs button {
  flex: 1;
  border: none;
  background: transparent;
  border-radius: 999px;
  padding: 7px 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary, #71717a);
  cursor: pointer;
  transition: all 0.2s ease-out;
}

.mode-tabs button.on {
  background: #fff;
  color: #18181b;
  box-shadow: 0 1px 4px rgba(24, 24, 27, 0.1);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field span {
  font-size: 13px;
  color: var(--text-secondary, #52525b);
}

.field input {
  height: 42px;
  border: 1px solid var(--border-color, #e4e4e7);
  border-radius: 10px;
  padding: 0 12px;
  font-size: 14px;
  background: transparent;
  color: var(--text-primary, #18181b);
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.field input:focus {
  border-color: #60a5fa;
  box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.15);
}

.auth-error {
  font-size: 13px;
  color: #ef4444;
}

.auth-submit {
  height: 44px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.2s;
}

.auth-submit:hover:not(:disabled) {
  opacity: 0.92;
  transform: translateY(-1px);
}

.auth-submit:disabled {
  opacity: 0.6;
  cursor: default;
}

.auth-hint {
  margin-top: 18px;
  font-size: 13px;
  color: var(--text-muted, #a1a1aa);
  text-align: center;
}

.switch-link {
  border: none;
  background: none;
  color: #3b82f6;
  font-size: 13px;
  cursor: pointer;
  padding: 0;
}

.switch-link:hover {
  text-decoration: underline;
}
</style>
