<template>
  <div
    ref="rootEl"
    class="lp-root"
    :class="{ 'lp-paused': paused, 'lp-drift-on': drift && !reducedMotion }"
  >
    <div v-show="!hiddenForOffscreen" class="lp-drift">
      <iframe
        v-if="active"
        ref="iframeEl"
        class="lp-iframe"
        :src="src"
        :title="title"
        tabindex="-1"
        :style="{ '--s': scale, width: previewW + 'px', height: previewH + 'px' }"
        @load="onLoad"
      ></iframe>
    </div>
    <div v-if="active && !loaded" class="lp-skeleton" aria-hidden="true"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = withDefaults(
  defineProps<{
    src: string
    title?: string
    active?: boolean
    paused?: boolean
    previewW?: number
    previewH?: number
    drift?: boolean
    /** 下行巡览速度（px/s，按预览视口坐标）：博客等文字页用慢速，胶片等大步进页面按网站节奏提速 */
    scrollSpeed?: number
    /** 导览模式：适用于页面自身无滚动的体验页（如 3D 展厅），按脚本定时“按”页内导览按钮驱动运镜 */
    tour?: boolean
  }>(),
  {
    title: '实时预览',
    active: true,
    paused: false,
    previewW: 1000,
    previewH: 625,
    drift: true,
    scrollSpeed: 26,
    tour: false,
  },
)

const rootEl = ref<HTMLElement | null>(null)
const iframeEl = ref<HTMLIFrameElement | null>(null)
const scale = ref(0.5)
const loaded = ref(false)
// 离屏时隐藏 iframe：不渲染的 iframe 里 rAF/WebGL 会停掉，省 GPU
const hiddenForOffscreen = ref(false)

/* ================= 展厅导览脚本（tour 模式） ================= */
/* ================= 展厅导览脚本（tour 模式） ================= */
/** sel 为预览页内的按钮选择器，holdMs 为按下后的停留时长（运镜动画本身约 0.78s） */
type TourStep = { sel: string; holdMs: number }
const nextSteps = (n = 1): TourStep[] =>
  Array.from({ length: n }, () => ({ sel: '[aria-label="Next painting"]', holdMs: 1000 }))
const zoomSteps = (label: 'Zoom in' | 'Zoom out', n = 1): TourStep[] =>
  // 缩放运镜本身仅 0.22s，停留 0.48s 让连续点按干脆利落
  Array.from({ length: n }, () => ({ sel: `[aria-label="${label}"]`, holdMs: 480 }))
/**
 * 主循环 5s（5 幅 × 1s 纯巡馆）；每 2 圈插入一段原地“三级放大 → 三级缩回”
 * 的呼吸式演示（点按间隔 0.48s，节奏明快），随后继续巡馆。
 */
const GALLERY_TOUR_SCRIPT: TourStep[] = [
  ...nextSteps(5),
  ...zoomSteps('Zoom in', 3),
  ...zoomSteps('Zoom out', 3),
  ...nextSteps(1),
  ...nextSteps(5),
  ...zoomSteps('Zoom in', 3),
  ...zoomSteps('Zoom out', 3),
  ...nextSteps(1),
]
let tourStep = 0

/* ================= 缩放 ================= */
let resizeObserver: ResizeObserver | null = null

function updateScale() {
  const w = rootEl.value?.clientWidth
  if (w) scale.value = w / props.previewW
}

/* ================= 预览内样式注入（隐藏站点导航/页脚/滚动条） ================= */
const PREVIEW_CSS =
  '.slim-header,.site-footer,.search-overlay{display:none!important}' +
  '::-webkit-scrollbar{width:0;height:0}html{scrollbar-width:none}'

function onLoad() {
  loaded.value = true
  tourStep = 0
  const win = iframeEl.value?.contentWindow
  if (!win || !win.document) return
  try {
    const style = win.document.createElement('style')
    style.textContent = PREVIEW_CSS
    win.document.head.appendChild(style)
  } catch (_) {}
  const st = getScrollState()
  st.pos = 0
  st.dir = 1
  st.last = performance.now()
  // 导览模式（3D 展厅）首步多留些时间：WebGL 初始化 + 挂画请求都要先完成
  st.holdUntil = performance.now() + (props.tour ? 4500 : 2200)
}

/* ================= 自动滚动巡览 ================= */
interface ScrollState {
  pos: number
  dir: 1 | -1
  holdUntil: number
  last: number
}
const scrollState: ScrollState = {
  pos: 0,
  dir: 1,
  holdUntil: 0,
  last: performance.now(),
}

function getScrollState(): ScrollState {
  return scrollState
}

let rafId: number | null = null
let onVisChange: (() => void) | null = null
let mq: MediaQueryList | null = null
let mqListener: ((e: MediaQueryListEvent) => void) | null = null
const reducedMotion = ref(false)
const onScreen = ref(true)

