<template>
  <div class="agent-page">
    <div class="container">
      <header class="page-head">
        <div>
          <h1>Agent 开发</h1>
          <p>Agent 开发 18 周学习计划：原理 → 框架 → 项目 → 查缺补强（2026-10 修订），附资源清单。打卡进度保存在本地{{ cloudActive ? '，并已云端同步' : '' }}。</p>
        </div>
        <div class="overall-progress">
          <div class="overall-num">{{ doneSet.size }}<span> / {{ totalItems }}</span></div>
          <div class="bar"><div class="bar-fill" :style="{ width: overallPercent + '%' }"></div></div>
        </div>
      </header>

      <div class="cat-tabs">
        <button
          v-for="t in tabs"
          :key="t.key"
          class="cat-pill"
          :class="{ active: tab === t.key }"
          @click="selectTab(t.key)"
        >
          {{ t.name }}
          <span class="cat-count">{{ doneOf(t.key) }}/{{ t.total }}</span>
        </button>
      </div>

      <div v-if="loading" class="loading">加载中...</div>

      <!-- ============ 阶段一 / 阶段二 ============ -->
      <template v-else-if="phaseTab && phase">
        <details
          v-for="week in phase.weeks"
          :key="week.key"
          class="chapter"
          :open="openWeeks.has(week.key)"
          @toggle="onToggle($event, week.key)"
        >
          <summary class="chapter-head">
            <span class="chapter-title">
              {{ week.num === '复习' ? '综合复习周' : '第 ' + week.num.slice(1) + ' 周' }}
              <span class="tag" :class="'tag-' + week.tagClass">{{ week.tag }}</span>
            </span>
            <span class="chapter-progress">{{ weekDone(week) }}/{{ weekTotal(week) }}</span>
          </summary>
          <div class="chapter-body">
            <p class="week-title">{{ week.title }}</p>
            <p class="week-sub">{{ week.sub }}</p>

            <div class="day-row" v-for="day in week.days" :key="day.id" :class="{ done: isDone(day.id) }">
              <span class="day-check">
                <input type="checkbox" :checked="isDone(day.id)" @change="toggle(day.id)" />
              </span>
              <span class="day-name">{{ day.dow }}<br /><em>{{ day.date }}</em></span>
              <span class="day-tasks">
                <span v-for="(t, i) in day.tasks" :key="i" class="task-line">• {{ t }}</span>
              </span>
              <span class="day-output">{{ day.output }}</span>
            </div>

            <div class="check-block">
              <h4>{{ week.checkTitle }}</h4>
              <label
                v-for="item in week.checkItems"
                :key="item.id"
                class="check-item"
                :class="{ done: isDone(item.id) }"
              >
                <input type="checkbox" :checked="isDone(item.id)" @change="toggle(item.id)" />
                <span>{{ item.text }}</span>
              </label>
            </div>
          </div>
        </details>
      </template>

      <!-- ============ 阶段三 ============ -->
      <template v-else-if="tab === 'p3' && phase3">
        <section v-for="(proj, pi) in phase3.projects" :key="pi" class="proj-card">
          <h2 class="proj-title">{{ proj.title }}</h2>
          <p class="proj-sub">{{ proj.sub }}</p>
          <p class="proj-tech">{{ proj.tech }}</p>
          <pre class="arch">{{ proj.arch }}</pre>

          <div v-for="week in proj.weeks" :key="week.key" class="week-block">
            <div class="week-head">{{ week.header }}</div>
            <div class="day-row" v-for="day in week.days" :key="day.id" :class="{ done: isDone(day.id) }">
              <span class="day-check">
                <input type="checkbox" :checked="isDone(day.id)" @change="toggle(day.id)" />
              </span>
              <span class="day-name">{{ day.dow }}<br /><em>{{ day.date }}</em></span>
              <span class="day-tasks">
                <span v-for="(t, i) in day.tasks" :key="i" class="task-line">• {{ t }}</span>
              </span>
              <span class="day-output">{{ day.output }}</span>
            </div>
          </div>

          <div class="resume-box">
            <strong>简历 Bullet 示例（完成后填入具体数字）：</strong>
            <p v-for="(r, i) in proj.resume" :key="i">{{ r }}</p>
          </div>

          <div class="check-block">
            <h4>{{ proj.checkTitle }}</h4>
            <label
              v-for="item in proj.checklist"
              :key="item.id"
              class="check-item"
              :class="{ done: isDone(item.id) }"
            >
              <input type="checkbox" :checked="isDone(item.id)" @change="toggle(item.id)" />
              <span>{{ item.text }}</span>
            </label>
          </div>
        </section>

        <section class="proj-card">
          <h2 class="proj-title">最终冲刺</h2>
          <div class="week-block">
            <div class="week-head">{{ phase3.final.header }}</div>
            <div class="day-row" v-for="day in phase3.final.days" :key="day.id" :class="{ done: isDone(day.id) }">
              <span class="day-check">
                <input type="checkbox" :checked="isDone(day.id)" @change="toggle(day.id)" />
              </span>
              <span class="day-name">{{ day.dow }}<br /><em>{{ day.date }}</em></span>
              <span class="day-tasks">
                <span v-for="(t, i) in day.tasks" :key="i" class="task-line">• {{ t }}</span>
              </span>
              <span class="day-output">{{ day.output }}</span>
            </div>
          </div>
          <div class="check-block">
            <h4>{{ phase3.finalCheckTitle }}</h4>
            <label
              v-for="item in phase3.finalChecklist"
              :key="item.id"
              class="check-item"
              :class="{ done: isDone(item.id) }"
            >
              <input type="checkbox" :checked="isDone(item.id)" @change="toggle(item.id)" />
              <span>{{ item.text }}</span>
            </label>
          </div>
        </section>
      </template>

      <!-- ============ 资源推荐 ============ -->
      <template v-else-if="tab === 'res' && resources">
        <div class="toolbar">
          <div class="status-filter">
            <button :class="{ on: status === 'all' }" @click="status = 'all'">全部</button>
            <button :class="{ on: status === 'todo' }" @click="status = 'todo'">未看</button>
            <button :class="{ on: status === 'done' }" @click="status = 'done'">已看</button>
          </div>
          <span class="toolbar-hint">在 B 站搜索 UP 主名称或 BV 号即可找到对应视频</span>
        </div>

        <template v-for="group in resources.groups" :key="group.key">
          <div v-if="visibleRes(group).length" class="res-group">
            <h3 class="res-group-title">{{ group.title }} <span class="res-stars">{{ group.stars }}</span></h3>
            <p class="res-group-sub">{{ group.sub }}</p>
            <div
              v-for="item in visibleRes(group)"
              :key="item.id"
              class="res-card"
              :class="{ done: isDone(item.id) }"
            >
              <span class="res-check">
                <input type="checkbox" :checked="isDone(item.id)" @change="toggle(item.id)" />
              </span>
              <div class="res-body">
                <div class="res-name">
                  {{ item.name }}
                  <a v-if="item.link" :href="item.link" target="_blank" rel="noopener">直达</a>
                  <span v-else-if="item.note" class="res-note">{{ item.note }}</span>
                </div>
                <div class="res-desc">{{ item.desc }}</div>
                <div v-if="item.tags.length" class="res-tags">
                  <span v-for="(t, i) in item.tags" :key="i" class="res-tag">{{ t }}</span>
                </div>
              </div>
            </div>
          </div>
        </template>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { progressApi } from '@/api'
