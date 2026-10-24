<template>
  <div class="cropper">
    <!-- 取景框：比例锁定为画框比例，图片在其下平移/缩放 -->
    <div
      ref="stageEl"
      class="stage"
      :style="stageStyle"
      @pointerdown="onPointerDown"
      @pointermove="onPointerMove"
      @pointerup="onPointerUp"
      @pointercancel="onPointerUp"
      @wheel.prevent="onWheel"
    >
      <img
        v-if="imgSrc"
        ref="imgEl"
        class="stage-img"
        :src="imgSrc"
        :style="imgStyle"
        alt=""
        draggable="false"
        @load="onImgLoad"
      />
      <div v-if="!ready" class="stage-loading">加载图片…</div>

      <!-- 九宫格参考线 -->
      <div class="grid" aria-hidden="true">
        <i class="v" style="left: 33.33%"></i><i class="v" style="left: 66.66%"></i>
        <i class="h" style="top: 33.33%"></i><i class="h" style="top: 66.66%"></i>
      </div>
    </div>

    <div class="ctrl">
      <span class="zoom-label">缩放</span>
      <el-slider
        :model-value="zoom"
        :min="1"
        :max="4"
        :step="0.01"
        :show-tooltip="false"
        class="zoom-slider"
        @update:model-value="setZoom"
      />
      <span class="zoom-val">{{ zoom.toFixed(2) }}x</span>
      <el-button size="small" @click="reset">复位</el-button>
    </div>
    <p class="hint">拖动图片调整位置，滚轮或滑杆缩放，取景框比例已锁定为画框比例</p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

/**
 * 按固定比例裁图：取景框比例锁定，图片平移 + 缩放（cover 起步，1–4 倍）。
 * 确认时按原始分辨率导出可见区域（长边上限 2400px）。
 */
const props = defineProps<{ src: string; ratio: number }>()
const emit = defineEmits<{ (e: 'crop', blob: Blob, previewUrl: string): void }>()

const stageEl = ref<HTMLElement | null>(null)
const imgEl = ref<HTMLImageElement | null>(null)
const imgSrc = ref('')
const ready = ref(false)
const zoom = ref(1)

/* 图片中心在取景框内的坐标 + 总缩放（base*zoom），clamp 保证始终铺满 */
const cx = ref(0)
const cy = ref(0)
let baseScale = 1
let natW = 0
let natH = 0

const STAGE_W = 360

const stageStyle = computed(() => ({
  width: STAGE_W + 'px',
  height: Math.round(STAGE_W / props.ratio) + 'px',
}))

const scale = computed(() => baseScale * zoom.value)

const imgStyle = computed(() => {
  if (!ready.value) return {}
  const dw = natW * scale.value
  const dh = natH * scale.value
  return {
    width: dw + 'px',
    height: dh + 'px',
    left: cx.value - dw / 2 + 'px',
    top: cy.value - dh / 2 + 'px',
  }
})

watch(
  () => props.src,
  s => {
    ready.value = false
    zoom.value = 1
    imgSrc.value = s
  },
  { immediate: true },
)

function stageSize() {
  const el = stageEl.value
  if (!el) return { w: STAGE_W, h: STAGE_W / props.ratio }
  return { w: el.clientWidth, h: el.clientHeight }
}

function onImgLoad() {
  const img = imgEl.value
  if (!img) return
  natW = img.naturalWidth
  natH = img.naturalHeight
  if (!natW || !natH) return
  const { w, h } = stageSize()
  baseScale = Math.max(w / natW, h / natH)
  zoom.value = 1
  cx.value = w / 2
  cy.value = h / 2
  clamp()
  ready.value = true
}

