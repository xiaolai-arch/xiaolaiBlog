<template>
  <div class="film-page">
    <!-- 顶栏：返回 + 品名 -->
    <header class="fp-top">
      <router-link to="/" class="fp-back">← 回首页</router-link>
      <span class="fp-brand">Film</span>
    </header>

    <!-- 照片带（scrub 渲染循环直接写样式） -->
    <div class="fp-strip" ref="stripEl">
      <button
        v-for="(ph, i) in photos"
        :key="ph.id"
        :ref="el => setFrame(i, el as HTMLElement | null)"
        class="fp-frame"
        :aria-label="`查看 ${ph.title || ph.originalName || '照片'}`"
        @click="onFrameClick(i)"
      >
        <img :src="ph.url" :alt="ph.title || ph.originalName" draggable="false" />
      </button>
    </div>

    <!-- 暗房状态文案：加载时与每次切换过渡中浮现 -->
    <div class="fp-status" ref="statusEl" aria-hidden="true">{{ statusText }}</div>

    <!-- 文字层：标题 / 刻度 / 月份 -->
    <div class="fp-text" ref="textEl" v-if="photos.length">
      <p v-if="currentTitle" class="fp-title">{{ currentTitle }}</p>
      <div class="fp-ticks" role="tablist" aria-label="切换照片" @mouseleave="hoverTick = -1">
        <button
          v-for="(ph, i) in photos"
          :key="ph.id"
          class="fp-tick"
          :class="{ on: i === idx }"
          :aria-label="`第 ${i + 1} 张`"
          @click="tweenTo(i)"
          @mouseenter="hoverTick = i"
        >
          <span :ref="el => setTick(i, el as HTMLElement | null)"></span>
        </button>
      </div>
      <p class="fp-month">{{ currentMonth }}</p>
      <p class="fp-year">{{ currentYear }}</p>
    </div>

    <!-- 空态（加载中由暗房文案承担） -->
    <div v-if="!photos.length && !loading" class="fp-empty">
      <p>图库还没有照片</p>
      <p class="sub">后台「媒体」上传后展示在这里</p>
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
  aspect: number
}

const photos = ref<Photo[]>([])
const loading = ref(true)
/* 连续进度：0..n-1 = 照片（可有小数，scrub 插值），第一屏直接展示照片 */
const progress = ref(0)
const idx = computed(() => Math.round(progress.value))

/* 暗房文案：加载与切换过渡时轮换（取自参考站） */
const DARKROOM = [
  'rolling the negatives…',
  'mixing the chemicals…',
  'checking the light meter…',
  'hanging prints to dry…',
  'developing photos…',
  'film reel loading…',
]
const statusText = ref(DARKROOM[0])
let phraseIdx = 0
let inTransition = false

/* 标题仅在后台填了图片标题时展示，不回退显示文件名 */
const currentTitle = computed(() => photos.value[idx.value]?.title?.trim() || '')
const currentMonth = computed(() => {
  const t = photos.value[idx.value]?.createTime
  return t ? `${parseInt(t.slice(5, 7), 10)} 月` : ''
})
const currentYear = computed(() => photos.value[idx.value]?.createTime?.slice(0, 4) || '')

/* ================= DOM refs（渲染循环直写样式） ================= */
const stripEl = ref<HTMLElement | null>(null)
const statusEl = ref<HTMLElement | null>(null)
const textEl = ref<HTMLElement | null>(null)
const frameEls: Array<HTMLElement | null> = []
function setFrame(i: number, el: HTMLElement | null) {
  frameEls[i] = el
}

/* ================= 刻度条（复刻 film-linemark-scale.ts） =================
   高度 = 基础28 + 二次衰减增益8（|d|≤6），当前张×2，悬停附近4根再+14 二次衰减；逐帧 lerp 平滑 */
const hoverTick = ref(-1)
const tickSpans: Array<HTMLElement | null> = []
const tickHeights: number[] = []
function setTick(i: number, el: HTMLElement | null) {
  tickSpans[i] = el
}
let lastProgress = 0

const LINEMARK_MIN = 28
const LINEMARK_INTENSITY = 8
const LINEMARK_LIMIT = 6
const LINEMARK_SELECTED_MULT = 2
const LINEMARK_HOVER_BONUS = 14
const LINEMARK_HOVER_LIMIT = 4
const LINEMARK_IDLE_LERP = 0.14

