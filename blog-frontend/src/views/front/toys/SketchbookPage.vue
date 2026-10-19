<template>
  <div class="sketch-page">
    <div class="container">
      <header class="toy-head">
        <router-link to="/" class="back">← 返回</router-link>
        <h1>✏️ 数字速写本</h1>
        <p>想到什么画什么，画完可以导出带走。</p>
      </header>

      <div class="toolbar">
        <div class="color-row">
          <button
            v-for="c in colors"
            :key="c"
            class="color-dot"
            :class="{ on: color === c && !erasing }"
            :style="{ background: c }"
            @click="pickColor(c)"
            :aria-label="'颜色 ' + c"
          />
        </div>
        <div class="size-row">
          <button
            v-for="s in sizes"
            :key="s"
            class="size-btn"
            :class="{ on: size === s }"
            :style="{ width: 10 + s * 4 + 'px', height: 10 + s * 4 + 'px' }"
            @click="size = s"
            :aria-label="'粗细 ' + s"
          />
        </div>
        <div class="tool-row">
          <button class="tool-btn" :class="{ on: erasing }" @click="erasing = !erasing">🧽 橡皮</button>
          <button class="tool-btn" @click="undo">↩︎ 撤销</button>
          <button class="tool-btn danger" @click="clear">🗑 清空</button>
          <button class="tool-btn primary" @click="download">⬇️ 导出 PNG</button>
        </div>
      </div>

      <div class="paper-wrap">
        <canvas
          ref="canvasRef"
          class="paper"
          @pointerdown="start"
          @pointermove="move"
          @pointerup="end"
          @pointerleave="end"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const canvasRef = ref<HTMLCanvasElement | null>(null)
const color = ref('#1b1a1f')
const size = ref(2)
const erasing = ref(false)
const drawing = ref(false)
const history: ImageData[] = []

const colors = ['#1b1a1f', '#e0708a', '#6c5ce7', '#3ba55d', '#f0a632', '#3d9bd9']
const sizes = [1, 2, 4]

function ctx2d() {
  return canvasRef.value!.getContext('2d')!
}

function resize() {
  const canvas = canvasRef.value!
  const wrap = canvas.parentElement!
  const dpr = window.devicePixelRatio || 1
  const data = canvas.width ? ctx2d().getImageData(0, 0, canvas.width, canvas.height) : null
  canvas.width = wrap.clientWidth * dpr
  canvas.height = wrap.clientHeight * dpr
  const ctx = ctx2d()
  ctx.scale(dpr, dpr)
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  if (data) ctx.putImageData(data, 0, 0)
}

onMounted(resize)

function pickColor(c: string) {
  color.value = c
  erasing.value = false
}

function pos(e: PointerEvent) {
  const rect = canvasRef.value!.getBoundingClientRect()
  return { x: e.clientX - rect.left, y: e.clientY - rect.top }
}

function start(e: PointerEvent) {
  const canvas = canvasRef.value!
  history.push(ctx2d().getImageData(0, 0, canvas.width, canvas.height))
  if (history.length > 20) history.shift()
  drawing.value = true
  const ctx = ctx2d()
  const { x, y } = pos(e)
  ctx.beginPath()
  ctx.moveTo(x, y)
  ctx.strokeStyle = erasing.value ? '#fefcf7' : color.value
  ctx.lineWidth = erasing.value ? size.value * 6 : size.value * 3
  ctx.lineTo(x + 0.1, y)
  ctx.stroke()
}

function move(e: PointerEvent) {
  if (!drawing.value) return
  const ctx = ctx2d()
  const { x, y } = pos(e)
  ctx.strokeStyle = erasing.value ? '#fefcf7' : color.value
  ctx.lineWidth = erasing.value ? size.value * 6 : size.value * 3
  ctx.lineTo(x, y)
  ctx.stroke()
}

function end() {
  drawing.value = false
}

function undo() {
  const data = history.pop()
  if (data) ctx2d().putImageData(data, 0, 0)
}

function clear() {
  const canvas = canvasRef.value!
  history.push(ctx2d().getImageData(0, 0, canvas.width, canvas.height))
  ctx2d().clearRect(0, 0, canvas.width, canvas.height)
}

function download() {
  const link = document.createElement('a')
  link.download = `sketch-${Date.now()}.png`
  link.href = canvasRef.value!.toDataURL('image/png')
  link.click()
}
</script>

<style scoped>
.sketch-page {
  padding-top: 40px;
  padding-bottom: 60px;
}

.toy-head {
  text-align: center;
  margin-bottom: 28px;
  position: relative;
}

.back {
  position: absolute;
  left: 0;
  top: 6px;
  font-size: 13.5px;
  color: var(--text-muted);
}

.back:hover {
  color: var(--accent-color);
}

.toy-head h1 {
  font-family: var(--font-display);
  font-size: 32px;
  font-weight: 800;
  letter-spacing: -0.03em;
  margin-bottom: 8px;
}

.toy-head p {
  color: var(--text-secondary);
  font-size: 14.5px;
}

/* 工具条 */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 18px;
  background: var(--card-bg);
  border-radius: 999px;
  box-shadow: var(--shadow-soft);
  padding: 12px 22px;
  margin-bottom: 20px;
}

.color-row {
  display: flex;
  gap: 10px;
}

.color-dot {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 2.5px solid transparent;
  cursor: pointer;
  transition: transform 0.15s;
}

.color-dot.on {
  border-color: var(--accent-color);
  transform: scale(1.18);
}

.size-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 8px;
}

.size-btn {
  border-radius: 50%;
  border: none;
  cursor: pointer;
  background: var(--text-primary);
  transition: transform 0.15s;
}

.size-btn.on {
  outline: 2.5px solid var(--accent-color);
  outline-offset: 2px;
  transform: scale(1.15);
}

.tool-row {
  display: flex;
  gap: 8px;
}

.tool-btn {
  border: 1.5px solid var(--border-color);
  background: var(--bg-primary);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  padding: 8px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.15s;
}

.tool-btn.on {
  border-color: var(--accent-color);
  color: var(--accent-color);
  background: var(--tint-lavender);
}

.tool-btn.danger:hover {
  border-color: #e0708a;
  color: #e0708a;
}

.tool-btn.primary {
  background: var(--accent-color);
  border-color: var(--accent-color);
  color: #fff;
}

.tool-btn.primary:hover {
  background: var(--accent-hover);
}

/* 画纸 */
.paper-wrap {
  height: 560px;
  border-radius: var(--radius-lg);
  background: #fefcf7;
  box-shadow: var(--shadow-hover);
  overflow: hidden;
}

.paper {
  width: 100%;
  height: 100%;
  display: block;
  cursor: crosshair;
  touch-action: none;
}

@media (max-width: 720px) {
  .paper-wrap {
    height: 440px;
  }
}
</style>