import { requireLoginForAction } from '@/utils/requireLogin'
import { logStudy, unlogStudy } from '@/utils/studyLog'

interface CheckItem { id: string; text: string }
interface Day { id: string; dow: string; date: string; tasks: string[]; output: string }
interface Week {
  key: string; num: string; title: string; dates: string
  tag: string; tagClass: string; sub: string
  checkTitle: string; checkItems: CheckItem[]; days: Day[]
}
interface Phase { name: string; sub: string; weeks: Week[] }

interface ProjWeek { key: string; header: string; days: Day[] }
interface Project {
  title: string; sub: string; tech: string; arch: string
  weeks: ProjWeek[]; resume: string[]; checkTitle: string; checklist: CheckItem[]
}
interface Phase3 {
  name: string; sub: string; projects: Project[]
  final: ProjWeek; finalCheckTitle: string; finalChecklist: CheckItem[]
}

interface ResItem { id: string; name: string; link: string; note: string; desc: string; tags: string[] }
interface ResGroup { key: string; title: string; stars: string; sub: string; items: ResItem[] }
interface Resources { groups: ResGroup[] }

const DONE_KEY = 'agent:done'

const loading = ref(true)
const tab = ref('p1')
const status = ref<'all' | 'todo' | 'done'>('all')
const doneSet = ref<Set<string>>(new Set())
const openWeeks = ref<Set<string>>(new Set())
const cloudActive = ref(false)
let pushTimer: number | undefined

