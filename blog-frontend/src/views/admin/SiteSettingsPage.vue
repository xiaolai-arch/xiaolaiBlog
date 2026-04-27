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

        <el-divider>Copyright & Social</el-divider>
        <el-form-item label="Copyright">
          <el-input v-model="form.copyright" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="GitHub URL">
              <el-input v-model="form.social_github" placeholder="https://github.com/username" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { siteApi } from '@/api'
import { ElMessage } from 'element-plus'

const form = ref<Record<string, string>>({})
const loading = ref(false)
const saving = ref(false)

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
</style>