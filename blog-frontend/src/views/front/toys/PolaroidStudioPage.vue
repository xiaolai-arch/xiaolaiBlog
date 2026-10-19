<template>
  <div class="polaroid-page">
    <div class="container">
      <header class="toy-head">
        <router-link to="/" class="back">← 返回</router-link>
        <h1>📷 拍立得工作室</h1>
        <p>上传一张照片，选个滤镜，写句话 —— 一张拍立得就冲印好啦。</p>
      </header>

      <div class="studio">
        <!-- 左：控制台 -->
        <div class="controls">
          <label class="upload-btn">
            {{ img ? '换一张照片' : '📤 上传照片' }}
            <input type="file" accept="image/*" @change="onFile" hidden />
          </label>

          <div class="ctrl-group">
            <h3>滤镜</h3>
            <div class="filter-row">
              <button
                v-for="f in filters"
                :key="f.key"
                class="filter-btn"
                :class="{ on: filter === f.key }"
                @click="filter = f.key"
              >
                {{ f.label }}
              </button>
            </div>
          </div>

          <div class="ctrl-group">
            <h3>手写一句话</h3>
            <el-input v-model="caption" maxlength="24" show-word-limit placeholder="比如：今天也要加油鸭！" />
          </div>

          <div class="ctrl-group">
            <h3>日期</h3>
            <el-date-picker v-model="date" type="date" format="YYYY.MM.DD" value-format="YYYY.MM.DD" style="width: 100%" />
          </div>

          <div class="ctrl-group">
            <h3>摆放角度</h3>
            <div class="angle-row">
              <button class="mini-btn" @click="angle = -4">↺ 左倾</button>
              <button class="mini-btn" @click="angle = 0">摆正</button>
              <button class="mini-btn" @click="angle = 4">↻ 右倾</button>
            </div>
          </div>

          <button class="print-btn" :disabled="!img" @click="download">🖨️ 冲印并下载</button>
        </div>

        <!-- 右：相纸预览 -->
        <div class="stage" :class="{ empty: !img }">
          <canvas v-show="img" ref="canvasRef" class="polaroid" :style="{ transform: `rotate(${angle}deg)` }"></canvas>
          <div v-if="!img" class="stage-hint">
            <span class="big-emoji">🖼️</span>
            <p>照片会冲印在这里</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'

const img = ref<HTMLImageElement | null>(null)
const filter = ref('original')
const caption = ref('')
const date = ref('')
const angle = ref(-3)
const canvasRef = ref<HTMLCanvasElement | null>(null)

const filters = [
  { key: 'original', label: '原片', css: 'none' },
  { key: 'warm', label: '暖阳', css: 'sepia(0.32) saturate(1.35) contrast(1.05)' },
  { key: 'bw', label: '黑白', css: 'grayscale(1) contrast(1.12)' },
  { key: 'retro', label: '复古', css: 'sepia(0.55) saturate(1.4) brightness(1.06) contrast(0.95)' },
  { key: 'fade', label: '奶雾', css: 'saturate(0.72) contrast(0.92) brightness(1.12)' },
]

onMounted(() => {
  date.value = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '.')
})

function onFile(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    const image = new Image()
    image.onload = () => {
      img.value = image
      render()
    }
    image.src = reader.result as string
  }
  reader.readAsDataURL(file)
}

function render() {
  const canvas = canvasRef.value
  const image = img.value
  if (!canvas || !image) return

  const W = 640
  const pad = 38
  const photo = W - pad * 2
  const bottom = 150
  const H = pad + photo + bottom

  canvas.width = W
  canvas.height = H
  const ctx = canvas.getContext('2d')!

  // 相纸底
  ctx.fillStyle = '#fefdf8'
  ctx.fillRect(0, 0, W, H)
  // 轻微阴影边
  ctx.strokeStyle = 'rgba(0,0,0,0.06)'
  ctx.lineWidth = 2
  ctx.strokeRect(1, 1, W - 2, H - 2)

  // 照片（居中裁方）
  const css = filters.find((f) => f.key === filter.value)?.css || 'none'
  ctx.save()
  ctx.filter = css === 'none' ? 'none' : css
  const side = Math.min(image.width, image.height)
  const sx = (image.width - side) / 2
  const sy = (image.height - side) / 2
  ctx.drawImage(image, sx, sy, side, side, pad, pad, photo, photo)
  ctx.restore()

  // 手写文案
  const text = caption.value.trim()
  if (text) {
    ctx.fillStyle = '#3a3a3a'
    ctx.font = '600 30px "Segoe Script", "Comic Sans MS", "Kaiti SC", "KaiTi", cursive'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(text, W / 2, pad + photo + 58)
  }

  // 日期 + 小爱心
  ctx.fillStyle = '#9a9a9a'
  ctx.font = '22px "DM Mono", "JetBrains Mono", Consolas, monospace'
  ctx.textAlign = 'left'
  ctx.fillText(date.value || '', pad + 4, H - 34)
  ctx.textAlign = 'right'
  ctx.fillStyle = '#e0708a'
  ctx.font = '20px sans-serif'
  ctx.fillText('♥', W - pad - 4, H - 34)
}

watch([filter, caption, date], render)

function download() {
  const canvas = canvasRef.value
  if (!canvas) return
  const link = document.createElement('a')
  link.download = `polaroid-${Date.now()}.png`
  link.href = canvas.toDataURL('image/png')
  link.click()
}
</script>

<style scoped>
.polaroid-page {
  padding-top: 40px;
  padding-bottom: 60px;
}

.toy-head {
  text-align: center;
  margin-bottom: 36px;
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

.studio {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 28px;
  align-items: start;
}

/* 控制台 */
.controls {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.upload-btn {
  display: block;
  text-align: center;
  background: var(--accent-color);
  color: #fff;
  font-weight: 700;
  font-size: 14.5px;
  padding: 13px;
  border-radius: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.upload-btn:hover {
  background: var(--accent-hover);
}

.ctrl-group h3 {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-secondary);
  margin-bottom: 10px;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-btn {
  border: 1.5px solid var(--border-color);
  background: var(--bg-primary);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  padding: 7px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.15s;
}

.filter-btn.on {
  border-color: var(--accent-color);
  color: var(--accent-color);
  background: var(--tint-lavender);
}

.angle-row {
  display: flex;
  gap: 8px;
}

.mini-btn {
  flex: 1;
  border: 1.5px solid var(--border-color);
  background: var(--bg-primary);
  color: var(--text-secondary);
  font-size: 12.5px;
  padding: 7px 0;
  border-radius: 10px;
  cursor: pointer;
}

.mini-btn:hover {
  border-color: var(--accent-color);
  color: var(--accent-color);
}

.print-btn {
  border: none;
  background: var(--tint-mint);
  color: var(--text-primary);
  font-weight: 700;
  font-size: 14.5px;
  padding: 13px;
  border-radius: 14px;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}

.print-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-soft);
}

.print-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* 舞台 */
.stage {
  min-height: 460px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--tint-butter);
  border-radius: var(--radius-lg);
  padding: 44px;
}

.stage.empty {
  border: 2px dashed var(--border-color);
  background: transparent;
}

.stage-hint {
  text-align: center;
  color: var(--text-muted);
}

.big-emoji {
  font-size: 56px;
  display: block;
  margin-bottom: 10px;
}

.polaroid {
  width: min(420px, 100%);
  height: auto;
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.18);
  border-radius: 4px;
  transition: transform 0.25s;
}

@media (max-width: 860px) {
  .studio {
    grid-template-columns: 1fr;
  }
}
</style>