const phase = ref<Phase | null>(null)
const phase3 = ref<Phase3 | null>(null)
const resources = ref<Resources | null>(null)
const totals = ref<Record<string, number>>({ p1: 0, p2: 0, p3: 0, p4: 0, res: 0 })
const totalItems = ref(0)

const PHASE_TABS = ['p1', 'p2', 'p4']

const tabs = computed(() => [
  { key: 'p1', name: '阶段一 · 速成基础', total: totals.value.p1 },
  { key: 'p2', name: '阶段二 · 框架实操', total: totals.value.p2 },
  { key: 'p3', name: '阶段三 · 项目冲刺', total: totals.value.p3 },
  { key: 'p4', name: '阶段四 · 查缺补强', total: totals.value.p4 },
  { key: 'res', name: '资源推荐', total: totals.value.res },
])
const overallPercent = computed(() =>
  totalItems.value ? Math.round((doneSet.value.size / totalItems.value) * 1000) / 10 : 0)

const phaseCache = ref<Record<string, Phase>>({})

/* ---------------- 进度 ---------------- */
function isDone(id: string) {
  return doneSet.value.has(id)
}

function toggle(id: string) {
  const next = new Set(doneSet.value)
  if (next.has(id)) {
    next.delete(id)
    unlogStudy(`agent:${id}`)
  } else {
    next.add(id)
    logStudy(`agent:${id}`)
  }
  doneSet.value = next
  localStorage.setItem(DONE_KEY, JSON.stringify([...next]))
  if (cloudActive.value) pushDone(next)
  else if (!localStorage.getItem('token')) requireLoginForAction('云端同步打卡进度')
}

function pushDone(set: Set<string>) {
  window.clearTimeout(pushTimer)
  pushTimer = window.setTimeout(() => {
    progressApi.put('agent', JSON.stringify([...set])).catch(() => { cloudActive.value = false })
  }, 1500)
}

// 登录时合并云端进度（并集）
async function loadDone() {
  let local: string[] = []
  try {
    local = (JSON.parse(localStorage.getItem(DONE_KEY) || '[]') as any[]).map(String)
  } catch (_) {}
  const merged = new Set<string>(local)
  if (localStorage.getItem('token')) {
    try {
      const res = await progressApi.get('agent')
      if (res.data.code === 200) {
        cloudActive.value = true
        const cloud = JSON.parse(res.data.data.value || '[]')
        if (Array.isArray(cloud)) cloud.forEach((x) => merged.add(String(x)))
        if (merged.size !== local.length) {
          localStorage.setItem(DONE_KEY, JSON.stringify([...merged]))
          pushDone(merged)
        }
      }
    } catch (_) {}
  }
  doneSet.value = merged
}

/* ---------------- 统计 ---------------- */
function weekTotal(week: Week) {
  return week.days.length + week.checkItems.length
}

