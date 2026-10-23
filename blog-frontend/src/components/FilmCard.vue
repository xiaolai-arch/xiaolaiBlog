<template>
  <div class="film-card">
    <div class="stage">
      <!-- 照片层叠带：主图 + 右侧渐远 -->
      <div v-if="photos.length" class="strip">
        <div
          v-for="(p, i) in photos"
          :key="p.id"
          class="frame"
          :class="'pos-' + posOf(i)"
          v-show="posOf(i) <= 2"
          @click="posOf(i) === 0 ? next(1) : go(i)"
        >
          <img :src="p.url" :alt="p.title || p.originalName" loading="lazy" />
        </div>

        <!-- 左右切换 -->
        <button class="nav prev" aria-label="上一张" @click.stop="next(-1)">‹</button>
        <button class="nav next" aria-label="下一张" @click.stop="next(1)">›</button>
      </div>
      <div v-else class="empty">
        <p>{{ loading ? '加载中…' : '图库还没有照片' }}</p>
        <p class="empty-sub">后台「媒体」上传后展示在这里</p>
      </div>

      <!-- 当前照片标题 -->
      <p class="photo-title">{{ currentTitle }}</p>

      <!-- 刻度轴 -->
      <div class="ticks" role="tablist" aria-label="切换照片">
        <button
          v-for="(p, i) in photos"
          :key="p.id"
          class="tick"
          :class="{ on: i === index }"
          :aria-label="`第 ${i + 1} 张`"
          @click="go(i)"
        ></button>
      </div>
      <p class="photo-date">{{ currentDate }}</p>
    </div>

    <div class="card-caption">
      <p class="caption-title">
        <span>我的图集</span>
        <span class="tp-year"> • {{ photos.length }} 张</span>
      </p>
      <span class="caption-more">Film</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

interface Photo {
  id: number
  url: string
  title?: string
  originalName?: string
  createTime?: string
}

const photos = ref<Photo[]>([])
const loading = ref(true)
const index = ref(0)

const currentTitle = computed(() => {
  const p = photos.value[index.value]
  if (!p) return ''
  return p.title || p.originalName || ''
})

const currentDate = computed(() => {
  const p = photos.value[index.value]
  if (!p?.createTime) return ''
  return p.createTime.slice(0, 10).replace(/-/g, '.')
})

/* i 相对当前张的偏移位置：0=主图，1/2=右侧渐远 */
function posOf(i: number) {
  const n = photos.value.length
  if (!n) return 0
  return (i - index.value + n) % n
}

function go(i: number) {
  if (!photos.value.length) return
  index.value = ((i % photos.value.length) + photos.value.length) % photos.value.length
}

function next(dir: 1 | -1) {
  go(index.value + dir)
}

/* 自动轮播（尊重系统减少动态设置） */
let timer: number | null = null
let reduced = false

function startAuto() {
  if (reduced || photos.value.length < 2) return
  stopAuto()
  timer = window.setInterval(() => next(1), 5000)
}

function stopAuto() {
  if (timer !== null) {
    window.clearInterval(timer)
    timer = null
  }
}

onMounted(async () => {
  reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  try {
    const res = await fetch('/api/v1/gallery/images?page=1&size=8')
    const body = await res.json()
    if (body?.code === 200 && Array.isArray(body.data?.list)) {
      photos.value = body.data.list
    }
  } catch (_) {}
  loading.value = false
  startAuto()
})

onBeforeUnmount(stopAuto)
</script>

<style scoped>
.film-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  animation: filmCardEnter 450ms cubic-bezier(0.25, 0.46, 0.45, 0.94) 300ms both;
}

/* 浅色纸感舞台，与参考站 film 页一致 */
.stage {
  position: relative;
  flex: 1;
  min-height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  padding: 18px 20px 16px;
  border-radius: 26px;
  overflow: hidden;
  isolation: isolate;
  background: linear-gradient(160deg, #fdfdfc, #f7f7f5 60%, #f1f2ef);
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.04),
    0 12px 34px rgba(24, 24, 27, 0.08);
}

