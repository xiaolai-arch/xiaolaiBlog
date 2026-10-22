<template>
  <div class="music-card">
    <!-- 深色播放面：参考 liumichelle/library 的底部播放条，浓缩成一张卡 -->
    <div class="player" @click="togglePlay">
      <div class="disc" :class="{ spinning: playing }" aria-hidden="true">
        <img v-if="meta.cover" class="disc-cover" :src="meta.cover" alt="" />
        <span class="disc-hole"></span>
      </div>

      <div class="info">
        <p class="title-row">
          <span class="song-title">{{ meta.title }}</span>
          <span class="eq" aria-hidden="true"><i></i><i></i><i></i><i></i></span>
        </p>
        <p class="artist">{{ meta.artist }} • {{ meta.album }}</p>
        <Transition name="lyric" mode="out-in">
          <p class="lyric" :key="lyricIndex">{{ currentLyric }}</p>
        </Transition>
      </div>

      <div class="ctrl" @click.stop>
        <button
          class="play-btn"
          :aria-label="playing ? '暂停' : '播放'"
          @click="togglePlay"
        >
          <svg v-if="!playing" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M8 5.14v13.72c0 .8.87 1.3 1.56.9l11-6.86a1.05 1.05 0 0 0 0-1.8l-11-6.86A1.05 1.05 0 0 0 8 5.14Z" />
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <rect x="6" y="4" width="4" height="16" rx="1.4" />
            <rect x="14" y="4" width="4" height="16" rx="1.4" />
          </svg>
        </button>

        <span class="time">{{ fmt(cur) }}</span>
        <div
          class="progress"
          ref="progressEl"
          role="slider"
          :aria-valuenow="Math.round(cur)"
          :aria-valuemin="0"
          :aria-valuemax="Math.round(duration)"
          aria-label="播放进度"
          tabindex="0"
          @click="seekByEvent"
          @keydown.left="nudge(-5)"
          @keydown.right="nudge(5)"
        >
          <div class="p-fill" :style="{ width: progressPct + '%' }"></div>
          <div class="p-dot" :style="{ left: progressPct + '%' }"></div>
        </div>
        <span class="time t-total">{{ fmt(duration) }}</span>

        <svg class="vol-icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path d="M4 9.5v5c0 .55.45 1 1 1h3l3.7 3.2c.65.55 1.65.1 1.65-.76V5.06c0-.86-1-1.32-1.65-.76L8 9.5H5c-.55 0-1 .45-1 1Z" />
          <path v-if="volume > 0.05" d="M16 8.6a4.6 4.6 0 0 1 0 6.8 1 1 0 0 0 1.3 1.52 6.6 6.6 0 0 0 0-9.84A1 1 0 0 0 16 8.6Z" />
        </svg>
        <input
          v-model.number="volume"
          class="vol"
          type="range"
          min="0"
          max="1"
          step="0.01"
          aria-label="音量"
          @input="onVolume"
        />
      </div>
    </div>

    <div class="card-caption">
      <p class="caption-title">
        <span>我喜欢的音乐</span>
        <span class="tp-year"> • Now Playing</span>
      </p>
      <span class="caption-song">♪ {{ meta.title }} — {{ meta.artist }}</span>
    </div>

    <audio
      ref="audioEl"
      :src="meta.audio"
      preload="none"
      @loadedmetadata="onMeta"
      @ended="playing = false"
    ></audio>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

interface Lyric {
  t: number
  text: string
}
interface MusicMeta {
  title: string
  artist: string
  album: string
  audio: string
  cover: string
  duration: number
  lyrics: Lyric[]
}

const meta = ref<MusicMeta>({
  title: '…',
  artist: '…',
  album: '',
  audio: '',
  cover: '',
  duration: 0,
  lyrics: [],
})

const audioEl = ref<HTMLAudioElement | null>(null)
const playing = ref(false)
const cur = ref(0)
const duration = ref(0)
const volume = ref(0.8)
const lyricIndex = ref(-1)
const progressEl = ref<HTMLElement | null>(null)

const progressPct = computed(() =>
  duration.value > 0 ? Math.min(100, (cur.value / duration.value) * 100) : 0,
)
const currentLyric = computed(() => {
  const l = meta.value.lyrics[lyricIndex.value]
  return l ? l.text : '♪'
})

function fmt(s: number) {
  if (!isFinite(s) || s < 0) s = 0
  const m = Math.floor(s / 60)
  const sec = Math.floor(s % 60)
  return `${m}:${sec.toString().padStart(2, '0')}`
}

