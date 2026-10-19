<template>
  <div class="leetcode-page">
    <div class="container">
      <header class="page-head">
        <div>
          <h1>算法刷题</h1>
          <p>按原帖章节整理的力扣题单，点击题目直达原题；勾选记录你的进度（保存在本地）。</p>
        </div>
        <div class="overall-progress" v-if="index">
          <div class="overall-num">{{ doneSet.size }}<span> / {{ totalUnique }}</span></div>
          <div class="bar"><div class="bar-fill" :style="{ width: overallPercent + '%' }"></div></div>
        </div>
      </header>

      <div v-if="!index" class="loading">加载中...</div>
      <template v-else>
        <div class="cat-tabs">
          <button
            v-for="cat in index.categories"
            :key="cat.key"
            class="cat-pill"
            :class="{ active: cat.key === activeKey }"
            @click="selectCategory(cat.key)"
          >
            {{ cat.name }}
            <span class="cat-count">{{ doneOf(cat.key) }}/{{ cat.uniqueTotal }}</span>
          </button>
        </div>

        <div class="toolbar">
          <div class="status-filter">
            <button :class="{ on: status === 'all' }" @click="status = 'all'">全部</button>
            <button :class="{ on: status === 'todo' }" @click="status = 'todo'">未做</button>
            <button :class="{ on: status === 'done' }" @click="status = 'done'">已做</button>
          </div>
          <el-input v-model="search" placeholder="搜索题号或题名…" size="small" clearable class="search-input">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <span class="toolbar-hint">{{ activeCatName }}：{{ activeDone }} / {{ activeUnique }} 题（{{ activeItems }} 条目）<template v-if="cloudActive"> · 已云同步</template></span>
        </div>

        <div v-if="loadingCat" class="loading">加载中...</div>
        <template v-else>
          <details
            v-for="chapter in visibleChapters"
            :key="chapter.title"
            class="chapter"
            :open="forceOpen || openChapters.has(chapter.title)"
            @toggle="onToggle($event, chapter.title)"
          >
            <summary class="chapter-head">
              <span class="chapter-title">{{ chapter.title }}</span>
              <span class="chapter-progress">{{ chapterDone(chapter) }}/{{ chapterTotal(chapter) }}</span>
            </summary>
            <div class="chapter-body">
              <div v-for="section in chapter.sections" :key="chapter.title + section.title" class="section">
                <h3 v-if="section.title" class="section-title">{{ section.title }}</h3>
                <div v-if="section.notes.length" class="section-notes">
                  <a v-for="(n, i) in section.notes" :key="i" :href="n.url" target="_blank" rel="noopener">
                    {{ n.label }}
                  </a>
                </div>
                <ul class="prob-list">
                  <li
                    v-for="p in visibleProblems(section)"
                    :key="section.title + p.pid + p.label"
                    :class="{ done: doneSet.has(p.pid) }"
                  >
                    <input type="checkbox" :checked="doneSet.has(p.pid)" @change="toggle(p.pid)" />
                    <a :href="p.url" target="_blank" rel="noopener">{{ p.label }}</a>
                    <span v-if="p.suffix" class="suffix">{{ p.suffix }}</span>
                  </li>
                </ul>
              </div>
            </div>
          </details>
          <div v-if="!visibleChapters.length" class="loading">没有匹配的题目</div>
        </template>

        <footer class="attribution">
          题单整理自力扣用户
          <a href="https://leetcode.cn/u/endlesscheng/" target="_blank" rel="noopener">@灵茶山艾府</a>
          的
          <a href="https://leetcode.cn/discuss/post/3141566/ru-he-ke-xue-shua-ti-by-endlesscheng-q3yd/" target="_blank" rel="noopener">《如何科学刷题》</a>
          系列算法题单（
          <a v-if="activeSource" :href="activeSource" target="_blank" rel="noopener">当前专题原帖</a> ｜
          <a href="https://gitee.com/li-dawei10086/leetcode-problem-list" target="_blank" rel="noopener">数据归档</a>
          ）。本页按原帖章节与顺序收录题目链接，讲解请看原帖。
        </footer>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { progressApi } from '@/api'
