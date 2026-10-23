<template>
  <div class="ml-home">
    <!-- ============ PageHeader：渐变 + logo + hero 文案 ============ -->
    <div class="page-header">
      <div class="grain" aria-hidden="true"></div>

      <!-- Hero 文案 -->
      <div class="hero-block">
        <p class="hero-name">咲来</p>
        <div class="hero-desc">
          <span>你好！我是阿咲，一名喜欢通过创造学习的软件工程师。</span>
          <span><br aria-hidden="true" /></span>
          <span>最近我一直在深入学习 Agent 智能体开发和大型语言模型。</span>
        </div>
      </div>

      <!-- ============ NavigationTabs：Work / Art / About ============ -->
      <nav class="tabs">
        <div class="tabs-inner">
          <router-link to="/" class="tab active">
            <span>Work</span>
          </router-link>
          <router-link to="/gallery" class="tab">
            <span>Art</span>
          </router-link>
          <router-link to="/about" class="tab">
            <span>About</span>
          </router-link>
        </div>
        <div class="tabs-divider"></div>
      </nav>
    </div>

    <!-- ============ 项目网格（2 列） ============ -->
    <div class="project-grid">
      <component
        :is="card.to ? 'router-link' : 'div'"
        v-for="(card, i) in cards"
        :key="card.title"
        :to="card.to"
        class="project-card"
        :class="{ featured: card.featured }"
        :style="{ animationDelay: Math.min(i * 60, 300) + 'ms' }"
        @mouseenter="card.live ? (liveHovered[card.to] = true) : undefined"
        @mouseleave="card.live ? (liveHovered[card.to] = false) : undefined"
      >
        <div
          class="media"
          :class="card.grad ? 'grad-' + card.grad : ''"
        >
          <!-- live 卡（3D 美术馆）：媒体区为展厅页实时预览，导览运镜与真实操作一致 -->
          <LivePreview
            v-if="card.live"
            :active="liveReady[card.to] === true"
            :src="card.live"
            :title="card.title + '实时预览'"
            :paused="liveHovered[card.to] === true"
            :preview-w="1280"
            :preview-h="800"
            tour
            :tour-steps="card.tourSteps"
            :drift="false"
          />
          <video
            v-else
            class="media-video"
            :src="card.video"
            autoplay
            muted
            loop
            playsinline
            preload="auto"
            disablepictureinpicture
          ></video>
          <div class="media-ring" aria-hidden="true"></div>
          <div v-if="card.featured" class="title-pill">
            <span class="tp-title">{{ card.title }}</span>
            <span v-if="!card.tryIt" class="tp-year"> • {{ card.year }}</span>
            <template v-if="card.tryIt">
              <span class="tp-dot"> • </span>
              <span class="tp-try">↗ Try It Out!</span>
            </template>
          </div>
        </div>

        <!-- featured：图片下方一行描述 -->
        <div v-if="card.featured" class="card-desc">{{ card.desc }}</div>

        <!-- 非 featured：标题行 + Try It Out -->
        <div v-else class="card-caption">
          <p class="caption-title">
            <span>{{ card.title }}</span>
            <span class="tp-year"> • {{ card.year }}</span>
          </p>
          <span class="caption-try">↗ Try It Out!</span>
        </div>
      </component>

      <!-- Blog 小组件：与项目卡同规格的一块，媒体区为归档页实时滚动预览 -->
      <router-link
        to="/archives"
        class="project-card"
        :style="{ animationDelay: '180ms' }"
        @mouseenter="blogHovered = true"
        @mouseleave="blogHovered = false"
      >
        <div class="media grad-paper">
          <LivePreview
            :active="blogLiveReady"
            src="/archives"
            title="博客文章实时预览"
            :paused="blogHovered"
          />
          <div class="media-ring" aria-hidden="true"></div>
        </div>
        <div class="card-caption">
          <p class="caption-title">
            <span>博客文章</span>
            <span class="tp-year"> • Blog</span>
          </p>
          <span class="caption-try">↗ 全部文章</span>
        </div>
      </router-link>

      <!-- 我喜欢的音乐 + 胶片图集：并排的小组件行 -->
      <MusicCard />
      <FilmCard />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import LivePreview, { type TourStep } from '@/components/LivePreview.vue'
import MusicCard from '@/components/MusicCard.vue'
import FilmCard from '@/components/FilmCard.vue'


/* Blog 卡：延迟挂载 iframe，错开三段演示视频的加载；悬停时暂停滚动便于阅读 */
const blogLiveReady = ref(false)
const blogHovered = ref(false)

/* live 卡（3D 展厅 / 拍立得）：按路由延迟挂载（初始化较重），悬停时暂停导览便于细看 */
const liveReady = reactive<Record<string, boolean>>({})
const liveHovered = reactive<Record<string, boolean>>({})

/* 拍立得卡导览脚本：依次点过七种机身色，循环往复 */
const POLAROID_TOUR_STEPS: TourStep[] = [
  'Red', 'Orange', 'Yellow', 'Green', 'Cyan', 'Blue', 'Purple',
].map((name) => ({ sel: `[aria-label="Select ${name} color"]`, holdMs: 1600 }))

