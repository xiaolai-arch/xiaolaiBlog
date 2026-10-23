<template>
  <div class="anime-card">
    <div class="wall">
      <div v-if="!loading && !list.length" class="empty">
        <p>还没有收藏的番剧</p>
        <p class="empty-sub">后台「番剧」里添加后展示在这里</p>
      </div>
      <div
        v-for="(a, i) in displayList"
        :key="a.id ?? i"
        class="cell"
        :class="'st-' + a.status"
        :style="a.coverUrl ? {} : { background: placeholderGrad(i) }"
      >
        <img v-if="a.coverUrl" class="cover" :src="a.coverUrl" :alt="a.title" loading="lazy" />
        <span v-else class="cover-fallback">{{ a.title.slice(0, 1) }}</span>

        <span class="badge">{{ statusLabel(a.status) }}</span>
        <span v-if="a.rating" class="rating">★ {{ a.rating }}</span>

        <!-- 悬停信息层 -->
        <div class="veil">
          <p class="v-title">{{ a.title }}</p>
          <p v-if="a.comment" class="v-comment">{{ a.comment }}</p>
        </div>
      </div>
    </div>

    <div class="card-caption">
      <p class="caption-title">
        <span>我的番剧</span>
        <span class="tp-year"> • {{ list.length }} 部</span>
      </p>
      <span class="caption-more">Anime</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

interface AnimeItem {
  id?: number
  title: string
  coverUrl?: string
  status?: string
  rating?: number
  comment?: string
}

const list = ref<AnimeItem[]>([])
const loading = ref(true)

/* 首页小组件最多展示 6 部，2 行 3 列 */
const displayList = computed(() => list.value.slice(0, 6))

const STATUS_LABELS: Record<string, string> = {
  watched: '看完',
  watching: '在看',
  wish: '想看',
}

function statusLabel(s?: string) {
  return STATUS_LABELS[s || 'watched'] || '看完'
}

/* 无封面时的渐变占位（按索引循环取色） */
const GRADS = [
  'linear-gradient(150deg, #c7d2fe, #e0e7ff 55%, #fbcfe8)',
  'linear-gradient(150deg, #bae6fd, #cffafe 55%, #ddd6fe)',
  'linear-gradient(150deg, #fde68a, #fed7aa 55%, #fbcfe8)',
  'linear-gradient(150deg, #bbf7d0, #d9f99d 55%, #a5f3fc)',
  'linear-gradient(150deg, #fbcfe8, #e9d5ff 55%, #c7d2fe)',
  'linear-gradient(150deg, #fecaca, #fde68a 55%, #bbf7d0)',
]

function placeholderGrad(i: number) {
  return GRADS[i % GRADS.length]
}

onMounted(async () => {
  try {
    const res = await fetch('/api/v1/anime/list')
    const body = await res.json()
    if (body?.code === 200 && Array.isArray(body.data)) {
      list.value = body.data
    }
  } catch (_) {}
  loading.value = false
})
</script>

<style scoped>
.anime-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  animation: animeCardEnter 450ms cubic-bezier(0.25, 0.46, 0.45, 0.94) 300ms both;
}

/* 封面墙：2 行 3 列，占满媒体区 */
.wall {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(2, 1fr);
  gap: 8px;
  flex: 1;
  min-height: 300px;
  border-radius: 26px;
  overflow: hidden;
  isolation: isolate;
}

.cell {
  position: relative;
  border-radius: 14px;
  overflow: hidden;
  min-height: 0;
  box-shadow: 0 2px 10px rgba(24, 24, 27, 0.08);
}

.cover {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.45s cubic-bezier(0.22, 1, 0.36, 1);
}

.cell:hover .cover {
  transform: scale(1.06);
}

/* 无封面：渐变占位 + 首字 */
.cover-fallback {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 700;
  color: rgba(24, 24, 27, 0.42);
}

/* 状态角标 */
.badge {
  position: absolute;
  left: 7px;
  bottom: 7px;
  z-index: 2;
  padding: 2px 8px 1.5px;
  border-radius: 999px;
  font-size: 10.5px;
  font-weight: 500;
  letter-spacing: 0.02em;
  color: #fff;
  background: rgba(24, 24, 27, 0.55);
  backdrop-filter: blur(6px);
}

.st-watching .badge {
  background: rgba(59, 130, 246, 0.75);
}

.st-wish .badge {
  background: rgba(234, 88, 12, 0.72);
}

/* 评分 */
.rating {
  position: absolute;
  right: 7px;
  top: 7px;
  z-index: 2;
  padding: 1.5px 7px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  color: #f59e0b;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 1px 4px rgba(24, 24, 27, 0.12);
}

/* 悬停信息层 */
.veil {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 3px;
  padding: 10px;
  background: linear-gradient(180deg, transparent 30%, rgba(24, 24, 27, 0.82));
  opacity: 0;
  transition: opacity 0.3s ease-out;
}

.cell:hover .veil {
  opacity: 1;
}

.v-title {
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.v-comment {
  font-size: 11px;
  line-height: 1.45;
  color: rgba(255, 255, 255, 0.82);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 空态 */
.empty {
  grid-column: 1 / -1;
  align-self: center;
  justify-self: center;
  text-align: center;
  color: var(--text-muted, #a1a1aa);
  font-size: 14px;
}

.empty-sub {
  margin-top: 4px;
  font-size: 12px;
  opacity: 0.8;
}

/* ============ 说明行 ============ */
.card-caption {
  display: flex;
  align-items: flex-start;
  width: 100%;
  padding: 0 13px;
  margin: -6px 0 2px;
  font-size: 16px;
  line-height: 1.375;
  letter-spacing: 0.005em;
}

.caption-title {
  color: #18181b;
  font-weight: 500;
}

[data-theme='dark'] .caption-title {
  color: var(--text-primary);
}

.tp-year {
  color: #a1a1aa;
}

.caption-more {
  margin-left: auto;
  color: #60a5fa;
  opacity: 0;
  transition: opacity 0.3s ease-out;
}

.anime-card:hover .caption-more {
  opacity: 1;
}

@keyframes animeCardEnter {
  from {
    opacity: 0;
    transform: translate3d(0, 12px, 0);
  }
  to {
    opacity: 1;
    transform: translate3d(0, 0, 0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .anime-card {
    animation: none;
  }
  .cover {
    transition: none;
  }
  .veil {
    transition: none;
  }
}

/* 移动端：6 部全展示，格子矮一点 */
@media (max-width: 768px) {
  .wall {
    grid-template-rows: repeat(2, minmax(130px, auto));
    height: auto;
  }

  .veil {
    opacity: 1;
  }

  .caption-more {
    opacity: 1;
  }
}
</style>
