<template>
  <div class="film-page">
    <!-- 顶栏：返回 + 品名 -->
    <header class="fp-top">
      <router-link to="/" class="fp-back">← 回首页</router-link>
      <span class="fp-brand">Film</span>
    </header>

    <!-- 固定舞台：滚轮/触摸/键盘驱动换片 -->
    <div class="fp-stage">
      <!-- 开场：冲洗底片中 -->
      <div class="fp-develop" :class="{ gone: !atIntro }" aria-hidden="true">
        <span>rolling the negatives…</span>
        <span class="fp-develop-hint">滚动或按 ↓ 开始翻看</span>
      </div>

      <template v-if="photos.length">
        <div class="fp-strip" :class="{ 'fp-dim': atIntro }">
          <div
            v-for="(p, i) in photos"
            :key="p.id"
            class="fp-frame"
            :class="'pos-' + posOf(i)"
            v-show="posOf(i) <= 3"
            @click="posOf(i) === 0 ? step(1) : go(i)"
          >
            <img :src="p.url" :alt="p.title || p.originalName" />
          </div>
        </div>

        <p class="fp-title">{{ currentTitle }}</p>

        <div class="fp-ticks" role="tablist" aria-label="切换照片">
          <button
            v-for="(p, i) in photos"
            :key="p.id"
            class="fp-tick"
            :class="{ on: i === index }"
            :aria-label="`第 ${i + 1} 张`"
            @click="go(i)"
          ></button>
        </div>
        <div class="fp-month">
          <span>{{ currentMonth }}</span>
          <span class="fp-year">{{ currentYear }}</span>
        </div>

        <!-- 左右切换 -->
        <button class="fp-nav prev" aria-label="上一张" :disabled="atIntro" @click="step(-1)">‹</button>
        <button class="fp-nav next" aria-label="下一张" @click="step(1)">›</button>

        <div v-if="index === 0" class="fp-hint">继续滚动翻看照片</div>
        <button v-if="atLast" class="fp-restart" @click="go(0)">↑ 回到第一张</button>
      </template>

      <div v-else class="fp-empty">
        <p>{{ loading ? '加载中…' : '图库还没有照片' }}</p>
        <p class="sub">后台「媒体」上传后展示在这里</p>
      </div>
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
/* -1 = 开场"冲洗底片"屏，0..n-1 = 照片 */
const index = ref(-1)

const atIntro = computed(() => index.value < 0)
const atLast = computed(() => photos.value.length > 0 && index.value === photos.value.length - 1)

const currentTitle = computed(() => photos.value[index.value]?.title || photos.value[index.value]?.originalName || '')

const currentMonth = computed(() => {
  const t = photos.value[index.value]?.createTime
  if (!t) return ''
  return `${parseInt(t.slice(5, 7), 10)} 月`
})

const currentYear = computed(() => photos.value[index.value]?.createTime?.slice(0, 4) || '')

function posOf(i: number) {
  const n = photos.value.length
  if (!n) return 0
  return (i - index.value + n) % n
}

/* ================= 换片（带过渡锁） ================= */
const LOCK_MS = 850
let lockedUntil = 0

function go(i: number) {
  const n = photos.value.length
  if (!n) return
  const target = Math.min(n - 1, Math.max(-1, i))
  if (target !== index.value) {
    index.value = target
    lockedUntil = performance.now() + LOCK_MS
  }
}

function step(dir: 1 | -1) {
  go(index.value + dir)
}

/* ================= 滚轮 / 触摸 / 键盘 ================= */
function onWheel(e: WheelEvent) {
  if (Math.abs(e.deltaY) < 18) return
  e.preventDefault()
  if (performance.now() < lockedUntil) return
  step(e.deltaY > 0 ? 1 : -1)
}

let touchStartY = 0
let touchStartX = 0

function onTouchStart(e: TouchEvent) {
  touchStartY = e.touches[0].clientY
  touchStartX = e.touches[0].clientX
}

function onTouchEnd(e: TouchEvent) {
  const dy = touchStartY - e.changedTouches[0].clientY
  const dx = touchStartX - e.changedTouches[0].clientX
  if (Math.abs(dy) > 55 && Math.abs(dy) > Math.abs(dx)) {
    if (performance.now() >= lockedUntil) step(dy > 0 ? 1 : -1)
  }
}

