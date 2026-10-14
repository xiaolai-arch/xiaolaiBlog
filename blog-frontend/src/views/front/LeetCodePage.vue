<template>
  <div class="leetcode-page">
    <div class="container">
      <header class="page-head">
        <div>
          <h1>🧮 算法刷题</h1>
          <p>按专题整理的力扣题单，点击题目直达原题；勾选记录你的进度（保存在本地）。</p>
        </div>
        <div class="overall-progress" v-if="index">
          <div class="overall-num">{{ doneCount }}<span> / {{ index.uniqueTotal }}</span></div>
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
            <span class="cat-count">{{ doneOf(cat.key) }}/{{ cat.total }}</span>
          </button>
        </div>

        <div class="toolbar">
          <select v-model="chapter" class="chapter-select">
            <option value="">全部章节</option>
            <option v-for="c in chapters" :key="c" :value="c">{{ c }}</option>
          </select>
          <div class="status-filter">
            <button :class="{ on: status === 'all' }" @click="status = 'all'">全部</button>
            <button :class="{ on: status === 'todo' }" @click="status = 'todo'">未做</button>
            <button :class="{ on: status === 'done' }" @click="status = 'done'">已做</button>
          </div>
          <el-input v-model="search" placeholder="搜索题号或题名…" size="small" clearable class="search-input">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>

        <div class="cat-bar">
          <span>{{ activeCatName }}：{{ activeDone }} / {{ activeTotal }} 题</span>
          <span class="cat-bar-right">已加载 {{ problems.length }} 题</span>
        </div>

        <div v-if="loadingCat" class="loading">加载中...</div>
        <template v-else>
          <div class="prob-table">
            <div class="prob-row head">
              <span class="col-check"></span>
              <span class="col-id">题号</span>
              <span class="col-title">题目</span>
              <span class="col-chapter">章节</span>
            </div>
            <div v-for="p in paged" :key="activeKey + p.id" class="prob-row" :class="{ done: isDone(p.id) }">
              <span class="col-check">
                <input type="checkbox" :checked="isDone(p.id)" @change="toggle(p.id)" />
              </span>
              <span class="col-id">{{ p.id }}</span>
              <span class="col-title">
                <a :href="p.url" target="_blank" rel="noopener">{{ p.title }}</a>
              </span>
              <span class="col-chapter" :title="p.section">{{ p.chapter }}</span>
            </div>
            <div v-if="!filtered.length" class="loading">没有匹配的题目</div>
          </div>

          <div class="pagination" v-if="filtered.length > pageSize">
            <el-pagination
              :current-page="page"
              :page-size="pageSize"
              :total="filtered.length"
              layout="prev, pager, next"
              @current-change="onPageChange"
              background
              small
            />
          </div>
        </template>

        <div class="guides" v-if="guides.length">
          <button class="guides-toggle" @click="showGuides = !showGuides">
            📎 讲解与套路（{{ guides.length }}）{{ showGuides ? ' ▴' : ' ▾' }}
          </button>
          <div v-if="showGuides" class="guides-list">
            <a v-for="(g, i) in guides" :key="i" :href="g.url" target="_blank" rel="noopener">{{ g.label }}</a>
          </div>
        </div>

        <footer class="attribution">
          题单整理自力扣用户
          <a :href="activeSource || 'https://leetcode.cn/u/endlesscheng/'" target="_blank" rel="noopener">@灵茶山艾府</a>
          发布的算法题单（点击查看当前专题原帖），
          <a href="https://gitee.com/li-dawei10086/leetcode-problem-list" target="_blank" rel="noopener">数据归档仓库</a>。
          本页仅收录题目索引与链接，讲解请看原帖。
        </footer>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'

interface Problem {
  id: number | string
  title: string
  url: string
  chapter: string
  section: string
}

const DONE_KEY = 'leetcode:done'
const pageSize = 50

const index = ref<any>(null)
// 已加载专题的数据缓存（响应式，供 computed 派生）
const cache = ref<Record<string, { name: string; source: string; total: number; problems: Problem[]; guides: any[] }>>({})

const activeKey = ref('01')
const loadingCat = ref(false)
const search = ref('')
const chapter = ref('')
const status = ref<'all' | 'todo' | 'done'>('all')
const page = ref(1)
const showGuides = ref(false)
const doneSet = ref<Set<number | string>>(new Set())