/* ================= 播放控制 ================= */
async function togglePlay() {
  const a = audioEl.value
  if (!a || !a.src) return
  if (a.paused) {
    try {
      await a.play()
    } catch (_) {
      playing.value = false
      return
    }
    playing.value = true
  } else {
    a.pause()
    playing.value = false
  }
}

function onMeta() {
  const d = audioEl.value?.duration
  if (d && isFinite(d)) duration.value = d
  else duration.value = meta.value.duration
}

function onVolume() {
  if (audioEl.value) audioEl.value.volume = volume.value
}

function seekByEvent(e: MouseEvent) {
  const el = progressEl.value
  if (!el || duration.value <= 0) return
  const rect = el.getBoundingClientRect()
  const ratio = Math.min(1, Math.max(0, (e.clientX - rect.left) / rect.width))
  audioEl.value!.currentTime = ratio * duration.value
  cur.value = ratio * duration.value
  syncLyric()
}

function nudge(delta: number) {
  const a = audioEl.value
  if (!a || duration.value <= 0) return
  a.currentTime = Math.min(duration.value, Math.max(0, a.currentTime + delta))
  cur.value = a.currentTime
  syncLyric()
}

/* ================= rAF 进度 + 歌词同步 ================= */
let rafId: number | null = null

function syncLyric() {
  const t = cur.value
  const ls = meta.value.lyrics
  let idx = -1
  for (let i = 0; i < ls.length; i++) {
    if (ls[i].t <= t) idx = i
    else break
  }
  if (idx !== lyricIndex.value) lyricIndex.value = idx
}

function frame() {
  rafId = requestAnimationFrame(frame)
  const a = audioEl.value
  if (!a || a.paused) return
  cur.value = a.currentTime
  syncLyric()
}

onMounted(async () => {
  // 优先读后台"音乐管理"最新上传的一首；没有数据时回退到内置静态歌
  try {
    const res = await fetch('/api/v1/music/current')
    const body = await res.json()
    const m = body?.data
    if (body?.code === 200 && m?.audioUrl) {
      meta.value = {
        title: m.title || '未命名',
        artist: m.artist || '',
        album: m.album || '',
        audio: m.audioUrl,
        cover: m.coverUrl || '',
        duration: m.duration || 0,
        lyrics: parseLrc(m.lyrics || ''),
      }
    } else {
      meta.value = await fetch('/music/memory.json').then(r => r.json())
    }
  } catch (_) {
    try {
      meta.value = await fetch('/music/memory.json').then(r => r.json())
    } catch (_) {
      meta.value.lyrics = []
    }
  }
  if (audioEl.value) audioEl.value.volume = volume.value
  rafId = requestAnimationFrame(frame)
})

/* 解析 LRC 文本为时间轴；过滤空行与超长广告行（如 99:00 标记） */
function parseLrc(text: string): Lyric[] {
  const lines: Lyric[] = []
  for (const raw of text.split('\n')) {
    const m = raw.match(/^\s*\[(\d+):(\d+(?:\.\d+)?)\](.*)$/)
    if (!m) continue
    const t = parseInt(m[1]) * 60 + parseFloat(m[2])
    const textPart = m[3].trim()
    if (!textPart || t > 3600) continue
    lines.push({ t, text: textPart })
  }
  return lines.sort((a, b) => a.t - b.t)
}

onBeforeUnmount(() => {
  if (rafId !== null) cancelAnimationFrame(rafId)
  audioEl.value?.pause()
})
</script>

<style scoped>
.music-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  animation: musicCardEnter 450ms cubic-bezier(0.25, 0.46, 0.45, 0.94) 240ms both;
}

/* 深色播放面：浅色页面里的视觉锚点（同参考站） */
.player {
  position: relative;
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 26px 32px;
  border-radius: 26px;
  overflow: hidden;
  cursor: pointer;
  isolation: isolate;
  color: #f4f4f5;
  background:
    radial-gradient(120% 160% at 12% -10%, rgba(96, 165, 250, 0.14), transparent 46%),
    radial-gradient(120% 150% at 88% 120%, rgba(244, 114, 182, 0.1), transparent 44%),
    linear-gradient(150deg, #27272a, #18181b 62%, #1c1c20);
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.06),
    0 12px 34px rgba(24, 24, 27, 0.16);
}

.player::after {
  content: '';
  position: absolute;
  inset: 0;
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 26px;
  pointer-events: none;
}

