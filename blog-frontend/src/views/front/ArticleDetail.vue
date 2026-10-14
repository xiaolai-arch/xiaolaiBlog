<template>
  <div class="article-detail">
    <!-- 悬浮目录（正文右侧有足够空间才显示） -->
    <aside class="toc-panel" v-if="showToc && toc.length >= 2" :style="{ left: tocLeft + 'px' }">
      <div class="toc-title">目录</div>
      <a
        v-for="(h, i) in toc"
        :key="i"
        :href="'#' + h.id"
        class="toc-link"
        :class="{ lvl3: h.level >= 3, active: i === activeToc }"
        @click.prevent="scrollToToc(h.id)"
      >{{ h.text }}</a>
    </aside>
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

        <!-- Related articles -->
        <section class="related-section" v-if="related.length">
          <h3>相关文章</h3>
          <div class="related-grid">
            <router-link v-for="rel in related" :key="rel.id" :to="`/articles/${rel.slug}`" class="related-item">
              <div class="related-title">{{ rel.title }}</div>
              <div class="related-meta">{{ formatDate(rel.createTime) }} · {{ rel.views || 0 }} 阅读</div>
            </router-link>
          </div>
        </section>

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
                <img :src="getAvatar(comment.emailHash)" :alt="comment.nickname" loading="lazy" />
              </div>
              <div class="comment-body">
                <div class="comment-header">
                  <strong>{{ comment.nickname }}</strong>
                  <time>{{ timeAgo(comment.createTime) }}</time>
                </div>
                <div class="comment-content markdown-body" v-html="sanitize(comment.contentHtml)"></div>
                <div v-for="reply in comment.replies || []" :key="reply.id" class="comment-reply">
                  <div class="comment-header">
                    <strong>{{ reply.nickname }}</strong>
                    <span v-if="reply.isAdmin === 1" class="admin-badge">博主</span>
                    <time>{{ timeAgo(reply.createTime) }}</time>
                  </div>
                  <div class="comment-content markdown-body" v-html="sanitize(reply.contentHtml)"></div>
                </div>
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
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { articleApi, commentApi, siteApi } from '@/api'
import { ElMessage } from 'element-plus'
import DOMPurify from 'dompurify'
import dayjs from 'dayjs'
import hljs from 'highlight.js/lib/common'
import dockerfile from 'highlight.js/lib/languages/dockerfile'
import properties from 'highlight.js/lib/languages/properties'

hljs.registerLanguage('dockerfile', dockerfile)
hljs.registerLanguage('properties', properties)

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
const related = ref<any[]>([])
const toc = ref<{ id: string; text: string; level: number }[]>([])
const activeToc = ref(0)
const showToc = ref(false)
const tocLeft = ref(0)

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
  await highlightCode()
  buildToc()
  loadRelated()
  updateTocPos()
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('resize', updateTocPos)
}

// 目录锚定正文容器右边缘：位置不随视口居中假设漂移，放不下就隐藏
function updateTocPos() {
  const container = document.querySelector('.article-detail .container')
  if (!container) return
  const right = container.getBoundingClientRect().right
  tocLeft.value = right + 24
  showToc.value = tocLeft.value + 230 < window.innerWidth - 16
}

// 从渲染后的正文提取 h2/h3 生成目录
function buildToc() {
  const headings = document.querySelectorAll('.article-content h2, .article-content h3')
  toc.value = []
  headings.forEach((h, i) => {
    const id = `toc-${i}`
    h.id = id
    toc.value.push({ id, text: h.textContent || '', level: h.tagName === 'H2' ? 2 : 3 })
  })
}

function scrollToToc(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function onScroll() {
  const headings = toc.value
    .map(h => document.getElementById(h.id))
    .filter((el): el is HTMLElement => !!el)
  let current = 0
  for (let i = 0; i < headings.length; i++) {
    if (headings[i].getBoundingClientRect().top <= 90) current = i
  }
  activeToc.value = current
}

// 同分类的相关文章
async function loadRelated() {
  const slug = route.params.slug as string
  const catSlug = article.value?.categorySlug
  if (!catSlug) return
  try {
    const res = await articleApi.getList({ page: 1, size: 4, category: catSlug })
    if (res.data.code === 200) {
      related.value = (res.data.data.list || []).filter((a: any) => a.slug !== slug).slice(0, 3)
    }
  } catch (_) {}
}

// The backend renders markdown without highlighting; enhance the code blocks
// client-side so the hljs theme (swapped in stores/theme.ts) applies.
async function highlightCode() {
  await nextTick()
  document.querySelectorAll<HTMLElement>('.article-content pre code:not([data-hl])').forEach((el) => {
    el.dataset.hl = '1'
    try {
      hljs.highlightElement(el)
    } catch (_) {}
  })
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
  commentSubmitted.value = false
  try {
    const slug = route.params.slug as string
    const res = await commentApi.create(slug, commentForm.value)
    if (res.data.code === 200) {
      commentSubmitted.value = true
      commentForm.value = { nickname: '', email: '', website: '', contentMd: '' }
    } else if (res.data.code === 429) {
      ElMessage.warning('评论太频繁了，请稍后再试')
    } else {
      ElMessage.error(res.data.message || '提交失败')
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '提交失败，请检查填写内容')
  }
  submitting.value = false
}

// Backend already escapes comment HTML; this is a second line of defence for the v-html sink.
function sanitize(html?: string) {
  return html ? DOMPurify.sanitize(html) : ''
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

// Cravatar is a Gravatar-compatible mirror reachable from mainland China.
function getAvatar(emailHash?: string) {
  return `https://cravatar.cn/avatar/${emailHash || ''}?s=48&d=mp`
}

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('resize', updateTocPos)
})
</script>

<style scoped>
.container {
  max-width: 760px;
}

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

.comment-reply {
  margin-top: 12px;
  padding: 10px 14px;
  border-left: 3px solid var(--accent-color);
  background: var(--bg-secondary);
  border-radius: 4px;
}

.admin-badge {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 3px;
  color: #fff;
  background: var(--accent-color);
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

/* 悬浮目录：left/显示与否由 JS 按正文容器位置计算 */
.toc-panel {
  position: fixed;
  top: 100px;
  width: 230px;
  max-height: calc(100vh - 150px);
  overflow-y: auto;
  border-left: 2px solid var(--border-color);
  padding-left: 14px;
}

.toc-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  margin-bottom: 8px;
}

.toc-link {
  display: block;
  font-size: 13px;
  color: var(--text-secondary);
  padding: 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toc-link.lvl3 {
  padding-left: 14px;
  font-size: 12px;
}

.toc-link:hover {
  color: var(--accent-color);
}

.toc-link.active {
  color: var(--accent-color);
  font-weight: 600;
}

/* 相关文章 */
.related-section {
  padding: 8px 0 24px;
}

.related-section h3 {
  font-size: 18px;
  margin-bottom: 16px;
}

.related-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.related-item {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  transition: border-color 0.2s, transform 0.2s;
}

.related-item:hover {
  border-color: var(--accent-color);
  transform: translateY(-2px);
}

.related-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.related-item:hover .related-title {
  color: var(--accent-color);
}

.related-meta {
  font-size: 12px;
  color: var(--text-muted);
}
</style>