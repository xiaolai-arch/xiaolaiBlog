<template>
  <div class="media-manage">
    <div class="page-header">
      <h2>图库管理</h2>
      <div class="header-actions">
        <el-button @click="showCategoryDialog = true">
          <el-icon><Folder /></el-icon> 管理分类
        </el-button>
        <el-button type="primary" @click="openUploadDialog">
            <el-icon><Upload /></el-icon> 上传图片
          </el-button>
      </div>
    </div>

    <!-- Category filter -->
    <div class="category-bar" v-if="categories.length > 0">
      <el-button
        :type="!filterCategoryId ? 'primary' : 'default'"
        size="small"
        @click="filterByCategory(null)"
      >
        全部
      </el-button>
      <el-button
        v-for="cat in categories"
        :key="cat.id"
        :type="filterCategoryId === cat.id ? 'primary' : 'default'"
        size="small"
        @click="filterByCategory(cat.id)"
      >
        {{ cat.name }}
      </el-button>
    </div>

    <!-- Upload dialog -->
    <el-dialog v-model="uploadDialogVisible" title="上传图片" width="460px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="选择文件">
          <div class="custom-upload" @click="triggerFileInput">
            <input
              ref="fileInputRef"
              type="file"
              accept="image/*"
              style="display:none"
              @change="onFileSelected"
            />
            <div v-if="!pendingFile" class="upload-placeholder">
              <el-icon :size="40"><UploadFilled /></el-icon>
              <p>点击选择图片</p>
            </div>
            <div v-else class="upload-preview">
              <img :src="pendingPreview" alt="preview" />
              <p class="upload-filename">{{ pendingFile.name }}</p>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="图片标题">
          <el-input v-model="uploadTitle" placeholder="可选，图片下方显示的标题" />
        </el-form-item>
        <el-form-item label="所属分类">
          <el-select v-model="uploadCategoryId" placeholder="选择分类" clearable style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelUpload">取消</el-button>
        <el-button type="primary" @click="submitUpload" :disabled="!pendingFile" :loading="uploading">
          确认上传
        </el-button>
      </template>
    </el-dialog>

    <!-- Edit dialog -->
    <el-dialog v-model="editDialogVisible" title="编辑图片信息" width="420px">
      <el-form label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="editTitle" placeholder="图片标题" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="editCategoryId" placeholder="选择分类" clearable style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- Category management dialog -->
    <el-dialog v-model="showCategoryDialog" title="管理图库分类" width="500px">
      <div style="margin-bottom: 16px; display: flex; gap: 8px">
        <el-input v-model="newCatName" placeholder="分类名称" style="flex:1" />
        <el-input v-model="newCatSlug" placeholder="slug (选填)" style="flex:1" />
        <el-button type="primary" @click="addCategory">添加</el-button>
      </div>
      <el-table :data="categories" size="small">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="slug" label="Slug" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="editCategory(row)">编辑</el-button>
            <el-button size="small" text type="danger" @click="deleteCategory(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <!-- Edit category inline in dialog -->
      <div v-if="editingCat" style="margin-top: 16px; padding: 12px; background: var(--bg-secondary); border-radius: 8px;">
        <p style="font-size:13px;color:var(--text-muted);margin-bottom:8px">编辑分类: {{ editingCat.name }}</p>
        <div style="display:flex;gap:8px">
          <el-input v-model="editingCat.name" placeholder="名称" style="flex:1" size="small" />
          <el-input v-model="editingCat.slug" placeholder="slug" style="flex:1" size="small" />
          <el-button size="small" type="primary" @click="saveEditCategory">保存</el-button>
          <el-button size="small" @click="editingCat = null">取消</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- Media grid -->
    <el-card v-loading="loading">
      <div v-if="files.length === 0" class="empty">
        <p>暂无图片</p>
        <p class="empty-hint">点击右上角"上传图片"按钮开始添加</p>
      </div>
      <div v-else class="media-grid">
        <div v-for="file in files" :key="file.id" class="media-item">
          <div class="media-preview-wrapper">
            <img :src="file.url" :alt="file.title || file.originalName" class="media-preview" />
          </div>
          <div class="media-info">
            <p class="media-title">{{ file.title || file.originalName }}</p>
            <p class="media-meta" v-if="file.categoryId">
              {{ getCategoryName(file.categoryId) }}
            </p>
            <div class="media-actions">
              <el-button size="small" @click="openEdit(file)">编辑</el-button>
              <el-button size="small" @click="copyUrl(file.url)">复制链接</el-button>
              <el-button size="small" type="danger" @click="remove(file)">删除</el-button>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { mediaApi, mediaCategoryApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

interface MediaFile {
  id: number
  originalName: string
  title: string | null
  url: string
  categoryId: number | null
}

interface MediaCategory {
  id: number
  name: string
  slug: string
  description?: string
}

const files = ref<MediaFile[]>([])
const categories = ref<MediaCategory[]>([])
const loading = ref(false)
const filterCategoryId = ref<number | null>(null)

// Upload dialog
const uploadDialogVisible = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)
const pendingFile = ref<File | null>(null)
const pendingPreview = ref('')
const uploadTitle = ref('')
const uploadCategoryId = ref<number | null>(null)
const uploading = ref(false)

// Edit dialog
const editDialogVisible = ref(false)
const editingFile = ref<MediaFile | null>(null)
const editTitle = ref('')
const editCategoryId = ref<number | null>(null)

// Category dialog
const showCategoryDialog = ref(false)
const newCatName = ref('')
const newCatSlug = ref('')
const editingCat = ref<MediaCategory | null>(null)

onMounted(async () => {
  await loadCategories()
  await load()
})

