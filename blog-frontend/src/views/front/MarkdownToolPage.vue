<template>
  <div class="markdown-tool">
    <div class="tool-header">
      <div class="tool-title">
        <h1>我的笔记</h1>
        <p>Typora 式即时渲染 · {{ mode === 'cloud' ? '云端同步，多设备可用' : '自动保存到本地浏览器' }}</p>
      </div>
      <div class="header-actions">
        <button v-if="mode === 'cloud'" class="ghost-btn" @click="exportAll">导出全部</button>
        <button v-if="mode === 'cloud'" class="ghost-btn danger" @click="logoutCloud">退出同步</button>
      </div>
    </div>

    <div v-if="mode === 'local'" class="mode-banner">
      <span>当前为本地模式，笔记只存在这台浏览器里。</span>
      <router-link :to="{ path: '/login', query: { redirect: $route.fullPath } }">注册 / 登录后开启云端同步 →</router-link>
    </div>
    <div v-else-if="pendingLocalCount > 0" class="mode-banner">
      <span>检测到 {{ pendingLocalCount }} 篇本地笔记，是否上传到云端？</span>
      <span class="banner-actions">
        <button class="mini-btn" :disabled="migrating" @click="migrateLocal">
          {{ migrating ? '上传中…' : '上传到云端' }}
        </button>
        <button class="mini-btn ghost" :disabled="migrating" @click="dismissMigration">忽略</button>
      </span>
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
        <div class="note-count">
          {{ notes.length }} 篇笔记 ·
          {{ mode === 'cloud' ? savedLabel : '本地保存' }}
        </div>
      </aside>

      <section class="note-editor">
        <div class="note-title-bar">
          <input
            v-model="activeTitle"
            :disabled="!editorReady || (mode === 'cloud' && activeNote?.content == null)"
            class="note-title-input"
            placeholder="笔记标题…"
            @input="touchActive"
          />
        </div>
        <p v-if="loadError" role="alert">{{ loadError }} <button @click="retryContent">重试加载</button></p>
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
import { noteApi } from '@/api'
import { accountKey, accountScope } from '@/utils/accountStorage'
import { createNoteSaveQueue } from '@/utils/noteSaveQueue'
import { useAuthStore } from '@/stores/auth'

const scope = accountScope()
const sessionToken = localStorage.getItem('token')
const NOTES_KEY = scope === 'guest' ? 'tools:notes' : accountKey('tools:notes', scope)
const ACTIVE_KEY = `${NOTES_KEY}:active`
const LEGACY_DRAFT_KEY = 'tools:markdown-draft'
const MIGRATED_FLAG = accountKey('tools:notes:migrated', scope)

interface Note {
  id: string | number
  title: string
  content?: string | null
  updateTime: number | string
  createTime?: number | string
}

const themeStore = useThemeStore()
const editorRef = ref<HTMLElement>()
const mode = ref<'cloud' | 'local'>('local')
const notes = ref<Note[]>([])
const activeId = ref<string | number>('')
const search = ref('')
const savedLabel = ref('已同步')
const pendingLocalCount = ref(0)
const migrating = ref(false)
let vditor: Vditor | null = null
const editorReady = ref(false)
const loadError = ref('')
let disposed = false
let switching = false
const outbox = createNoteSaveQueue({
  storage: localStorage,
  key: accountKey('notes:outbox', scope),
  save: async draft => {
    if (!sessionToken || localStorage.getItem('token') !== sessionToken) throw new Error('Session changed')
    const res = await noteApi.update(Number(draft.id), { title: draft.title, content: draft.content })
    if (res.data.code !== 200) throw new Error(res.data.message || '保存失败')
  },
  onState: state => { savedLabel.value = state === 'saved' ? `已保存 ${dayjs().format('HH:mm:ss')}` : state === 'failed' ? '保存失败，草稿已保留' : '待同步' },
})

const activeNote = computed(() => notes.value.find(n => n.id === activeId.value))
const activeTitle = computed({
  get: () => activeNote.value?.title || '',
  set: (v: string) => { if (activeNote.value) activeNote.value.title = v },
})
const filteredNotes = computed(() => {
  const kw = search.value.trim().toLowerCase()
  const ts = (n: Note) => dayjs(n.updateTime).valueOf()
  const list = [...notes.value].sort((a, b) => ts(b) - ts(a))
  if (!kw) return list
  return list.filter(n =>
    (n.title || '').toLowerCase().includes(kw) || (n.content || '').toLowerCase().includes(kw))
})

