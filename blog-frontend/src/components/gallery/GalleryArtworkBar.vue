<template>
  <div class="artwork-bar">
    <!-- 收起态：换画 + 下载 -->
    <div v-if="!open" class="bar-row">
      <button class="bar-pill" @click="open = true">
        <span class="pill-icon">🖼</span> 换一幅画
      </button>
      <button v-if="canDownload" class="bar-pill" @click="emit('download')">
        <span class="pill-icon">↓</span> 下载
      </button>
    </div>

    <!-- 展开态：缩略图画廊 -->
    <div v-else class="picker">
      <div class="picker-head">
        <span class="picker-title">{{ heading }}</span>
        <button class="picker-close" @click="open = false">收起</button>
      </div>
      <div v-if="loading" class="picker-loading">加载中…</div>
      <div v-else class="picker-strip">
        <button
          v-for="art in artworks"
          :key="art.url"
          class="thumb"
          :class="{ active: art.url === currentUrl }"
          :title="art.label"
          @click="pick(art)"
        >
          <img :src="art.url" :alt="art.label" loading="lazy" />
        </button>
      </div>
      <p class="picker-hint">点击任意一幅，挂到当前选中的画框</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { galleryApi } from '@/api'

export type ArtworkOption = { label: string; url: string }

const props = defineProps<{
  focusedId: string
  canDownload: boolean
  openSignal: number
  currentUrl?: string
}>()

const emit = defineEmits<{
  (e: 'download'): void
  (e: 'expanded-change', open: boolean): void
  (e: 'select', selection: ArtworkOption): void
}>()

const open = ref(false)
const loading = ref(false)
const artworks = ref<ArtworkOption[]>([])
const heading = ref('选择画作')

watch(open, (v) => emit('expanded-change', v))

// 外部请求展开（例如点击空白画框）
watch(
  () => props.openSignal,
  () => {
    open.value = true
  },
)

onMounted(loadArtworks)

async function loadArtworks() {
  if (artworks.value.length) return
  loading.value = true
  // 优先用本站后台上传的图片；没有则回退到内置名画藏品
  try {
    const res = await galleryApi.getImages({ page: 1, size: 24 })
    if (res.data.code === 200) {
      const list = res.data.data?.records || res.data.data || []
      if (Array.isArray(list) && list.length) {
        artworks.value = list.map((it: any) => ({
          url: it.url,
          label: it.title || it.originalName || '未命名',
        }))
        heading.value = '从我的图库选择'
        loading.value = false
        return
      }
    }
  } catch (_) {}
  await loadCollection()
  loading.value = false
}

async function loadCollection() {
  try {
    const res = await fetch('/met-art/manifest.json')
    const list = (await res.json()) as Array<{ file: string; title: string; artist: string }>
    artworks.value = list.map((it) => ({
      url: '/met-art/' + it.file,
      label: it.artist ? `${it.title} — ${it.artist}` : it.title,
    }))
    heading.value = '大都会博物馆藏品（公有领域）'
  } catch (_) {
    artworks.value = []
  }
}

function pick(art: ArtworkOption) {
  emit('select', art)
  open.value = false
}
</script>

<style scoped>
.artwork-bar {
  pointer-events: auto;
  display: flex;
  justify-content: center;
}

.bar-row {
  display: flex;
  gap: 10px;
}

.bar-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.16);
  color: #18181b;
  font-size: 13.5px;
  font-weight: 600;
  padding: 11px 20px;
  cursor: pointer;
  transition: transform 0.15s, background 0.15s;
}

.bar-pill:hover {
  transform: translateY(-2px);
  background: #fff;
}

.pill-icon {
  font-size: 15px;
  line-height: 1;
}

/* 展开态 */
.picker {
  width: min(760px, 92vw);
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(12px);
  border-radius: 20px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.22);
  padding: 14px 16px 12px;
}

.picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.picker-title {
  font-size: 12.5px;
  font-weight: 600;
  color: #71717a;
}

.picker-close {
  border: none;
  background: none;
  color: #71717a;
  font-size: 12.5px;
  cursor: pointer;
  padding: 2px 6px;
}

.picker-close:hover {
  color: #18181b;
}

.picker-strip {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 6px;
  scrollbar-width: thin;
}

.thumb {
  flex: none;
  width: 74px;
  height: 74px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 12px;
  overflow: hidden;
  background: #e9e8e4;
  cursor: pointer;
  transition: border-color 0.15s, transform 0.15s;
}

.thumb:hover {
  transform: translateY(-2px);
}

.thumb.active {
  border-color: #18181b;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.picker-hint {
  margin-top: 6px;
  font-size: 11.5px;
  color: #a1a1aa;
  text-align: center;
}

.picker-loading {
  padding: 20px;
  text-align: center;
  font-size: 13px;
  color: #a1a1aa;
}
</style>
