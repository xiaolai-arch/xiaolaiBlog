<template>
  <div class="film-page">
    <!-- 顶栏：返回 + 品名 -->
    <header class="fp-top">
      <router-link to="/" class="fp-back">← 回首页</router-link>
      <span class="fp-brand">图集</span>
    </header>

    <!-- 照片带（scrub 渲染循环直接写样式） -->
    <div class="fp-strip" ref="stripEl">
      <button
        v-for="(ph, i) in photos"
        :key="ph.id"
        :ref="el => setFrame(i, el as HTMLElement | null)"
        class="fp-frame"
        :aria-label="`查看 ${ph.title || '照片'}`"
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

/* 标题仅在后台填了图片标题时展示，不回退显示文件名 */
const currentTitle = computed(() => photos.value[idx.value]?.title?.trim() || '')

const currentMonth = computed(() => {
  const t = photos.value[idx.value]?.createTime
  if (!t) return ''
  const MONTHS = ['January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December']
  return MONTHS[parseInt(t.slice(5, 7), 10) - 1] || ''
})
const currentYear = computed(() => photos.value[idx.value]?.createTime?.slice(0, 4) || '')

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

/* ================= DOM refs（渲染循环直写样式） ================= */
const stripEl = ref<HTMLElement | null>(null)
const statusEl = ref<HTMLElement | null>(null)
const textEl = ref<HTMLElement | null>(null)
const frameEls: Array<HTMLElement | null> = []
function setFrame(i: number, el: HTMLElement | null) {
  frameEls[i] = el
}

/* ================= 刻度条（复刻 film-linemark-scale.ts） ================= */
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

/* ================= 照片带几何（复刻 FilmPage.tsx 布局常量） =================
   绝对像素尺寸：槽宽 120px / 移动 72px，间距 4px；宽度在 72 与
   min(480, 横图495/竖图 bw×2.2) 之间按 smoothstep 曲线过渡（范围 2.12 槽位）；
   焦点照片中心锚定屏幕中线，纵向 midY 同源码 play band/timeline 公式 */
const GAP = 4
const BASE_W_DESKTOP = 120
const BASE_W_MOBILE = 72
const W_COLLAPSED = 72
const W_EXPANDED_MAX = 480
const LANDSCAPE_CENTER_MAX = 495
const PORTRAIT_CENTER_SCALE_DESKTOP = 2.2
const PORTRAIT_CENTER_SCALE_MOBILE = 1.92
const WIDEN_SLOT_RANGE = 2.12

const INTRO_STAGGER_MS = 60
const INTRO_FADE_MS = 520
const INTRO_SLIDE_PX = 18

let introStart: number | null = null

function smoothstep(t: number): number {
  const c = Math.max(0, Math.min(1, t))
  return c * c * (3 - 2 * c)
}

function clampAspect(ar: number): number {
  return Math.min(3, Math.max(0.35, ar || 1))
}

function frameWidthOpacity(i: number, p: number, vw: number): { w: number; op: number } {
  const mobile = vw < 640
  const bw = mobile ? BASE_W_MOBILE : BASE_W_DESKTOP
  const ar = clampAspect(photos.value[i].aspect)
  const d = Math.abs(p - i)
  const u = Math.min(1, d / WIDEN_SLOT_RANGE)
  const widenT = smoothstep(1 - u)
  const collapsed = Math.min(W_COLLAPSED, bw)
  const expanded = Math.min(
    W_EXPANDED_MAX,
    ar > 1
      ? mobile
        ? vw * 0.64
        : LANDSCAPE_CENTER_MAX
      : bw * (mobile ? PORTRAIT_CENTER_SCALE_MOBILE : PORTRAIT_CENTER_SCALE_DESKTOP),
  )
  return { w: collapsed + (expanded - collapsed) * widenT, op: 0.5 + 0.5 * widenT }
}