import { logStudy, unlogStudy } from '@/utils/studyLog'

interface Problem {
  pid: string
  label: string
  url: string
  suffix: string
}
interface Section {
  title: string
  notes: { label: string; url: string }[]
  problems: Problem[]
}
interface Chapter {
  title: string
  sections: Section[]
}

const DONE_KEY = 'leetcode:done'

const index = ref<any>(null)
const cache = ref<Record<string, { name: string; source: string; itemsTotal: number; uniqueTotal: number; chapters: Chapter[] }>>({})

const activeKey = ref('01')
const loadingCat = ref(false)
const search = ref('')
const status = ref<'all' | 'todo' | 'done'>('all')
const doneSet = ref<Set<string>>(new Set())
const openChapters = ref<Set<string>>(new Set())
const cloudActive = ref(false)
let pushTimer: number | undefined

const activeData = computed(() => cache.value[activeKey.value])
const activeCatName = computed(() => activeData.value?.name || '')
const activeSource = computed(() => activeData.value?.source || '')
const activeItems = computed(() => activeData.value?.itemsTotal || 0)
const activeUnique = computed(() => activeData.value?.uniqueTotal || 0)
const totalUnique = computed(() =>
  index.value ? index.value.categories.reduce((s: number, c: any) => s + c.uniqueTotal, 0) : 0)
const overallPercent = computed(() =>
  totalUnique.value ? Math.round((doneSet.value.size / totalUnique.value) * 1000) / 10 : 0)

const activeDone = computed(() => {
  const data = activeData.value
  if (!data) return 0
  const pids = new Set<string>()
  for (const c of data.chapters) for (const s of c.sections) for (const p of s.problems) pids.add(p.pid)
  return [...pids].filter(pid => doneSet.value.has(pid)).length
})

const visibleChapters = computed(() => {
  const data = activeData.value
  if (!data) return []
  const kw = search.value.trim().toLowerCase()
  let chapters = data.chapters
  if (kw || status.value !== 'all') {
    const matchProblem = (p: Problem) => {
      if (status.value === 'done' && !doneSet.value.has(p.pid)) return false
      if (status.value === 'todo' && doneSet.value.has(p.pid)) return false
      if (kw && !p.label.toLowerCase().includes(kw) && !p.pid.toLowerCase().includes(kw)) return false
      return true
    }
    chapters = chapters
      .map(c => ({
        ...c,
        sections: c.sections
          .map(s => ({ ...s, problems: s.problems.filter(matchProblem) }))
          .filter(s => s.problems.length),
      }))
      .filter(c => c.sections.length)
  }
  return chapters
})

const forceOpen = computed(() => !!search.value.trim() || status.value !== 'all')

function chapterPids(chapter: Chapter): Set<string> {
  const pids = new Set<string>()
  for (const s of chapter.sections) for (const p of s.problems) pids.add(p.pid)
  return pids
}

function chapterDone(chapter: Chapter) {
  return [...chapterPids(chapter)].filter(pid => doneSet.value.has(pid)).length
}

function chapterTotal(chapter: Chapter) {
  return chapterPids(chapter).size
}

function visibleProblems(section: Section) {
  // 过滤已在 computed 里完成；这里只负责展示（保留原顺序与重复）
  return section.problems
}

function doneOf(key: string) {
  const data = cache.value[key]
  if (!data) return 0
  const pids = new Set<string>()
  for (const c of data.chapters) for (const s of c.sections) for (const p of s.problems) pids.add(p.pid)
  return [...pids].filter(pid => doneSet.value.has(pid)).length
}

function onToggle(e: Event, title: string) {
  const open = (e.target as HTMLDetailsElement).open
  const next = new Set(openChapters.value)
  if (open) next.add(title)
  else next.delete(title)
  openChapters.value = next
}