function linemarkTarget(d: number): number {
  if (d > LINEMARK_LIMIT) return LINEMARK_MIN
  const nd = 1 - d / LINEMARK_LIMIT
  return LINEMARK_MIN + LINEMARK_INTENSITY * nd * nd
}

function layoutTicks(p: number, reduced: boolean) {
  const sel = Math.round(p)
  const vel = Math.abs(p - lastProgress)
  lastProgress = p
  const t = reduced ? 1 : Math.max(LINEMARK_IDLE_LERP, Math.min(1, vel * 55))
  for (let i = 0; i < photos.value.length; i++) {
    const el = tickSpans[i]
    if (!el) continue
    let target = linemarkTarget(Math.abs(i - p))
    if (i === sel) target *= LINEMARK_SELECTED_MULT
    if (hoverTick.value >= 0) {
      const hd = Math.abs(i - hoverTick.value)
      if (hd <= LINEMARK_HOVER_LIMIT) {
        const nd = 1 - hd / LINEMARK_HOVER_LIMIT
        target += LINEMARK_HOVER_BONUS * nd * nd
      }
    }
    if (tickHeights[i] === undefined) tickHeights[i] = LINEMARK_MIN
    tickHeights[i] += (target - tickHeights[i]) * t
    el.style.height = tickHeights[i].toFixed(2) + 'px'
  }
}

/* ================= 几何模型（量自参考站 1440 宽） =================
   宽度曲线：焦点 480(=vw/3) → 邻位 293 → 76 → 72；透明度 1 → .771 → .505 → .5
   照片等差横排 4px 间隙，焦点左缘固定 vw/3，垂直中线 46% */
const W_PROFILE: Array<[number, number]> = [[0, 480], [1, 293], [2, 76], [3, 72]]
const O_PROFILE: Array<[number, number]> = [[0, 1], [1, 0.771], [2, 0.505], [3, 0.5]]

function profileValue(profile: Array<[number, number]>, d: number): number {
  const x = Math.abs(d)
  if (x >= profile[profile.length - 1][0]) return profile[profile.length - 1][1]
  for (let k = 0; k < profile.length - 1; k++) {
    const [x0, v0] = profile[k]
    const [x1, v1] = profile[k + 1]
    if (x >= x0 && x <= x1) return v0 + ((v1 - v0) * (x - x0)) / (x1 - x0)
  }
  return profile[profile.length - 1][1]
}

/* ================= scrub 渲染循环 ================= */
let rafId: number | null = null

function layout() {
  const n = photos.value.length
  const vw = window.innerWidth
  const vh = window.innerHeight
  const scale = vw / 1440
  const gap = 4 * scale
  const focusLeft = vw / 3
  const midline = vh * 0.46
  const p = progress.value

  if (!n) return

  // 各照片宽度与累计 left
  const widths: number[] = []
  const lefts: number[] = []
  let acc = 0
  for (let i = 0; i < n; i++) {
    const w = profileValue(W_PROFILE, i - p) * scale
    widths.push(w)
    lefts.push(acc)
    acc += w + gap
  }

  // 轨道平移：把"虚拟焦点位置"（floor 与 ceil 照片 left 的插值）对齐到 vw/3
  const i0 = Math.max(0, Math.min(n - 1, Math.floor(p)))
  const i1 = Math.max(0, Math.min(n - 1, Math.ceil(p)))
  const frac = Math.abs(p - Math.floor(p))
  const virtualFocus = lefts[i0] + (lefts[i1] - lefts[i0]) * frac
  const trackOffset = focusLeft - virtualFocus

  for (let i = 0; i < n; i++) {
    const el = frameEls[i]
    if (!el) continue
    const d = Math.abs(i - p)
    if (d > 6) {
      el.style.visibility = 'hidden'
      continue
    }
    el.style.visibility = 'visible'
    const w = widths[i]
    const h = w / (photos.value[i].aspect || 1.5)
    el.style.width = w + 'px'
    el.style.height = h + 'px'
    el.style.transform = `translate3d(${lefts[i] + trackOffset}px, ${midline - h / 2}px, 0)`
    el.style.opacity = String(profileValue(O_PROFILE, i - p))
    el.style.zIndex = String(100 - Math.round(d * 10))
  }

  // 暗房文案：加载中常显；切换过渡按偏离程度淡入淡出，并轮换短语
  if (statusEl.value) {
    const dev = Math.abs(p - Math.round(p))
    const op = loading.value ? 1 : Math.max(0, Math.min(1, dev * 3.4 - 0.12))
    statusEl.value.style.opacity = String(op)
    if (dev > 0.22 && !inTransition) {
      inTransition = true
      phraseIdx = (phraseIdx + 1) % DARKROOM.length
      statusText.value = DARKROOM[phraseIdx]
    } else if (dev < 0.04) {
      inTransition = false
    }
  }

  // 文字层：过渡中淡出
  if (textEl.value) {
    const settled = 1 - Math.min(1, Math.abs(p - Math.round(p)) * 2.4)
    textEl.value.style.opacity = String(Math.max(0, Math.min(1, settled)))
  }
}