function frameLoop(now: number) {
  rafId = requestAnimationFrame(frameLoop)
  if (document.hidden || reducedMotion.value || props.paused || !onScreen.value) return
  const win = iframeEl.value?.contentWindow
  if (!win || !win.document || !win.document.documentElement) return
  const st = getScrollState()
  const dt = Math.min(64, now - st.last)
  st.last = now
  if (now < st.holdUntil) return

  if (props.tour) {
    // 导览模式：页面本身没有文档滚动（如 3D 展厅），按脚本点击页内导览控件，
    // 运镜动画与真实用户按按钮完全一致。脚本节奏：两幅一停的快速巡馆，
    // 每圈插一段“放大两级 → 保持放大平移到下一幅 → 缩回两级”的缩放演示。
    const step = GALLERY_TOUR_SCRIPT[tourStep % GALLERY_TOUR_SCRIPT.length]
    const btn = win.document.querySelector<HTMLButtonElement>(step.sel)
    if (btn) {
      btn.click()
      tourStep++
      st.holdUntil = now + step.holdMs
    } else {
      st.holdUntil = now + 1500
    }
    return
  }

  const doc = win.document.documentElement
  let max = 0
  try {
    max = Math.max(0, doc.scrollHeight - win.innerHeight)
  } catch (_) {
    return
  }
  if (max <= 4) {
    // 一屏放得下：内容可能变化，隔几秒复查
    st.holdUntil = now + 4000
    return
  }
  const speed = st.dir === 1 ? props.scrollSpeed : props.scrollSpeed * 2.5 // 下行巡览，回顶快一些
  st.pos += (st.dir * speed * dt) / 1000
  if (st.pos >= max) {
    st.pos = max
    st.dir = -1
    st.holdUntil = now + 1600
  } else if (st.pos <= 0) {
    st.pos = 0
    st.dir = 1
    st.holdUntil = now + 2800
  }
  try {
    win.scrollTo(0, st.pos)
  } catch (_) {}
}

/* ================= 离屏检测（带去抖，避免边界闪烁） ================= */
let intersectionObserver: IntersectionObserver | null = null
let hideTimer: number | null = null

function setOnScreen(v: boolean) {
  onScreen.value = v
  if (hideTimer !== null) {
    window.clearTimeout(hideTimer)
    hideTimer = null
  }
  if (v) {
    hiddenForOffscreen.value = false
    scrollState.last = performance.now()
  } else {
    hideTimer = window.setTimeout(() => {
      hiddenForOffscreen.value = true
    }, 400)
  }
}

onMounted(() => {
  resizeObserver = new ResizeObserver(updateScale)
  resizeObserver.observe(rootEl.value!)
  updateScale()

  intersectionObserver = new IntersectionObserver(
    entries => {
      for (const entry of entries) setOnScreen(entry.isIntersecting)
    },
    { rootMargin: '120px' },
  )
  intersectionObserver.observe(rootEl.value!)

  onVisChange = () => {
    if (!document.hidden) scrollState.last = performance.now()
  }
  document.addEventListener('visibilitychange', onVisChange)

  mq = window.matchMedia('(prefers-reduced-motion: reduce)')
  mqListener = () => {
    reducedMotion.value = mq!.matches
  }
  mq.addEventListener('change', mqListener)
  reducedMotion.value = mq.matches

  rafId = requestAnimationFrame(frameLoop)
})

onBeforeUnmount(() => {
  if (rafId !== null) cancelAnimationFrame(rafId)
  if (hideTimer !== null) window.clearTimeout(hideTimer)
  resizeObserver?.disconnect()
  intersectionObserver?.disconnect()
  if (onVisChange) document.removeEventListener('visibilitychange', onVisChange)
  if (mq && mqListener) mq.removeEventListener('change', mqListener)
})
</script>

<style scoped>
.lp-root {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.lp-drift {
  position: absolute;
  inset: 0;
}

/* 缓慢漂移（Ken Burns）：静态页面也有细微的生命感 */
.lp-root.lp-drift-on .lp-drift {
  animation: lpDrift 26s ease-in-out infinite;
  transform-origin: 50% 42%;
}

.lp-root.lp-paused .lp-drift {
  animation-play-state: paused;
}

@keyframes lpDrift {
  0% {
    transform: scale(1) translate(0, 0);
  }
  50% {
    transform: scale(1.03) translate(-0.5%, 0.7%);
  }
  100% {
    transform: scale(1) translate(0, 0);
  }
}

.lp-iframe {
  border: 0;
  display: block;
  pointer-events: none;
  transform: scale(var(--s, 0.5));
  transform-origin: 0 0;
  transition: transform 0.7s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform;
}

/* 悬停（paused 由父级传入）时轻微放大，0.7s 丝滑缓动 */
.lp-root.lp-paused .lp-iframe {
  transform: scale(calc(var(--s, 0.5) * 1.03));
}

.lp-skeleton {
  position: absolute;
  inset: 0;
  z-index: 2;
  background: linear-gradient(100deg, transparent 30%, rgba(255, 255, 255, 0.45) 50%, transparent 70%);
  background-size: 200% 100%;
  animation: lpShimmer 1.6s ease-in-out infinite;
}

[data-theme='dark'] .lp-skeleton {
  background: linear-gradient(100deg, transparent 30%, rgba(255, 255, 255, 0.08) 50%, transparent 70%);
  background-size: 200% 100%;
}

@keyframes lpShimmer {
  from {
    background-position: 120% 0;
  }
  to {
    background-position: -80% 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .lp-drift {
    animation: none;
  }
  .lp-iframe,
  .lp-skeleton {
    transition: none;
    animation: none;
  }
}
</style>