function clamp() {
  const { w, h } = stageSize()
  const dw = natW * scale.value / 2
  const dh = natH * scale.value / 2
  // 图片铺满取景框时中心可动范围；图小于框（理论不会，cover 起）则居中
  cx.value = dw * 2 >= w ? Math.min(dw, Math.max(w - dw, cx.value)) : w / 2
  cy.value = dh * 2 >= h ? Math.min(dh, Math.max(h - dh, cy.value)) : h / 2
}

function setZoom(z: number) {
  zoom.value = Math.min(4, Math.max(1, z))
  clamp()
}

function reset() {
  zoom.value = 1
  const { w, h } = stageSize()
  cx.value = w / 2
  cy.value = h / 2
  clamp()
}

/* ================= 拖动平移 ================= */
let dragging = false
let lastX = 0
let lastY = 0

function onPointerDown(e: PointerEvent) {
  if (!ready.value) return
  dragging = true
  lastX = e.clientX
  lastY = e.clientY
  ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
}

function onPointerMove(e: PointerEvent) {
  if (!dragging) return
  cx.value += e.clientX - lastX
  cy.value += e.clientY - lastY
  lastX = e.clientX
  lastY = e.clientY
  clamp()
}

function onPointerUp(e: PointerEvent) {
  dragging = false
  try {
    ;(e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId)
  } catch (_) {}
}

function onWheel(e: WheelEvent) {
  if (!ready.value) return
  setZoom(zoom.value * (e.deltaY < 0 ? 1.08 : 1 / 1.08))
}

/* ================= 导出 ================= */
async function exportCrop() {
  if (!ready.value) return
  const { w, h } = stageSize()
  const s = scale.value
  // 取景框在原图自然像素里的可见区域
  const srcW = w / s
  const srcH = h / s
  const left = cx.value - (natW * s) / 2
  const srcX = Math.max(0, -left / s)
  const top = cy.value - (natH * s) / 2
  const srcY = Math.max(0, -top / s)

  // 输出保持裁剪区域自然分辨率，长边压到 2400 内
  let outW = srcW
  let outH = srcH
  const k = Math.min(1, 2400 / Math.max(srcW, srcH))
  outW = Math.round(srcW * k)
  outH = Math.round(srcH * k)

  const canvas = document.createElement('canvas')
  canvas.width = outW
  canvas.height = outH
  const ctx = canvas.getContext('2d')!
  ctx.imageSmoothingQuality = 'high'
  ctx.drawImage(imgEl.value!, srcX, srcY, srcW, srcH, 0, 0, outW, outH)

  const blob = await new Promise<Blob | null>(resolve =>
    canvas.toBlob(b => resolve(b), 'image/jpeg', 0.92),
  )
  if (blob) emit('crop', blob, canvas.toDataURL('image/jpeg', 0.92))
}

defineExpose({ exportCrop })
</script>

<style scoped>
.cropper {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
}

.stage {
  position: relative;
  overflow: hidden;
  border-radius: 12px;
  background:
    repeating-conic-gradient(#e4e4e7 0% 25%, #f4f4f5 0% 50%) 0 0 / 20px 20px;
  cursor: grab;
  touch-action: none;
  user-select: none;
}

.stage:active {
  cursor: grabbing;
}

.stage-img {
  position: absolute;
  pointer-events: none;
  max-width: none;
}

.stage-loading {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  font-size: 13px;
  color: #71717a;
  background: #f4f4f5;
}

.grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.grid i {
  position: absolute;
}

.grid .v {
  top: 0;
  bottom: 0;
  width: 1px;
  background: rgba(255, 255, 255, 0.55);
}

.grid .h {
  left: 0;
  right: 0;
  height: 1px;
  background: rgba(255, 255, 255, 0.55);
}

.ctrl {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.zoom-label,
.zoom-val {
  font-size: 12px;
  color: #71717a;
  white-space: nowrap;
}

.zoom-val {
  width: 42px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.zoom-slider {
  flex: 1;
}

.hint {
  margin: 0;
  font-size: 12px;
  color: #a1a1aa;
  text-align: center;
}
</style>