function frameLoop() {
  rafId = requestAnimationFrame(frameLoop)
  if (tweenTarget !== null) {
    const t = Math.min(1, (performance.now() - tweenStart) / 450)
    const e = 1 - Math.pow(1 - t, 3)
    progress.value = tweenFrom + (tweenTarget - tweenFrom) * e
    if (t >= 1) tweenTarget = null
  }
  layout()
  layoutTicks(progress.value, reducedMotion)
}

let reducedMotion = false

/* ================= 交互：滚轮 / 触摸 / 键盘 / 点击 ================= */
function clampP(v: number) {
  return Math.max(0, Math.min(photos.value.length - 1, v))
}

let tweenTarget: number | null = null
let tweenFrom = 0
let tweenStart = 0
let snapTimer: number | null = null

function tweenTo(t: number) {
  tweenTarget = clampP(t)
  tweenFrom = progress.value
  tweenStart = performance.now()
}

function scheduleSnap() {
  if (snapTimer !== null) window.clearTimeout(snapTimer)
  snapTimer = window.setTimeout(() => tweenTo(Math.round(progress.value)), 260)
}

function onWheel(e: WheelEvent) {
  e.preventDefault()
  if (Math.abs(e.deltaY) < 2) return
  tweenTarget = null
  progress.value = clampP(progress.value + e.deltaY / 890)
  scheduleSnap()
}

let touchY = 0
let touchX = 0
function onTouchStart(e: TouchEvent) {
  touchY = e.touches[0].clientY
  touchX = e.touches[0].clientX
  tweenTarget = null
}
function onTouchMove(e: TouchEvent) {
  const dy = touchY - e.touches[0].clientY
  const dx = touchX - e.touches[0].clientX
  if (Math.abs(dy) > Math.abs(dx)) {
    progress.value = clampP(progress.value + dy / 890)
  }
}
function onTouchEnd() {
  scheduleSnap()
}

function onKey(e: KeyboardEvent) {
  const k = e.key
  if (k === 'ArrowDown' || k === 'PageDown' || k === ' ' || k === 'ArrowRight') {
    e.preventDefault()
    tweenTo(Math.round(progress.value) + 1)
  } else if (k === 'ArrowUp' || k === 'PageUp' || k === 'ArrowLeft') {
    e.preventDefault()
    tweenTo(Math.round(progress.value) - 1)
  } else if (k === 'Home') {
    tweenTo(0)
  } else if (k === 'End') {
    tweenTo(photos.value.length - 1)
  }
}

function onFrameClick(i: number) {
  if (i === idx.value && Math.abs(progress.value - i) < 0.2) tweenTo(i + 1)
  else tweenTo(i)
}

onMounted(async () => {
  reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  try {
    const res = await fetch('/api/v1/gallery/images?page=1&size=24')
    const body = await res.json()
    if (body?.code === 200 && Array.isArray(body.data?.list)) {
      photos.value = body.data.list.map((p: Photo) => ({ ...p, aspect: 1.5 }))
      // 读自然宽高比（未读到前按 3:2 兜底）
      photos.value.forEach((p, i) => {
        const img = new Image()
        img.onload = () => {
          if (img.naturalWidth && img.naturalHeight) {
            photos.value[i].aspect = img.naturalWidth / img.naturalHeight
          }
        }
        img.src = p.url
      })
    }
  } catch (_) {}
  loading.value = false
  window.addEventListener('wheel', onWheel, { passive: false })
  window.addEventListener('touchstart', onTouchStart, { passive: true })
  window.addEventListener('touchmove', onTouchMove, { passive: true })
  window.addEventListener('touchend', onTouchEnd, { passive: true })
  window.addEventListener('keydown', onKey)
  window.addEventListener('resize', layout)
  rafId = requestAnimationFrame(frameLoop)
})

