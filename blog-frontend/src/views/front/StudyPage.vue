<template>
  <div class="study-page">
    <div class="container">
      <header class="page-head">
        <div>
          <h1>📊 学习概览</h1>
          <p>刷题、看课、笔记的进度总览（{{ cloudLabel }}）。</p>
        </div>
        <router-link v-if="!loggedIn" to="/admin/login" class="login-hint">登录后进度云端同步 →</router-link>
      </header>

      <!-- 三项进度摘要 -->
      <div class="summary-grid" v-if="index">
        <router-link to="/leetcode" class="sum-card">
          <span class="sum-icon">🧮</span>
          <div class="sum-body">
            <h3>算法刷题</h3>
            <div class="sum-num">{{ doneSets.leetcode.size }}<span> / {{ lcTotal }}</span></div>
            <div class="bar"><div class="bar-fill" :style="{ width: pct(doneSets.leetcode.size, lcTotal) }"></div></div>
          </div>
        </router-link>
        <router-link to="/cet4" class="sum-card">
          <span class="sum-icon">📖</span>
          <div class="sum-body">
            <h3>四级备考</h3>
            <div class="sum-num">{{ doneSets.cet4.size }}<span> / {{ cet4Total }}</span></div>
            <div class="bar"><div class="bar-fill" :style="{ width: pct(doneSets.cet4.size, cet4Total) }"></div></div>
          </div>
        </router-link>
        <router-link to="/agent" class="sum-card">
          <span class="sum-icon">🎯</span>
          <div class="sum-body">
            <h3>秋招冲刺</h3>
            <div class="sum-num">{{ doneSets.agent.size }}<span> / {{ agentTotal }}</span></div>
            <div class="bar"><div class="bar-fill" :style="{ width: pct(doneSets.agent.size, agentTotal) }"></div></div>
          </div>
        </router-link>
        <router-link to="/tools/markdown" class="sum-card">
          <span class="sum-icon">✍️</span>
          <div class="sum-body">
            <h3>笔记</h3>
            <div class="sum-num">{{ noteCount }}<span> 篇</span></div>
            <div class="bar"><div class="bar-fill" :style="{ width: noteCount ? '100%' : '0%' }"></div></div>
          </div>
        </router-link>
      </div>

      <!-- 连续打卡 + 今日 -->
      <div class="stat-row" v-if="log.length">
        <div class="stat-card">🔥 连续打卡 <strong>{{ streak }}</strong> 天</div>
        <div class="stat-card">今日已完成 <strong>{{ todayCount }}</strong> 项</div>
        <div class="stat-card">四级已学 <strong>{{ doneDurationLabel }}</strong></div>
      </div>

      <!-- 热力图 -->
      <section class="heatmap-card" v-if="log.length">
        <h3>📅 最近 20 周</h3>
        <div class="heatmap">
          <div class="hm-col" v-for="(week, wi) in heatmap" :key="wi">
            <div
              v-for="day in week"
              :key="day.date"
              class="hm-cell"
              :class="'lv' + day.level"
              :title="day.date + ' · ' + day.count + ' 项'"
            ></div>
          </div>
        </div>
        <div class="hm-legend">
          少 <span class="hm-cell lv0"></span><span class="hm-cell lv1"></span><span class="hm-cell lv2"></span><span class="hm-cell lv3"></span><span class="hm-cell lv4"></span> 多
        </div>
      </section>
      <section class="heatmap-card empty" v-else>
        <h3>📅 打卡记录</h3>
        <p>还没有打卡记录：去<a href="/leetcode">刷题</a>或<a href="/cet4">看课</a>勾选第一项吧，这里会生成你的学习热力图。</p>
      </section>

      <!-- 今日任务 -->
      <section class="todo-card">
        <h3>🎯 接下来学什么</h3>
        <div class="todo-grid">
          <a v-if="lcTask" :href="lcTask.url" target="_blank" rel="noopener" class="todo-item">
            <span class="todo-tag">算法</span>
            <span class="todo-name">{{ lcTask.label }}</span>
            <span class="todo-sub">{{ lcTask.from }}</span>
          </a>
          <a v-if="cet4Task" href="/cet4" class="todo-item">
            <span class="todo-tag">四级</span>
            <span class="todo-name">{{ cet4Task.title }}</span>
            <span class="todo-sub">{{ cet4Task.from }} · {{ cet4Task.dur }}</span>
          </a>
          <a v-if="agentTask" href="/agent" class="todo-item">
            <span class="todo-tag">秋招</span>
            <span class="todo-name">{{ agentTask.title }}</span>
            <span class="todo-sub">{{ agentTask.from }}</span>
          </a>
          <router-link to="/tools/markdown" class="todo-item">
            <span class="todo-tag">笔记</span>
            <span class="todo-name">打开笔记本</span>
            <span class="todo-sub">记录今天的收获</span>
          </router-link>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getStudyLog } from '@/utils/studyLog'
