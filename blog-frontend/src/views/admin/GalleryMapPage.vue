<template>
  <div class="gallery-map">
    <div class="page-header">
      <div>
        <h2>画廊映射</h2>
        <p class="page-sub">为 3D 美术馆的 12 个画框指定挂画，按导览顺序排列；未配置的画框沿用自动填充（本站上传优先，其次内置藏品）。</p>
      </div>
      <div class="header-actions">
        <el-button @click="loadAll" :loading="loading">刷新</el-button>
        <el-button type="primary" :loading="saving" @click="saveAll">保存映射</el-button>
      </div>
    </div>

    <div class="frame-grid" v-loading="loading">
      <div v-for="frame in frames" :key="frame.id" class="frame-card" :class="{ filled: Boolean(model[frame.id]) }">
        <div class="frame-thumb">
          <img v-if="model[frame.id]?.imageUrl" :src="model[frame.id]?.imageUrl" :alt="frame.id" />
          <span v-else class="empty-hint">自动填充</span>
        </div>
        <div class="frame-meta">
          <p class="frame-name">{{ frameTitle(frame) }}</p>
          <p class="frame-id">{{ frame.id }}</p>
          <el-select
            v-model="model[frame.id]"
            placeholder="选择媒体库图片"
            size="small"
            filterable
            clearable
            value-key="imageUrl"
            class="frame-select"
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api, { galleryFrameApi } from '@/api'
import { GALLERY_PAINTINGS } from '@/gallery/galleryPaintings'

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

function frameTitle(frame: (typeof frames)[number]) {
  return `${WALL_NAMES[frame.wall]} 第 ${frame.slot + 1} 幅`
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
  aspect-ratio: 4 / 3;
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

.frame-id {
  margin: -4px 0 0;
  font-size: 12px;
  color: var(--text-muted, #a1a1aa);
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
</style>