function onKey(e: KeyboardEvent) {
  const k = e.key
  if (k === 'ArrowDown' || k === 'PageDown' || k === ' ' || k === 'ArrowRight') {
    e.preventDefault()
    if (performance.now() >= lockedUntil) step(1)
  } else if (k === 'ArrowUp' || k === 'PageUp' || k === 'ArrowLeft') {
    e.preventDefault()
    if (performance.now() >= lockedUntil) step(-1)
  } else if (k === 'Home') {
    go(0)
  } else if (k === 'End') {
    go(photos.value.length - 1)
  }
}

onMounted(async () => {
  try {
    const res = await fetch('/api/v1/gallery/images?page=1&size=12')
    const body = await res.json()
    if (body?.code === 200 && Array.isArray(body.data?.list)) {
      photos.value = body.data.list
    }
  } catch (_) {}
  loading.value = false
  window.addEventListener('wheel', onWheel, { passive: false })
  window.addEventListener('touchstart', onTouchStart, { passive: true })
  window.addEventListener('touchend', onTouchEnd, { passive: true })
  window.addEventListener('keydown', onKey)
})

onBeforeUnmount(() => {
  window.removeEventListener('wheel', onWheel)
  window.removeEventListener('touchstart', onTouchStart)
  window.removeEventListener('touchend', onTouchEnd)
  window.removeEventListener('keydown', onKey)
})
</script>