/* 照片带垂直中线（同源码：play band 底与 timeline 顶之间的中点） */
function stripMidY(vh: number, mobile: boolean): number {
  const rem = 16
  const playBarTop = Math.max(1.625 * rem + vh * 0.07, 2.75 * rem)
  const playBandBottom = playBarTop + 40
  const timelineInset = Math.max(0.375 * rem + vh * 0.025, 0.75 * rem)
  const reserve = timelineInset + (mobile ? 148 : 172)
  return (playBandBottom + (vh - reserve)) / 2
}

function layout() {
  const n = photos.value.length
  if (!n) return
  const vw = window.innerWidth
  const vh = window.innerHeight
  const mobile = vw < 640
  const midY = stripMidY(vh, mobile)
  const p = progress.value

  // 各照片宽度/高度与累计 left（间距 4px 累积排布）
  const widths: number[] = []
  const heights: number[] = []
  const ops: number[] = []
  const lefts: number[] = []
  let acc = 0
  for (let i = 0; i < n; i++) {
    const { w, op } = frameWidthOpacity(i, p, vw)
    const h = w / clampAspect(photos.value[i].aspect)
    widths.push(w)
    heights.push(h)
    ops.push(op)
    lefts.push(acc)
    acc += w + GAP
  }

  // 轨道平移：把焦点照片中心（floor/ceil 插值）锚定屏幕中线
  const i0 = Math.max(0, Math.min(n - 1, Math.floor(p)))
  const i1 = Math.max(0, Math.min(n - 1, Math.ceil(p)))
  const frac = Math.abs(p - Math.floor(p))
  const c0 = lefts[i0] + widths[i0] / 2
  const c1 = lefts[i1] + widths[i1] / 2
  const trackOffset = vw / 2 - (c0 + (c1 - c0) * frac)

  // 入场：每张错峰 60ms、520ms 淡入 + 18px 滑入
  const now = performance.now()
  if (introStart === null) introStart = now
  const introActive = now - introStart < (n - 1) * INTRO_STAGGER_MS + INTRO_FADE_MS + 80

  for (let i = 0; i < n; i++) {
    const el = frameEls[i]
    if (!el) continue
    el.style.left = lefts[i] + trackOffset + 'px'
    el.style.top = midY - heights[i] / 2 + 'px'
    el.style.width = widths[i] + 'px'
    el.style.height = heights[i] + 'px'
    el.style.zIndex = String(100 - Math.round(Math.abs(i - p) * 10))

    let introL = 1
    if (introActive) {
      const u = Math.max(0, Math.min(1, (now - introStart - i * INTRO_STAGGER_MS) / INTRO_FADE_MS))
      introL = smoothstep(u)
    }
    el.style.opacity = String(introL < 1 ? ops[i] * introL : ops[i])
    el.style.transform = introL < 1 ? `translate3d(${-(1 - introL) * INTRO_SLIDE_PX}px, 0, 0)` : ''
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

/* ================= scrub 渲染循环 ================= */
let rafId: number | null = null
let reducedMotion = false

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

/* 源码：wheel 系数 0.68，每槽位 124px（移动端 76px×0.55） */
const WHEEL_FACTOR = 0.68

function onWheel(e: WheelEvent) {
  e.preventDefault()
  if (Math.abs(e.deltaY) < 2) return
  const vw = window.innerWidth
  const pitch = (vw < 640 ? BASE_W_MOBILE : BASE_W_DESKTOP) + GAP
  const step = vw < 640 ? pitch * 0.55 : pitch
  tweenTarget = null
  progress.value = clampP(progress.value + (WHEEL_FACTOR * e.deltaY) / step)
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
    progress.value = clampP(progress.value + dy / 300)
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
  transform-origin: center center;
  will-change: left, top, width, height, opacity;
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

/* ============ 暗房状态文案 ============ */
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

/* ============ 文字层 ============ */
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

/* 刻度条（几何与色彩复刻源码） */
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
