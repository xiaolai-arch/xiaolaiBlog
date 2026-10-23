<template>
  <div class="music-card" @click="enter" role="link" aria-label="进入音乐书架页" tabindex="0" @keydown.enter="enter">
    <div class="player">
      <!-- 当前曲：迷你黑胶 + 信息 + 歌词 -->
      <div class="now" @click.stop="togglePlay">
        <div class="disc" :class="{ spinning: playing }" aria-hidden="true">
          <img v-if="meta.cover" class="disc-cover" :src="meta.cover" alt="" />
          <span class="disc-hole"></span>
        </div>
        <div class="now-info">
          <p class="title-row">
            <span class="song-title">{{ meta.title }}</span>
            <span class="eq" aria-hidden="true"><i></i><i></i><i></i><i></i></span>
          </p>
          <p class="artist">{{ meta.artist }}<template v-if="meta.album"> • {{ meta.album }}</template></p>
          <Transition name="lyric" mode="out-in">
            <p class="lyric" :key="lyricIndex + '-' + songIndex">{{ currentLyric }}</p>
          </Transition>
        </div>
      </div>

      <!-- 歌单：点击切歌 -->
      <ul class="queue">
        <li
          v-for="(s, i) in songs"
          :key="i"
          :class="{ active: i === songIndex }"
          :title="s.title"
          @click.stop="playAt(i)"
        >
          <span class="q-index">{{ i === songIndex && playing ? '♪' : i + 1 }}</span>
          <span class="q-title">{{ s.title }}</span>
          <span class="q-artist">{{ s.artist }}</span>
        </li>
      </ul>

      <!-- 控制行 -->
      <div class="ctrl" @click.stop>
        <button class="skip-btn" aria-label="上一首" :disabled="songs.length < 2" @click="skip(-1)">
          <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M7 6c0-.55.45-1 1-1s1 .45 1 1v12c0 .55-.45 1-1 1s-1-.45-1-1V6Zm3.6 6.5 8.1-5.6c.66-.46 1.57.01 1.57.82v11.13c0 .8-.9 1.28-1.57.82l-8.1-5.6a1 1 0 0 1 0-1.57Z" />
          </svg>
        </button>
        <button class="play-btn" :aria-label="playing ? '暂停' : '播放'" @click="togglePlay">
          <svg v-if="!playing" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M8 5.14v13.72c0 .8.87 1.3 1.56.9l11-6.86a1.05 1.05 0 0 0 0-1.8l-11-6.86A1.05 1.05 0 0 0 8 5.14Z" />
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <rect x="6" y="4" width="4" height="16" rx="1.4" />
            <rect x="14" y="4" width="4" height="16" rx="1.4" />
          </svg>
        </button>
        <button class="skip-btn" aria-label="下一首" :disabled="songs.length < 2" @click="skip(1)">
          <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M17 6c0-.55-.45-1-1-1s-1 .45-1 1v12c0 .55.45 1 1 1s1-.45 1-1V6Zm-11.27 5.9 8.1 5.6c.66.46 1.57-.01 1.57-.82V5.55c0-.8-.9-1.28-1.57-.82l-8.1 5.6a1 1 0 0 0 0 1.57Z" />
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
      </div>
    </div>

    <div class="card-caption">
      <p class="caption-title">
        <span>我喜欢的音乐</span>
        <span class="tp-year"> • {{ songs.length }} 首</span>
      </p>
      <span class="caption-song">♪ {{ meta.title }} · 进入 Library →</span>
    </div>

    <audio ref="audioEl" preload="none" @loadedmetadata="onMeta" @ended="onEnded"></audio>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

/* 点击整卡进入音乐书架页 */
function enter() {
  router.push('/music')
}

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

/* 歌单：默认占位，onMounted 里被后台数据或内置静态歌替换 */
const songs = ref<MusicMeta[]>([])
const songIndex = ref(0)
const meta = computed<MusicMeta>(
  () =>
    songs.value[songIndex.value] || {
      title: '…',
      artist: '',
      album: '',
      audio: '',
      cover: '',
      duration: 0,
      lyrics: [],
    },
)

const audioEl = ref<HTMLAudioElement | null>(null)
const playing = ref(false)
const cur = ref(0)
const duration = ref(0)
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
function resetProgress() {
  cur.value = 0
  duration.value = meta.value.duration || 0
  lyricIndex.value = -1
}

function loadSong(index: number, autoplay: boolean) {
  const a = audioEl.value
  if (!a || !songs.value.length) return
  songIndex.value = (index + songs.value.length) % songs.value.length
  resetProgress()
  a.src = meta.value.audio
  if (autoplay) {
    a.play()
      .then(() => {
        playing.value = true
      })
      .catch(() => {
        playing.value = false
      })
  }
}

function playAt(index: number) {
  if (index === songIndex.value) {
    togglePlay()
    return
  }
  loadSong(index, true)
}

function skip(dir: 1 | -1) {
  if (songs.value.length < 2) return
  loadSong(songIndex.value + dir, playing.value)
}

async function togglePlay() {
  const a = audioEl.value
  if (!a || !a.src) return
  if (a.paused) {
    try {
      await a.play()
      playing.value = true
    } catch (_) {
      playing.value = false
    }
  } else {
    a.pause()
    playing.value = false
  }
}

function onMeta() {
  const d = audioEl.value?.duration
  if (d && isFinite(d)) duration.value = d
}