import { noteApi } from '@/api'

const loggedIn = ref(false)
const index = ref<any>(null)
const cloudLabel = computed(() => (loggedIn.value ? '云端已同步' : '本地记录'))
const log = ref<{ d: string; k: string; s?: number }[]>([])
const doneSets = ref({ leetcode: new Set<string>(), cet4: new Set<string>(), agent: new Set<string>() })
const lcTotal = ref(0)
const cet4Total = ref(0)
const agentTotal = ref(0)
const noteCount = ref(0)
const lcTask = ref<{ label: string; url: string; from: string } | null>(null)
const cet4Task = ref<{ title: string; url: string; from: string; dur: string } | null>(null)
const agentTask = ref<{ title: string; from: string } | null>(null)

function dateStr(d: Date) {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

const perDay = computed(() => {
  const map = new Map<string, number>()
  for (const e of log.value) map.set(e.d, (map.get(e.d) || 0) + 1)
  return map
})

const todayCount = computed(() => perDay.value.get(dateStr(new Date())) || 0)

const heatmap = computed(() => {
  const weeks = 20
  const today = new Date()
  const cells: { date: string; count: number; level: number }[] = []
  for (let i = weeks * 7 - 1; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    const ds = dateStr(d)
    const count = perDay.value.get(ds) || 0
    cells.push({ date: ds, count, level: count === 0 ? 0 : count < 3 ? 1 : count < 6 ? 2 : count < 10 ? 3 : 4 })
  }
  // 补齐第一周空位，让列对齐星期
  const firstPad = cells[0] ? new Date(cells[0].date).getDay() : 0
  const grid: { date: string; count: number; level: number }[][] = []
  let col: typeof cells = Array(firstPad).fill({ date: '', count: 0, level: -1 })
  for (const c of cells) {
    col.push(c)
    if (col.length === 7) {
      grid.push(col)
      col = []
    }
  }
  if (col.length) grid.push([...col, ...Array(7 - col.length).fill({ date: '', count: 0, level: -1 })])
  return grid
})

const streak = computed(() => {
  let n = 0
  const d = new Date()
  if (!perDay.value.get(dateStr(d))) d.setDate(d.getDate() - 1) // 今天还没学，从昨天起算
  while (perDay.value.get(dateStr(d))) {
    n++
    d.setDate(d.getDate() - 1)
  }
  return n
})

const doneDurationLabel = computed(() => {
  const sec = log.value.reduce((s, e) => s + (e.s || 0), 0)
  return `${Math.floor(sec / 3600)} 小时 ${Math.floor((sec % 3600) / 60)} 分钟`
})

function pct(done: number, total: number) {
  return total ? Math.round((done / total) * 100) + '%' : '0%'
}

async function loadJson(path: string) {
  const res = await fetch(path)
  return res.json()
}

// 今日任务：按原帖/清单顺序找第一个未完成项
async function findLcTask() {
  if (!index.value) return
  for (const cat of index.value.categories) {
    try {
      const data = await loadJson(`/data/leetcode/${cat.key}.json`)
      for (const ch of data.chapters) {
        for (const sec of ch.sections) {
          for (const p of sec.problems) {
            if (!doneSets.value.leetcode.has(String(p.pid))) {
              lcTask.value = { label: p.label, url: p.url, from: `${cat.name} · ${sec.title}` }
              return
            }
          }
        }
      }
    } catch (_) {}
  }
}

async function findCet4Task() {
  try {
    const data = await loadJson('/data/cet4/01.json')
    for (const ch of data.chapters) {
      for (const sec of ch.sections) {
        for (const it of sec.items) {
          if (!doneSets.value.cet4.has(it.id)) {
            cet4Task.value = { title: it.title, url: '/cet4', from: `${ch.title}${sec.title ? ' / ' + sec.title : ''}`, dur: it.dur }
            return
          }
        }
      }
    }
  } catch (_) {}
}

// 秋招计划：按阶段一→三顺序找第一个未完成项
async function findAgentTask() {
  const phases = [
    { f: 'phase1.json', label: '阶段一' },
    { f: 'phase2.json', label: '阶段二' },
    { f: 'phase3.json', label: '阶段三' },
  ]
  for (const { f, label } of phases) {
    try {
      const data = await loadJson(`/data/agent/${f}`)
      const weeks: any[] = data.weeks || (data.projects || []).flatMap((p: any) => p.weeks).concat(data.final ? [data.final] : [])
      for (const w of weeks) {
        for (const day of w.days || []) {
          if (!doneSets.value.agent.has(day.id)) {
            agentTask.value = {
              title: day.tasks[0],
              from: `${label} · ${day.dow} ${day.date}`,
            }
            return
          }
        }
      }
    } catch (_) {}
  }
}

onMounted(async () => {
  loggedIn.value = !!localStorage.getItem('token')
  try {
    doneSets.value.leetcode = new Set((JSON.parse(localStorage.getItem('leetcode:done') || '[]') as any[]).map(String))
    doneSets.value.cet4 = new Set((JSON.parse(localStorage.getItem('cet4:done') || '[]') as any[]).map(String))
    doneSets.value.agent = new Set((JSON.parse(localStorage.getItem('agent:done') || '[]') as any[]).map(String))
  } catch (_) {}
  log.value = getStudyLog()
  try {
    const [lc, c4, ag] = await Promise.all([
      loadJson('/data/leetcode/index.json'),
      loadJson('/data/cet4/index.json'),
      loadJson('/data/agent/index.json'),
    ])
    lcTotal.value = lc.categories.reduce((s: number, c: any) => s + c.uniqueTotal, 0)
    cet4Total.value = c4.totalItems
    agentTotal.value = ag.totalItems
    index.value = lc
  } catch (_) {}
  if (loggedIn.value) {
    try {
      const res = await noteApi.list()
      if (res.data.code === 200) noteCount.value = (res.data.data || []).length
    } catch (_) {}
  } else {
    try { noteCount.value = (JSON.parse(localStorage.getItem('tools:notes') || '[]')).length } catch (_) {}
  }
  await Promise.all([findLcTask(), findCet4Task(), findAgentTask()])
})
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
  padding: 36px 0 24px;
}

