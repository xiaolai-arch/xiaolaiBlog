<template>
  <div class="article-detail">
    <div class="container">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="!article" class="loading">文章未找到</div>
      <template v-else>
        <!-- Article header -->
        <header class="article-header">
          <h1 class="article-title">{{ article.title }}</h1>
          <div class="article-meta">
            <span>{{ formatDate(article.createTime) }}</span>
            <span v-if="article.categoryName" class="category-link">
              <router-link :to="`/categories/${article.categorySlug}`">{{ article.categoryName }}</router-link>
            </span>
            <span>{{ article.views }} 阅读</span>
          </div>
          <div class="article-tags" v-if="article.tags?.length">
            <router-link v-for="tag in article.tags" :key="tag.id" :to="`/tags/${tag.slug}`" class="tag">
              #{{ tag.name }}
            </router-link>
          </div>
        </header>

        <!-- Article content -->
        <div class="article-content markdown-body" v-html="article.contentHtml"></div>

        <!-- Copyright -->
        <div class="copyright-notice">
          <p>{{ copyrightText }}</p>
        </div>

        <!-- Navigation -->
        <nav class="article-nav" v-if="prev || next">
          <div class="nav-prev" v-if="prev">
            <span class="nav-label">上一篇</span>
            <router-link :to="`/articles/${prev.slug}`">{{ prev.title }}</router-link>
          </div>
          <div class="nav-next" v-if="next">
            <span class="nav-label">下一篇</span>
            <router-link :to="`/articles/${next.slug}`">{{ next.title }}</router-link>
          </div>
        </nav>

        <!-- Comments -->
        <section class="comments-section">
          <h3>{{ comments.length > 0 ? `${comments.length} 条评论` : '发表评论' }}</h3>

          <div class="comment-form">
            <el-form :model="commentForm" label-position="top">
              <div class="form-row">
                <el-form-item label="姓名 *" required>
                  <el-input v-model="commentForm.nickname" placeholder="您的姓名" />
                </el-form-item>
                <el-form-item label="邮箱 *" required>
                  <el-input v-model="commentForm.email" placeholder="your@email.com" />
                </el-form-item>
              </div>
              <el-form-item label="网站">
                <el-input v-model="commentForm.website" placeholder="https://yoursite.com" />
              </el-form-item>
              <el-form-item label="内容 *" required>
                <el-input
                  v-model="commentForm.contentMd"
                  type="textarea"
                  :rows="4"
                  placeholder="输入评论内容... 支持 Markdown"
                />
              </el-form-item>
              <el-button type="primary" @click="submitComment" :disabled="submitting">
                {{ submitting ? '提交中...' : '提交评论' }}
              </el-button>
              <p v-if="commentSubmitted" class="comment-hint">
                评论已提交审核，审核通过后将显示。
              </p>
            </el-form>
          </div>

          <div class="comments-list" v-if="comments.length > 0">
            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <div class="comment-avatar">
                <img :src="getGravatar(comment.email)" :alt="comment.nickname" />
              </div>
              <div class="comment-body">
                <div class="comment-header">
                  <strong>{{ comment.nickname }}</strong>
                  <time>{{ timeAgo(comment.createTime) }}</time>
                </div>
                <div class="comment-content markdown-body" v-html="comment.contentHtml"></div>
              </div>
            </div>
          </div>
          <div v-else class="no-comments">
            暂无评论，快来抢沙发吧！
          </div>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { articleApi, commentApi, siteApi } from '@/api'
import dayjs from 'dayjs'

const route = useRoute()
const article = ref<any>(null)
const prev = ref<any>(null)
const next = ref<any>(null)
const loading = ref(true)
const copyrightText = ref('保留所有权利。')
const comments = ref<any[]>([])
const commentForm = ref({ nickname: '', email: '', website: '', contentMd: '' })
const submitting = ref(false)
const commentSubmitted = ref(false)

onMounted(async () => {
  await loadArticle()
  await loadComments()
  try {
    const res = await siteApi.getConfig()
    if (res.data.code === 200 && res.data.data.copyright) {
      copyrightText.value = res.data.data.copyright
    }
  } catch (_) {}
})

async function loadArticle() {
  try {
    const slug = route.params.slug as string
    const res = await articleApi.getBySlug(slug)
    if (res.data.code === 200) {
      article.value = res.data.data
      document.title = article.value.title + ' - 小来博客'
    }
  } catch (_) {}
  loading.value = false
}

async function loadComments() {
  try {
    const slug = route.params.slug as string
    const res = await commentApi.getByArticleSlug(slug)
    if (res.data.code === 200) {
      comments.value = res.data.data || []
    }
  } catch (_) {}
}

async function submitComment() {
  submitting.value = true
  try {
    const slug = route.params.slug as string
    await commentApi.create(slug, commentForm.value)
    commentSubmitted.value = true
    commentForm.value = { nickname: '', email: '', website: '', contentMd: '' }
  } catch (_) {}
  submitting.value = false
}

function formatDate(d: string) {
  return dayjs(d).format('YYYY-MM-DD')
}

function timeAgo(d: string) {
  const diff = Date.now() - dayjs(d).valueOf()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  return dayjs(d).format('MM-DD')
}

function getGravatar(email: string) {
  const hash = email.trim().toLowerCase()
  return `https://www.gravatar.com/avatar/${hash}?s=48&d=mp`
}
</script>

<style scoped>
.article-header {
  padding: 40px 0 32px;
}

.article-title {
  font-size: 28px;
  font-weight: 800;
  line-height: 1.4;
  margin-bottom: 16px;
}

.article-meta {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 12px;
}

.category-link a {
  color: var(--accent-color);
}

.article-tags {
  display: flex;
  gap: 8px;
}

.tag {
  color: var(--accent-color);
  font-size: 13px;
}

.article-content {
  padding-bottom: 40px;
  border-bottom: 1px solid var(--border-color);
}

.copyright-notice {
  padding: 24px 0;
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
  border-bottom: 1px solid var(--border-color);
}

.article-nav {
  display: flex;
  justify-content: space-between;
  padding: 24px 0;
  gap: 40px;
}

.nav-label {
  font-size: 12px;
  text-transform: uppercase;
  color: var(--text-muted);
  display: block;
  margin-bottom: 4px;
}

.nav-prev, .nav-next {
  flex: 1;
}

.nav-next {
  text-align: right;
}

.comments-section {
  padding: 40px 0;
}

.comments-section h3 {
  font-size: 20px;
  margin-bottom: 24px;
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-row .el-form-item {
  flex: 1;
}

.comment-hint {
  margin-top: 12px;
  font-size: 13px;
  color: #e67e22;
}

.comments-list {
  margin-top: 32px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid var(--border-color);
}

.comment-avatar img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
}

.comment-header {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 8px;
}

.comment-header time {
  font-size: 12px;
  color: var(--text-muted);
}

.comment-content {
  font-size: 14px;
}

.no-comments {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
}

.loading {
  text-align: center;
  padding: 80px;
  color: var(--text-muted);
}
</style>