<template>
  <div class="tag-manage">
    <div class="page-header">
      <h2>标签管理</h2>
      <el-button type="primary" @click="showDialog = true">
        <el-icon><Plus /></el-icon> 新建标签
      </el-button>
    </div>

    <el-card>
      <el-table :data="tags" v-loading="loading" stripe>
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="slug" label="URL 别名" />
        <el-table-column prop="articleCount" label="文章数" width="100" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" @click="edit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showDialog" :title="editing ? '编辑标签' : '新建标签'" width="400">
      <el-form :model="form" label-position="top">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="标签名称" />
        </el-form-item>
        <el-form-item label="URL 别名">
          <el-input v-model="form.slug" placeholder="URL 标识" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="save" :loading="saving">{{ saving ? '保存中…' : '保存' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { tagApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const tags = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const editing = ref<number | null>(null)
const form = ref({ name: '', slug: '' })

onMounted(() => load())

async function load() {
  loading.value = true
  try { const res = await tagApi.getAdminList(); tags.value = res.data.data || [] } catch (_) {}
  loading.value = false
}

function edit(row: any) {
  editing.value = row.id
  form.value = { name: row.name, slug: row.slug }
  showDialog.value = true
}

function resetForm() { editing.value = null; form.value = { name: '', slug: '' } }

async function save() {
  saving.value = true
  try {
    if (editing.value) { await tagApi.update(editing.value, form.value); ElMessage.success('已更新') }
    else { await tagApi.create(form.value); ElMessage.success('已创建') }
    showDialog.value = false; resetForm(); load()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '操作失败') }
  saving.value = false
}

function remove(row: any) {
  ElMessageBox.confirm(`确定删除「${row.name}」？`, '确认', { type: 'warning' })
    .then(async () => { await tagApi.delete(row.id); ElMessage.success('已删除'); load() })
    .catch(() => {})
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { font-size: 24px; }
</style>