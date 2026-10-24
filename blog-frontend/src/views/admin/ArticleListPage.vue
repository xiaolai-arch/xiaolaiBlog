<template>
  <div class="article-list-page">
    <div class="page-header">
      <h2>文章管理</h2>
      <el-button type="primary" @click="$router.push('/admin/articles/create')">
        <el-icon><Plus /></el-icon> 新建文章
      </el-button>
    </div>

    <el-card>
      <div class="table-toolbar">
        <el-input v-model="keyword" placeholder="搜索标题…" clearable style="width: 280px" @clear="loadArticles" @keyup.enter="loadArticles" />
        <el-select v-model="statusFilter" placeholder="状态" clearable @change="loadArticles" style="width: 140px">
          <el-option label="已发布" :value="1" />
          <el-option label="草稿" :value="0" />
        </el-select>
        <el-button @click="loadArticles">搜索</el-button>
      </div>

      <el-table :data="articles" v-loading="loading" stripe>
        <el-table-column prop="title" label="标题" min-width="250" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="views" label="浏览量" width="80" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/admin/articles/${row.id}/edit`)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page" v-model:page-size="size"
          :total="total" layout="prev, pager, next" background
          @current-change="loadArticles"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { articleApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const articles = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const keyword = ref('')
const statusFilter = ref<number | ''>('')

onMounted(() => loadArticles())

async function loadArticles() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value !== '') params.status = statusFilter.value
    const res = await articleApi.getAdminList(params)
    if (res.data.code === 200) {
      articles.value = res.data.data.list
      total.value = res.data.data.total
    }
  } catch (_) {}
  loading.value = false
}

function handleDelete(row: any) {
  ElMessageBox.confirm(`确定删除「${row.title}」？`, '确认', { type: 'warning' })
    .then(async () => {
      await articleApi.delete(row.id)
      ElMessage.success('已删除')
      loadArticles()
    }).catch(() => {})
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { font-size: 24px; }
.table-toolbar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 20px; }
</style>