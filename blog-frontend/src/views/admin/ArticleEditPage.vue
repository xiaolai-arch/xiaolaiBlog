<template>
  <div class="article-edit-page">
    <el-alert v-if="draftAvailable" title="发现此账号在本机保存的草稿" type="info" :closable="false"><el-button @click="recoverDraft" class="rounded-full font-medium transition-all duration-300 active:scale-[0.98]">恢复到编辑器</el-button><el-button @click="discardDraft" class="rounded-full font-medium transition-all duration-300 active:scale-[0.98]">丢弃本机草稿</el-button></el-alert>
    <div class="page-header editor-header">
      <div><h2 class="page-title">{{ isEdit ? '编辑文章' : '新建文章' }}</h2><p class="draft-status" role="status">{{ draftStatus }}</p></div>
      <div class="header-actions"><el-button @click="preview = !preview" class="rounded-full font-medium transition-all duration-300 active:scale-[0.98]">{{ preview ? '继续编辑' : '预览' }}</el-button><el-button v-if="isEdit" @click="showHistory" class="rounded-full font-medium transition-all duration-300 active:scale-[0.98]">版本历史</el-button></div>
    </div>

    <el-form :model="form" label-position="top" v-loading="loading">
      <el-row :gutter="20">
        <el-col :xs="24" :lg="15">
          <el-card class="bg-white rounded-2xl shadow-sm">
            <el-form-item label="标题" required>
              <el-input v-model="form.title" placeholder="文章标题" size="large" class="bg-[#f5f5f7] rounded-xl text-[#1d1d1f] focus:outline-none focus:ring-2 focus:ring-[#0071e3]/30 transition-all" />
            </el-form-item>

            <el-form-item label="URL 别名">
              <el-input v-model="form.slug" placeholder="URL 标识（留空自动生成）" class="bg-[#f5f5f7] rounded-xl text-[#1d1d1f] focus:outline-none focus:ring-2 focus:ring-[#0071e3]/30 transition-all" />
            </el-form-item>

            <div v-if="preview" class="markdown-body" v-html="previewHtml"></div>
            <el-form-item v-else label="内容（Markdown）">
              <el-input
                v-model="form.contentMd"
                type="textarea"
                :rows="20"
                placeholder="用 Markdown 写作……"
              class="bg-[#f5f5f7] rounded-xl text-[#1d1d1f] focus:outline-none focus:ring-2 focus:ring-[#0071e3]/30 transition-all" />
            </el-form-item>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="9">
          <el-card style="margin-bottom: 16px" class="bg-white rounded-2xl shadow-sm">
            <template #header>发布</template>
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="0">草稿</el-radio>
                <el-radio :value="1">已发布</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="定时发布（北京时间，留空立即发布）">
              <el-date-picker v-model="form.publishAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" clearable />
            </el-form-item>
            <el-form-item label="系列名称"><el-input v-model="form.seriesName" maxlength="100" class="bg-[#f5f5f7] rounded-xl text-[#1d1d1f] focus:outline-none focus:ring-2 focus:ring-[#0071e3]/30 transition-all" /></el-form-item>
            <el-form-item label="系列顺序"><el-input-number v-model="form.seriesOrder" :min="0" /></el-form-item>
            <el-form-item label="置顶">
              <el-switch v-model="isTop" />
            </el-form-item>
            <el-button type="primary" @click="save" :disabled="!loadSucceeded || loading" :loading="saving" style="width: 100%" class="rounded-full font-medium transition-all duration-300 active:scale-[0.98]">
              {{ saving ? '保存中…' : '保存' }}
            </el-button>
          </el-card>

          <el-card style="margin-bottom: 16px" class="bg-white rounded-2xl shadow-sm">
            <template #header>分类</template>
            <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%" class="bg-[#f5f5f7] rounded-xl text-[#1d1d1f] focus:outline-none focus:ring-2 focus:ring-[#0071e3]/30 transition-all">
              <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
            </el-select>
          </el-card>

          <el-card style="margin-bottom: 16px" class="bg-white rounded-2xl shadow-sm">
            <template #header>标签</template>
            <el-select v-model="form.tagIds" multiple placeholder="选择标签" style="width: 100%" class="bg-[#f5f5f7] rounded-xl text-[#1d1d1f] focus:outline-none focus:ring-2 focus:ring-[#0071e3]/30 transition-all">
              <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
            </el-select>
          </el-card>

          <el-card class="bg-white rounded-2xl shadow-sm">
            <template #header>封面图片</template>
            <el-input v-model="form.coverUrl" placeholder="封面图片地址" class="bg-[#f5f5f7] rounded-xl text-[#1d1d1f] focus:outline-none focus:ring-2 focus:ring-[#0071e3]/30 transition-all" />
            <el-upload
              class="upload-btn"
              :http-request="handleUpload"
              :show-file-list="false"
              accept="image/*"
            >
              <el-button size="small" class="rounded-full font-medium transition-all duration-300 active:scale-[0.98]">上传</el-button>
            </el-upload>
          </el-card>
        </el-col>
      </el-row>
    </el-form>
    <el-dialog v-model="historyOpen" title="版本历史 · 恢复后为草稿" width="90%">
      <el-select v-model="selectedRevision" placeholder="选择历史版本" @change="loadRevision" class="bg-[#f5f5f7] rounded-xl text-[#1d1d1f] focus:outline-none focus:ring-2 focus:ring-[#0071e3]/30 transition-all"><el-option v-for="r in revisions" :key="r.id" :value="r.id" :label="`${r.createTime} · ${r.title}`" /></el-select>
      <div class="revision-compare"><div><h3>当前正文</h3><pre>{{ form.contentMd }}</pre></div><div><h3>历史正文</h3><pre>{{ revision?.content }}</pre></div></div>
      <el-button :disabled="!revision" @click="restoreRevision" class="rounded-full font-medium transition-all duration-300 active:scale-[0.98]">恢复此版本</el-button>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articleApi, categoryApi, tagApi, mediaApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { accountKey } from '@/utils/accountStorage'
import 'github-markdown-css/github-markdown.css'
const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)

