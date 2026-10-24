<template>
  <div class="article-edit-page">
    <h2 class="page-title">{{ isEdit ? '编辑文章' : '新建文章' }}</h2>

    <el-form :model="form" label-position="top" v-loading="loading">
      <el-row :gutter="20">
        <el-col :span="16">
          <el-card>
            <el-form-item label="标题" required>
              <el-input v-model="form.title" placeholder="文章标题" size="large" />
            </el-form-item>

            <el-form-item label="URL 别名">
              <el-input v-model="form.slug" placeholder="URL 标识（留空自动生成）" />
            </el-form-item>

            <el-form-item label="内容（Markdown）">
              <el-input
                v-model="form.contentMd"
                type="textarea"
                :rows="20"
                placeholder="用 Markdown 写作……"
              />
            </el-form-item>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card style="margin-bottom: 16px">
            <template #header>发布</template>
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="0">草稿</el-radio>
                <el-radio :value="1">已发布</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="置顶">
              <el-switch v-model="isTop" />
            </el-form-item>
            <el-button type="primary" @click="save" :loading="saving" style="width: 100%">
              {{ saving ? '保存中…' : '保存' }}
            </el-button>
          </el-card>

          <el-card style="margin-bottom: 16px">
            <template #header>分类</template>
            <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
              <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
            </el-select>
          </el-card>

          <el-card style="margin-bottom: 16px">
            <template #header>标签</template>
            <el-select v-model="form.tagIds" multiple placeholder="选择标签" style="width: 100%">
              <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
            </el-select>
          </el-card>

          <el-card>
            <template #header>封面图片</template>
            <el-input v-model="form.coverUrl" placeholder="封面图片地址" />
            <el-upload
              class="upload-btn"
              :http-request="handleUpload"
              :show-file-list="false"
              accept="image/*"
            >
              <el-button size="small">上传</el-button>
            </el-upload>
          </el-card>
        </el-col>
      </el-row>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articleApi, categoryApi, tagApi, mediaApi } from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)

const form = ref({
  title: '', slug: '', summary: '', contentMd: '',
  coverUrl: '', categoryId: null as number | null,
  status: 0, isTop: 0, seoTitle: '', seoDescription: '',
  tagIds: [] as number[]
})
const isTop = ref(false)
const categories = ref<any[]>([])
const tags = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)

onMounted(async () => {
  await Promise.all([loadCategories(), loadTags()])
  if (isEdit.value) await loadArticle()
})

async function loadCategories() {
  try { const res = await categoryApi.getAdminList(); categories.value = res.data.data || [] } catch (_) {}
}

async function loadTags() {
  try { const res = await tagApi.getAdminList(); tags.value = res.data.data || [] } catch (_) {}
}

async function loadArticle() {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res = await articleApi.getAdminDetail(id)
    if (res.data.code === 200) {
      const a = res.data.data
      form.value = {
        title: a.title, slug: a.slug, summary: a.summary || '',
        contentMd: a.contentMd, coverUrl: a.coverUrl || '',
        categoryId: a.categoryId, status: a.status,
        isTop: a.isTop || 0, seoTitle: a.seoTitle || '',
        seoDescription: a.seoDescription || '',
        tagIds: (a.tags || []).map((t: any) => t.id)
      }
      isTop.value = a.isTop === 1
    }
  } catch (_) { ElMessage.error('文章加载失败') }
  loading.value = false
}

async function save() {
  saving.value = true
  const payload = { ...form.value, isTop: isTop.value ? 1 : 0 }
  try {
    if (isEdit.value) {
      await articleApi.update(Number(route.params.id), payload)
      ElMessage.success('文章已更新')
    } else {
      await articleApi.create(payload)
      ElMessage.success('文章已创建')
    }
    router.push('/admin/articles')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  }
  saving.value = false
}

async function handleUpload(options: any) {
  try {
    const res = await mediaApi.upload(options.file)
    if (res.data.code === 200) {
      form.value.coverUrl = res.data.data.url
      ElMessage.success('上传成功')
    }
  } catch (_) { ElMessage.error('上传失败') }
}
</script>

<style scoped>
.page-title { font-size: 24px; margin-bottom: 20px; }
.upload-btn { margin-top: 8px; }
</style>