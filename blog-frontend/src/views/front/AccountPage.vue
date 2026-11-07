<template>
  <main class="account-page">
    <h1>账号设置</h1><p v-if="error" role="alert">{{ error }}</p>
    <el-form v-if="profile" label-position="top" @submit.prevent="saveProfile">
      <p>用户名：{{ profile.username }} · {{ profile.role === 'admin' ? '管理员' : '用户' }}</p>
      <el-form-item label="昵称"><el-input v-model="profile.nickname" maxlength="50" /></el-form-item>
      <el-form-item label="个人介绍"><el-input v-model="profile.bio" type="textarea" maxlength="500" /></el-form-item>
      <el-button native-type="submit" :loading="busy">保存资料</el-button>
    </el-form>
    <h2>修改密码</h2><p>修改后，所有设备需要重新登录。</p>
    <el-form label-position="top" @submit.prevent="changePassword">
      <el-form-item label="当前密码"><el-input v-model="password.oldPassword" type="password" autocomplete="current-password" /></el-form-item>
      <el-form-item label="新密码（8–64 位）"><el-input v-model="password.newPassword" type="password" autocomplete="new-password" /></el-form-item>
      <el-form-item label="确认新密码"><el-input v-model="confirmation" type="password" autocomplete="new-password" /></el-form-item>
      <el-button native-type="submit" :loading="busy">修改并重新登录</el-button>
    </el-form>
    <h2>验证邮箱与找回密码</h2>
    <p v-if="profile?.emailVerified">已验证：{{ profile.email }}</p>
    <p v-if="!profile?.recoveryEnabled">站点尚未开通安全邮件服务，暂不能绑定邮箱或通过邮件找回密码。</p>
    <el-form v-else @submit.prevent="bindEmail"><el-input v-model="email" type="email" placeholder="用于找回密码的邮箱" /><el-button native-type="submit" :loading="busy">发送验证邮件</el-button></el-form>
  </main>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { accountApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
const router = useRouter(), auth = useAuthStore()
const profile = ref<any>(), error = ref(''), busy = ref(false), email = ref(''), confirmation = ref('')
const password = ref({ oldPassword: '', newPassword: '' })
onMounted(async () => { try { profile.value = (await accountApi.me()).data.data } catch { error.value = '账号信息加载失败，请刷新重试' } })
async function run(action: () => Promise<void>) { if (busy.value) return; busy.value = true; error.value = ''; try { await action() } catch (e: any) { error.value = e.response?.data?.message || e.message || '操作失败' } finally { busy.value = false } }
function saveProfile() { return run(async () => { profile.value = (await accountApi.update(profile.value)).data.data; if (auth.user) { auth.user.nickname = profile.value.nickname; localStorage.setItem('user', JSON.stringify(auth.user)) }; ElMessage.success('资料已保存') }) }
function changePassword() { return run(async () => { if (password.value.newPassword !== confirmation.value) throw new Error('两次输入的新密码不一致'); await accountApi.password(password.value); await auth.logout(); ElMessage.success('密码已修改，请重新登录'); router.push('/login') }) }
function bindEmail() { return run(async () => { await accountApi.email(email.value); ElMessage.success('验证邮件已发送，请在 30 分钟内打开链接') }) }
</script>
<style scoped>.account-page { max-width: 640px; margin: 40px auto; padding: 0 16px 48px; } h2 { margin: 32px 0 12px; } p { margin: 16px 0; } form { margin-top: 16px; }</style>