/* ============ 黑胶 ============ */
.disc {
  position: relative;
  flex: none;
  width: 104px;
  height: 104px;
  border-radius: 50%;
  background:
    repeating-radial-gradient(circle, #0c0c0e 0 1.5px, #17171b 1.5px 3.2px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.45), inset 0 0 0 1px rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: discSpin 9s linear infinite;
  animation-play-state: paused;
}

.disc.spinning {
  animation-play-state: running;
}

.disc-cover {
  width: 62%;
  height: 62%;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.55);
  pointer-events: none;
}

.disc-hole {
  position: absolute;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #e4e4e7;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.5);
}

@keyframes discSpin {
  to {
    transform: rotate(360deg);
  }
}

/* ============ 歌曲信息 ============ */
.info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.song-title {
  font-family: var(--font-display);
  font-size: 21px;
  font-weight: 600;
  letter-spacing: 0.0125em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.artist {
  font-size: 13px;
  color: #a1a1aa;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.lyric {
  margin-top: 6px;
  font-size: 14px;
  line-height: 1.5;
  color: #d4d4d8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-height: 21px;
}

.lyric-enter-active,
.lyric-leave-active {
  transition: opacity 0.4s ease, transform 0.4s ease;
}

.lyric-enter-from {
  opacity: 0;
  transform: translateY(5px);
}

.lyric-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}

/* ============ 均衡器 ============ */
.eq {
  flex: none;
  display: flex;
  align-items: flex-end;
  gap: 2.5px;
  height: 14px;
}

.eq i {
  width: 3px;
  border-radius: 2px;
  background: #60a5fa;
  animation: eqBar 0.9s ease-in-out infinite;
  animation-play-state: paused;
  height: 30%;
}

.eq i:nth-child(2) { animation-delay: 0.18s; background: #f472b6; }
.eq i:nth-child(3) { animation-delay: 0.36s; background: #a78bfa; }
.eq i:nth-child(4) { animation-delay: 0.54s; background: #34d399; }

.disc.spinning ~ .info .eq i {
  animation-play-state: running;
}

@keyframes eqBar {
  0%, 100% { height: 26%; }
  50% { height: 100%; }
}

/* ============ 控制区 ============ */
.ctrl {
  flex: none;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: default;
}

.play-btn {
  flex: none;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: #f4f4f5;
  color: #18181b;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.25s;
}

.play-btn:hover {
  transform: scale(1.06);
  box-shadow: 0 6px 20px rgba(255, 255, 255, 0.18);
}

.play-btn svg {
  width: 19px;
  height: 19px;
}

.time {
  font-size: 12px;
  color: #a1a1aa;
  font-variant-numeric: tabular-nums;
  min-width: 34px;
  text-align: center;
}

.progress {
  position: relative;
  width: 210px;
  height: 18px;
  display: flex;
  align-items: center;
  cursor: pointer;
  outline: none;
}

.progress::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  height: 4px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
}

.p-fill {
  position: absolute;
  left: 0;
  height: 4px;
  border-radius: 999px;
  background: linear-gradient(90deg, #60a5fa, #a78bfa);
  max-width: 100%;
}

.p-dot {
  position: absolute;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.4);
  transform: translateX(-50%);
  transition: box-shadow 0.2s;
}

.progress:hover .p-dot,
.progress:focus-visible .p-dot {
  box-shadow: 0 0 0 5px rgba(96, 165, 250, 0.25);
}

.vol-icon {
  flex: none;
  width: 17px;
  height: 17px;
  color: #a1a1aa;
}

.vol {
  width: 78px;
  accent-color: #60a5fa;
  cursor: pointer;
}

/* ============ 说明行（与其他卡同规格） ============ */
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

.caption-song {
  margin-left: auto;
  color: #60a5fa;
  opacity: 0;
  transition: opacity 0.3s ease-out;
}

.music-card:hover .caption-song {
  opacity: 1;
}

/* ============ 动画 ============ */
@keyframes musicCardEnter {
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
  .music-card {
    animation: none;
  }
  .disc {
    animation: none;
  }
  .eq i {
    animation: none;
  }
  .lyric-enter-active,
  .lyric-leave-active {
    transition: none;
  }
}

/* ============ 移动端 ============ */
@media (max-width: 768px) {
  .player {
    flex-wrap: wrap;
    gap: 18px;
    padding: 20px 22px;
  }

  .info {
    flex: 1 1 calc(100% - 122px);
  }

  .song-title {
    font-size: 18px;
  }

  .ctrl {
    flex: 1 1 100%;
    justify-content: space-between;
  }

  .progress {
    flex: 1;
    width: auto;
  }

  .vol {
    width: 64px;
  }

  .caption-song {
    opacity: 1;
  }
}
</style>
