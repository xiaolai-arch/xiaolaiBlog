<template>
  <div class="projects-page">
    <div class="container">
      <header class="page-head">
        <div>
          <h1>我的项目</h1>
          <p>
            学习路上写过的一些东西，
            <a v-if="user" :href="user.html_url" target="_blank" rel="noopener">@{{ user.login }}</a>
            的源码都在 GitHub 上。
          </p>
        </div>
        <div class="head-stats" v-if="projects.length">
          <span><strong>{{ projects.length }}</strong> 个项目</span>
          <span><strong>{{ languages.length }}</strong> 种语言</span>
        </div>
      </header>

      <div v-if="loading" class="loading">加载中...</div>
      <template v-else>
        <div class="proj-grid">
          <article
            v-for="p in projects"
            :key="p.name"
            class="repo-card"
            :class="{ featured: p.featured }"
          >
            <div class="repo-head">
              <a class="repo-name" :href="p.url" target="_blank" rel="noopener">
                {{ p.name }}<span class="ext">↗</span>
              </a>
              <span v-if="p.badge" class="repo-badge">{{ p.badge }}</span>
              <span v-if="p.category" class="repo-cat">{{ p.category }}</span>
            </div>

            <p class="repo-desc">{{ p.desc }}</p>

            <ul v-if="p.highlights && p.highlights.length" class="repo-highlights">
              <li v-for="(h, i) in p.highlights" :key="i">{{ h }}</li>
            </ul>

            <div v-if="p.tags && p.tags.length" class="repo-tags">
              <span v-for="(t, i) in p.tags" :key="i" class="repo-tag">{{ t }}</span>
            </div>

            <div class="repo-foot">
              <span class="lang">
                <i :style="{ background: langColor(p.language) }"></i>{{ p.language || '其他' }}
              </span>
              <span v-if="p.stars" class="stars">★ {{ p.stars }}</span>
              <span class="repo-updated">更新于 {{ p.pushedAt }}</span>
              <a class="repo-github" :href="p.url" target="_blank" rel="noopener">GitHub ↗</a>
            </div>
          </article>
        </div>

        <footer class="note">
          语言 / 更新时间为 GitHub API 数据（{{ updatedAt }} 抓取），项目介绍为手动整理。
          <a v-if="user" :href="user.html_url" target="_blank" rel="noopener">去 GitHub 看全部仓库 →</a>
        </footer>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

interface Project {
  name: string
  desc: string
  category: string
  badge: string
  featured: boolean
  tags: string[]
  highlights: string[]
  url: string
  language: string
  stars: number
  forks: number
  pushedAt: string
}

const loading = ref(true)
const projects = ref<Project[]>([])
const user = ref<{ login: string; html_url: string } | null>(null)
const updatedAt = ref('')

const languages = computed(() => {
  const set = new Set(projects.value.map(p => p.language).filter(Boolean))
  return [...set]
})

const LANG_COLORS: Record<string, string> = {
  Java: '#b07219',
  Vue: '#41b883',
  JavaScript: '#f1e05a',
  TypeScript: '#3178c6',
  Python: '#3572A5',
  'Vim Script': '#199f4b',
  HTML: '#e34c26',
  CSS: '#563d7c',
  Shell: '#89e051',
  Go: '#00ADD8',
}

function langColor(lang: string) {
  return LANG_COLORS[lang] || '#8d8a99'
}

onMounted(async () => {
  try {
    const res = await fetch('/data/github/projects.json')
    const data = await res.json()
    user.value = data.user || null
    updatedAt.value = data.updatedAt || ''
    projects.value = data.projects || []
  } catch (_) {}
  loading.value = false
})
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

.head-stats {
  flex: none;
  display: flex;
  gap: 18px;
  font-size: 13px;
  color: var(--text-muted);
}

.head-stats strong {
  color: var(--accent-color);
  font-size: 18px;
  margin-right: 2px;
}

.proj-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
  padding-bottom: 8px;
}

.repo-card {
  display: flex;
  flex-direction: column;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  padding: 20px 22px;
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}

.repo-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-hover);
  border-color: var(--accent-color);
}

.repo-card.featured {
  border-left: 4px solid var(--accent-color);
}

.repo-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.repo-name {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
}

.repo-name:hover {
  color: var(--accent-color);
}

.ext {
  font-size: 12px;
  color: var(--text-muted);
  margin-left: 4px;
}

.repo-badge {
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  background: var(--accent-color);
  border-radius: 4px;
  padding: 2px 8px;
}

.repo-cat {
  font-size: 11px;
  color: var(--accent-color);
  background: var(--bg-secondary);
  border-radius: 999px;
  padding: 2px 10px;
}

.repo-desc {
  font-size: 13.5px;
  color: var(--text-secondary);
  line-height: 1.7;
  margin-bottom: 10px;
}

.repo-highlights {
  list-style: none;
  padding: 0;
  margin: 0 0 12px;
}

.repo-highlights li {
  position: relative;
  font-size: 12.5px;
  color: var(--text-secondary);
  line-height: 1.7;
  padding-left: 16px;
}

.repo-highlights li::before {
  content: '✓';
  position: absolute;
  left: 0;
  color: #06a35f;
  font-size: 11px;
}

.repo-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 14px;
}

.repo-tag {
  font-size: 11px;
  padding: 2px 9px;
  border-radius: 4px;
  background: var(--bg-secondary);
  color: var(--text-secondary);
}

.repo-foot {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
  font-size: 12px;
  color: var(--text-muted);
}

.lang {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.lang i {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.stars {
  color: #f59e0b;
}

.repo-updated {
  margin-left: auto;
}

.repo-github {
  color: var(--accent-color);
  font-weight: 500;
}

.note {
  margin-top: 18px;
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.8;
  border-top: 1px solid var(--border-color);
  padding: 14px 0 40px;
}

.note a {
  margin-left: 6px;
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
    gap: 12px;
  }

  .proj-grid {
    grid-template-columns: 1fr;
  }
}
</style>