interface HomeCard {
  title: string
  year: string
  desc: string
  to: string
  grad?: string
  /** 预录演示视频（无 live 时使用） */
  video?: string
  /** 实时预览路由（优先于 video） */
  live?: string
  /** 自定义导览脚本（live 卡在 tour 模式下按步点击页内控件） */
  tourSteps?: TourStep[]
  featured: boolean
  tryIt?: boolean
}

const cards: HomeCard[] = [
  {
    title: '3D 美术馆',
    year: 'NEW',
    desc: '',
    to: '/gallery/3d',
    grad: 'sky',
    // 预录 webm 换成展厅页实时预览：画面永远与真实页面一致，运镜走页面自身导览
    live: '/gallery/3d',
    featured: false,
    tryIt: true,
  },
  {
    title: '拍立得工作室',
    year: 'NEW',
    desc: '',
    to: '/toys/polaroid',
    // 实时预览 + 自动换色演示：和真实页面一模一样的定制器在跑
    live: '/toys/polaroid',
    tourSteps: POLAROID_TOUR_STEPS,
    featured: false,
    tryIt: true,
  },
  {
    title: '数字速写本',
    year: 'NEW',
    desc: '',
    to: '/toys/sketchbook',
    grad: 'rose',
    video: '/videos/sketchbook.webm',
    featured: false,
    tryIt: true,
  },
]

onMounted(() => {
  window.setTimeout(() => {
    blogLiveReady.value = true
  }, 600)
  window.setTimeout(() => {
    liveReady['/gallery/3d'] = true
  }, 1200)
  window.setTimeout(() => {
    liveReady['/toys/polaroid'] = true
  }, 1800)
})
</script>

<style scoped>
/* ============ PageHeader ============ */
.page-header {
  position: relative;
  background: linear-gradient(
    190deg,
    #d5e0ff 0%,
    #d5e0ff 8%,
    #e2eaff 15%,
    #f5e2ff 22%,
    #fde9fa 28%,
    #fff5fc 35%,
    #fffeff 42%,
    var(--bg-primary) 55%,
    var(--bg-primary) 100%
  );
  overflow: hidden;
}

[data-theme='dark'] .page-header {
  background: linear-gradient(
    190deg,
    #1c2333 0%,
    #1c2333 10%,
    #232036 20%,
    #2a2138 28%,
    var(--bg-primary) 55%,
    var(--bg-primary) 100%
  );
}

/* 噪点纹理（对标原站 grain overlay） */
.grain {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.5;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='160' height='160'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='2'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3CfeComponentTransfer%3E%3CfeFuncA type='linear' slope='0.05'/%3E%3C/feComponentTransfer%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
}

.hero-block {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 56px 64px 0;
  min-height: 176px;
}

.hero-name {
  font-family: var(--font-display);
  font-size: 36px;
  font-weight: 500;
  line-height: 1.5;
  letter-spacing: 0.0125em;
  color: #3f3f46;
  animation: heroFadeUp 450ms cubic-bezier(0.25, 0.46, 0.45, 0.94) both;
}

[data-theme='dark'] .hero-name {
  color: var(--text-primary);
}

.hero-desc {
  font-family: var(--font-body);
  font-size: 18px;
  font-weight: 400;
  letter-spacing: 0.025em;
  line-height: 1.5;
  color: #a1a1aa;
  margin-top: 4px;
  white-space: pre-wrap;
  animation: heroFadeUp 450ms cubic-bezier(0.25, 0.46, 0.45, 0.94) 120ms both;
}

[data-theme='dark'] .hero-desc {
  color: #8d8a96;
}

.hero-link {
  color: #71717a !important;
  font-weight: 500;
  text-decoration: underline;
  text-decoration-color: #d4d4d8;
  text-underline-offset: 3px;
  transition: color 0.2s;
}

.hero-link:hover {
  color: #3f3f46 !important;
}

[data-theme='dark'] .hero-link:hover {
  color: var(--text-primary) !important;
}

/* ============ NavigationTabs ============ */
.tabs {
  position: relative;
  z-index: 2;
  padding: 16px 64px 16px;
}

.tabs-inner {
  display: flex;
  gap: 4px;
  align-items: flex-start;
}

.tab {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 5px 14px 4px;
  border-radius: 999px;
  position: relative;
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 500;
  letter-spacing: 0.005em;
  color: #a1a1aa;
  white-space: nowrap;
  transition: color 0.2s ease-out;
}

.tab:hover {
  color: #52525b;
}

.tab.active {
  color: #52525b;
}

.tab.active::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  background: rgba(228, 228, 234, 0.6);
  backdrop-filter: blur(8px);
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.03),
    0 4px 14px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
  z-index: -1;
}

.tabs-divider {
  margin-top: 12px;
  height: 1px;
  background: var(--border-color);
}

/* ============ 项目网格（两个一排） ============ */
.project-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  padding: 10px 64px 8px;
}

.project-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  cursor: pointer;
  animation: projectCardEnter 450ms cubic-bezier(0.25, 0.46, 0.45, 0.94) both;
}