function toggle(pid: string) {
  const next = new Set(doneSet.value)
  if (next.has(pid)) {
    next.delete(pid)
    unlogStudy(`lc:${pid}`)
  } else {
    next.add(pid)
    logStudy(`lc:${pid}`)
  }
  doneSet.value = next
  localStorage.setItem(DONE_KEY, JSON.stringify([...next]))
  if (cloudActive.value) pushDone(next)
}

function pushDone(set: Set<string>) {
  window.clearTimeout(pushTimer)
  pushTimer = window.setTimeout(() => {
    progressApi.put('leetcode', JSON.stringify([...set])).catch(() => { cloudActive.value = false })
  }, 1500)
}

// 登录时把云端进度与本地合并（并集：条目只会由未做变已做，不会回退）
async function loadDone() {
  let local: string[] = []
  try {
    local = (JSON.parse(localStorage.getItem(DONE_KEY) || '[]') as any[]).map(String)
  } catch (_) {}
  const merged = new Set<string>(local)
  if (localStorage.getItem('token')) {
    try {
      const res = await progressApi.get('leetcode')
      if (res.data.code === 200) {
        cloudActive.value = true
        const cloud = JSON.parse(res.data.data.value || '[]')
        if (Array.isArray(cloud)) cloud.forEach(x => merged.add(String(x)))
        if (JSON.stringify([...merged].sort()) !== JSON.stringify([...new Set(local)].sort())) {
          localStorage.setItem(DONE_KEY, JSON.stringify([...merged]))
          pushDone(merged)
        }
      }
    } catch (_) {}
  }
  doneSet.value = merged
}

onMounted(async () => {
  await loadDone()
  try {
    const res = await fetch('/data/leetcode/index.json')
    index.value = await res.json()
    if (index.value.categories.length) {
      await loadCategory(index.value.categories[0].key)
      if (activeData.value?.chapters.length) {
        openChapters.value = new Set([activeData.value.chapters[0].title])
      }
    }
  } catch (_) {}
})

async function loadCategory(key: string) {
  if (cache.value[key]) return
  loadingCat.value = true
  try {
    const res = await fetch(`/data/leetcode/${key}.json`)
    cache.value[key] = await res.json()
  } catch (_) {}
  loadingCat.value = false
}

async function selectCategory(key: string) {
  activeKey.value = key
  search.value = ''
  status.value = 'all'
  await loadCategory(key)
  const chapters = activeData.value?.chapters || []
  openChapters.value = new Set(chapters.length ? [chapters[0].title] : [])
}
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

.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 16px;
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

.search-input {
  width: 220px;
}

.toolbar-hint {
  font-size: 13px;
  color: var(--text-muted);
  margin-left: auto;
}

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
}

.chapter[open] > .chapter-head::before {
  transform: rotate(90deg);
}

.chapter-progress {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-muted);
  background: var(--bg-secondary);
  border-radius: 999px;
  padding: 3px 10px;
  font-variant-numeric: tabular-nums;
}

.chapter-body {
  border-top: 1px solid var(--border-color);
  padding: 6px 20px 16px;
}

.section-title {
  font-size: 14px;
  color: var(--text-primary);
  margin: 14px 0 8px;
}

.section-notes {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin: 6px 0 8px;
}

.section-notes a {
  font-size: 13px;
  color: var(--accent-color);
}

.prob-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.prob-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 4px;
  border-bottom: 1px dashed var(--border-color);
  font-size: 14px;
}

.prob-list li:last-child {
  border-bottom: 0;
}

.prob-list input {
  width: 15px;
  height: 15px;
  accent-color: var(--accent-color);
  cursor: pointer;
  flex: none;
}

.prob-list a {
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.prob-list a:hover {
  color: var(--accent-color);
}

.prob-list li.done a {
  color: var(--text-muted);
  text-decoration: line-through;
}

.prob-list .suffix {
  font-size: 12px;
  color: var(--text-muted);
  flex: none;
}

.loading {
  text-align: center;
  padding: 60px;
  color: var(--text-muted);
}

.attribution {
  margin-top: 28px;
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.8;
  border-top: 1px solid var(--border-color);
  padding: 16px 0 40px;
}
</style>