[data-theme='dark'] .stage {
  background: linear-gradient(160deg, #232326, #1f1f22 60%, #26262a);
}

/* ============ 照片层叠带 ============ */
.strip {
  position: absolute;
  inset: 0 0 96px 0;
}

.frame {
  position: absolute;
  top: 46%;
  left: 9%;
  width: 50%;
  aspect-ratio: 4 / 3;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(24, 24, 27, 0.18);
  cursor: pointer;
  transition: transform 0.6s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.6s ease, left 0.6s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform, left, opacity;
}

.frame img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  pointer-events: none;
}

/* 主图 */
.pos-0 {
  transform: translateY(-50%);
  z-index: 3;
  opacity: 1;
}

/* 右侧渐远两张 */
.pos-1 {
  left: 64%;
  transform: translateY(-50%) scale(0.78);
  z-index: 2;
  opacity: 0.7;
}

.pos-2 {
  left: 85%;
  transform: translateY(-50%) scale(0.6);
  z-index: 1;
  opacity: 0.38;
}

/* 其余隐藏（保留在 DOM 以复用过渡） */
.frame:not(.pos-0):not(.pos-1):not(.pos-2) {
  opacity: 0;
  z-index: 0;
  pointer-events: none;
  left: 96%;
  transform: translateY(-50%) scale(0.5);
}

/* ============ 切换按钮 ============ */
.nav {
  position: absolute;
  top: 38%;
  z-index: 4;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 1px solid var(--border-color, rgba(24, 24, 27, 0.1));
  background: rgba(255, 255, 255, 0.85);
  color: #3f3f46;
  font-size: 17px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.25s, transform 0.25s;
  backdrop-filter: blur(4px);
}

.nav.prev { left: 14px; }
.nav.next { right: 14px; }

.stage:hover .nav {
  opacity: 1;
}

.nav:hover {
  transform: scale(1.1);
}

/* ============ 标题 / 刻度 / 日期 ============ */
.photo-title {
  position: relative;
  z-index: 3;
  max-width: 86%;
  margin: 0;
  font-size: 13.5px;
  font-weight: 500;
  color: #3f3f46;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

[data-theme='dark'] .photo-title {
  color: #d4d4d8;
}

.ticks {
  position: relative;
  z-index: 3;
  display: flex;
  align-items: flex-end;
  gap: 5px;
  margin-top: 12px;
  height: 22px;
}

.tick {
  width: 3px;
  height: 13px;
  border: none;
  border-radius: 1px;
  background: rgba(24, 24, 27, 0.18);
  cursor: pointer;
  padding: 0;
  transition: height 0.25s, background 0.25s;
}

[data-theme='dark'] .tick {
  background: rgba(255, 255, 255, 0.22);
}

.tick:hover {
  background: rgba(24, 24, 27, 0.45);
}

.tick.on {
  height: 22px;
  background: #18181b;
}

[data-theme='dark'] .tick.on {
  background: #f4f4f5;
}

.photo-date {
  position: relative;
  z-index: 3;
  margin: 6px 0 0;
  font-size: 11.5px;
  color: #a1a1aa;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

/* ============ 空态 ============ */
.empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: var(--text-muted, #a1a1aa);
  font-size: 14px;
  text-align: center;
}

.empty-sub {
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

.film-card:hover .caption-more {
  opacity: 1;
}

@keyframes filmCardEnter {
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
  .film-card {
    animation: none;
  }
  .frame {
    transition: none;
  }
  .nav {
    opacity: 1;
  }
}

/* ============ 移动端 ============ */
@media (max-width: 768px) {
  .stage {
    min-height: 340px;
  }

  .nav {
    opacity: 1;
  }

  .caption-more {
    opacity: 1;
  }
}
</style>