/* ---------------- 本地存储 ---------------- */
function loadLocalNotes(): Note[] {
  const legacy = scope === 'guest' ? localStorage.getItem(LEGACY_DRAFT_KEY) : null
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

function persistLocal() {
  localStorage.setItem(NOTES_KEY, JSON.stringify(notes.value))
  if (activeId.value !== '') localStorage.setItem(ACTIVE_KEY, String(activeId.value))
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

function initLocalMode() {
  mode.value = 'local'
  notes.value = loadLocalNotes()
  if (!notes.value.length) {
    notes.value = [blankNote()]
    persistLocal()
  }
  const saved = localStorage.getItem(ACTIVE_KEY)
  activeId.value = notes.value.some(n => n.id === saved) ? saved! : notes.value[0].id
}

/* ---------------- 云端 ---------------- */
async function tryCloudMode() {
  try {
    const res = await noteApi.list()
    if (res.data.code !== 200) throw new Error()
    mode.value = 'cloud'
    notes.value = res.data.data || []
    if (!notes.value.length) {
      const created = await noteApi.create({ title: '', content: '' })
      notes.value = [created.data.data]
    }
    activeId.value = notes.value[0].id
    // 本地未迁移的笔记提醒
    pendingLocalCount.value = migrationCandidates().length
    for (const draft of outbox.pending()) {
      const note = notes.value.find(n => String(n.id) === String(draft.id))
      if (note) Object.assign(note, draft)
    }
    void outbox.flush()
  } catch (_) {
    initLocalMode()
  }
}

async function migrateLocal() {
  const candidates = migrationCandidates()
  if (!candidates.length) {
    localStorage.setItem(MIGRATED_FLAG, '1')
    pendingLocalCount.value = 0
    return
  }
  migrating.value = true
  try {
    for (const { note, storageKey } of candidates) {
      await noteApi.create({ title: note.title || '无标题笔记', content: note.content || '' })
      // Keep not-yet-uploaded drafts on partial failure; do not upload successful ones twice.
      const remaining = readStoredNotes(storageKey).filter(n => n.id !== note.id)
      localStorage.setItem(storageKey, JSON.stringify(remaining))
    }
    localStorage.setItem(MIGRATED_FLAG, '1')
    const res = await noteApi.list()
    notes.value = res.data.data || []
    for (const draft of outbox.pending()) {
      const note = notes.value.find(n => String(n.id) === String(draft.id))
      if (note) Object.assign(note, draft)
    }
    activeId.value = notes.value[0]?.id ?? ''
    if (activeId.value !== '') await ensureContent(activeNote.value!)
    pendingLocalCount.value = 0
    ElMessage.success(`已上传 ${candidates.length} 篇到云端`)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '上传失败，请稍后再试')
    pendingLocalCount.value = migrationCandidates().length
  }
  migrating.value = false
}

function dismissMigration() {
  localStorage.setItem(MIGRATED_FLAG, '1')
  pendingLocalCount.value = 0
}

function readStoredNotes(key: string): Note[] {
  try {
    const value = JSON.parse(localStorage.getItem(key) || '[]')
    return Array.isArray(value) ? value : []
  } catch (_) { return [] }
}

function migrationCandidates() {
  const keys = [NOTES_KEY]
  if (NOTES_KEY !== 'tools:notes' && !localStorage.getItem(MIGRATED_FLAG)) keys.push('tools:notes')
  return keys.flatMap(storageKey => readStoredNotes(storageKey)
    .filter(note => (note.content || '').trim() || (note.title || '').trim())
    .map(note => ({ note, storageKey })))
}

function displayContent(content: string) {
  switching = true
  vditor?.setValue(content, true)
  switching = false
}

async function ensureContent(note: Note) {
  loadError.value = ''
  if (note.content != null) {
    if (activeNote.value?.id === note.id) {
      displayContent(note.content)
      if (editorReady.value) vditor?.enable()
    }
    return
  }
  vditor?.disabled()
  try {
    const res = await noteApi.detail(Number(note.id))
    if (res.data.code !== 200) throw new Error(res.data.message || '加载失败')
    if (disposed) return
    note.content = res.data.data.content || ''
    if (activeNote.value?.id === note.id) displayContent(note.content!)
  } catch (_) {
    if (activeNote.value?.id === note.id) loadError.value = '正文加载失败，暂时不能编辑。'
  } finally {
    if (!disposed && activeNote.value?.content != null && editorReady.value) vditor?.enable()
  }
}

function retryContent() { if (activeNote.value) void ensureContent(activeNote.value) }

async function logoutCloud() {
  await outbox.flush()
  await useAuthStore().logout()
  location.reload()
}

/* ---------------- 通用操作 ---------------- */
async function createNote() {
  if (mode.value === 'cloud') {
    try {
      const res = await noteApi.create({ title: '', content: '' })
      const note = res.data.data
      note.content = ''
      notes.value.unshift(note)
      activeId.value = ''
      setActive(note.id)
    } catch (e: any) {
      ElMessage.error('新建失败，请稍后再试')
    }
    return
  }
  const note = blankNote()
  notes.value.unshift(note)
  setActive(note.id)
  persistLocal()
}

function setActive(id: string | number) {
  if (activeId.value === id) return
  activeId.value = id
  loadError.value = ''
  const note = activeNote.value
  displayContent(note?.content || '')
  if (mode.value === 'cloud' && note) void ensureContent(note)
  if (mode.value === 'local') persistLocal()
}

function scheduleSave() {
  if (switching || !editorReady.value) return
  const note = activeNote.value
  if (!note || (mode.value === 'cloud' && note.content == null)) return
  // Snapshot now: after a route or note switch the editor belongs to another note.
  note.content = vditor?.getValue() || ''
  note.updateTime = Date.now()
  if (mode.value === 'local') persistLocal()
  else outbox.enqueue({ id: note.id, title: note.title, content: note.content })
}

function touchActive() { scheduleSave() }

async function removeNote(id: string | number) {
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
  if (mode.value === 'cloud') {
    try {
      await outbox.flush()
      const res = await noteApi.remove(Number(id))
      if (res.data.code !== 200) throw new Error('删除失败')
      await outbox.discard(id)
    } catch (_) {
      ElMessage.error('删除失败')
      return
    }
  }
  const idx = notes.value.findIndex(n => n.id === id)
  if (idx === -1) return
  notes.value.splice(idx, 1)
  if (activeId.value === id) {
    const next = notes.value[idx] || notes.value[idx - 1]
    if (next) {
      activeId.value = ''
      setActive(next.id)
    } else if (mode.value === 'local') {
      const fresh = blankNote()
      notes.value.unshift(fresh)
      activeId.value = ''
      setActive(fresh.id)
    } else {
      const created = await noteApi.create({ title: '', content: '' })
      created.data.data.content = ''
      notes.value.unshift(created.data.data)
      activeId.value = ''
      setActive(created.data.data.id)
    }
  }
  if (mode.value === 'local') persistLocal()
  ElMessage.success('已删除')
}

async function exportAll() {
  let md = ''
  const list = [...notes.value].sort((a, b) => dayjs(a.updateTime).valueOf() - dayjs(b.updateTime).valueOf())
  for (const n of list) {
    let content = n.content
    if (content == null) {
      try {
        const res = await noteApi.detail(Number(n.id))
        content = res.data.data.content || ''
        n.content = content
      } catch (_) { ElMessage.error('部分笔记未能加载，请重试导出'); return }
    }
    md += `# ${n.title || '无标题笔记'}\n\n${content || ''}\n\n---\n\n`
  }
  const blob = new Blob([md || '（空）'], { type: 'text/markdown;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `notes-backup-${dayjs().format('YYYYMMDD-HHmm')}.md`
  a.click()
  URL.revokeObjectURL(a.href)
}

/* ---------------- 编辑器 ---------------- */
function applyTheme() {
  const dark = themeStore.isDark
  vditor?.setTheme(dark ? 'dark' : 'classic', dark ? 'dark' : 'light', dark ? 'github-dark' : 'github')
}

onMounted(async () => {
  if (localStorage.getItem('token')) {
    await tryCloudMode()
  } else {
    initLocalMode()
  }

  if (disposed) return
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
    after: () => {
      editorReady.value = true
      applyTheme()
      if (mode.value === 'cloud' && activeNote.value) void ensureContent(activeNote.value)
    },
    input: scheduleSave,
  })

  window.addEventListener('pagehide', onPageHide)
})

watch(() => themeStore.isDark, applyTheme)

function onPageHide() { void outbox.flush() }
onBeforeUnmount(() => {
  disposed = true
  window.removeEventListener('pagehide', onPageHide)
  // Pending snapshots have already been persisted; requests may finish after unmount.
  void outbox.flush()
  vditor?.destroy()
  vditor = null
})

function timeAgo(ts: number | string) {
  const d = dayjs(ts)
  const minutes = Math.floor((Date.now() - d.valueOf()) / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  if (dayjs().isSame(d, 'day')) return d.format('HH:mm')
  if (dayjs().subtract(1, 'day').isSame(d, 'day')) return '昨天'
  return d.format('MM-DD')
}
</script>

<style scoped>
.tool-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
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

.header-actions {
  display: flex;
  gap: 8px;
}

.ghost-btn {
  border: 1px solid var(--border-color);
  background: var(--card-bg);
  color: var(--text-secondary);
  font-size: 13px;
  padding: 7px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}

.ghost-btn:hover {
  color: var(--accent-color);
  border-color: var(--accent-color);
}

.ghost-btn.danger:hover {
  color: #e74c3c;
  border-color: #e74c3c;
}

.mode-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  background: var(--bg-secondary);
  border: 1px dashed var(--border-color);
  border-radius: var(--radius-md);
  padding: 10px 16px;
  margin-bottom: 16px;
  font-size: 13px;
  color: var(--text-secondary);
}

.mode-banner a {
  color: var(--accent-color);
  white-space: nowrap;
}

.banner-actions {
  display: flex;
  gap: 8px;
}

.mini-btn {
  border: 0;
  background: var(--accent-color);
  color: #fff;
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 6px;
  cursor: pointer;
}

.mini-btn.ghost {
  background: transparent;
  color: var(--text-muted);
  border: 1px solid var(--border-color);
}

.mini-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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