async function loadCategories() {
  try {
    const res = await mediaCategoryApi.getList()
    if (res.data.code === 200) categories.value = res.data.data || []
  } catch (_) {}
}

async function load() {
  loading.value = true
  try {
    const res = await mediaApi.getList(filterCategoryId.value || undefined)
    if (res.data.code === 200) files.value = res.data.data || []
  } catch (_) {}
  loading.value = false
}

function filterByCategory(catId: number | null) {
  filterCategoryId.value = catId
  load()
}

function getCategoryName(catId: number): string {
  return categories.value.find(c => c.id === catId)?.name || ''
}

// Upload flow
function openUploadDialog() {
  uploadTitle.value = ''
  uploadCategoryId.value = null
  pendingFile.value = null
  pendingPreview.value = ''
  uploadDialogVisible.value = true
}

function triggerFileInput() {
  fileInputRef.value?.click()
}

function onFileSelected(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files && input.files[0]) {
    pendingFile.value = input.files[0]
    pendingPreview.value = URL.createObjectURL(input.files[0])
  }
}

function cancelUpload() {
  uploadDialogVisible.value = false
  if (pendingPreview.value) {
    URL.revokeObjectURL(pendingPreview.value)
  }
  pendingFile.value = null
  pendingPreview.value = ''
}

async function submitUpload() {
  if (!pendingFile.value) return
  uploading.value = true
  try {
    const res = await mediaApi.upload(
      pendingFile.value,
      uploadTitle.value || undefined,
      uploadCategoryId.value || undefined,
    )
    if (res.data.code === 200) {
      ElMessage.success('上传成功')
      uploadDialogVisible.value = false
      if (pendingPreview.value) URL.revokeObjectURL(pendingPreview.value)
      pendingFile.value = null
      pendingPreview.value = ''
      load()
    } else {
      ElMessage.error(res.data.message || '上传失败')
    }
  } catch (_: any) {
    ElMessage.error(_.response?.data?.message || '上传失败')
  }
  uploading.value = false
}

// Edit
function openEdit(file: MediaFile) {
  editingFile.value = file
  editTitle.value = file.title || ''
  editCategoryId.value = file.categoryId
  editDialogVisible.value = true
}

async function saveEdit() {
  if (!editingFile.value) return
  try {
    await mediaApi.update(editingFile.value.id, {
      title: editTitle.value || undefined,
      categoryId: editCategoryId.value || undefined,
    })
    ElMessage.success('已保存')
    editDialogVisible.value = false
    load()
  } catch (_) {
    ElMessage.error('保存失败')
  }
}

function copyUrl(url: string) {
  navigator.clipboard.writeText(url).then(() => ElMessage.success('链接已复制'))
}

function remove(file: MediaFile) {
  ElMessageBox.confirm('确定删除该图片？删除后不可恢复。', '确认删除', { type: 'warning' })
    .then(async () => {
      await mediaApi.delete(file.id)
      ElMessage.success('已删除')
      load()
    })
    .catch(() => {})
}

// Category management
async function addCategory() {
  if (!newCatName.value.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  try {
    await mediaCategoryApi.create({
      name: newCatName.value.trim(),
      slug: newCatSlug.value.trim() || undefined,
    })
    ElMessage.success('分类已添加')
    newCatName.value = ''
    newCatSlug.value = ''
    await loadCategories()
  } catch (_: any) {
    ElMessage.error(_.response?.data?.message || '添加失败')
  }
}

function editCategory(cat: MediaCategory) {
  editingCat.value = { ...cat }
}

async function saveEditCategory() {
  if (!editingCat.value) return
  try {
    await mediaCategoryApi.update(editingCat.value.id, {
      name: editingCat.value.name,
      slug: editingCat.value.slug,
    })
    ElMessage.success('已保存')
    editingCat.value = null
    await loadCategories()
  } catch (_: any) {
    ElMessage.error(_.response?.data?.message || '保存失败')
  }
}

async function deleteCategory(cat: MediaCategory) {
  ElMessageBox.confirm(`确定删除分类"${cat.name}"？`, '确认删除', { type: 'warning' })
    .then(async () => {
      await mediaCategoryApi.delete(cat.id)
      ElMessage.success('已删除')
      await loadCategories()
    })
    .catch(() => {})
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h2 { font-size: 24px; margin: 0; }
.header-actions { display: flex; gap: 10px; }

.category-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.media-item {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  overflow: hidden;
  transition: box-shadow 0.2s;
}
.media-item:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.08); }

.media-preview-wrapper {
  width: 100%;
  padding-top: 75%;
  position: relative;
  overflow: hidden;
  background: var(--bg-secondary);
}
.media-preview {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.media-info { padding: 12px; }
.media-title {
  font-size: 13px;
  font-weight: 500;
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.media-meta {
  font-size: 12px;
  color: var(--text-muted);
  margin: 0 0 8px;
}
.media-actions { display: flex; gap: 6px; flex-wrap: wrap; }

.empty { text-align: center; padding: 80px; color: var(--text-muted); }
.empty p { margin: 0; }
.empty-hint { font-size: 13px; margin-top: 8px; opacity: 0.7; }

/* Custom upload */
.custom-upload {
  cursor: pointer;
  width: 100%;
  border: 2px dashed var(--border-color);
  border-radius: 10px;
  overflow: hidden;
  transition: border-color 0.2s;
}
.custom-upload:hover { border-color: var(--accent-color); }

.upload-placeholder {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-muted);
}
.upload-placeholder p { margin: 8px 0 0; font-size: 14px; }

.upload-preview {
  position: relative;
}
.upload-preview img {
  width: 100%;
  max-height: 260px;
  object-fit: contain;
  display: block;
  background: var(--bg-secondary);
}
.upload-filename {
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
  padding: 10px;
  margin: 0;
}
</style>