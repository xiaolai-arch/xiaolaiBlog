<template>
  <div class="markdown-tool">
    <div class="tool-header">
      <div class="tool-title">
        <h1>📓 我的笔记</h1>
        <p>Typora 式即时渲染 · 多篇笔记管理 · 自动保存到本地浏览器</p>
      </div>
    </div>

    <div class="notes-layout">
      <aside class="notes-panel">
        <button class="new-note-btn" @click="createNote">＋ 新建笔记</button>
        <el-input v-model="search" placeholder="搜索笔记…" size="small" clearable class="note-search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="note-list">
          <div
            v-for="note in filteredNotes"
            :key="note.id"
            class="note-item"
            :class="{ active: note.id === activeId }"
            @click="setActive(note.id)"
          >
            <div class="note-item-body">
              <div class="note-item-title">{{ note.title || '无标题笔记' }}</div>
              <div class="note-item-time">{{ timeAgo(note.updateTime) }}</div>
            </div>
            <button class="note-del" title="删除" @click.stop="removeNote(note.id)">✕</button>
          </div>
          <div v-if="!filteredNotes.length" class="note-empty">
            {{ notes.length ? '没有匹配的笔记' : '还没有笔记，点上面新建一篇吧' }}
          </div>
        </div>
        <div class="note-count">{{ notes.length }} 篇笔记 · 本地保存</div>
      </aside>

      <section class="note-editor">
        <div class="note-title-bar">
          <input
            v-model="activeTitle"
            class="note-title-input"
            placeholder="笔记标题…"
            @input="touchActive"
          />
        </div>
        <div ref="editorRef" class="editor-host"></div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useThemeStore } from '@/stores/theme'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import Vditor from 'vditor'
import 'vditor/dist/index.css'

const NOTES_KEY = 'tools:notes'
const ACTIVE_KEY = 'tools:notes:active'
const LEGACY_DRAFT_KEY = 'tools:markdown-draft'

interface Note {
  id: string
  title: string
  content: string
  updateTime: number
  createTime: number
}

const themeStore = useThemeStore()
const editorRef = ref<HTMLElement>()
const notes = ref<Note[]>([])
const activeId = ref('')
const search = ref('')
let vditor: Vditor | null = null
let saveTimer: number | undefined

const activeNote = computed(() => notes.value.find(n => n.id === activeId.value))
const activeTitle = computed({
  get: () => activeNote.value?.title || '',
  set: (v: string) => { if (activeNote.value) activeNote.value.title = v },
})
const filteredNotes = computed(() => {
  const kw = search.value.trim().toLowerCase()
  const list = [...notes.value].sort((a, b) => b.updateTime - a.updateTime)
  if (!kw) return list
  return list.filter(n =>
    (n.title || '').toLowerCase().includes(kw) || n.content.toLowerCase().includes(kw))
})

function loadNotes(): Note[] {
  // 迁移旧版单草稿
  const legacy = localStorage.getItem(LEGACY_DRAFT_KEY)
  const existing = localStorage.getItem(NOTES_KEY)
  if (!existing && legacy && legacy.trim()) {
    const migrated: Note[] = [{
      id: String(Date.now()),
      title: '导入的草稿',
      content: legacy,
      createTime: Date.now(),
      updateTime: Date.now(),
    }]
    localStorage.setItem(NOTES_KEY, JSON.stringify(migrated))
    localStorage.removeItem(LEGACY_DRAFT_KEY)
    return migrated
  }
  try {
    return JSON.parse(existing || '[]')
  } catch (_) {
    return []
  }
}

function persist() {
  localStorage.setItem(NOTES_KEY, JSON.stringify(notes.value))
  if (activeId.value) localStorage.setItem(ACTIVE_KEY, activeId.value)
}

function blankNote(): Note {
  return {
    id: String(Date.now()) + Math.random().toString(36).slice(2, 6),
    title: '',
    content: '',
    createTime: Date.now(),
    updateTime: Date.now(),
  }
}

function createNote() {
  const note = blankNote()
  notes.value.unshift(note)
  setActive(note.id)
  persist()
}

function setActive(id: string) {
  if (activeId.value === id) return
  activeId.value = id
  const note = notes.value.find(n => n.id === id)
  vditor?.setValue(note?.content || '', true)
  persist()
}

function scheduleSave() {
  const id = activeId.value
  window.clearTimeout(saveTimer)
  saveTimer = window.setTimeout(() => {
    const note = notes.value.find(n => n.id === id)
    if (!note) return
    note.content = vditor?.getValue() || ''
    note.updateTime = Date.now()
    persist()
  }, 600)
}

function touchActive() {
  if (activeNote.value) activeNote.value.updateTime = Date.now()
  scheduleSave()
}

