<template>
  <div class="gallery-map">
    <div class="page-header">
      <div>
        <h2>画廊映射</h2>
        <p class="page-sub">为 3D 美术馆的 12 个画框指定挂画，按导览顺序排列；未配置的画框沿用自动填充（本站上传优先，其次内置藏品）。选图后可按画框比例裁剪。</p>
      </div>
      <div class="header-actions">
        <el-button @click="loadAll" :loading="loading">刷新</el-button>
        <el-button type="primary" :loading="saving" @click="saveAll">保存映射</el-button>
      </div>
    </div>

    <div class="frame-grid" v-loading="loading">
      <div v-for="frame in frames" :key="frame.id" class="frame-card" :class="{ filled: Boolean(model[frame.id]) }">
        <div class="frame-thumb" :style="{ aspectRatio: frameRatio(frame) }">
          <img v-if="model[frame.id]?.imageUrl" :src="model[frame.id]?.imageUrl" :alt="frame.id" />
          <span v-else class="empty-hint">自动填充</span>
          <button
            v-if="model[frame.id]?.imageUrl"
            class="crop-btn"
            type="button"
            @click="openCrop(frame)"
          >✂ 裁剪</button>
        </div>
        <div class="frame-meta">
          <p class="frame-name">{{ frameTitle(frame) }}<span class="frame-aspect">{{ frame.aspect === 'portrait' ? '3:4' : '3:2' }}</span></p>
          <p class="frame-id">{{ frame.id }}<span v-if="model[frame.id]?.label" class="frame-label"> · {{ model[frame.id]?.label }}</span></p>
          <el-select
            :model-value="model[frame.id]"
            placeholder="选择媒体库图片"
            size="small"
            filterable
            clearable
            value-key="imageUrl"
            class="frame-select"
            @update:model-value="(v: FrameValue | null) => onPick(frame, v)"
          >
            <el-option
              v-for="m in mediaList"
              :key="m.id"
              :value="{ mediaId: m.id, imageUrl: m.url, label: m.title || m.originalName || '未命名' }"
              :label="m.title || m.originalName || '未命名'"
            >
              <div class="media-option">
                <img :src="m.url" :alt="m.title || undefined" />
                <span class="media-option-name">{{ m.title || m.originalName || '未命名' }}</span>
              </div>
            </el-option>
          </el-select>
        </div>
      </div>
    </div>

    <!-- 裁剪对话框：取景框比例锁定为画框比例 -->
    <el-dialog
      v-model="cropOpen"
      :title="`裁剪挂画 — ${cropFrame ? frameTitle(cropFrame) : ''}`"
      width="460px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-if="cropOpen && cropFrame" class="crop-body">
        <ImageCropper
          ref="cropperRef"
          :src="cropSourceUrl"
          :ratio="frameRatioNum(cropFrame)"
          @crop="onCropDone"
        />
      </div>
      <template #footer>
        <el-button @click="useOriginal">使用原图</el-button>
        <el-button type="primary" :loading="cropping" @click="confirmCrop">确认裁剪</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api, { galleryFrameApi } from '@/api'
import { GALLERY_PAINTINGS } from '@/gallery/galleryPaintings'
import ImageCropper from '@/components/ImageCropper.vue'

interface MediaItem {
  id: number
  url: string
  title?: string | null
  originalName?: string | null
}

interface FrameValue {
  mediaId?: number
  imageUrl: string
  label?: string
}

const WALL_NAMES: Record<string, string> = {
  left: '左墙',
  back: '后墙',
  right: '右墙',
  front: '入口墙',
}

const frames = GALLERY_PAINTINGS
const mediaList = ref<MediaItem[]>([])
const model = reactive<Record<string, FrameValue | null | undefined>>({})
const loading = ref(false)
const saving = ref(false)

/* ================= 裁剪流程 ================= */
const cropOpen = ref(false)
const cropping = ref(false)
const cropFrame = ref<(typeof frames)[number] | null>(null)
const cropSourceUrl = ref('')
const cropPendingValue = ref<FrameValue | null>(null)
const cropperRef = ref<InstanceType<typeof ImageCropper> | null>(null)

function frameTitle(frame: (typeof frames)[number]) {
  return `${WALL_NAMES[frame.wall]} 第 ${frame.slot + 1} 幅`
}

/* 画框宽高比（w/h）：竖幅 3:4，横幅 3:2 */
function frameRatioNum(frame: (typeof frames)[number]) {
  return frame.aspect === 'portrait' ? 3 / 4 : 3 / 2
}

function frameRatio(frame: (typeof frames)[number]) {
  return String(frameRatioNum(frame))
}

