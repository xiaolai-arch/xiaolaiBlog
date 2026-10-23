<template>
  <div class="tools-page">
    <div class="container">
      <header class="page-head">
        <h1>工具箱</h1>
        <p>一些常用的小工具，全部在浏览器本地运行，内容不会上传。卡片里就是工具本体的实时预览。</p>
      </header>

      <div class="tool-grid">
        <router-link
          v-for="(tool, i) in tools"
          :key="tool.key"
          :to="tool.to"
          class="tool-card"
          :style="{ animationDelay: Math.min(i * 80, 320) + 'ms' }"
          @mouseenter="hoveredKey = tool.key"
          @mouseleave="hoveredKey = ''"
        >
          <div class="tool-media" :class="'grad-' + tool.grad">
            <!-- 活体预览：缩微 iframe，自动缓慢滚动 -->
            <LivePreview
              v-if="tool.live"
              :active="mountFlags[tool.key]"
              :src="tool.to"
              :title="tool.name + ' 实时预览'"
              :paused="hoveredKey === tool.key"
            />
            <!-- Markdown 即时渲染打字动画 -->
            <div v-else class="md-demo">
              <div class="md-pane md-raw">
                <span class="md-raw-text">{{ typedText }}</span><span class="md-caret"></span>
              </div>
              <div class="md-pane md-rendered">
                <TransitionGroup name="md-line">
                  <span
                    v-for="line in renderedLines"
                    :key="line.id"
                    class="md-line"
                    :class="'md-' + line.kind"
                    v-html="line.html"
                  ></span>
                </TransitionGroup>
              </div>
              <span class="md-badge">即时渲染</span>
            </div>

            <div class="media-ring" aria-hidden="true"></div>
          </div>

          <div class="tool-caption">
            <div class="cap-text">
              <p class="cap-title">
                <span>{{ tool.name }}</span>
              </p>
              <p class="cap-desc">{{ tool.desc }}</p>
            </div>
            <span class="cap-try">↗ 试试看</span>
          </div>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import LivePreview from '@/components/LivePreview.vue'

interface Tool {
  key: string
  name: string
  desc: string
  to: string
  grad: string
  live: boolean
}

const tools: Tool[] = [
  {
    key: 'markdown',
    name: 'Markdown 笔记本',
    desc: 'Typora 式即时渲染，多篇笔记管理；登录后云端同步，未登录本地保存',
    to: '/tools/markdown',
    grad: 'lavender',
    live: false,
  },
  {
    key: 'leetcode',
    name: '算法刷题',
    desc: '12 个专题的力扣题单（原帖章节顺序），支持打卡记录进度',
    to: '/leetcode',
    grad: 'sky',
    live: true,
  },
  {
    key: 'cet4',
    name: '四级备考',
    desc: '刘晓艳四级课程清单（2064 节），看课打卡、统计已学时长',
    to: '/cet4',
    grad: 'mint',
    live: true,
  },
  {
    key: 'agent',
    name: 'Agent 开发',
    desc: 'Agent 开发 14 周学习计划（原理/框架/项目）+ 资源推荐',
    to: '/agent',
    grad: 'butter',
    live: true,
  },
]

/* ================= 悬停 ================= */
const hoveredKey = ref('')
const reducedMotion = ref(false)

/* ================= 交错挂载调度 ================= */
const mountFlags = reactive<Record<string, boolean>>({})

/* Markdown 打字动画需要知道卡片是否悬停（暂停打字），
   LivePreview 的滚动/漂移/离屏由组件内部自理 */

/* ================= Markdown 即时渲染打字动画 ================= */
interface MdLine { id: number; kind: string; html: string }
const SNIPPETS: Array<{ kind: string; raw: string; html: string }> = [
  { kind: 'h', raw: '# 我的学习笔记', html: '我的学习笔记' },
  { kind: 'p', raw: '今天搞懂了 **Transformer** 架构！', html: '今天搞懂了 <strong>Transformer</strong> 架构！' },
  { kind: 'li', raw: '- 上午：注意力机制', html: '上午：注意力机制' },
  { kind: 'li', raw: '- 晚上：手写 Q/K/V 矩阵', html: '晚上：手写 Q/K/V 矩阵' },
  { kind: 'quote', raw: '> 输入即所见，写作不打断', html: '输入即所见，写作不打断' },
  { kind: 'code', raw: '`model.fit(attention)`', html: 'model.fit(attention)' },
]
const typedText = ref('')
const renderedLines = ref<MdLine[]>([])
let typeTimer: number | null = null
let snippetIdx = 0
let charIdx = 0
let lineId = 0

function scheduleType(delay: number) {
  typeTimer = window.setTimeout(stepType, delay)
}