const form = ref({
  title: '', slug: '', summary: '', contentMd: '',
  coverUrl: '', categoryId: null as number | null,
  status: 0, isTop: 0, seoTitle: '', seoDescription: '',
  publishAt: null as string | null, seriesName: '', seriesOrder: 0,
  tagIds: [] as number[]
})
const isTop = ref(false)
const categories = ref<any[]>([])
const tags = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const loadSucceeded = ref(!isEdit.value)

onMounted(async () => {
  await Promise.all([loadCategories(), loadTags()])
  if (isEdit.value) await loadArticle()
  draftAvailable.value = !!localStorage.getItem(draftKey.value)
  initialized = true
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
        publishAt: a.publishAt || null, seriesName: a.seriesName || '', seriesOrder: a.seriesOrder || 0,
        title: a.title, slug: a.slug, summary: a.summary || '',
        contentMd: a.contentMd, coverUrl: a.coverUrl || '',
        categoryId: a.categoryId, status: a.status,
        isTop: a.isTop || 0, seoTitle: a.seoTitle || '',
        seoDescription: a.seoDescription || '',
        tagIds: (a.tags || []).map((t: any) => t.id)
      }
      isTop.value = a.isTop === 1
      loadSucceeded.value = true
    }
  } catch (_) { ElMessage.error('文章加载失败') }
  loading.value = false
}

async function save() {
  if (!loadSucceeded.value || loading.value || saving.value) return
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
    localStorage.removeItem(draftKey.value)
    initialized = false
    router.push('/admin/articles')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  }
  saving.value = false
}

const preview = ref(false)
const previewHtml = computed(() => DOMPurify.sanitize(marked.parse(form.value.contentMd, { async: false }) as string))
const draftKey = computed(() => accountKey(`article:draft:${route.params.id || 'new'}`))
const draftAvailable = ref(false)
const draftStatus = ref('修改后自动保存本机草稿')
let initialized = false
watch([form, isTop], () => {
  if (!initialized || draftAvailable.value) return
  try { localStorage.setItem(draftKey.value, JSON.stringify({ form: form.value, isTop: isTop.value })); draftStatus.value = '本机草稿已保存' }
  catch { draftStatus.value = '本机空间不足，请手动保存文章' }
}, { deep: true })
function recoverDraft() {
  try { const draft = JSON.parse(localStorage.getItem(draftKey.value) || '{}'); if (!draft.form || typeof draft.form.contentMd !== 'string') throw new Error(); form.value = { ...form.value, ...draft.form }; isTop.value = !!draft.isTop; draftAvailable.value = false }
  catch { ElMessage.error('草稿无法读取') }
}
function discardDraft() { localStorage.removeItem(draftKey.value); draftAvailable.value = false }
const historyOpen = ref(false), revisions = ref<any[]>([]), selectedRevision = ref<number>(), revision = ref<any>(null)
async function showHistory() {
  try { revisions.value = (await articleApi.revisions(Number(route.params.id))).data.data; revision.value = null; selectedRevision.value = undefined; historyOpen.value = true }
  catch { ElMessage.error('历史加载失败') }
}
let revisionGeneration = 0
async function loadRevision(id: number) {
  const current = ++revisionGeneration; revision.value = null
  try { const res = await articleApi.revision(Number(route.params.id), id); if (current === revisionGeneration) revision.value = res.data.data }
  catch { if (current === revisionGeneration) ElMessage.error('版本加载失败') }
}
async function restoreRevision() {
  try {
    await ElMessageBox.confirm('当前云端内容会保留为历史版本，恢复结果将作为草稿。确定恢复？', '恢复版本', { confirmButtonText: '恢复', cancelButtonText: '取消' })
    await articleApi.restoreRevision(Number(route.params.id), selectedRevision.value!)
    await loadArticle(); historyOpen.value = false; ElMessage.success('已恢复为草稿')
  } catch (e: any) { if (e !== 'cancel' && e !== 'close') ElMessage.error('恢复失败') }
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
.revision-compare { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
@media (max-width: 600px) { .revision-compare { grid-template-columns: 1fr; } }
.revision-compare pre { white-space: pre-wrap; overflow-wrap: anywhere; max-height: 50vh; overflow: auto; }
.markdown-body { padding: 16px; }
.page-title { font-size: 24px; margin-bottom: 20px; }
.upload-btn { margin-top: 8px; }
</style>