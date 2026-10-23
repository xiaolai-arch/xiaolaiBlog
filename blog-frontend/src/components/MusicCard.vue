<template>
  <div class="music-card" @click="enter" role="link" aria-label="进入音乐书架页" tabindex="0" @keydown.enter="enter">
    <!-- 纯展示：迷你书架，聚光灯自动轮播，无播放控件 -->
    <div class="showcase">
      <div class="mini-shelf">
        <div
          v-for="(s, i) in displaySongs"
          :key="s.id ?? i"
          class="mini-book"
          :class="{ featured: i === featured }"
        >
          <img v-if="s.cover" :src="s.cover" :alt="s.title" loading="lazy" />
          <span v-else class="mini-fallback">{{ s.title.slice(0, 1) }}</span>
          <span v-if="i === featured" class="mini-eq" aria-hidden="true">
            <i v-for="n in 3" :key="n" :style="{ animationDelay: (n - 1) * 0.18 + 's' }"></i>
          </span>
        </div>
      </div>

      <Transition name="songfade" mode="out-in">
        <p class="featured-song" :key="featured">
          <span class="f-title">{{ featuredSong.title }}</span>
          <span class="f-artist">{{ featuredSong.artist }}</span>
        </p>
      </Transition>

      <span class="enter-hint">进入 Library ↗</span>
    </div>

    <div class="card-caption">
      <p class="caption-title">
        <span>我喜欢的音乐</span>
        <span class="tp-year"> • {{ songs.length }} 首</span>
      </p>
      <span class="caption-more">进入 Library →</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'

interface Song {
  id?: number
  title: string
  artist?: string
  cover?: string
}

const router = useRouter()

function enter() {
  router.push('/music')
}

const songs = ref<Song[]>([])
const featured = ref(0)

/* 书架展示全部歌曲（最多 8 本，4×2 小书封，同图书页的密集书架感） */
const displaySongs = computed(() => songs.value.slice(0, 8))
const featuredSong = computed(
  () => displaySongs.value[featured.value] || { title: '', artist: '' },
)

let timer: number | null = null

onMounted(async () => {
  try {
    const res = await fetch('/api/v1/music/list')
    const body = await res.json()
    if (body?.code === 200 && Array.isArray(body.data)) {
      songs.value = body.data.map((m: any) => ({
        id: m.id,
        title: m.title || '未命名',
        artist: m.artist || '',
        cover: m.coverUrl || '',
      }))
    }
  } catch (_) {}
  if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches && displaySongs.value.length > 1) {
    timer = window.setInterval(() => {
      featured.value = (featured.value + 1) % displaySongs.value.length
    }, 3200)
  }
})