.page-head h1 {
  font-size: 28px;
  margin-bottom: 8px;
}

.page-head p {
  color: var(--text-secondary);
  font-size: 14px;
}

.login-hint {
  font-size: 13px;
  color: var(--accent-color);
  white-space: nowrap;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.sum-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 18px;
  box-shadow: var(--shadow-soft);
  transition: transform 0.2s, box-shadow 0.2s;
}

.sum-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-hover);
}

.sum-icon {
  font-size: 26px;
}

.sum-body {
  flex: 1;
  min-width: 0;
}

.sum-body h3 {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.sum-num {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.sum-num span {
  font-size: 12px;
  font-weight: 400;
  color: var(--text-muted);
}

.bar {
  height: 6px;
  border-radius: 999px;
  background: var(--bg-secondary);
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--accent-color), #38bdf8);
}

.stat-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.stat-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 10px 16px;
  font-size: 13px;
  color: var(--text-secondary);
  box-shadow: var(--shadow-soft);
}

.stat-card strong {
  color: var(--accent-color);
  font-size: 16px;
  margin: 0 2px;
}

.heatmap-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  margin-bottom: 16px;
  box-shadow: var(--shadow-soft);
}

.heatmap-card h3 {
  font-size: 14px;
  margin-bottom: 14px;
}

.heatmap-card.empty p {
  font-size: 13px;
  color: var(--text-muted);
}

.heatmap-card.empty a {
  color: var(--accent-color);
}

.heatmap {
  display: flex;
  gap: 4px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.hm-col {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.hm-cell {
  width: 13px;
  height: 13px;
  border-radius: 3px;
  background: var(--bg-secondary);
  flex: none;
}

.hm-legend .hm-cell {
  display: inline-block;
  vertical-align: middle;
  margin: 0 2px;
}

.lv0 { background: var(--bg-secondary); }
.lv1 { background: #bfd7ff; }
.lv2 { background: #7fb0fa; }
.lv3 { background: #4383f5; }
.lv4 { background: #1d4ed8; }

[data-theme='dark'] .lv1 { background: #1e3a8a55; }
[data-theme='dark'] .lv2 { background: #1e40af; }
[data-theme='dark'] .lv3 { background: #3b82f6; }
[data-theme='dark'] .lv4 { background: #93c5fd; }

.todo-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  box-shadow: var(--shadow-soft);
  margin-bottom: 40px;
}

.todo-card h3 {
  font-size: 14px;
  margin-bottom: 14px;
}

.todo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.todo-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 14px;
  transition: border-color 0.2s, transform 0.2s;
}

.todo-item:hover {
  border-color: var(--accent-color);
  transform: translateY(-2px);
}

.todo-tag {
  font-size: 11px;
  color: var(--accent-color);
  background: var(--bg-secondary);
  border-radius: 999px;
  padding: 2px 10px;
  width: fit-content;
}

.todo-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.todo-sub {
  font-size: 12px;
  color: var(--text-muted);
}

.loading {
  text-align: center;
  padding: 60px;
  color: var(--text-muted);
}

@media (max-width: 760px) {
  .page-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