const activeData = computed(() => cache.value[activeKey.value])
const problems = computed<Problem[]>(() => activeData.value?.problems || [])
const guides = computed(() => activeData.value?.guides || [])
const activeSource = computed(() => activeData.value?.source || '')
const activeCatName = computed(() => activeData.value?.name || '')
const activeTotal = computed(() => activeData.value?.total || 0)
const activeDone = computed(() => problems.value.filter(p => doneSet.value.has(p.id)).length)
const doneCount = computed(() => doneSet.value.size)
const overallPercent = computed(() =>
  index.value ? Math.round((doneCount.value / index.value.uniqueTotal) * 1000) / 10 : 0)

const chapters = computed(() => {
  const set = new Set(problems.value.map(p => p.chapter))
  return [...set]
})

const filtered = computed(() => {
  let list = problems.value
  if (chapter.value) list = list.filter(p => p.chapter === chapter.value)
  if (status.value !== 'all') {
    list = list.filter(p => (status.value === 'done') === doneSet.value.has(p.id))
  }
  const kw = search.value.trim().toLowerCase()
  if (kw) {
    list = list.filter(p =>
      String(p.id).toLowerCase().includes(kw) || p.title.toLowerCase().includes(kw))
  }
  return list
})

const paged = computed(() => filtered.value.slice((page.value - 1) * pageSize, page.value * pageSize))

onMounted(async () => {
  try {
    const saved = JSON.parse(localStorage.getItem(DONE_KEY) || '[]')
    doneSet.value = new Set(saved)
  } catch (_) {}
  try {
    const res = await fetch('/data/leetcode/index.json')
    index.value = await res.json()
    if (index.value.categories.length) {
      await loadCategory(index.value.categories[0].key)
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
  page.value = 1
  chapter.value = ''
  showGuides.value = false
  await loadCategory(key)
}

function doneOf(key: string) {
  const data = cache.value[key]
  if (!data) return 0
  return data.problems.filter(p => doneSet.value.has(p.id)).length
}

function isDone(id: number | string) {
  return doneSet.value.has(id)
}

function toggle(id: number | string) {
  const next = new Set(doneSet.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  doneSet.value = next
  localStorage.setItem(DONE_KEY, JSON.stringify([...next]))
}

function onPageChange(p: number) {
  page.value = p
  window.scrollTo({ top: 240, behavior: 'smooth' })
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
  margin-bottom: 12px;
}

.chapter-select {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--card-bg);
  color: var(--text-primary);
  font-size: 13px;
  padding: 6px 10px;
  outline: none;
  max-width: 220px;
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

.cat-bar {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--text-muted);
  padding: 8px 2px;
}

.prob-table {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  overflow: hidden;
}

.prob-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 16px;
  border-top: 1px solid var(--border-color);
  font-size: 14px;
}

.prob-row.head {
  border-top: 0;
  background: var(--bg-secondary);
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 600;
}

.prob-row:not(.head):hover {
  background: var(--bg-secondary);
}

.prob-row.done .col-title a {
  color: var(--text-muted);
}

.prob-row.done .col-id,
.prob-row.done .col-chapter {
  color: var(--text-muted);
}

.col-check {
  flex: none;
  width: 22px;
}

.col-check input {
  width: 15px;
  height: 15px;
  accent-color: var(--accent-color);
  cursor: pointer;
}

.col-id {
  flex: none;
  width: 74px;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}

.col-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-chapter {
  flex: none;
  width: 180px;
  font-size: 12px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination {
  display: flex;
  justify-content: center;
  padding: 24px 0 8px;
}

.guides {
  margin-top: 20px;
}

.guides-toggle {
  border: 1px dashed var(--border-color);
  background: var(--card-bg);
  color: var(--text-secondary);
  font-size: 13px;
  padding: 9px 16px;
  border-radius: 10px;
  cursor: pointer;
  width: 100%;
  text-align: left;
}

.guides-toggle:hover {
  color: var(--accent-color);
  border-color: var(--accent-color);
}

.guides-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
}

.guides-list a {
  font-size: 13px;
  color: var(--text-secondary);
}

.guides-list a:hover {
  color: var(--accent-color);
}

.attribution {
  margin-top: 28px;
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.8;
  border-top: 1px solid var(--border-color);
  padding: 16px 0 40px;
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

  .col-chapter {
    display: none;
  }
}
</style>