function onPick(frame: (typeof frames)[number], value: FrameValue | null) {
  model[frame.id] = value
  // 选了新图 → 直接进入裁剪（比例已按该画框锁定）；清空则不动
  if (value?.imageUrl) openCrop(frame)
}

function openCrop(frame: (typeof frames)[number]) {
  const current = model[frame.id]
  if (!current?.imageUrl) return
  cropFrame.value = frame
  cropSourceUrl.value = current.imageUrl
  cropPendingValue.value = current
  cropOpen.value = true
}

function useOriginal() {
  cropOpen.value = false
}

async function confirmCrop() {
  if (!cropFrame.value || !cropperRef.value) return
  cropping.value = true
  try {
    await cropperRef.value.exportCrop()
  } finally {
    cropping.value = false
  }
}

async function onCropDone(blob: Blob) {
  const frame = cropFrame.value
  if (!frame) return
  const base = cropPendingValue.value?.label || '画框挂画'
  const file = new File([blob], `frame-${frame.id}.jpg`, { type: 'image/jpeg' })
  try {
    const res = await api.post('/admin/gallery-frames/upload', toFormData(file), {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    if (res.data.code === 200 && res.data.data) {
      model[frame.id] = {
        imageUrl: res.data.data,
        label: `${base}（裁剪）`,
      }
      ElMessage.success('已裁剪，记得保存映射')
      cropOpen.value = false
    } else {
      ElMessage.error(res.data.message || '裁剪上传失败')
    }
  } catch (_) {
    ElMessage.error('裁剪上传失败')
  }
}

function toFormData(file: File) {
  const fd = new FormData()
  fd.append('file', file)
  return fd
}

async function loadAll() {
  loading.value = true
  try {
    const [mediaRes, frameRes] = await Promise.all([
      api.get('/admin/files'),
      galleryFrameApi.adminList(),
    ])
    if (mediaRes.data.code === 200 && Array.isArray(mediaRes.data.data)) {
      mediaList.value = mediaRes.data.data
    }
    if (frameRes.data.code === 200 && Array.isArray(frameRes.data.data)) {
      for (const f of frameRes.data.data as Array<{ frameId: string; imageUrl: string; label?: string | null }>) {
        model[f.frameId] = { imageUrl: f.imageUrl, label: f.label ?? undefined }
      }
    }
  } catch (_) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

async function saveAll() {
  saving.value = true
  try {
    const list = frames
      .map(f => ({ frame: f, value: model[f.id] }))
      .filter((it): it is { frame: (typeof frames)[number]; value: FrameValue } =>
        Boolean(it.value && typeof it.value === 'object' && it.value.imageUrl))
      .map(it => ({ frameId: it.frame.id, imageUrl: it.value.imageUrl, label: it.value.label ?? null }))
    const res = await galleryFrameApi.adminSave(list)
    if (res.data.code === 200) {
      ElMessage.success('已保存，前台 3D 美术馆立即生效')
    } else {
      ElMessage.error(res.data.message || '保存失败')
    }
  } catch (_) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.gallery-map {
  max-width: 1200px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header h2 { font-size: 24px; margin: 0 0 6px; }

.page-sub {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary, #71717a);
  max-width: 560px;
}

.frame-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.frame-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  border: 1px solid var(--border-color, #e4e4e7);
  border-radius: 14px;
  background: var(--card-bg, #fff);
}

.frame-card.filled {
  border-color: #86efac;
}

.frame-thumb {
  position: relative;
  border-radius: 10px;
  overflow: hidden;
  background: var(--bg-secondary, #f4f4f5);
  display: grid;
  place-items: center;
}

.frame-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.empty-hint {
  font-size: 12px;
  color: var(--text-muted, #a1a1aa);
}

.crop-btn {
  position: absolute;
  right: 8px;
  bottom: 8px;
  padding: 4px 10px;
  border: none;
  border-radius: 999px;
  background: rgba(24, 24, 27, 0.72);
  color: #fff;
  font-size: 12px;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
}

.frame-thumb:hover .crop-btn {
  opacity: 1;
}

.crop-btn:hover {
  background: rgba(24, 24, 27, 0.9);
}

.frame-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.frame-name {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary, #18181b);
}

.frame-aspect {
  margin-left: 8px;
  font-size: 11px;
  font-weight: 500;
  color: #a1a1aa;
}

.frame-id {
  margin: -4px 0 0;
  font-size: 12px;
  color: var(--text-muted, #a1a1aa);
}

.frame-label {
  color: #86efac;
}

.frame-select {
  width: 100%;
}

.media-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.media-option img {
  width: 28px;
  height: 28px;
  object-fit: cover;
  border-radius: 6px;
}

.media-option-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.crop-body {
  display: flex;
  justify-content: center;
}
</style>
