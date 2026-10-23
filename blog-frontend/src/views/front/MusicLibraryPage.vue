<template>
  <div class="ml-page">
    <!-- 顶栏 -->
    <header class="ml-top">
      <router-link to="/" class="ml-back">← 回首页</router-link>
      <span class="ml-brand">Library</span>
    </header>

    <!-- 页头 + 书架：同一内容宽度，聚拢居中 -->
    <div class="ml-shelf-wrap">
      <div class="ml-head">
        <h1 class="ml-title">我喜欢的音乐</h1>
        <span class="ml-count">{{ songs.length }} 首</span>
      </div>

      <!-- 书架墙（复刻 BookCard/LibraryPage 网格） -->
      <div v-if="loading" class="ml-empty">加载中…</div>
      <div v-else-if="!songs.length" class="ml-empty">
        <p>书架还是空的</p>
        <p class="sub">后台「音乐」里上传后展示在这里</p>
      </div>
      <div v-else class="ml-shelf">
        <button
          v-for="(s, i) in songs"
          :key="s.id ?? i"
          class="ml-book"
          :class="{ playing: i === current }"
          :aria-label="`播放 ${s.title}`"
          @click="playAt(i)"
        >
          <div class="ml-cover">
            <img v-if="s.cover" :src="s.cover" :alt="s.title" loading="lazy" />
            <span v-else class="ml-cover-fallback">{{ s.title.slice(0, 1) }}</span>
            <span v-if="i === current" class="ml-playing-badge">
              <i v-for="n in 3" :key="n" :style="{ animationDelay: (n - 1) * 0.18 + 's' }"></i>
            </span>
          </div>
          <div class="ml-meta">
            <p class="ml-book-title">{{ s.title }}</p>
            <p class="ml-book-artist">{{ s.artist || '未知歌手' }}</p>
            <p class="ml-book-extra">{{ fmt(s.duration) }}</p>
          </div>
        </button>
      </div>
    </div>

    <!-- 底部播放条 -->
    <div v-if="songs.length" class="ml-bar" :class="{ lifted: barLifted }">
      <img v-if="cur.cover" class="ml-bar-cover" :src="cur.cover" alt="" />
      <div v-else class="ml-bar-cover ml-bar-cover-empty">♪</div>
      <div class="ml-bar-info">
        <p class="ml-bar-title">{{ cur.title }}</p>
        <p class="ml-bar-artist">{{ cur.artist }}</p>
      </div>
      <button class="ml-ctrl" aria-label="上一首" :disabled="songs.length < 2" @click="skip(-1)">‹</button>
      <button class="ml-play" :aria-label="playing ? '暂停' : '播放'" @click="togglePlay">
        <svg v-if="!playing" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path d="M8 5.14v13.72c0 .8.87 1.3 1.56.9l11-6.86a1.05 1.05 0 0 0 0-1.8l-11-6.86A1.05 1.05 0 0 0 8 5.14Z" />
        </svg>
        <svg v-else viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <rect x="6" y="4" width="4" height="16" rx="1.4" />
          <rect x="14" y="4" width="4" height="16" rx="1.4" />
        </svg>
      </button>
      <button class="ml-ctrl" aria-label="下一首" :disabled="songs.length < 2" @click="skip(1)">›</button>
      <span class="ml-time">{{ fmt(curTime) }}</span>
      <div class="ml-progress" ref="progressEl" @click="seek">
        <div class="ml-p-fill" :style="{ width: pct + '%' }"></div>
      </div>
      <span class="ml-time">{{ fmt(cur.duration) }}</span>
    </div>

    <audio ref="audioEl" preload="none" @loadedmetadata="onMeta" @ended="onEnded"></audio>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

interface Song {
  id?: number
  title: string
  artist?: string
  album?: string
  audio: string
  cover?: string
  duration?: number
}

const songs = ref<Song[]>([])
const loading = ref(true)
const current = ref(0)
const playing = ref(false)
const curTime = ref(0)
const curDuration = ref(0)
const audioEl = ref<HTMLAudioElement | null>(null)
const progressEl = ref<HTMLElement | null>(null)

const cur = computed<Song>(() => songs.value[current.value] || { title: '', artist: '', audio: '', duration: 0 })
const pct = computed(() => (curDuration.value > 0 ? Math.min(100, (curTime.value / curDuration.value) * 100) : 0))
/* 首次播放后底部播放条升起 */
const barLifted = ref(false)

function fmt(s?: number) {
  if (!s || !isFinite(s) || s < 0) s = 0
  const m = Math.floor(s / 60)
  const sec = Math.floor(s % 60)
  return `${m}:${sec.toString().padStart(2, '0')}`
}

function loadSong(i: number, autoplay: boolean) {
  const a = audioEl.value
  if (!a || !songs.value.length) return
  current.value = ((i % songs.value.length) + songs.value.length) % songs.value.length
  curTime.value = 0
  curDuration.value = cur.value.duration || 0
  a.src = cur.value.audio
  if (autoplay) {
    a.play().then(() => (playing.value = true)).catch(() => (playing.value = false))
    barLifted.value = true
  }
}