onBeforeUnmount(() => {
  if (timer !== null) window.clearInterval(timer)
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

/* 浅色展示面（与胶片图集卡同系的纸感浅底） */
.showcase {
  position: relative;
  flex: 1;
  min-height: 300px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 20px;
  padding: 20px 22px;
  border-radius: 26px;
  overflow: hidden;
  isolation: isolate;
  color: #18181b;
  background: linear-gradient(175deg, #fbfbfa, #f6f6f4 55%, #f2f2ef);
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.04),
    0 12px 34px rgba(24, 24, 27, 0.08);
}

[data-theme='dark'] .showcase {
  color: #f4f4f5;
  background: linear-gradient(175deg, #232326, #1f1f22 55%, #26262a);
}

.showcase::after {
  content: '';
  position: absolute;
  inset: 0;
  border: 1px solid rgba(24, 24, 27, 0.06);
  border-radius: 26px;
  pointer-events: none;
}

[data-theme='dark'] .showcase::after {
  border-color: rgba(255, 255, 255, 0.07);
}

/* ============ 迷你书架：4×2 小书封（同 /music 书架的密集感） ============ */
.mini-shelf {
  display: grid;
  grid-template-columns: repeat(4, 104px);
  justify-content: center;
  gap: 18px 12px;
}

.mini-book {
  position: relative;
  aspect-ratio: 2 / 3;
  border-radius: 2.5px;
  overflow: hidden;
  background: linear-gradient(150deg, #e4e4e7, #d4d4d8 60%, #c9c9ce);
  box-shadow: 0 3px 10px rgba(24, 24, 27, 0.14);
  opacity: 0.82;
  transform: translateY(0);
  transition:
    opacity 0.6s ease,
    transform 0.6s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.6s ease;
}

[data-theme='dark'] .mini-book {
  background: linear-gradient(150deg, #2c2c31, #26262b 60%, #202024);
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.35);
}

.mini-book img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.mini-fallback {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 700;
  color: rgba(24, 24, 27, 0.35);
}

/* 聚光灯：上提、放大、点亮（浅色下靠白描边+更深投影突出） */
.mini-book.featured {
  opacity: 1;
  transform: translateY(-7px) scale(1.05);
  box-shadow:
    0 0 0 3px #fff,
    0 8px 20px rgba(24, 24, 27, 0.22);
  z-index: 2;
}

[data-theme='dark'] .mini-book.featured {
  box-shadow:
    0 0 0 3px #1c1c20,
    0 9px 22px rgba(0, 0, 0, 0.5);
}

/* 封面右下角均衡器角标 */
.mini-eq {
  position: absolute;
  right: 5px;
  bottom: 5px;
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 9px;
  padding: 2.5px 4px;
  border-radius: 999px;
  background: rgba(24, 24, 27, 0.55);
  backdrop-filter: blur(4px);
}

.mini-eq i {
  width: 2px;
  border-radius: 1px;
  background: #60a5fa;
  animation: miniEq 0.9s ease-in-out infinite;
  height: 30%;
}

@keyframes miniEq {
  0%, 100% { height: 26%; }
  50% { height: 100%; }
}

/* ============ 轮播中的歌曲名 ============ */
.featured-song {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  text-align: center;
  min-height: 40px;
  justify-content: center;
}

.f-title {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 600;
  color: #18181b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

[data-theme='dark'] .f-title {
  color: #f4f4f5;
}

.f-artist {
  font-size: 12.5px;
  color: #a1a1aa;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.songfade-enter-active,
.songfade-leave-active {
  transition: opacity 0.45s ease, transform 0.45s ease;
}

.songfade-enter-from {
  opacity: 0;
  transform: translateY(5px);
}

.songfade-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}

/* 进入提示（悬停浮现，浅色胶囊） */
.enter-hint {
  position: absolute;
  right: 16px;
  top: 14px;
  z-index: 3;
  padding: 4px 12px 3.5px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  color: #3f3f46;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(24, 24, 27, 0.08);
  backdrop-filter: blur(6px);
  opacity: 0;
  transform: translateY(-3px);
  transition: opacity 0.3s ease, transform 0.3s ease;
  pointer-events: none;
}

[data-theme='dark'] .enter-hint {
  color: #e4e4e7;
  background: rgba(39, 39, 46, 0.85);
  border-color: rgba(255, 255, 255, 0.1);
}

.music-card:hover .enter-hint,
.music-card:focus-visible .enter-hint {
  opacity: 1;
  transform: translateY(0);
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

.caption-more {
  margin-left: auto;
  color: #60a5fa;
  opacity: 0;
  transition: opacity 0.3s ease-out;
  white-space: nowrap;
}

.music-card:hover .caption-more {
  opacity: 1;
}

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
  .mini-book {
    transition: none;
  }
  .mini-eq i {
    animation: none;
  }
  .songfade-enter-active,
  .songfade-leave-active {
    transition: none;
  }
}

/* ============ 移动端 ============ */
@media (max-width: 768px) {
  .mini-shelf {
    grid-template-columns: repeat(4, 1fr);
    max-width: 440px;
    width: 100%;
    margin: 0 auto;
  }

  .caption-more {
    opacity: 1;
  }
}
</style>