function stepType() {
  typeTimer = null
  if (hoveredKey.value === 'markdown' || reducedMotion.value) {
    scheduleType(400) // 悬停时暂停打字，稍后继续
    return
  }
  const snip = SNIPPETS[snippetIdx]
  if (charIdx < snip.raw.length) {
    charIdx++
    typedText.value = snip.raw.slice(0, charIdx)
    scheduleType(snip.kind === 'h' ? 90 : 30 + Math.random() * 50)
  } else {
    // 本行打完 → 渲染结果滑入
    renderedLines.value.push({ id: ++lineId, kind: snip.kind, html: snip.html })
    if (renderedLines.value.length > 6) renderedLines.value.shift()
    charIdx = 0
    snippetIdx = (snippetIdx + 1) % SNIPPETS.length
    if (snippetIdx === 0) {
      // 一轮结束：清空重来
      typeTimer = window.setTimeout(() => {
        renderedLines.value = []
        typedText.value = ''
        scheduleType(500)
      }, 2600)
    } else {
      scheduleType(420)
    }
  }
}

function startMdDemo() {
  if (reducedMotion.value) {
    // 减少动态：直接静态展示完整状态
    typedText.value = ''
    renderedLines.value = SNIPPETS.map(s => ({ id: ++lineId, kind: s.kind, html: s.html }))
    return
  }
  typedText.value = ''
  renderedLines.value = []
  snippetIdx = 0
  charIdx = 0
  scheduleType(600)
}

function stopMdDemo() {
  if (typeTimer !== null) {
    window.clearTimeout(typeTimer)
    typeTimer = null
  }
}

/* ================= 生命周期 ================= */
let mq: MediaQueryList | null = null
let mqListener: ((e: MediaQueryListEvent) => void) | null = null

onMounted(() => {
  // 交错挂载 iframe，避免同时启动 3 个应用抢主线程
  const liveKeys = tools.filter(t => t.live).map(t => t.key)
  liveKeys.forEach((key, i) => {
    window.setTimeout(() => { mountFlags[key] = true }, 350 + i * 300)
  })

  mq = window.matchMedia('(prefers-reduced-motion: reduce)')
  mqListener = () => {
    reducedMotion.value = mq!.matches
    if (mq!.matches) {
      stopMdDemo()
      typedText.value = ''
      renderedLines.value = SNIPPETS.map(s => ({ id: ++lineId, kind: s.kind, html: s.html }))
    } else {
      startMdDemo()
    }
  }
  mq.addEventListener('change', mqListener)
  reducedMotion.value = mq.matches

  startMdDemo()
})

onBeforeUnmount(() => {
  stopMdDemo()
  if (mq && mqListener) mq.removeEventListener('change', mqListener)
})
</script>

<style scoped>
.page-head {
  padding: 36px 0 28px;
}

.page-head h1 {
  font-size: 28px;
  margin-bottom: 8px;
}

.page-head p {
  color: var(--text-secondary);
  font-size: 14px;
}

/* ============ 网格 ============ */
.tool-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 24px;
  padding-bottom: 48px;
}

.tool-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  cursor: pointer;
  animation: toolCardEnter 520ms cubic-bezier(0.22, 1, 0.36, 1) both;
}

/* ============ 媒体区（参考站式圆角大媒体） ============ */
.tool-media {
  position: relative;
  border-radius: 26px;
  overflow: hidden;
  aspect-ratio: 16 / 10;
  isolation: isolate;
  transition:
    transform 0.45s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.45s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform;
}

.tool-card:hover .tool-media {
  transform: translateY(-4px);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.12);
}

.media-ring {
  position: absolute;
  inset: 0;
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: 26px;
  pointer-events: none;
  z-index: 3;
}


[data-theme='dark'] .media-ring {
  border-color: rgba(255, 255, 255, 0.08);
}