function playAt(i: number) {
  if (i === current.value && audioEl.value?.src) {
    togglePlay()
    return
  }
  loadSong(i, true)
}

async function togglePlay() {
  const a = audioEl.value
  if (!a || !a.src) {
    loadSong(current.value, true)
    return
  }
  if (a.paused) {
    try {
      await a.play()
      playing.value = true
      barLifted.value = true
    } catch (_) {
      playing.value = false
    }
  } else {
    a.pause()
    playing.value = false
  }
}

function skip(dir: 1 | -1) {
  loadSong(current.value + dir, playing.value || !!audioEl.value?.src)
}

function onMeta() {
  const d = audioEl.value?.duration
  if (d && isFinite(d)) curDuration.value = d
}

function onEnded() {
  playing.value = false
  loadSong(current.value + 1, true)
}

function seek(e: MouseEvent) {
  const el = progressEl.value
  const a = audioEl.value
  if (!el || !a || curDuration.value <= 0) return
  const rect = el.getBoundingClientRect()
  const ratio = Math.min(1, Math.max(0, (e.clientX - rect.left) / rect.width))
  a.currentTime = ratio * curDuration.value
  curTime.value = ratio * curDuration.value
}

let rafId: number | null = null
function frame() {
  rafId = requestAnimationFrame(frame)
  const a = audioEl.value
  if (a && !a.paused) curTime.value = a.currentTime
}

onMounted(async () => {
  try {
    const res = await fetch('/api/v1/music/list')
    const body = await res.json()
    if (body?.code === 200 && Array.isArray(body.data)) {
      songs.value = body.data.map((m: any) => ({
        id: m.id,
        title: m.title || '未命名',
        artist: m.artist || '',
        album: m.album || '',
        audio: m.audioUrl,
        cover: m.coverUrl || '',
        duration: m.duration || 0,
      }))
    }
  } catch (_) {}
  loading.value = false
  loadSong(0, false)
  rafId = requestAnimationFrame(frame)
})

onBeforeUnmount(() => {
  if (rafId !== null) cancelAnimationFrame(rafId)
  audioEl.value?.pause()
})
</script>

<style scoped>
.ml-page {
  min-height: 100vh;
  background: #fff;
  padding: 0 64px 150px;
}

/* 书架不摊满全宽：参考站在书少时同样聚拢居中 */
.ml-shelf-wrap {
  max-width: 1080px;
  margin: 0 auto;
}

[data-theme='dark'] .ml-page {
  background: #1c1c20;
}

/* ============ 顶栏 ============ */
.ml-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 0 0;
}

.ml-back {
  font-size: 13.5px;
  color: #71717a !important;
  text-decoration: none;
  transition: color 0.2s;
}

.ml-back:hover {
  color: #18181b !important;
}

[data-theme='dark'] .ml-back {
  color: #8d8a96 !important;
}

[data-theme='dark'] .ml-back:hover {
  color: #e4e4e7 !important;
}

.ml-brand {
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #18181b;
}

[data-theme='dark'] .ml-brand {
  color: #e4e4e7;
}

/* ============ 页头 ============ */
.ml-head {
  display: flex;
  align-items: baseline;
  gap: 14px;
  padding: 48px 0 36px;
}

.ml-title {
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 600;
  color: #18181b;
  margin: 0;
}

[data-theme='dark'] .ml-title {
  color: #e4e4e7;
}

.ml-count {
  font-size: 14px;
  color: #a1a1aa;
}

/* ============ 书架（网格复刻：4/5/6 列，行距同参考站 ~135px） ============ */
.ml-shelf {
  display: grid;
  grid-template-columns: repeat(4, 148px);
  justify-content: space-between;
  row-gap: 136px;
  padding-top: 8px;
}

@media (min-width: 1024px) {
  .ml-shelf {
    grid-template-columns: repeat(5, 160px);
  }
}

@media (min-width: 1280px) {
  .ml-shelf {
    grid-template-columns: repeat(6, 160px);
  }
}

.ml-book {
  position: relative;
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
  aspect-ratio: 2 / 3;
}