async function removeNote(id: string) {
  const note = notes.value.find(n => n.id === id)
  try {
    await ElMessageBox.confirm(`确定删除「${note?.title || '无标题笔记'}」吗？删除后无法恢复。`, '删除笔记', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch (_) {
    return
  }
  const idx = notes.value.findIndex(n => n.id === id)
  if (idx === -1) return
  notes.value.splice(idx, 1)
  if (activeId.value === id) {
    const next = notes.value[idx] || notes.value[idx - 1]
    if (next) {
      activeId.value = ''
      setActive(next.id)
    } else {
      const fresh = blankNote()
      notes.value.unshift(fresh)
      activeId.value = ''
      setActive(fresh.id)
    }
  }
  persist()
  ElMessage.success('已删除')
}

function applyTheme() {
  const dark = themeStore.isDark
  vditor?.setTheme(dark ? 'dark' : 'classic', dark ? 'dark' : 'light', dark ? 'github-dark' : 'github')
}

onMounted(() => {
  notes.value = loadNotes()
  if (!notes.value.length) {
    notes.value = [blankNote()]
    persist()
  }
  const savedActive = localStorage.getItem(ACTIVE_KEY)
  activeId.value = notes.value.some(n => n.id === savedActive)
    ? savedActive!
    : notes.value[0].id

  vditor = new Vditor(editorRef.value!, {
    mode: 'ir',
    // 运行期资源（lute/高亮/katex）已拷贝到 public/vendor/vditor，离线可用
    cdn: '/vendor/vditor',
    lang: 'zh_CN',
    placeholder: '开始书写吧…支持标题、列表、代码块、表格、公式',
    value: activeNote.value?.content || '',
    cache: { enable: false },
    counter: { enable: true },
    height: Math.min(820, Math.max(440, window.innerHeight - 330)),
    toolbar: [
      'headings', 'bold', 'italic', 'strike', '|',
      'list', 'ordered-list', 'check', 'quote', '|',
      'code', 'inline-code', 'link', 'table', '|',
      'undo', 'redo', '|',
      'edit-mode', 'outline', 'preview', 'fullscreen', 'export',
    ],
    after: applyTheme,
    input: scheduleSave,
  })
})

watch(() => themeStore.isDark, applyTheme)

onBeforeUnmount(() => {
  window.clearTimeout(saveTimer)
  const note = activeNote.value
  if (note) note.content = vditor?.getValue() || ''
  persist()
  vditor?.destroy()
  vditor = null
})

function timeAgo(ts: number) {
  const d = dayjs(ts)
  const minutes = Math.floor((Date.now() - ts) / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  if (dayjs().isSame(d, 'day')) return d.format('HH:mm')
  if (dayjs().subtract(1, 'day').isSame(d, 'day')) return '昨天'
  return d.format('MM-DD')
}
</script>

<style scoped>
.tool-header {
  padding: 36px 0 20px;
}

.tool-header h1 {
  font-size: 26px;
  margin-bottom: 6px;
}

.tool-header p {
  color: var(--text-secondary);
  font-size: 13px;
}

.notes-layout {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  gap: 16px;
  align-items: stretch;
  padding-bottom: 40px;
}

.notes-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 14px;
  box-shadow: var(--shadow-soft);
  min-height: 480px;
}

.new-note-btn {
  border: 0;
  cursor: pointer;
  background: var(--accent-color);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  padding: 10px;
  border-radius: 10px;
  transition: background 0.2s;
  flex: none;
}

.new-note-btn:hover {
  background: var(--accent-hover);
}

.note-search {
  flex: none;
}

.note-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.note-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 10px;
  border-radius: 10px;
  cursor: pointer;
  border: 1px solid transparent;
}

.note-item:hover {
  background: var(--bg-secondary);
}

.note-item.active {
  background: var(--bg-secondary);
  border-color: var(--accent-color);
}

.note-item-body {
  flex: 1;
  min-width: 0;
}

.note-item-title {
  font-size: 13px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.note-item.active .note-item-title {
  color: var(--accent-color);
  font-weight: 600;
}

.note-item-time {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}

.note-del {
  border: 0;
  background: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 12px;
  padding: 4px;
  border-radius: 6px;
  opacity: 0;
  transition: opacity 0.15s, color 0.15s;
  flex: none;
}

.note-item:hover .note-del {
  opacity: 1;
}

.note-del:hover {
  color: #e74c3c;
}

.note-empty {
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
  padding: 30px 0;
}

.note-count {
  flex: none;
  font-size: 11px;
  color: var(--text-muted);
  text-align: center;
  border-top: 1px solid var(--border-color);
  padding-top: 10px;
}

.note-editor {
  min-width: 0;
}

.note-title-bar {
  margin-bottom: 10px;
}

.note-title-input {
  width: 100%;
  border: 1px solid transparent;
  background: var(--card-bg);
  border-radius: 10px;
  padding: 10px 14px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  outline: none;
  box-shadow: var(--shadow-soft);
}

.note-title-input:focus {
  border-color: var(--accent-color);
}

.editor-host :deep(.vditor) {
  border-radius: var(--radius-lg);
  border-color: var(--border-color);
  box-shadow: var(--shadow-soft);
}

.editor-host :deep(.vditor-toolbar) {
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
}

.editor-host :deep(.vditor-reset) {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Noto Sans SC', sans-serif;
}

@media (max-width: 760px) {
  .notes-layout {
    grid-template-columns: 1fr;
  }

  .notes-panel {
    min-height: 0;
  }

  .note-list {
    max-height: 180px;
  }
}
</style>
