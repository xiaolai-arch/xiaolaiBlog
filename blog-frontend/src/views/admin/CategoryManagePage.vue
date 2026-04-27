<template>
  <div class="category-manage">
    <div class="page-header">
      <h2>Categories</h2>
      <el-button type="primary" @click="showDialog = true">
        <el-icon><Plus /></el-icon> New Category
      </el-button>
    </div>

    <el-card>
      <el-table :data="categories" v-loading="loading" stripe>
        <el-table-column prop="name" label="Name" />
        <el-table-column prop="slug" label="Slug" />
        <el-table-column prop="description" label="Description" show-overflow-tooltip />
        <el-table-column prop="articleCount" label="Articles" width="100" />
        <el-table-column label="Actions" width="160">
          <template #default="{ row }">
            <el-button size="small" @click="edit(row)">Edit</el-button>
            <el-button size="small" type="danger" @click="remove(row)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showDialog" :title="editing ? 'Edit Category' : 'New Category'" width="500">
      <el-form :model="form" label-position="top">
        <el-form-item label="Name" required>
          <el-input v-model="form.name" placeholder="Category name" />
        </el-form-item>
        <el-form-item label="Slug">
          <el-input v-model="form.slug" placeholder="URL slug" />
        </el-form-item>
        <el-form-item label="Description">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">Cancel</el-button>
        <el-button type="primary" @click="save" :loading="saving">{{ saving ? 'Saving...' : 'Save' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { categoryApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const categories = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const editing = ref<number | null>(null)
const form = ref({ name: '', slug: '', description: '' })

onMounted(() => load())

async function load() {
  loading.value = true
  try { const res = await categoryApi.getAdminList(); categories.value = res.data.data || [] } catch (_) {}
  loading.value = false
}

function edit(row: any) {
  editing.value = row.id
  form.value = { name: row.name, slug: row.slug, description: row.description || '' }
  showDialog.value = true
}

function resetForm() {
  editing.value = null
  form.value = { name: '', slug: '', description: '' }
}

async function save() {
  saving.value = true
  try {
    if (editing.value) {
      await categoryApi.update(editing.value, form.value)
      ElMessage.success('Updated')
    } else {
      await categoryApi.create(form.value)
      ElMessage.success('Created')
    }
    showDialog.value = false
    resetForm()
    load()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || 'Failed') }
  saving.value = false
}

function remove(row: any) {
  ElMessageBox.confirm(`Delete "${row.name}"?`, 'Confirm', { type: 'warning' })
    .then(async () => {
      await categoryApi.delete(row.id)
      ElMessage.success('Deleted')
      load()
    }).catch(() => {})
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { font-size: 24px; }
</style>