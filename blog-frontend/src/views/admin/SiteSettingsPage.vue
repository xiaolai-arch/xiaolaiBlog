<template>
  <div class="settings-page">
    <h2 class="page-title">Site Settings</h2>

    <el-card v-loading="loading">
      <el-form :model="form" label-position="top">
        <el-divider>Basic Info</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Site Title">
              <el-input v-model="form.site_title" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Site Subtitle">
              <el-input v-model="form.site_subtitle" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="Site Description">
          <el-input v-model="form.site_description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="Announcement（侧边栏公告，留空则不显示）">
          <el-input v-model="form.announcement" placeholder="例如：正在学习微服务，欢迎交流！" />
        </el-form-item>

        <el-divider>Copyright & Social</el-divider>
        <el-form-item label="Copyright">
          <el-input v-model="form.copyright" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="GitHub URL">
              <el-input v-model="form.social_github" placeholder="https://github.com/username" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Bilibili URL">
              <el-input v-model="form.social_bilibili" placeholder="https://space.bilibili.com/xxxx" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Email">
              <el-input v-model="form.social_email" placeholder="email@example.com" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider>SEO</el-divider>
        <el-form-item label="SEO Keywords">
          <el-input v-model="form.seo_keywords" />
        </el-form-item>
        <el-form-item label="SEO Description">
          <el-input v-model="form.seo_description" type="textarea" :rows="2" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="save" :loading="saving">
            {{ saving ? 'Saving...' : 'Save Settings' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="password-card">
      <template #header>Change Password</template>
      <el-form :model="pwForm" label-position="top" class="password-form" @submit.prevent="changePassword">
        <el-form-item label="Current Password">
          <el-input v-model="pwForm.oldPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="New Password (at least 8 characters)">
          <el-input v-model="pwForm.newPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="Confirm New Password">
          <el-input v-model="pwForm.confirm" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="changingPw">Update Password</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { siteApi, authApi } from '@/api'
import { ElMessage } from 'element-plus'

const form = ref<Record<string, string>>({})
const loading = ref(false)
const saving = ref(false)
const pwForm = ref({ oldPassword: '', newPassword: '', confirm: '' })
const changingPw = ref(false)

async function changePassword() {
  const { oldPassword, newPassword, confirm } = pwForm.value
  if (!oldPassword || !newPassword) return ElMessage.warning('Please fill in all password fields')
  if (newPassword.length < 8) return ElMessage.warning('New password must be at least 8 characters')
  if (newPassword !== confirm) return ElMessage.warning('The two new passwords do not match')

  changingPw.value = true
  try {
    const res = await authApi.changePassword({ oldPassword, newPassword })
    if (res.data.code === 200) {
      ElMessage.success('Password updated')
      pwForm.value = { oldPassword: '', newPassword: '', confirm: '' }
    } else {
      ElMessage.error(res.data.message || 'Update failed')
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || 'Update failed')
  }
  changingPw.value = false
}

onMounted(() => load())

async function load() {
  loading.value = true
  try {
    const res = await siteApi.getAdminConfig()
    if (res.data.code === 200) form.value = res.data.data || {}
  } catch (_) {}
  loading.value = false
}

async function save() {
  saving.value = true
  try {
    await siteApi.updateConfig(form.value)
    ElMessage.success('Settings saved')
  } catch (_) { ElMessage.error('Save failed') }
  saving.value = false
}
</script>

<style scoped>
.page-title { font-size: 24px; margin-bottom: 20px; }
.password-card { margin-top: 20px; }
.password-form { max-width: 420px; }
</style>