<template>
  <div class="cet4-page">
    <div class="container">
      <header class="page-head">
        <div>
          <h1>📖 四级备考</h1>
          <p>刘晓艳四级课程清单，看完一节勾一节，进度保存在本地浏览器。</p>
        </div>
        <div class="overall-progress" v-if="index">
          <div class="overall-num">{{ doneSet.size }}<span> / {{ totalItems }}</span></div>
          <div class="bar"><div class="bar-fill" :style="{ width: overallPercent + '%' }"></div></div>
          <div class="overall-extra">本书已学 {{ doneDuration }}<template v-if="cloudActive"> · ☁</template></div>
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
            {{ shortName(cat.name) }}
            <span class="cat-count">{{ doneOf(cat.key) }}/{{ cat.itemsTotal }}</span>
          </button>
        </div>

        <div class="toolbar">
          <div class="status-filter">
            <button :class="{ on: status === 'all' }" @click="status = 'all'">全部</button>
            <button :class="{ on: status === 'todo' }" @click="status = 'todo'">未学</button>
            <button :class="{ on: status === 'done' }" @click="status = 'done'">已学</button>
          </div>
          <el-input v-model="search" placeholder="搜索课程…" size="small" clearable class="search-input">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <span class="toolbar-hint">{{ activeData?.name }}：{{ activeDone }} / {{ activeData?.itemsTotal }} 节 · 全书 {{ activeData?.duration }}</span>
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
                <ul class="lesson-list">
                  <li
                    v-for="item in visibleItems(section)"
                    :key="item.id"
                    :class="{ done: doneSet.has(item.id) }"
                  >
                    <input type="checkbox" :checked="doneSet.has(item.id)" @change="toggle(item.id, item.sec)" />
                    <span class="lesson-title">{{ item.title }}</span>
                    <span class="lesson-dur">{{ item.dur }}</span>
                  </li>
                </ul>
              </div>
            </div>
          </details>
          <div v-if="!visibleChapters.length" class="loading">没有匹配的课程</div>
        </template>

        <footer class="attribution">
          课程数据来自学城网「我的书课」（刘晓艳四级系列，共 {{ index.totalItems }} 节 / {{ index.totalDuration }}）。
          本页仅作个人学习进度记录，进度保存在本地浏览器。
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

interface Item {
  id: string
  title: string
  dur: string
  sec: number
}
interface Section {
  title: string
  items: Item[]
}
interface Chapter {
  title: string
  sections: Section[]
}

const DONE_KEY = 'cet4:done'

const index = ref<any>(null)
const cache = ref<Record<string, { name: string; itemsTotal: number; duration: string; chapters: Chapter[] }>>({})

const activeKey = ref('01')
const loadingCat = ref(false)
const search = ref('')
const status = ref<'all' | 'todo' | 'done'>('all')
const doneSet = ref<Set<string>>(new Set())
const doneSeconds = ref(0)
const openChapters = ref<Set<string>>(new Set())
const cloudActive = ref(false)
let pushTimer: number | undefined

const activeData = computed(() => cache.value[activeKey.value])
const totalItems = computed(() => index.value?.totalItems || 0)
const overallPercent = computed(() =>
  totalItems.value ? Math.round((doneSet.value.size / totalItems.value) * 1000) / 10 : 0)
const doneDuration = computed(() => {
  const h = doneSeconds.value
  return `${Math.floor(h / 3600)} 小时 ${Math.floor((h % 3600) / 60)} 分钟`
})

const visibleChapters = computed(() => {
  const data = activeData.value
  if (!data) return []
  const kw = search.value.trim().toLowerCase()
  if (!kw && status.value === 'all') return data.chapters
  const result = []
  for (const ch of data.chapters) {
    const sections = ch.sections
      .map(s => ({
        ...s,
        items: s.items.filter(it => {
          if (status.value === 'done' && !doneSet.value.has(it.id)) return false
          if (status.value === 'todo' && doneSet.value.has(it.id)) return false
          if (kw && !it.title.toLowerCase().includes(kw)) return false
          return true
        }),
      }))
      .filter(s => s.items.length)
    if (sections.length) result.push({ ...ch, sections })
  }
  return result
})

const forceOpen = computed(() => !!search.value.trim() || status.value !== 'all')

