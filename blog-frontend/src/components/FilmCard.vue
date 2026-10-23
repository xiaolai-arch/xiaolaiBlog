<template>
  <div
    class="film-card"
    @click="enter"
    role="link"
    aria-label="进入胶片图集页"
    tabindex="0"
    @keydown.enter="enter"
    @mouseenter="hovered = true"
    @mouseleave="hovered = false"
  >
    <!-- 媒体区即真正的 /film 页实时缩微预览：所见即所得，与点进去的页面一模一样 -->
    <div class="stage">
      <LivePreview
        :active="ready"
        src="/film"
        title="胶片图集实时预览"
        :paused="hovered"
        :preview-w="1280"
        :preview-h="800"
      />
    </div>

    <div class="card-caption">
      <p class="caption-title">
        <span>我的图集</span>
        <span class="tp-year"> • {{ count }} 张</span>
      </p>
      <span class="caption-more">进入 Film →</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import LivePreview from '@/components/LivePreview.vue'

const router = useRouter()
const count = ref(0)
const ready = ref(false)
const hovered = ref(false)

function enter() {
  router.push('/film')
}

onMounted(async () => {
  // 卡片进入视口后再挂 iframe（省资源）；数量仅用于角标文案
  requestAnimationFrame(() => {
    ready.value = true
  })
  try {
    const res = await fetch('/api/v1/gallery/images?page=1&size=1')
    const body = await res.json()
    if (body?.code === 200) count.value = body.data?.total ?? 0
  } catch (_) {}
})
</script>

<style scoped>
.film-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  cursor: pointer;
  animation: filmCardEnter 450ms cubic-bezier(0.25, 0.46, 0.45, 0.94) 300ms both;
}

/* 浅色舞台：承载 /film 实时预览 */
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

.stage::after {
  content: '';
  position: absolute;
  inset: 0;
  border: 1px solid rgba(24, 24, 27, 0.06);
  border-radius: 26px;
  pointer-events: none;
}

[data-theme='dark'] .stage::after {
  border-color: rgba(255, 255, 255, 0.07);
}

/* ============ 说明行：隐式显示，悬停卡片时浮现（同音乐卡） ============ */
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
  white-space: nowrap;
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
}
</style>