/* 渐变兜底（与首页卡片同一色系） */
.grad-lavender { background: linear-gradient(150deg, #d5e0ff, #ece8ff 60%, #f5e2ff); }
.grad-sky { background: linear-gradient(150deg, #d5e0ff, #dff0ff 60%, #e2eaff); }
.grad-mint { background: linear-gradient(150deg, #e2f4ec, #eaf8f0 55%, #fde9fa); }
.grad-butter { background: linear-gradient(150deg, #fdf3d7, #fff0e0 60%, #fde9fa); }
.grad-peach { background: linear-gradient(150deg, #fde9fa, #ffe6d8 55%, #fff5fc); }
.grad-rose { background: linear-gradient(150deg, #fde9fa, #ffe9f2 55%, #f5e2ff); }

[data-theme='dark'] .grad-lavender { background: linear-gradient(150deg, #26304a, #2c2846 60%, #332a48); }
[data-theme='dark'] .grad-sky { background: linear-gradient(150deg, #25324a, #233149 60%, #26304a); }
[data-theme='dark'] .grad-mint { background: linear-gradient(150deg, #22352c, #26382f 55%, #2c2846); }
[data-theme='dark'] .grad-butter { background: linear-gradient(150deg, #38351f, #3a3324 60%, #332a48); }
[data-theme='dark'] .grad-peach { background: linear-gradient(150deg, #3a2d33, #40303a 55%, #35283a); }
[data-theme='dark'] .grad-rose { background: linear-gradient(150deg, #38283a, #3a2c3c 55%, #332a48); }

/* ============ Markdown 即时渲染动画 ============ */
.md-demo {
  position: absolute;
  inset: 0;
  display: flex;
  gap: 10px;
  padding: 22px 24px;
  align-items: stretch;
}

.md-pane {
  flex: 1;
  border-radius: 14px;
  padding: 16px 16px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 4px 18px rgba(31, 27, 60, 0.08);
  overflow: hidden;
  font-size: 13px;
  line-height: 1.7;
  backdrop-filter: blur(4px);
}

[data-theme='dark'] .md-pane {
  background: rgba(24, 26, 38, 0.78);
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.3);
}

.md-raw {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  color: #52525b;
  white-space: pre-wrap;
  word-break: break-all;
}

[data-theme='dark'] .md-raw {
  color: #a1a1aa;
}

.md-caret {
  display: inline-block;
  width: 2px;
  height: 1em;
  margin-left: 2px;
  vertical-align: -0.15em;
  background: var(--accent-color, #6366f1);
  animation: caretBlink 0.9s steps(1) infinite;
}

@keyframes caretBlink {
  50% { opacity: 0; }
}

.md-rendered {
  color: #27272a;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

[data-theme='dark'] .md-rendered {
  color: #e4e4e7;
}

.md-line { display: block; }
.md-h { font-size: 17px; font-weight: 700; line-height: 1.4; }
.md-p { font-size: 13px; }
.md-li { font-size: 13px; padding-left: 14px; position: relative; }
.md-li::before {
  content: '';
  position: absolute;
  left: 2px;
  top: 0.62em;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.55;
}
.md-quote {
  font-size: 13px;
  padding-left: 10px;
  border-left: 3px solid var(--accent-color, #6366f1);
  color: #52525b;
  font-style: italic;
}
[data-theme='dark'] .md-quote { color: #a1a1aa; }
.md-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  background: rgba(99, 102, 241, 0.1);
  color: #4f46e5;
  border-radius: 6px;
  padding: 2px 7px;
  display: inline-block;
  width: fit-content;
}
[data-theme='dark'] .md-code {
  background: rgba(129, 140, 248, 0.16);
  color: #c7d2fe;
}

.md-line strong { font-weight: 700; color: #4f46e5; }
[data-theme='dark'] .md-line strong { color: #a5b4fc; }

/* 渲染行滑入动画 */
.md-line-enter-active {
  transition: opacity 0.35s cubic-bezier(0.22, 1, 0.36, 1), transform 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}
.md-line-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.md-badge {
  position: absolute;
  right: 14px;
  bottom: 12px;
  z-index: 3;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: 999px;
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 500;
  color: #3f3f46;
  opacity: 0;
  transform: translateY(4px);
  transition: opacity 0.35s ease-out, transform 0.35s ease-out;
}

[data-theme='dark'] .md-badge {
  background: rgba(39, 39, 52, 0.92);
  border-color: rgba(255, 255, 255, 0.1);
  color: #d4d4d8;
}

.tool-card:hover .md-badge {
  opacity: 1;
  transform: translateY(0);
}

/* ============ 说明文字 ============ */
.tool-caption {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 0 13px;
  margin: -4px 0 2px;
}

.cap-text {
  flex: 1;
  min-width: 0;
}

.cap-title {
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 0.005em;
  color: var(--text-primary);
  line-height: 1.375;
}

.cap-desc {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.6;
  margin-top: 2px;
}

.cap-try {
  flex-shrink: 0;
  font-size: 14px;
  font-weight: 500;
  color: #60a5fa;
  opacity: 0;
  transform: translateX(-6px);
  transition: opacity 0.35s cubic-bezier(0.22, 1, 0.36, 1), transform 0.35s cubic-bezier(0.22, 1, 0.36, 1);
  margin-top: 2px;
}

.tool-card:hover .cap-try,
.tool-card:focus-visible .cap-try {
  opacity: 1;
  transform: translateX(0);
}

.tool-card:focus-visible {
  outline: 2px solid var(--accent-color);
  outline-offset: 4px;
  border-radius: 8px;
}

/* ============ 动画 ============ */
@keyframes toolCardEnter {
  from {
    opacity: 0;
    transform: translate3d(0, 14px, 0);
  }
  to {
    opacity: 1;
    transform: translate3d(0, 0, 0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .tool-card {
    animation: none;
  }
  .tool-media,
  .cap-try,
  .md-badge {
    transition: none;
  }
}

/* ============ 移动端 ============ */
@media (max-width: 768px) {
  .page-head {
    padding: 24px 0 20px;
  }

  .tool-grid {
    grid-template-columns: 1fr;
    gap: 28px;
    padding-bottom: 36px;
  }

  .cap-try {
    opacity: 1;
    transform: none;
  }
}

@media (max-width: 480px) {
  .md-demo {
    flex-direction: column;
    gap: 8px;
    padding: 14px 16px;
  }

  .md-pane {
    padding: 10px 12px;
    font-size: 12px;
  }
}
</style>
