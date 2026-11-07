<template>
  <main class="review-page"><h1>学习计划与复习</h1><p>把学习内容和笔记关联起来，安排下次复习时间。</p><p v-if="error" role="alert">{{ error }} <button @click="load">重试加载</button></p>
    <el-form label-position="top" @submit.prevent="save">
      <el-form-item label="学习内容"><el-input v-model="form.title" maxlength="200" required /></el-form-item>
      <el-form-item label="学习链接"><el-input v-model="form.sourceUrl" maxlength="500" placeholder="https://… 或 /study" /></el-form-item>
      <el-form-item label="状态"><el-select v-model="form.state"><el-option v-for="(label, state) in states" :key="state" :value="state" :label="label" /></el-select></el-form-item>
      <el-form-item label="关联笔记"><el-select v-model="form.noteId" clearable filterable><el-option v-for="n in notes" :key="n.id" :value="n.id" :label="n.title || '无标题笔记'" /></el-select></el-form-item>
      <el-form-item label="下次复习（北京时间）"><el-date-picker v-model="form.reviewAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" clearable /></el-form-item>
      <el-button native-type="submit" :loading="busy">{{ editId ? '保存修改' : '添加计划' }}</el-button><el-button v-if="editId" @click="reset">取消编辑</el-button>
    </el-form>
    <el-radio-group v-model="filter"><el-radio-button value="all">全部</el-radio-button><el-radio-button value="due">待复习 {{ dueCount }}</el-radio-button><el-radio-button v-for="(label, state) in states" :key="state" :value="state">{{ label }}</el-radio-button></el-radio-group>
    <p v-if="loading">加载中…</p><p v-else-if="!visible.length">当前没有学习项。可以从算法、四级或 Agent 学习页选一个目标开始。</p>
    <article v-for="item in visible" :key="item.id"><h3>{{ item.title }}</h3><p>{{ states[item.state] }} <span v-if="item.reviewAt">· {{ dayjs(item.reviewAt).format('YYYY-MM-DD HH:mm') }}</span><strong v-if="due(item)"> · 到期复习</strong></p>
      <a v-if="safeUrl(item.sourceUrl)" :href="item.sourceUrl" target="_blank" rel="noopener noreferrer">打开学习内容</a>
      <router-link v-if="item.noteId" :to="{ path: '/tools/markdown', query: { note: item.noteId } }">关联笔记</router-link>
      <div><el-button @click="edit(item)" :disabled="busy">编辑</el-button><el-button @click="reviewed(item)" :disabled="busy">已复习 · 7 天后再看</el-button><el-button @click="remove(item.id)" :disabled="busy">删除</el-button></div>
    </article>
  </main>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { studyApi, noteApi } from '@/api'
import dayjs from 'dayjs'
const states: Record<string, string> = { todo: '待学习', learning: '学习中', mastered: '已掌握', review: '待复习' }
const blank = () => ({ title: '', sourceUrl: '', state: 'todo', noteId: null as number | null, reviewAt: null as string | null })
const form = ref(blank()), editId = ref<number>(), items = ref<any[]>([]), notes = ref<any[]>([]), filter = ref('all'), error = ref(''), busy = ref(false), loading = ref(true)
const due = (item: any) => item.state === 'review' || (item.reviewAt && dayjs(item.reviewAt).valueOf() <= Date.now())
const dueCount = computed(() => items.value.filter(due).length)
const visible = computed(() => items.value.filter(i => filter.value === 'all' || (filter.value === 'due' ? due(i) : i.state === filter.value)))
const safeUrl = (url: string) => /^https?:\/\/[^\s]+$|^\/[^/\\]/.test(url || '')
async function run(action: () => Promise<void>) { if (busy.value) return; busy.value = true; error.value = ''; try { await action() } catch (e: any) { if (e !== 'cancel' && e !== 'close') error.value = e.response?.data?.message || '操作失败，请重试' } finally { busy.value = false } }
async function load() { loading.value = true; await run(async () => { const [a, b] = await Promise.all([studyApi.list(), noteApi.list()]); items.value = a.data.data; notes.value = b.data.data }); loading.value = false }
function reset() { form.value = blank(); editId.value = undefined }
function edit(item: any) { form.value = { ...item }; editId.value = item.id; window.scrollTo({ top: 0, behavior: 'smooth' }) }
function save() { return run(async () => { const res = editId.value ? await studyApi.update(editId.value, form.value) : await studyApi.create(form.value); items.value = res.data.data; reset() }) }
function reviewed(item: any) { return run(async () => { items.value = (await studyApi.update(item.id, { ...item, state: 'learning', reviewAt: dayjs().add(7, 'day').format('YYYY-MM-DDTHH:mm:ss') })).data.data }) }
function remove(id: number) { return run(async () => { await ElMessageBox.confirm('确定删除这项学习计划？', '删除计划'); await studyApi.remove(id); items.value = items.value.filter(i => i.id !== id) }) }
onMounted(load)
</script>
<style scoped>.review-page { max-width: 800px; padding: 40px 16px; margin: auto; } p { margin: 16px 0; } form { margin: 24px 0; } article { border: 1px solid var(--border-color); padding: 20px; margin-top: 16px; border-radius: 12px; } article a { margin-right: 16px; } article div { margin-top: 12px; display: flex; flex-wrap: wrap; gap: 8px; } strong { color: var(--accent-color); }</style>