/* 书封：2:3、圆角、阴影；悬停上提微放大、阴影消失 */
.ml-cover {
  position: absolute;
  inset: 0;
  border-radius: 3px;
  overflow: hidden;
  background: linear-gradient(150deg, #e4e4e7, #d4d4d8 60%, #c9c9ce);
  box-shadow: 0 4px 14px rgba(24, 24, 27, 0.14);
  transition: transform 0.4s ease-out, box-shadow 0.4s ease-out;
}

[data-theme='dark'] .ml-cover {
  background: linear-gradient(150deg, #2c2c31, #26262b 60%, #202024);
}

.ml-book:hover .ml-cover,
.ml-book:focus-visible .ml-cover {
  transform: translateY(-16px) scale(1.035);
  box-shadow: none;
}

.ml-cover img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ml-cover-fallback {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-size: 34px;
  font-weight: 700;
  color: rgba(24, 24, 27, 0.35);
}

/* 当前播放标记：封面右下角均衡器 */
.ml-playing-badge {
  position: absolute;
  right: 8px;
  bottom: 8px;
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 12px;
  padding: 3px 5px;
  border-radius: 999px;
  background: rgba(24, 24, 27, 0.55);
  backdrop-filter: blur(4px);
}

.ml-playing-badge i {
  width: 2.5px;
  border-radius: 1px;
  background: #60a5fa;
  animation: mlEq 0.9s ease-in-out infinite;
  height: 30%;
}

@keyframes mlEq {
  0%, 100% { height: 26%; }
  50% { height: 100%; }
}

/* 悬停浮现的文字（同源码：封面下方居中） */
.ml-meta {
  position: absolute;
  top: calc(100% + 6px);
  left: 50%;
  transform: translateX(-50%);
  width: 160px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 1px;
  opacity: 0;
  transition: opacity 0.4s ease-out;
  pointer-events: none;
}

.ml-book:hover .ml-meta,
.ml-book:focus-visible .ml-meta,
.ml-book.playing .ml-meta {
  opacity: 1;
}

.ml-book-title {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
  line-height: 1.35;
  color: #18181b;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ml-book-artist {
  margin: 0;
  font-size: 14px;
  color: #71717a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.ml-book-extra {
  margin: 2px 0 0;
  font-size: 14px;
  color: #a1a1aa;
  font-variant-numeric: tabular-nums;
}

[data-theme='dark'] .ml-book-title {
  color: #e4e4e7;
}

[data-theme='dark'] .ml-book-artist {
  color: #8d8a96;
}

/* ============ 空态 ============ */
.ml-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  gap: 6px;
  color: #a1a1aa;
  font-size: 15px;
}

.ml-empty .sub {
  font-size: 12.5px;
  opacity: 0.8;
}

/* ============ 底部播放条 ============ */
.ml-bar {
  position: fixed;
  left: 50%;
  bottom: 20px;
  transform: translate(-50%, 140%);
  display: flex;
  align-items: center;
  gap: 14px;
  width: min(720px, calc(100vw - 40px));
  padding: 10px 18px;
  border-radius: 18px;
  background: rgba(24, 24, 27, 0.94);
  backdrop-filter: blur(12px);
  box-shadow: 0 14px 40px rgba(24, 24, 27, 0.35);
  color: #f4f4f5;
  transition: transform 0.55s cubic-bezier(0.22, 1, 0.36, 1);
  z-index: 30;
}

.ml-bar.lifted {
  transform: translate(-50%, 0);
}

.ml-bar-cover {
  flex: none;
  width: 44px;
  height: 44px;
  border-radius: 8px;
  object-fit: cover;
}

.ml-bar-cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.08);
  color: #a1a1aa;
  font-size: 18px;
}

.ml-bar-info {
  flex: 1;
  min-width: 0;
}

.ml-bar-title {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ml-bar-artist {
  margin: 1px 0 0;
  font-size: 12px;
  color: #a1a1aa;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ml-ctrl {
  flex: none;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #a1a1aa;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  transition: color 0.2s, transform 0.2s;
}

.ml-ctrl:hover:not(:disabled) {
  color: #f4f4f5;
  transform: scale(1.12);
}

.ml-ctrl:disabled {
  opacity: 0.3;
  cursor: default;
}

.ml-play {
  flex: none;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: #f4f4f5;
  color: #18181b;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1);
}

.ml-play:hover {
  transform: scale(1.07);
}

.ml-play svg {
  width: 17px;
  height: 17px;
}

.ml-time {
  flex: none;
  font-size: 11.5px;
  color: #a1a1aa;
  font-variant-numeric: tabular-nums;
  min-width: 32px;
  text-align: center;
}

.ml-progress {
  position: relative;
  flex: none;
  width: 120px;
  height: 14px;
  display: flex;
  align-items: center;
  cursor: pointer;
}

.ml-progress::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  height: 3px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
}

.ml-p-fill {
  position: absolute;
  left: 0;
  height: 3px;
  border-radius: 999px;
  background: #60a5fa;
  max-width: 100%;
}

/* ============ 移动端 ============ */
@media (max-width: 640px) {
  .ml-page {
    padding: 0 20px 140px;
  }

  .ml-shelf-wrap {
    max-width: none;
  }

  .ml-title {
    font-size: 24px;
  }

  .ml-head {
    padding: 32px 0 28px;
  }

  .ml-shelf {
    grid-template-columns: repeat(3, 1fr);
    column-gap: 14px;
    row-gap: 96px;
  }

  .ml-meta {
    width: 100%;
  }

  .ml-progress {
    display: none;
  }
}
</style>
