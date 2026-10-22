<template>
  <section v-if="visible" class="gh-section">
    <h2>{{ title }}</h2>
    <div class="gh-card">
      <div v-if="loading" class="gh-loading">加载中…</div>
      <template v-else>
        <div class="gh-grid" role="img" aria-label="GitHub 年度贡献热力图" @mouseleave="hovered = null">
          <div v-for="(week, wi) in weeks" :key="wi" class="gh-col">
            <span class="gh-month">{{ monthLabels[wi] || '' }}</span>
            <span
              v-for="d in week.days"
              :key="d.date"
              class="gh-cell"
              :data-level="d.level"
              :aria-label="`${d.count} 次贡献 · ${d.date}`"
              @mouseenter="showTip($event, d)"
            />
          </div>
        </div>
        <div class="gh-foot">
          <span>{{ total }} {{ unit }} in the last year</span>
          <span class="gh-legend">
            Less
            <i v-for="l in 5" :key="l" class="gh-cell gh-legend-cell" :data-level="l - 1" />
            More
          </span>
        </div>
        <!-- 悬停浮层：跟随格子出现在其上方 -->
        <div v-if="hovered" class="gh-tip" :style="{ left: hovered.left + 'px', top: hovered.top + 'px' }">
          {{ hovered.text }}
        </div>
      </template>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

interface DayData { date: string; count: number; level?: number }

const props = withDefaults(defineProps<{
  /** 数据接口地址（返回 {code,data:[{date,count,level?}]}） */
  url?: string
  /** 卡片标题 */
  title?: string
  /** 悬停与页脚的数量单位 */
  unit?: string
  /** GitHub 用户名（与 url 二选一） */
  user?: string
}>(), {
  url: '',
  title: '活跃度',
  unit: 'contributions',
  user: '',
})

const loading = ref(true)
const days = ref<DayData[]>([])

const visible = computed(() => !loading.value && days.value.length > 0)

/* 顺序切分成周列（GitHub 数据从周日开始，每列 7 天） */
const weeks = computed(() => {
  const cols: Array<{ days: DayData[] }> = []
  let cur: { days: DayData[] } = { days: [] }
  for (const d of days.value) {
    if (cur.days.length === 7) {
      cols.push(cur)
      cur = { days: [] }
    }
    cur.days.push(d)
  }
  if (cur.days.length) cols.push(cur)
  return cols
})

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

/* 每列首日跨月时打月份标签 */
const monthLabels = computed(() => {
  const labels: string[] = []
  let prevMonth = -1
  for (const w of weeks.value) {
    const first = w.days[0]
    if (!first) {
      labels.push('')
      continue
    }
    const m = Number(first.date.slice(5, 7)) - 1
    if (m !== prevMonth) {
      labels.push(MONTHS[m])
      prevMonth = m
    } else {
      labels.push('')
    }
  }
  return labels
})

const total = computed(() => days.value.reduce((s, d) => s + d.count, 0))

/* 悬停浮层：定位在格子正上方（相对卡片定位，左右防溢出） */
const hovered = ref<{ text: string; left: number; top: number } | null>(null)

function showTip(e: MouseEvent, d: DayData) {
  const cell = e.currentTarget as HTMLElement
  const card = cell.closest('.gh-card') as HTMLElement
  const c = cell.getBoundingClientRect()
  const k = card.getBoundingClientRect()
  const text = d.count > 0
    ? `${d.count} ${props.unit} on ${d.date}`
    : `No ${props.unit} on ${d.date}`
  hovered.value = {
    text,
    left: Math.min(Math.max(c.left - k.left + c.width / 2, 70), k.width - 70),
    top: c.top - k.top,
  }
}

function levelOf(count: number): number {
  if (count <= 0) return 0
  if (count <= 2) return 1
  if (count <= 4) return 2
  if (count <= 7) return 3
  return 4
}

async function load() {
  loading.value = true
  try {
    const url = props.url ||
      `/api/v1/github/contributions?user=${encodeURIComponent(props.user)}`
    const res = await fetch(url)
    const json = await res.json()
    if (json.code === 200 && Array.isArray(json.data)) {
      days.value = json.data.map((d: DayData) => ({ ...d, level: d.level ?? levelOf(d.count) }))
    }
  } catch (_) {} finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.gh-section {
  margin-bottom: 48px;
}

.gh-section h2 {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 800;
  margin-bottom: 18px;
}

.gh-card {
  position: relative;
  border: 1px solid var(--border-color);
  background: var(--card-bg);
  border-radius: 20px;
  padding: 22px 22px 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.gh-loading {
  padding: 30px 0;
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
}

.gh-grid {
  display: flex;
  gap: 3px;
  width: 100%;
}

.gh-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.gh-month {
  height: 15px;
  font-size: 10.5px;
  line-height: 15px;
  color: var(--text-muted);
  white-space: nowrap;
}

.gh-cell {
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 20%;
  background: #ececee;
}

.gh-cell[data-level='1'] { background: #d4d4d8; }
.gh-cell[data-level='2'] { background: #a1a1aa; }
.gh-cell[data-level='3'] { background: #71717a; }
.gh-cell[data-level='4'] { background: #3f3f46; }

[data-theme='dark'] .gh-cell { background: #2a2a30; }
[data-theme='dark'] .gh-cell[data-level='1'] { background: #3f3f46; }
[data-theme='dark'] .gh-cell[data-level='2'] { background: #52525b; }
[data-theme='dark'] .gh-cell[data-level='3'] { background: #8b8b93; }
[data-theme='dark'] .gh-cell[data-level='4'] { background: #d4d4d8; }

.gh-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  font-size: 12.5px;
  color: var(--text-secondary);
}

.gh-legend {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.gh-legend-cell {
  width: 10px;
  height: 10px;
  flex-shrink: 0;
}

/* 悬停浮层 */
.gh-tip {
  position: absolute;
  z-index: 20;
  transform: translate(-50%, calc(-100% - 8px));
  background: #18181b;
  color: #fafafa;
  font-size: 12px;
  line-height: 1;
  padding: 7px 10px;
  border-radius: 8px;
  white-space: nowrap;
  pointer-events: none;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.25);
}

.gh-tip::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: -4px;
  width: 8px;
  height: 8px;
  background: #18181b;
  transform: translateX(-50%) rotate(45deg);
}

[data-theme='dark'] .gh-tip {
  background: #f4f4f5;
  color: #18181b;
}

[data-theme='dark'] .gh-tip::after {
  background: #f4f4f5;
}
</style>