function weekDone(week: Week) {
  return [...week.days.map((d) => d.id), ...week.checkItems.map((c) => c.id)]
    .filter((id) => doneSet.value.has(id)).length
}

function countPhase(prefix: string) {
  const data = phaseCache.value[prefix]
  if (!data) return 0
  return data.weeks.reduce((s, w) => s + weekDone(w), 0)
}

const phaseTab = computed(() => PHASE_TABS.includes(tab.value))

function doneOf(key: string) {
  if (PHASE_TABS.includes(key)) return countPhase(key)
  if (key === 'p3') {
    const p = phase3.value
    if (!p) return 0
    const ids: string[] = []
    for (const pr of p.projects) {
      for (const w of pr.weeks) ids.push(...w.days.map((d) => d.id))
      ids.push(...pr.checklist.map((c) => c.id))
    }
    ids.push(...p.final.days.map((d) => d.id), ...p.finalChecklist.map((c) => c.id))
    return ids.filter((id) => doneSet.value.has(id)).length
  }
  const r = resources.value
  if (!r) return 0
  return r.groups.reduce((s, g) => s + g.items.filter((it) => doneSet.value.has(it.id)).length, 0)
}

/* ---------------- 交互 ---------------- */
function onToggle(e: Event, key: string) {
  const open = (e.target as HTMLDetailsElement).open
  const next = new Set(openWeeks.value)
  if (open) next.add(key)
  else next.delete(key)
  openWeeks.value = next
}

function visibleRes(group: ResGroup) {
  if (status.value === 'all') return group.items
  return group.items.filter((it) =>
    status.value === 'done' ? doneSet.value.has(it.id) : !doneSet.value.has(it.id))
}

async function selectTab(key: string) {
  tab.value = key
  status.value = 'all'
  await ensureLoaded(key)
  if (PHASE_TABS.includes(key)) {
    const data = phaseCache.value[key]
    openWeeks.value = new Set(data?.weeks.length ? [data.weeks[0].key] : [])
  }
}

/* ---------------- 数据加载 ---------------- */
async function fetchJson(path: string) {
  const res = await fetch(path)
  return res.json()
}

async function ensureLoaded(key: string) {
  if (PHASE_TABS.includes(key)) {
    if (phaseCache.value[key]) {
      phase.value = phaseCache.value[key]
      return
    }
    const num = key.slice(1)
    const [data, index] = await Promise.all([
      fetchJson(`/data/agent/phase${num}.json`),
      fetchJson('/data/agent/index.json'),
    ])
    phaseCache.value[key] = data
    phase.value = data
    totals.value[key] = index.phases.find((p: any) => p.key === key)?.total || 0
    totalItems.value = index.totalItems
  } else if (key === 'p3') {
    if (!phase3.value) {
      const [data, index] = await Promise.all([
        fetchJson('/data/agent/phase3.json'),
        fetchJson('/data/agent/index.json'),
      ])
      phase3.value = data
      totals.value.p3 = index.phases.find((p: any) => p.key === 'p3')?.total || 0
    }
  } else if (key === 'res') {
    if (!resources.value) {
      const [data, index] = await Promise.all([
        fetchJson('/data/agent/resources.json'),
        fetchJson('/data/agent/index.json'),
      ])
      resources.value = data
      totals.value.res = index.resources?.total || 0
    }
  }
}

onMounted(async () => {
  await loadDone()
  try {
    const index = await fetchJson('/data/agent/index.json')
    totalItems.value = index.totalItems
    for (const p of index.phases) totals.value[p.key] = p.total
    totals.value.res = index.resources?.total || 0
  } catch (_) {}
  await ensureLoaded('p1')
  const data = phaseCache.value.p1
  openWeeks.value = new Set(data?.weeks.length ? [data.weeks[0].key] : [])
  loading.value = false
})
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
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

.overall-progress {
  flex: none;
  min-width: 180px;
  text-align: right;
}

.overall-num {
  font-size: 22px;
  font-weight: 700;
  color: var(--accent-color);
  margin-bottom: 6px;
}