<style scoped>
.film-page {
  position: relative;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  overscroll-behavior: none;
  background: linear-gradient(175deg, #fbfbfa, #f6f6f4 55%, #f2f2ef);
}

[data-theme='dark'] .film-page {
  background: linear-gradient(175deg, #202024, #1d1d21 55%, #232327);
}

/* ============ 顶栏 ============ */
.fp-top {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 36px;
}

.fp-back {
  font-size: 13.5px;
  color: #71717a !important;
  text-decoration: none;
  transition: color 0.2s;
}

.fp-back:hover {
  color: #18181b !important;
}

[data-theme='dark'] .fp-back {
  color: #8d8a96 !important;
}

[data-theme='dark'] .fp-back:hover {
  color: #e4e4e7 !important;
}

.fp-brand {
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #18181b;
}

[data-theme='dark'] .fp-brand {
  color: #e4e4e7;
}

/* ============ 舞台 ============ */
.fp-stage {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

/* 开场 */
.fp-develop {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  z-index: 10;
  transition: opacity 0.7s ease;
}

.fp-develop.gone {
  opacity: 0;
  pointer-events: none;
}

.fp-develop span:first-child {
  font-family: var(--font-display);
  font-size: 17px;
  letter-spacing: 0.1em;
  color: #a1a1aa;
}

.fp-develop-hint {
  font-size: 12px;
  color: #c4c4ca;
  animation: fpFloatY 2.2s ease-in-out infinite;
}

@keyframes fpFloatY {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(6px); }
}

/* ============ 照片层叠带 ============ */
.fp-strip {
  position: absolute;
  inset: 0 0 110px 0;
  transition: opacity 0.7s ease;
}

.fp-strip.fp-dim {
  opacity: 0;
}

.fp-frame {
  position: absolute;
  top: 50%;
  left: 12vw;
  width: 42vw;
  aspect-ratio: 4 / 3;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 24px 60px rgba(24, 24, 27, 0.18);
  cursor: pointer;
  transition:
    transform 0.8s cubic-bezier(0.22, 1, 0.36, 1),
    opacity 0.8s ease,
    left 0.8s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform, left, opacity;
}

.fp-frame img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  pointer-events: none;
}

.pos-0 {
  transform: translateY(-50%);
  z-index: 5;
  opacity: 1;
}

.pos-1 {
  left: 40vw;
  transform: translateY(-50%) scale(0.74);
  z-index: 4;
  opacity: 0.82;
  box-shadow: 0 14px 36px rgba(24, 24, 27, 0.1);
}

.pos-2 {
  left: 57vw;
  transform: translateY(-50%) scale(0.56);
  z-index: 3;
  opacity: 0.5;
  box-shadow: 0 10px 26px rgba(24, 24, 27, 0.07);
}

.pos-3 {
  left: 70vw;
  transform: translateY(-50%) scale(0.42);
  z-index: 2;
  opacity: 0.28;
  box-shadow: 0 6px 18px rgba(24, 24, 27, 0.05);
}

.fp-frame:not(.pos-0):not(.pos-1):not(.pos-2):not(.pos-3) {
  opacity: 0;
  z-index: 1;
  pointer-events: none;
  left: 80vw;
  transform: translateY(-50%) scale(0.32);
}

/* ============ 左右切换 ============ */
.fp-nav {
  position: absolute;
  top: 44%;
  z-index: 8;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: 1px solid rgba(24, 24, 27, 0.1);
  background: rgba(255, 255, 255, 0.85);
  color: #3f3f46;
  font-size: 20px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.25s, transform 0.25s;
  backdrop-filter: blur(4px);
}

[data-theme='dark'] .fp-nav {
  background: rgba(39, 39, 46, 0.85);
  border-color: rgba(255, 255, 255, 0.1);
  color: #d4d4d8;
}

.fp-nav.prev { left: 22px; }
.fp-nav.next { right: 22px; }

.film-page:hover .fp-nav {
  opacity: 1;
}

.fp-nav:hover:not(:disabled) {
  transform: scale(1.1);
}

.fp-nav:disabled {
  opacity: 0.3 !important;
  cursor: default;
}

/* ============ 标题 / 刻度 / 月份 ============ */
.fp-title {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 84px;
  z-index: 6;
  margin: 0;
  padding: 0 32px;
  font-size: 15px;
  font-weight: 500;
  color: #3f3f46;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

[data-theme='dark'] .fp-title {
  color: #d4d4d8;
}

.fp-ticks {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 52px;
  z-index: 6;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 7px;
  height: 26px;
}

.fp-tick {
  width: 2px;
  height: 13px;
  border: none;
  border-radius: 1px;
  background: rgba(24, 24, 27, 0.22);
  cursor: pointer;
  padding: 0;
  transition: height 0.3s cubic-bezier(0.22, 1, 0.36, 1), background 0.3s;
}

[data-theme='dark'] .fp-tick {
  background: rgba(255, 255, 255, 0.25);
}

.fp-tick:hover {
  background: rgba(24, 24, 27, 0.5);
  height: 17px;
}

.fp-tick.on {
  height: 26px;
  background: #18181b;
}

[data-theme='dark'] .fp-tick.on {
  background: #f4f4f5;
}

.fp-month {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 16px;
  z-index: 6;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  font-size: 14px;
  line-height: 1.3;
  color: #52525b;
}

[data-theme='dark'] .fp-month {
  color: #a1a1aa;
}

.fp-year {
  font-size: 12.5px;
  color: #a1a1aa;
}

/* 提示 */
.fp-hint {
  position: absolute;
  left: 50%;
  bottom: 132px;
  z-index: 6;
  transform: translateX(-50%);
  font-size: 12px;
  letter-spacing: 0.06em;
  color: #a1a1aa;
  animation: fpFloat 2.2s ease-in-out infinite;
}

@keyframes fpFloat {
  0%, 100% { transform: translate(-50%, 0); }
  50% { transform: translate(-50%, 6px); }
}

.fp-restart {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: 132px;
  z-index: 6;
  border: none;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(24, 24, 27, 0.08);
  border-radius: 999px;
  padding: 6px 16px;
  font-size: 12.5px;
  color: #3f3f46;
  cursor: pointer;
  backdrop-filter: blur(6px);
}

[data-theme='dark'] .fp-restart {
  background: rgba(39, 39, 46, 0.85);
  border-color: rgba(255, 255, 255, 0.1);
  color: #d4d4d8;
}

.fp-restart:hover {
  transform: translateX(-50%) translateY(-2px);
}

/* ============ 空态 ============ */
.fp-empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #a1a1aa;
  font-size: 15px;
}

.fp-empty .sub {
  font-size: 12.5px;
  opacity: 0.8;
}

@media (prefers-reduced-motion: reduce) {
  .fp-frame {
    transition: none;
  }
  .fp-hint,
  .fp-develop-hint {
    animation: none;
  }
}

/* ============ 移动端 ============ */
@media (max-width: 768px) {
  .fp-top {
    padding: 16px 20px;
  }

  .fp-frame {
    left: 6vw;
    width: 58vw;
  }

  .pos-1 { left: 52vw; }
  .pos-2 { left: 74vw; }
  .pos-3 { left: 88vw; }

  .fp-title {
    bottom: 92px;
    font-size: 13.5px;
  }

  .fp-ticks {
    bottom: 58px;
  }

  .fp-month {
    bottom: 18px;
  }

  .fp-nav {
    opacity: 1;
    width: 34px;
    height: 34px;
  }
}
</style>