const activeDone = computed(() => {
  const data = activeData.value
  if (!data) return 0
  const ids = new Set<string>()
  for (const c of data.chapters) for (const s of c.sections) for (const it of s.items) ids.add(it.id)
  return [...ids].filter(id => doneSet.value.has(id)).length
})

function visibleItems(section: Section) {
  return section.items
}

function chapterDone(chapter: Chapter) {
  return chapter.sections.reduce((s, sec) => s + sec.items.filter(it => doneSet.value.has(it.id)).length, 0)
}

function chapterTotal(chapter: Chapter) {
  return chapter.sections.reduce((s, sec) => s + sec.items.length, 0)
}

function doneOf(key: string) {
  const data = cache.value[key]
  if (!data) return 0
  return data.chapters.reduce((s, c) => s + c.sections.reduce((t, sec) => t + sec.items.filter(it => doneSet.value.has(it.id)).length, 0), 0)
}

function shortName(name: string) {
  return name.replace(/（.*?）/g, '')
}

function onToggle(e: Event, title: string) {
  const open = (e.target as HTMLDetailsElement).open
  const next = new Set(openChapters.value)
  if (open) next.add(title)
  else next.delete(title)
  openChapters.value = next
}

function toggle(id: string, sec: number) {
  const next = new Set(doneSet.value)
  if (next.has(id)) {
    next.delete(id)
    unlogStudy(`cet4:${id}`)
  } else {
    next.add(id)
    logStudy(`cet4:${id}`, sec)
  }
  doneSet.value = next
  localStorage.setItem(DONE_KEY, JSON.stringify([...next]))
  recalcSeconds()
  if (cloudActive.value) pushDone(next)
}

function pushDone(set: Set<string>) {
  window.clearTimeout(pushTimer)
  pushTimer = window.setTimeout(() => {
    progressApi.put('cet4', JSON.stringify([...set])).catch(() => { cloudActive.value = false })
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
      const res = await progressApi.get('cet4')
      if (res.data.code === 200) {
        cloudActive.value = true
        const cloud = JSON.parse(res.data.data.value || '[]')
        if (Array.isArray(cloud)) cloud.forEach(x => merged.add(String(x)))
        if (merged.size !== local.length) {
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
    const res = await fetch('/data/cet4/index.json')
    index.value = await res.json()
    if (index.value.categories.length) {
      await loadCategory(index.value.categories[0].key)
      if (activeData.value?.chapters.length) {
        openChapters.value = new Set([activeData.value.chapters[0].title])
      }
    }
  } catch (_) {}
  recalcSeconds()
})

async function loadCategory(key: string) {
  if (cache.value[key]) return
  loadingCat.value = true
  try {
    const res = await fetch(`/data/cet4/${key}.json`)
    cache.value[key] = await res.json()
  } catch (_) {}
  loadingCat.value = false
}

async function selectCategory(key: string) {
  activeKey.value = key
  search.value = ''
  status.value = 'all'
  await loadCategory(key)
  recalcSeconds()
  const chapters = activeData.value?.chapters || []
  openChapters.value = new Set(chapters.length ? [chapters[0].title] : [])
}

// 已学时长按全量数据重算，避免本地计数漂移
function recalcSeconds() {
  let sec = 0
  for (const data of Object.values(cache.value)) {
    for (const c of data.chapters) {
      for (const s of c.sections) {
        for (const it of s.items) {
          if (doneSet.value.has(it.id)) sec += it.sec
        }
      }
    }
  }
  doneSeconds.value = sec
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
  min-width: 200px;
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

.overall-extra {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 5px;
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
  width: 200px;
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

.lesson-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.lesson-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 4px;
  border-bottom: 1px dashed var(--border-color);
  font-size: 14px;
}

.lesson-list li:last-child {
  border-bottom: 0;
}

.lesson-list input {
  width: 15px;
  height: 15px;
  accent-color: var(--accent-color);
  cursor: pointer;
  flex: none;
}

.lesson-title {
  flex: 1;
  min-width: 0;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lesson-list li.done .lesson-title {
  color: var(--text-muted);
  text-decoration: line-through;
}

.lesson-dur {
  flex: none;
  font-size: 12px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
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

@media (max-width: 760px) {
  .page-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