function onEnded() {
  playing.value = false
  loadSong(songIndex.value + 1, true)
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

interface ApiMusic {
  title: string
  artist?: string
  album?: string
  audioUrl: string
  coverUrl?: string
  lyrics?: string
  duration?: number
}

function toMeta(m: ApiMusic): MusicMeta {
  return {
    title: m.title || '未命名',
    artist: m.artist || '',
    album: m.album || '',
    audio: m.audioUrl,
    cover: m.coverUrl || '',
    duration: m.duration || 0,
    lyrics: parseLrc(m.lyrics || ''),
  }
}

const FALLBACK_JSON = '/music/memory.json'

onMounted(async () => {
  // 优先读后台"音乐管理"的歌单；没有数据时回退到内置静态歌
  try {
    const res = await fetch('/api/v1/music/list')
    const body = await res.json()
    const list: ApiMusic[] = body?.data || []
    if (body?.code === 200 && list.length) {
      songs.value = list.map(toMeta)
    } else {
      songs.value = [await fetch(FALLBACK_JSON).then(r => r.json())]
    }
  } catch (_) {
    try {
      songs.value = [await fetch(FALLBACK_JSON).then(r => r.json())]
    } catch (_) {
      songs.value = []
    }
  }
  loadSong(0, false)
  rafId = requestAnimationFrame(frame)
})

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
  cursor: pointer;
  animation: musicCardEnter 450ms cubic-bezier(0.25, 0.46, 0.45, 0.94) 240ms both;
}

/* 深色播放面：与动漫卡并排的一对小组件 */
.player {
  position: relative;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 300px;
  padding: 18px 20px 14px;
  border-radius: 26px;
  overflow: hidden;
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

/* ============ 当前曲 ============ */
.now {
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  min-width: 0;
}

.disc {
  position: relative;
  flex: none;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: repeating-radial-gradient(circle, #0c0c0e 0 1.5px, #17171b 1.5px 3.2px);
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
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #e4e4e7;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.5);
}

@keyframes discSpin {
  to {
    transform: rotate(360deg);
  }
}

.now-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.song-title {
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.0125em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.artist {
  font-size: 12px;
  color: #a1a1aa;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.lyric {
  font-size: 12px;
  line-height: 1.5;
  color: #d4d4d8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-height: 18px;
}

.lyric-enter-active,
.lyric-leave-active {
  transition: opacity 0.4s ease, transform 0.4s ease;
}

.lyric-enter-from {
  opacity: 0;
  transform: translateY(4px);
}

.lyric-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ============ 均衡器 ============ */
.eq {
  flex: none;
  display: flex;
  align-items: flex-end;
  gap: 2.5px;
  height: 13px;
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

.disc.spinning ~ .now-info .eq i {
  animation-play-state: running;
}

@keyframes eqBar {
  0%, 100% { height: 26%; }
  50% { height: 100%; }
}

/* ============ 歌单 ============ */
.queue {
  list-style: none;
  margin: 12px 0 10px;
  padding: 0 2px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  scrollbar-width: none;
}

.queue::-webkit-scrollbar {
  width: 0;
}

.queue li {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 5px 6px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s ease-out;
}

.queue li:hover {
  background: rgba(255, 255, 255, 0.07);
}

.queue li.active {
  background: rgba(96, 165, 250, 0.12);
}

.q-index {
  flex: none;
  width: 18px;
  font-size: 11px;
  color: #71717a;
  font-variant-numeric: tabular-nums;
  text-align: center;
}

.queue li.active .q-index {
  color: #60a5fa;
}

.q-title {
  flex: 0 1 auto;
  min-width: 0;
  font-size: 13px;
  font-weight: 500;
  color: #e4e4e7;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.q-artist {
  flex: 1 1 0;
  min-width: 0;
  font-size: 11px;
  color: #71717a;
  text-align: right;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ============ 控制行 ============ */
.ctrl {
  flex: none;
  display: flex;
  align-items: center;
  gap: 10px;
}

.skip-btn {
  flex: none;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: #a1a1aa;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: color 0.2s, transform 0.2s;
}

.skip-btn:hover:not(:disabled) {
  color: #f4f4f5;
  transform: scale(1.1);
}

.skip-btn:disabled {
  opacity: 0.3;
  cursor: default;
}

.skip-btn svg {
  width: 16px;
  height: 16px;
}

.play-btn {
  flex: none;
  width: 38px;
  height: 38px;
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
  width: 16px;
  height: 16px;
}

.time {
  flex: none;
  font-size: 11px;
  color: #a1a1aa;
  font-variant-numeric: tabular-nums;
  min-width: 30px;
  text-align: center;
}

.progress {
  position: relative;
  flex: 1;
  height: 16px;
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
  height: 3.5px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
}

.p-fill {
  position: absolute;
  left: 0;
  height: 3.5px;
  border-radius: 999px;
  background: linear-gradient(90deg, #60a5fa, #a78bfa);
  max-width: 100%;
}

.p-dot {
  position: absolute;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.4);
  transform: translateX(-50%);
  transition: box-shadow 0.2s;
}

.progress:hover .p-dot,
.progress:focus-visible .p-dot {
  box-shadow: 0 0 0 4px rgba(96, 165, 250, 0.25);
}

/* ============ 说明行 ============ */
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
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 55%;
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
  .queue li:nth-child(n + 5) {
    display: none;
  }

  .caption-song {
    opacity: 1;
  }
}
</style>