.overall-num span {
  font-size: 13px;
  font-weight: 400;
  color: var(--text-muted);
}

.bar {
  height: 8px;
  border-radius: 999px;
  background: var(--bg-secondary);
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--accent-color), #38bdf8);
  transition: width 0.3s;
}

.cat-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.cat-pill {
  border: 1px solid var(--border-color);
  background: var(--card-bg);
  color: var(--text-secondary);
  font-size: 13px;
  padding: 7px 13px;
  border-radius: 999px;
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s, background 0.15s;
}

.cat-pill:hover {
  color: var(--accent-color);
  border-color: var(--accent-color);
}

.cat-pill.active {
  background: var(--accent-color);
  border-color: var(--accent-color);
  color: #fff;
  font-weight: 600;
}

.cat-count {
  font-size: 11px;
  opacity: 0.75;
  margin-left: 4px;
}

/* ---------- 周卡片（阶段一/二） ---------- */
.chapter {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  margin-bottom: 14px;
  overflow: hidden;
}

.chapter-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  cursor: pointer;
  user-select: none;
  font-weight: 700;
  font-size: 15px;
  color: var(--text-primary);
  list-style: none;
}

.chapter-head::-webkit-details-marker {
  display: none;
}

.chapter-head::before {
  content: '▸';
  color: var(--text-muted);
  transition: transform 0.15s;
  margin-right: 2px;
}

.chapter[open] > .chapter-head::before {
  transform: rotate(90deg);
}

.chapter-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tag {
  font-size: 11px;
  padding: 2px 9px;
  border-radius: 4px;
  font-weight: 500;
}

