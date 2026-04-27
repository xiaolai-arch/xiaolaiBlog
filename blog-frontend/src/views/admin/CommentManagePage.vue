<template>
  <div class="comment-manage">
    <div class="page-header">
      <h2>评论管理</h2>
      <div class="tabs">
        <el-button :type="statusFilter === '' ? 'primary' : ''" @click="statusFilter = ''; load()">全部</el-button>
        <el-button :type="statusFilter === 0 ? 'primary' : ''" @click="statusFilter = 0; load()">待审核</el-button>
        <el-button :type="statusFilter === 1 ? 'primary' : ''" @click="statusFilter = 1; load()">已通过</el-button>
        <el-button :type="statusFilter === 3 ? 'primary' : ''" @click="statusFilter = 3; load()">垃圾</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="comments" v-loading="loading" stripe>
        <el-table-column prop="nickname" label="作者" width="120" />
        <el-table-column prop="contentMd" label="内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0" type="warning" size="small">待审核</el-tag>
            <el-tag v-else-if="row.status === 1" type="success" size="small">已通过</el-tag>
            <el-tag v-else-if="row.status === 3" type="danger" size="small">垃圾</el-tag>
            <el-tag v-else size="small">已拒绝</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="日期" width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 1" size="small" type="success" @click="approve(row)">通过</el-button>
            <el-button v-if="row.status !== 3" size="small" type="warning" @click="markSpam(row)">垃圾</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page" v-model:page-size="size" :total="total"
          layout="prev, pager, next" background @current-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { commentApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const comments = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const statusFilter = ref<number | ''>('')

onMounted(() => load())

async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (statusFilter.value !== '') params.status = statusFilter.value
    const res = await commentApi.getAdminList(params)
    if (res.data.code === 200) { comments.value = res.data.data.list; total.value = res.data.data.total }
  } catch (_) {}
  loading.value = false
}

async function approve(row: any) { await commentApi.approve(row.id); ElMessage.success('已通过'); load() }
async function markSpam(row: any) { await commentApi.markSpam(row.id); ElMessage.success('已标记为垃圾'); load() }
function remove(row: any) {
  ElMessageBox.confirm('确定删除此评论？', '确认', { type: 'warning' })
    .then(async () => { await commentApi.delete(row.id); ElMessage.success('已删除'); load() }).catch(() => {})
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { font-size: 24px; }
.tabs { display: flex; gap: 8px; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 20px; }
</style>