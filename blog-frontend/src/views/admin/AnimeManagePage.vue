<template>
  <div class="anime-manage">
    <div class="page-header">
      <h2>番剧管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="openDialog()">
          <el-icon><Plus /></el-icon> 添加番剧
        </el-button>
      </div>
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="首页「我的番剧」小组件按展示顺序最多显示 6 部；封面可选，不上传时用渐变占位 + 番名首字。"
      style="margin-bottom: 16px"
    />

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑番剧' : '添加番剧'" width="520px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="番剧名">
          <el-input v-model="form.title" placeholder="如：葬送的芙莉莲" maxlength="100" />
        </el-form-item>
        <el-form-item label="封面">
          <div class="cover-pick" @click="coverInput?.click()">
            <input ref="coverInput" type="file" accept="image/*" style="display: none" @change="onCoverPicked" />
            <img v-if="coverPreview" :src="coverPreview" class="cover-preview" />
            <div v-else class="cover-placeholder">
              <el-icon :size="22"><Picture /></el-icon>
              <span class="pick-hint">上传封面（可选）</span>
            </div>
          </div>
          <el-button v-if="form.coverUrl" link type="danger" style="margin-left: 10px" @click="removeCover">移除封面</el-button>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button value="watched">看完</el-radio-button>
            <el-radio-button value="watching">在看</el-radio-button>
            <el-radio-button value="wish">想看</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="评分">
          <el-rate v-model="form.rating" :max="10" show-score score-template="{value} / 10" />
        </el-form-item>
        <el-form-item label="一句话">
          <el-input v-model="form.comment" placeholder="感想，显示在封面悬停层（可选）" maxlength="200" />
        </el-form-item>
        <el-form-item label="展示顺序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
          <span class="sort-hint">数字小的排前面，首页最多展示前 6 部</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">{{ saving ? '保存中…' : '保存' }}</el-button>
      </template>
    </el-dialog>

    <!-- 列表 -->
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column label="封面" width="72">
        <template #default="{ row }">
          <img v-if="row.coverUrl" :src="row.coverUrl" class="row-cover" />
          <div v-else class="row-cover row-cover-empty"><el-icon><Film /></el-icon></div>
        </template>
      </el-table-column>
      <el-table-column label="番剧" min-width="200">
        <template #default="{ row }">
          <span class="a-title">{{ row.title }}</span>
          <p v-if="row.comment" class="a-comment">{{ row.comment }}</p>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="TAG_TYPE[row.status] || 'info'" size="small" effect="light">{{ STATUS_LABELS[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="评分" width="110">
        <template #default="{ row }">
          <span v-if="row.rating" class="a-rating">★ {{ row.rating }}/10</span>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="顺序" width="70" prop="sortOrder" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确定删除这部番剧？" width="200" @confirm="remove(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="还没有添加番剧，点右上角「添加番剧」开始" />
      </template>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Picture, Film } from '@element-plus/icons-vue'
import { animeApi, mediaApi } from '@/api'

interface AnimeItem {
  id: number
  title: string
  coverUrl?: string
  status?: string
  rating?: number
  comment?: string
  sortOrder?: number
}

const STATUS_LABELS: Record<string, string> = { watched: '看完', watching: '在看', wish: '想看' }
const TAG_TYPE: Record<string, 'success' | 'primary' | 'warning'> = { watched: 'success', watching: 'primary', wish: 'warning' }

const list = ref<AnimeItem[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editId = ref(0)
const coverInput = ref<HTMLInputElement | null>(null)
const coverPreview = ref('')
const uploadingCover = ref(false)

const form = reactive<{ title: string; coverUrl: string; status: string; rating: number; comment: string; sortOrder: number }>({
  title: '',
  coverUrl: '',
  status: 'watched',
  rating: 0,
  comment: '',
  sortOrder: 0,
})

async function loadList() {
  loading.value = true
  try {
    const res = await animeApi.getAdminList()
    if (res.data.code === 200) list.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

function openDialog(row?: AnimeItem) {
  editId.value = row?.id || 0
  form.title = row?.title || ''
  form.coverUrl = row?.coverUrl || ''
  form.status = row?.status || 'watched'
  form.rating = row?.rating || 0
  form.comment = row?.comment || ''
  form.sortOrder = row?.sortOrder || 0
  coverPreview.value = form.coverUrl
  dialogVisible.value = true
}

async function onCoverPicked(e: Event) {
  const files = (e.target as HTMLInputElement).files
  if (!files || !files[0]) return
  const file = files[0]
  ;(e.target as HTMLInputElement).value = ''
  uploadingCover.value = true
  try {
    const res = await mediaApi.upload(file, file.name)
    if (res.data.code === 200) {
      form.coverUrl = res.data.data.url
      coverPreview.value = res.data.data.url
      ElMessage.success('封面上传成功')
    } else {
      ElMessage.error(res.data.message || '封面上传失败')
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '封面上传失败')
  } finally {
    uploadingCover.value = false
  }
}

function removeCover() {
  form.coverUrl = ''
  coverPreview.value = ''
}

async function submit() {
  if (!form.title.trim()) return ElMessage.warning('请填写番剧名')
  saving.value = true
  try {
    const payload = {
      title: form.title.trim(),
      coverUrl: form.coverUrl || undefined,
      status: form.status,
      rating: form.rating || undefined,
      comment: form.comment.trim() || undefined,
      sortOrder: form.sortOrder,
    }
    const res = editId.value ? await animeApi.update(editId.value, payload) : await animeApi.create(payload)
    if (res.data.code === 200) {
      ElMessage.success(editId.value ? '已保存' : '已添加')
      dialogVisible.value = false
      await loadList()
    } else {
      ElMessage.error(res.data.message || '保存失败')
    }
  } finally {
    saving.value = false
  }
}

async function remove(row: AnimeItem) {
  const res = await animeApi.delete(row.id)
  if (res.data.code === 200) {
    ElMessage.success('已删除')
    await loadList()
  } else {
    ElMessage.error(res.data.message || '删除失败')
  }
}

onMounted(loadList)
</script>

<style scoped>
.anime-manage {
  padding: 0;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.cover-pick {
  width: 96px;
  height: 128px;
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.2s;
}

.cover-pick:hover {
  border-color: var(--el-color-primary);
}

.cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: var(--el-text-color-secondary);
  text-align: center;
}

.pick-hint {
  font-size: 12px;
}

.cover-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sort-hint {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.row-cover {
  width: 44px;
  height: 58px;
  border-radius: 6px;
  object-fit: cover;
  display: block;
}

.row-cover-empty {
  border: 1px solid var(--el-border-color);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
}

.a-title {
  font-weight: 500;
}

.a-comment {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 320px;
}

.a-rating {
  color: #f59e0b;
  font-weight: 600;
}
</style>