/* 小组件行：音乐/图集卡都是入口（可点进整页） */

/* 媒体区：参考站同款——悬停无位移/缩放，动效全部来自循环播放的演示视频 */
.media {
  position: relative;
  border-radius: 26px;
  overflow: hidden;
  aspect-ratio: 16 / 10;
  display: flex;
  align-items: center;
  justify-content: center;
  isolation: isolate;
}

/* 卡片内的演示视频：铺满、裁切、圆角随容器 */
.media-video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  border-radius: inherit;
}

.media-ring {
  position: absolute;
  inset: 0;
  border: 1px solid #f4f4f5;
  border-radius: 26px;
  pointer-events: none;
}

[data-theme='dark'] .media-ring {
  border-color: rgba(255, 255, 255, 0.08);
}

/* 渐变底（取自原站 header-gradient 色系） */
.grad-lavender { background: linear-gradient(150deg, #d5e0ff, #ece8ff 60%, #f5e2ff); }
.grad-peach { background: linear-gradient(150deg, #fde9fa, #ffe6d8 55%, #fff5fc); }
.grad-sky { background: linear-gradient(150deg, #d5e0ff, #dff0ff 60%, #e2eaff); }
.grad-mint { background: linear-gradient(150deg, #e2f4ec, #eaf8f0 55%, #fde9fa); }
.grad-butter { background: linear-gradient(150deg, #fdf3d7, #fff0e0 60%, #fde9fa); }
.grad-rose { background: linear-gradient(150deg, #fde9fa, #ffe9f2 55%, #f5e2ff); }
/* Blog 块：纸感浅色，避免在视频卡中间显得突兀 */
.grad-paper { background: linear-gradient(150deg, #fefefe, #f8f8f6 55%, #f2f3f0); }

[data-theme='dark'] .grad-lavender { background: linear-gradient(150deg, #26304a, #2c2846 60%, #332a48); }
[data-theme='dark'] .grad-peach { background: linear-gradient(150deg, #3a2d33, #40303a 55%, #35283a); }
[data-theme='dark'] .grad-sky { background: linear-gradient(150deg, #25324a, #233149 60%, #26304a); }
[data-theme='dark'] .grad-mint { background: linear-gradient(150deg, #22352c, #26382f 55%, #2c2846); }
[data-theme='dark'] .grad-butter { background: linear-gradient(150deg, #38351f, #3a3324 60%, #332a48); }
[data-theme='dark'] .grad-rose { background: linear-gradient(150deg, #38283a, #3a2c3c 55%, #332a48); }
[data-theme='dark'] .grad-paper { background: linear-gradient(150deg, #232326, #1f1f22 60%, #26262a); }

/* featured：图片左下角标题胶囊 */
.title-pill {
  position: absolute;
  bottom: 12px;
  left: 12px;
  background: #fff;
  border: 1px solid #f4f4f5;
  border-radius: 999px;
  padding: 5px 12px 4.8px;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 0.005em;
  line-height: 1.375;
  color: #18181b;
}

.tp-year {
  color: #a1a1aa;
}

.tp-dot {
  color: #a1a1aa;
  opacity: 0;
  transition: opacity 0.3s ease-out;
}

.tp-try {
  color: #60a5fa;
  opacity: 0;
  transition: opacity 0.3s ease-out;
}

.project-card:hover .tp-dot,
.project-card:hover .tp-try {
  opacity: 1;
}

/* featured：图片下方描述 */
.card-desc {
  font-size: 16px;
  font-weight: 400;
  line-height: 1.375;
  letter-spacing: 0.005em;
  color: #a1a1aa;
  padding: 0 13px;
  margin: -6px 0 2px;
}

/* 非 featured：标题行 + Try It Out（隐式显示，悬停卡片时浮现） */
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

.project-card:hover .card-caption,
.project-card:focus-visible .card-caption {
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

.caption-try {
  margin-left: auto;
  color: #60a5fa;
  flex-shrink: 0;
}

/* ============ 动画 ============ */
@keyframes projectCardEnter {
  from {
    opacity: 0;
    transform: translate3d(0, 12px, 0);
  }
  to {
    opacity: 1;
    transform: translate3d(0, 0, 0);
  }
}

@keyframes heroFadeUp {
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
  .project-card,
  .hero-name,
  .hero-desc {
    animation: none;
  }
}

/* ============ 移动端 ============ */
@media (max-width: 768px) {
  .hero-block,
  .tabs {
    padding-left: 24px;
    padding-right: 24px;
  }

  .hero-block {
    padding-top: 80px;
    min-height: 210px;
  }

  .hero-name {
    font-size: 32px;
  }

  .hero-desc {
    font-size: 16px;
  }

  .project-grid {
    grid-template-columns: 1fr;
    gap: 32px;
    padding: 16px 24px;
  }

  .media {
    aspect-ratio: 16 / 10;
  }

  .tp-dot,
  .tp-try,
  /* 触屏无悬停：说明行常显 */
  .card-caption {
    opacity: 1;
    transform: none;
  }
}
</style>
