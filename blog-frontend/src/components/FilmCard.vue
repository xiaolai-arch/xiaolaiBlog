<template>
  <div class="film-card" @click="enter" role="link" aria-label="进入胶片图集页" tabindex="0" @keydown.enter="enter">
    <div class="stage">
      <!-- 开场：冲洗底片中 -->
      <Transition name="develop">
        <div v-if="developing" class="develop">rolling the negatives…</div>
      </Transition>

      <template v-if="!developing">
        <!-- 照片层叠带：入口预览，点击整卡进入胶片页 -->
        <div v-if="photos.length" class="strip">
          <div
            v-for="(p, i) in photos"
            :key="p.id"
            class="frame"
            :class="'pos-' + posOf(i)"
            v-show="posOf(i) <= 3"
          >
            <img :src="p.url" :alt="p.title || p.originalName" loading="lazy" />
          </div>

          <button class="nav prev" aria-label="上一张" @click.stop="next(-1)">‹</button>
          <button class="nav next" aria-label="下一张" @click.stop="next(1)">›</button>
        </div>
        <div v-else class="empty">
          <p>{{ loading ? '加载中…' : '图库还没有照片' }}</p>
          <p class="empty-sub">后台「媒体」上传后展示在这里</p>
        </div>

        <!-- 照片标题（仅后台填了标题时显示，不展示文件名） -->
        <p v-if="currentTitle" class="photo-title">{{ currentTitle }}</p>

        <!-- 刻度轴（仅做预览切换） -->
        <div class="ticks" role="tablist" aria-label="切换照片" @click.stop>
          <button
            v-for="(p, i) in photos"
            :key="p.id"
            class="tick"
            :class="{ on: i === index }"
            :aria-label="`第 ${i + 1} 张`"
            @click="go(i)"
          ></button>
        </div>

      </template>
    </div>

    <div class="card-caption">
      <p class="caption-title">
        <span>我的图集</span>
        <span class="tp-year"> • {{ photos.length }} 张</span>
      </p>
      <span class="caption-more">进入 Film →</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'

interface Photo {
  id: number
  url: string
  title?: string
  originalName?: string
  createTime?: string
}

const router = useRouter()
const photos = ref<Photo[]>([])
const loading = ref(true)
const index = ref(0)
const developing = ref(true)

function enter() {
  router.push('/film')
}

const currentTitle = computed(() => {
  const p = photos.value[index.value]
  return p?.title?.trim() || ''
})

/* i 相对当前张的偏移位置：0=主图，1..3=右侧渐远 */
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
  timer = window.setInterval(() => next(1), 1800)
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
  // 开场"冲洗底片"仪式感：1.4s 后显影
  setTimeout(() => {
    developing.value = false
    startAuto()
  }, 1400)
})

onBeforeUnmount(stopAuto)
</script>

<style scoped>
.film-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  cursor: pointer;
  animation: filmCardEnter 450ms cubic-bezier(0.25, 0.46, 0.45, 0.94) 300ms both;
}

/* 浅色舞台：满幅留白，照片带垂直居中（同参考站 film 页构图） */
.stage {
  position: relative;
  flex: 1;
  min-height: 300px;
  border-radius: 26px;
  overflow: hidden;
  isolation: isolate;
  background: linear-gradient(175deg, #fbfbfa, #f6f6f4 55%, #f2f2ef);
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.04),
    0 12px 34px rgba(24, 24, 27, 0.08);
}

[data-theme='dark'] .stage {
  background: linear-gradient(175deg, #232326, #1f1f22 55%, #26262a);
}

/* ============ 开场：冲洗底片 ============ */
.develop {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-size: 15px;
  letter-spacing: 0.08em;
  color: #a1a1aa;
}

.develop-enter-active,
.develop-leave-active {
  transition: opacity 0.8s ease;
}

.develop-enter-from,
.develop-leave-to {
  opacity: 0;
}

/* ============ 照片层叠带 ============ */
.strip {
  position: absolute;
  inset: 0 0 92px 0;
}

.frame {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 52%;
  aspect-ratio: 4 / 3;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 14px 34px rgba(24, 24, 27, 0.16);
  cursor: pointer;
  transition: transform 0.5s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.5s ease, left 0.5s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform, left, opacity;
}

.frame img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  pointer-events: none;
}

/* 主图：卡内水平居中（对齐 /film 整页焦点居中构图） */
.pos-0 {
  transform: translate(-50%, -50%);
  z-index: 5;
  opacity: 1;
}

/* 右侧渐远带（同整页比例 0.76/0.58/0.44） */
.pos-1 {
  left: 82%;
  transform: translateY(-50%) scale(0.76);
  z-index: 4;
  opacity: 0.85;
  box-shadow: 0 8px 22px rgba(24, 24, 27, 0.1);
}

.pos-2 {
  left: 102%;
  transform: translateY(-50%) scale(0.58);
  z-index: 3;
  opacity: 0.55;
  box-shadow: 0 6px 16px rgba(24, 24, 27, 0.07);
}

.pos-3 {
  left: 118%;
  transform: translateY(-50%) scale(0.44);
  z-index: 2;
  opacity: 0.3;
  box-shadow: 0 4px 12px rgba(24, 24, 27, 0.05);
}

/* 其余淡出 */
.frame:not(.pos-0):not(.pos-1):not(.pos-2):not(.pos-3) {
  opacity: 0;
  z-index: 1;
  pointer-events: none;
  left: 130%;
  transform: translateY(-50%) scale(0.34);
}

/* ============ 切换按钮 ============ */
.nav {
  position: absolute;
  top: 44%;
  z-index: 6;
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

/* ============ 标题 / 刻度 / 月份 ============ */
.photo-title {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 64px;
  z-index: 5;
  margin: 0;
  padding: 0 24px;
  font-size: 13px;
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
  position: absolute;
  left: 0;
  right: 0;
  bottom: 40px;
  z-index: 5;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 6px;
  height: 22px;
}

.tick {
  width: 1.5px;
  height: 11px;
  border: none;
  border-radius: 1px;
  background: rgba(24, 24, 27, 0.22);
  cursor: pointer;
  padding: 0;
  transition: height 0.3s cubic-bezier(0.22, 1, 0.36, 1), background 0.3s, width 0.3s;
}

[data-theme='dark'] .tick {
  background: rgba(255, 255, 255, 0.25);
}

.tick:hover {
  background: rgba(24, 24, 27, 0.5);
  height: 15px;
}

.tick.on {
  width: 2px;
  height: 22px;
  background: #18181b;
}

[data-theme='dark'] .tick.on {
  background: #f4f4f5;
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

/* ============ 说明行：隐式显示，悬停卡片时浮现 ============ */
.card-caption {
  display: flex;
  align-items: flex-start;
  width: 100%;
  padding: 0 13px;
  margin: -6px 0 2px;
  font-size: 16px;
  line-height: 1.375;
  letter-spacing: 0.005em;
  opacity: 0;
  transform: translateY(4px);
  transition: opacity 0.3s ease-out, transform 0.3s ease-out;
}

.film-card:hover .card-caption,
.film-card:focus-visible .card-caption {
  opacity: 1;
  transform: translateY(0);
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

  .frame {
    left: 8%;
    width: 56%;
  }

  .nav {
    opacity: 1;
  }

  /* 触屏无悬停：说明行常显 */
  .card-caption {
    opacity: 1;
    transform: none;
  }
}
</style>