onBeforeUnmount(() => {
  if (rafId !== null) cancelAnimationFrame(rafId)
  if (snapTimer !== null) window.clearTimeout(snapTimer)
  window.removeEventListener('wheel', onWheel)
  window.removeEventListener('touchstart', onTouchStart)
  window.removeEventListener('touchmove', onTouchMove)
  window.removeEventListener('touchend', onTouchEnd)
  window.removeEventListener('keydown', onKey)
  window.removeEventListener('resize', layout)
})
</script>

<style scoped>
.film-page {
  position: relative;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  overscroll-behavior: none;
  background: #fff;
}

[data-theme='dark'] .film-page {
  background: #1c1c20;
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

/* ============ 照片带 ============ */
.fp-strip {
  position: absolute;
  inset: 0;
  z-index: 5;
}

.fp-frame {
  position: absolute;
  top: 0;
  left: 0;
  padding: 0;
  border: 1px solid rgba(24, 24, 27, 0.08);
  border-radius: 4px;
  overflow: hidden;
  background: transparent;
  cursor: pointer;
  will-change: transform, width, height, opacity;
}

[data-theme='dark'] .fp-frame {
  border-color: rgba(255, 255, 255, 0.1);
}

.fp-frame img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  pointer-events: none;
  user-select: none;
}

/* ============ 暗房状态文案（同参考站：加载与过渡时居中浮现） ============ */
.fp-status {
  position: absolute;
  left: 0;
  right: 0;
  top: 49vh;
  z-index: 9;
  margin: 0;
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 0.04em;
  color: #71717a;
  text-align: center;
  pointer-events: none;
  opacity: 0;
}

[data-theme='dark'] .fp-status {
  color: #8d8a96;
}

/* ============ 文字层（量自参考站：标题 15/500，刻度 1.5×28，月份两行） ============ */
.fp-text {
  position: absolute;
  inset: 0;
  z-index: 8;
  pointer-events: none;
}

.fp-title {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 21.5vh;
  margin: 0;
  padding: 0 24px;
  font-size: 15px;
  font-weight: 500;
  color: #38383a;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

[data-theme='dark'] .fp-title {
  color: #d4d4d8;
}

/* 刻度条（几何与色彩复刻源码：按钮宽6 gap4，内条1.5px，idle #e4e4e7 / active #27272a） */
.fp-ticks {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 9vh;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 4px;
  min-height: 84px;
  pointer-events: auto;
  touch-action: none;
}

.fp-tick {
  width: 6px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 6px 0;
  margin: -6px 0;
  border: none;
  background: transparent;
  cursor: pointer;
}

.fp-tick span {
  display: block;
  width: 1.5px;
  height: 28px;
  border-radius: 999px;
  background: #e4e4e7;
}

.fp-tick.on span {
  background: #27272a;
}

[data-theme='dark'] .fp-tick span {
  background: #3f3f46;
}

[data-theme='dark'] .fp-tick.on span {
  background: #f4f4f5;
}

.fp-month {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 4.4vh;
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.35;
  color: #7f7f87;
  text-align: center;
}

.fp-year {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 2vh;
  margin: 0;
  font-size: 14px;
  font-weight: 400;
  line-height: 1.35;
  color: #a0a0a8;
  text-align: center;
}

[data-theme='dark'] .fp-month {
  color: #a1a1aa;
}

[data-theme='dark'] .fp-year {
  color: #8d8a96;
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
  z-index: 6;
}

.fp-empty .sub {
  font-size: 12.5px;
  opacity: 0.8;
}

@media (prefers-reduced-motion: reduce) {
  .fp-tick {
    transition: none;
  }
}

/* ============ 移动端 ============ */
@media (max-width: 768px) {
  .fp-top {
    padding: 16px 20px;
  }

  .fp-title {
    bottom: 24vh;
    font-size: 14px;
  }

  .fp-ticks {
    bottom: 12vh;
  }
}
</style>