.tag-blue { background: #e8f0fe; color: #4361ee; }
.tag-orange { background: #fef3e8; color: #f77f00; }
.tag-green { background: #e8faf0; color: #06a35f; }
.tag-red { background: #fde8e8; color: #e63946; }
.tag-purple { background: #f0e8fa; color: #7c3aed; }

[data-theme='dark'] .tag-blue { background: #1e3a8a44; color: #93b4ff; }
[data-theme='dark'] .tag-orange { background: #7c2d1244; color: #ffb37a; }
[data-theme='dark'] .tag-green { background: #064e3b44; color: #6ee7b7; }
[data-theme='dark'] .tag-red { background: #7f1d1d44; color: #fca5a5; }
[data-theme='dark'] .tag-purple { background: #4c1d9544; color: #c4b5fd; }

.chapter-progress {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-muted);
  background: var(--bg-secondary);
  border-radius: 999px;
  padding: 3px 10px;
  font-variant-numeric: tabular-nums;
  flex: none;
}

.chapter-body {
  border-top: 1px solid var(--border-color);
  padding: 14px 20px 18px;
}

.week-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.week-sub {
  font-size: 12.5px;
  color: var(--text-muted);
  margin-bottom: 12px;
  line-height: 1.6;
}

/* ---------- 天行 ---------- */
.day-row {
  display: grid;
  grid-template-columns: 22px 88px minmax(0, 1fr) 168px;
  gap: 10px;
  align-items: start;
  padding: 9px 4px;
  border-bottom: 1px dashed var(--border-color);
  font-size: 13.5px;
}

.day-row:last-of-type {
  border-bottom: 0;
}

.day-check input {
  width: 15px;
  height: 15px;
  accent-color: var(--accent-color);
  cursor: pointer;
  margin-top: 2px;
}

.day-name {
  color: var(--accent-color);
  font-weight: 600;
  font-size: 12.5px;
  line-height: 1.5;
}

.day-name em {
  font-style: normal;
  font-weight: 400;
  color: var(--text-muted);
}

.day-tasks {
  min-width: 0;
}

.task-line {
  display: block;
  color: var(--text-secondary);
  line-height: 1.65;
}

.day-output {
  font-size: 12px;
  color: #06a35f;
  background: #f0faf4;
  border-radius: 6px;
  padding: 4px 8px;
  text-align: center;
  line-height: 1.5;
}

[data-theme='dark'] .day-output {
  background: #064e3b44;
  color: #6ee7b7;
}

.day-row.done .task-line,
.day-row.done .day-name {
  color: var(--text-muted);
}

/* ---------- 验收清单 ---------- */
.check-block {
  margin-top: 12px;
  border-top: 1px solid var(--border-color);
  padding-top: 12px;
}

.check-block h4 {
  font-size: 13px;
  margin-bottom: 8px;
}

.check-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  line-height: 1.6;
}

.check-item input {
  width: 14px;
  height: 14px;
  accent-color: var(--accent-color);
  cursor: pointer;
  flex: none;
  margin-top: 3px;
}

.check-item.done span {
  text-decoration: line-through;
  color: var(--text-muted);
}

/* ---------- 阶段三 ---------- */
.proj-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-left: 4px solid var(--accent-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  padding: 20px 22px;
  margin-bottom: 18px;
}

.proj-title {
  font-size: 17px;
  margin-bottom: 6px;
}

.proj-sub {
  font-size: 12.5px;
  color: var(--text-muted);
  margin-bottom: 6px;
}

.proj-tech {
  font-size: 12.5px;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.arch {
  background: var(--bg-secondary);
  border-radius: 10px;
  padding: 14px 16px;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre;
  overflow-x: auto;
  color: var(--text-secondary);
  margin-bottom: 14px;
}

.week-block {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 12px;
}

.week-head {
  background: var(--bg-secondary);
  padding: 9px 14px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-color);
}

.week-block .day-row {
  padding: 9px 14px;
}

.resume-box {
  background: var(--bg-secondary);
  border-left: 3px solid var(--accent-color);
  border-radius: 0 8px 8px 0;
  padding: 12px 16px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-secondary);
  margin-bottom: 14px;
}

.resume-box strong {
  display: block;
  margin-bottom: 6px;
}

.resume-box p {
  margin-bottom: 4px;
}

/* ---------- 资源 ---------- */
.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.status-filter {
  display: flex;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
}

.status-filter button {
  border: 0;
  background: var(--card-bg);
  color: var(--text-secondary);
  font-size: 13px;
  padding: 6px 14px;
  cursor: pointer;
}

.status-filter button + button {
  border-left: 1px solid var(--border-color);
}

.status-filter button.on {
  background: var(--accent-color);
  color: #fff;
}

.toolbar-hint {
  font-size: 12px;
  color: var(--text-muted);
}

.res-group {
  margin-bottom: 22px;
}

.res-group-title {
  font-size: 15px;
  margin-bottom: 4px;
}

.res-stars {
  font-size: 12px;
  margin-left: 4px;
}

.res-group-sub {
  font-size: 12.5px;
  color: var(--text-muted);
  margin-bottom: 10px;
}

.res-card {
  display: flex;
  gap: 12px;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-soft);
  padding: 13px 16px;
  margin-bottom: 10px;
}

.res-check input {
  width: 15px;
  height: 15px;
  accent-color: var(--accent-color);
  cursor: pointer;
  margin-top: 2px;
}

.res-body {
  flex: 1;
  min-width: 0;
}

.res-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.res-name a {
  font-weight: 400;
  font-size: 12.5px;
  margin-left: 6px;
}

.res-note {
  font-size: 12px;
  color: var(--text-muted);
  margin-left: 6px;
  font-weight: 400;
}

.res-desc {
  font-size: 12.5px;
  color: var(--text-secondary);
  line-height: 1.65;
  margin-top: 3px;
}

.res-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 7px;
}

.res-tag {
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 4px;
  background: var(--bg-secondary);
  color: var(--accent-color);
}

.res-card.done .res-name,
.res-card.done .res-desc {
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

  .overall-progress {
    text-align: left;
  }

  .day-row {
    grid-template-columns: 22px 76px minmax(0, 1fr);
  }

  .day-output {
    display: none;
  }
}
</style>
