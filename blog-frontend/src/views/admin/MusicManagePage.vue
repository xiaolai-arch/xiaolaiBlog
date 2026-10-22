<template>
  <div class="music-manage">
    <div class="page-header">
      <h2>音乐管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="openUploadDialog">
          <el-icon><Upload /></el-icon> 上传音乐
        </el-button>
      </div>
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="首页「我喜欢的音乐」播放卡展示最新上传的一首；支持 mp3 / m4a / aac / flac / wav / ogg，单文件最大 30MB。歌词可直接粘贴 LRC 文本，播放时逐句同步。"
      style="margin-bottom: 16px"
    />

    <!-- 上传对话框 -->
    <el-dialog v-model="uploadVisible" title="上传音乐" width="520px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="音频">
          <div class="audio-pick" @click="audioInput?.click()">
            <input ref="audioInput" type="file" accept=".mp3,.m4a,.aac,.flac,.wav,.ogg" style="display: none" @change="e => onPick(e, 'audio')" />
            <el-icon v-if="!form.audio" :size="26"><UploadFilled /></el-icon>
            <span v-if="!form.audio" class="pick-hint">点击选择音频文件</span>
            <span v-else class="pick-name">{{ form.audio.name }}（{{ fmtSize(form.audio.size) }}）</span>
          </div>
        </el-form-item>
        <el-form-item label="封面">
          <div class="cover-pick" @click="coverInput?.click()">
            <input ref="coverInput" type="file" accept="image/*" style="display: none" @change="e => onPick(e, 'cover')" />
            <img v-if="coverPreview" :src="coverPreview" class="cover-preview" />
            <div v-else class="cover-placeholder">
              <el-icon :size="22"><Picture /></el-icon>
              <span class="pick-hint">选图（可选）</span>
            </div>
          </div>
          <el-button v-if="form.cover" link type="danger" @click="clearCover">移除封面</el-button>
        </el-form-item>
        <el-form-item label="歌名">
          <el-input v-model="form.title" placeholder="必填，如：把回忆拼好给你" maxlength="100" />
        </el-form-item>
        <el-form-item label="歌手">
          <el-input v-model="form.artist" placeholder="可选" maxlength="100" />
        </el-form-item>
        <el-form-item label="专辑">
          <el-input v-model="form.album" placeholder="可选" maxlength="100" />
        </el-form-item>
        <el-form-item label="歌词">
          <el-input
            v-model="form.lyrics"
            type="textarea"
            :rows="8"
            placeholder="可选，粘贴 LRC 歌词文本（[00:27.80]我们之间的回忆 …），首页播放时逐句同步"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">
          {{ uploading ? '上传中…' : '上传' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑信息对话框 -->
    <el-dialog v-model="editVisible" title="编辑歌曲信息" width="520px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="歌名"><el-input v-model="editForm.title" maxlength="100" /></el-form-item>
        <el-form-item label="歌手"><el-input v-model="editForm.artist" maxlength="100" /></el-form-item>
        <el-form-item label="专辑"><el-input v-model="editForm.album" maxlength="100" /></el-form-item>
        <el-form-item label="歌词">
          <el-input v-model="editForm.lyrics" type="textarea" :rows="10" placeholder="LRC 歌词文本" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 列表 -->
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column label="封面" width="72">
        <template #default="{ row }">
          <img v-if="row.coverUrl" :src="row.coverUrl" class="row-cover" />
          <div v-else class="row-cover row-cover-empty"><el-icon><Headset /></el-icon></div>
        </template>
      </el-table-column>
      <el-table-column label="歌曲" min-width="220">
        <template #default="{ row }">
          <div class="song-cell">
            <span class="song-title">{{ row.title }}</span>
            <el-tag v-if="row.id === list[0]?.id" size="small" type="success" effect="light">首页播放中</el-tag>
          </div>
          <span class="song-sub">{{ [row.artist, row.album].filter(Boolean).join(' • ') || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="时长" width="80">
        <template #default="{ row }">{{ row.duration ? fmtTime(row.duration) : '—' }}</template>
      </el-table-column>
      <el-table-column label="大小" width="90">
        <template #default="{ row }">{{ row.fileSize ? fmtSize(row.fileSize) : '—' }}</template>
      </el-table-column>
      <el-table-column label="歌词" width="70">
        <template #default="{ row }">
          <el-tag v-if="row.lyrics" size="small" effect="plain">有</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="上传时间" width="170">
        <template #default="{ row }">{{ row.createTime }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="删除后首页会回退到上一首，确定？" width="240" @confirm="remove(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="还没有上传过音乐" />
      </template>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, UploadFilled, Picture, Headset } from '@element-plus/icons-vue'
import { musicApi } from '@/api'

interface MusicItem {
  id: number
  title: string
  artist?: string
  album?: string
  audioUrl: string
  coverUrl?: string
  lyrics?: string
  duration?: number
  fileSize?: number
  createTime?: string
}

const list = ref<MusicItem[]>([])
const loading = ref(false)

const uploadVisible = ref(false)
const uploading = ref(false)
const coverPreview = ref('')
const audioInput = ref<HTMLInputElement | null>(null)
const coverInput = ref<HTMLInputElement | null>(null)
const form = reactive<{ audio: File | null; cover: File | null; title: string; artist: string; album: string; lyrics: string }>({
  audio: null,
  cover: null,
  title: '',
  artist: '',
  album: '',
  lyrics: '',
})

const editVisible = ref(false)
const saving = ref(false)
const editId = ref(0)
const editForm = reactive({ title: '', artist: '', album: '', lyrics: '' })

async function loadList() {
  loading.value = true
  try {
    const res = await musicApi.getAdminList()
    if (res.data.code === 200) list.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

function openUploadDialog() {
  form.audio = null
  clearCover()
  form.title = ''
  form.artist = ''
  form.album = ''
  form.lyrics = ''
  uploadVisible.value = true
}

function onPick(e: Event, which: 'audio' | 'cover') {
  const files = (e.target as HTMLInputElement).files
  if (!files || !files[0]) return
  const f = files[0]
  if (which === 'audio') {
    form.audio = f
    if (!form.title) form.title = f.name.replace(/\.[^.]+$/, '')
  } else {
    form.cover = f
    coverPreview.value = URL.createObjectURL(f)
  }
  ;(e.target as HTMLInputElement).value = ''
}

function clearCover() {
  form.cover = null
  coverPreview.value = ''
}

async function submitUpload() {
  if (!form.audio) return ElMessage.warning('请先选择音频文件')
  if (!form.title.trim()) return ElMessage.warning('请填写歌名')
  uploading.value = true
  try {
    // 预读音频时长，列表与前台兜底显示用
    const duration = await new Promise<number | undefined>(resolve => {
      const a = document.createElement('audio')
      a.preload = 'metadata'
      a.onloadedmetadata = () => resolve(isFinite(a.duration) ? Math.round(a.duration) : undefined)
      a.onerror = () => resolve(undefined)
      a.src = URL.createObjectURL(form.audio!)
      setTimeout(() => resolve(undefined), 4000)
    })
    const res = await musicApi.upload({
      audio: form.audio,
      cover: form.cover || undefined,
      title: form.title.trim(),
      artist: form.artist.trim() || undefined,
      album: form.album.trim() || undefined,
      lyrics: form.lyrics.trim() || undefined,
      duration,
    })
    if (res.data.code === 200) {
      ElMessage.success('上传成功，首页播放卡已更新')
      uploadVisible.value = false
      await loadList()
    } else {
      ElMessage.error(res.data.message || '上传失败')
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

function openEdit(row: MusicItem) {
  editId.value = row.id
  editForm.title = row.title
  editForm.artist = row.artist || ''
  editForm.album = row.album || ''
  editForm.lyrics = row.lyrics || ''
  editVisible.value = true
}

async function submitEdit() {
  if (!editForm.title.trim()) return ElMessage.warning('歌名不能为空')
  saving.value = true
  try {
    const res = await musicApi.update(editId.value, {
      title: editForm.title.trim(),
      artist: editForm.artist.trim(),
      album: editForm.album.trim(),
      lyrics: editForm.lyrics,
    })
    if (res.data.code === 200) {
      ElMessage.success('已保存')
      editVisible.value = false
      await loadList()
    } else {
      ElMessage.error(res.data.message || '保存失败')
    }
  } finally {
    saving.value = false
  }
}

async function remove(row: MusicItem) {
  const res = await musicApi.delete(row.id)
  if (res.data.code === 200) {
    ElMessage.success('已删除')
    await loadList()
  } else {
    ElMessage.error(res.data.message || '删除失败')
  }
}

function fmtSize(bytes: number) {
  if (bytes >= 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  return Math.round(bytes / 1024) + ' KB'
}

function fmtTime(sec: number) {
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}

onMounted(loadList)
</script>

<style scoped>
.music-manage {
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

.audio-pick {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
  cursor: pointer;
  color: var(--el-text-color-secondary);
  transition: border-color 0.2s;
}

.audio-pick:hover {
  border-color: var(--el-color-primary);
}

.pick-hint {
  font-size: 13px;
}

.pick-name {
  font-size: 13px;
  color: var(--el-color-primary);
  word-break: break-all;
}

.cover-pick {
  width: 96px;
  height: 96px;
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
}

.cover-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.row-cover {
  width: 44px;
  height: 44px;
  border-radius: 50%;
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

.song-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.song-title {
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
}

.song-sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
