<template>
  <div class="about-page">
    <div class="container-wide">
      <!-- Hero -->
      <section class="about-hero">
        <div class="orb orb-lavender"></div>
        <div class="orb orb-peach"></div>
        <h1 class="about-title">关于我</h1>
        <div class="about-profile">
          <div class="avatar"><img :src="avatarUrl" alt="头像" /></div>
          <p class="about-lead">
            {{ description || 'CV 出身、正在转向 AI Agent 方向的开发者。这里记录学习笔记、项目实践和一些还没整理完的想法。' }}
          </p>
        </div>
      </section>

      <!-- NOW -->
      <section class="now-card tint-paper">
        <h2>现在在做</h2>
        <ul class="now-list">
          <li><strong>转型进行时</strong> — 从 Java 后端开发工程师转向全栈工程师</li>
          <li><strong>AI Agent 开发</strong> — 当前在做 Claude / Codex 的 Skills 开发</li>
        </ul>
      </section>

      <!-- 技术栈：分类 + 图标胶囊（对标 ashishgogula.in 的 Tech Stack） -->
      <section class="stack-section">
        <h2>技术栈</h2>
        <div v-for="group in stackGroups" :key="group.label" class="stack-group">
          <p class="stack-label">{{ group.label }}</p>
          <div class="stack-pills">
            <component
              :is="t.href ? 'a' : 'span'"
              v-for="t in group.items"
              :key="t.name"
              :href="t.href || undefined"
              :target="t.href && t.href.startsWith('http') ? '_blank' : undefined"
              :rel="t.href && t.href.startsWith('http') ? 'noopener' : undefined"
              class="stack-pill"
            >
              <svg v-if="t.icon" viewBox="0 0 24 24" aria-hidden="true"><path :d="techIconPaths[t.icon]" /></svg>
              {{ t.name }}
            </component>
          </div>
        </div>
      </section>

      <!-- GitHub 年度贡献热力图 -->
      <GithubActivity v-if="githubUser" :user="githubUser" />

      <!-- 力扣打卡活跃度（数据来自打卡日志 lc-log） -->
      <GithubActivity
        title="刷题活跃度"
        unit="submissions"
        url="/api/v1/github/leetcode"
      />

      <!-- 自我介绍（后台可编辑；无内容时不渲染） -->
      <section v-if="content" class="about-content-wrap">
        <div v-if="loading" class="loading">加载中...</div>
        <div v-else class="about-content markdown-body" v-html="content"></div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { siteApi } from '@/api'
import { techIconPaths } from '@/data/techIcons'
import GithubActivity from '@/components/GithubActivity.vue'
import avatarUrl from '@/assets/avatar.png'

const content = ref('')
const loading = ref(true)
const description = ref('')
const github = ref('')
const email = ref('')

/* 从 GitHub 主页链接解析用户名，供贡献热力图使用 */
const githubUser = computed(() => {
  const m = github.value.match(/github\.com\/([A-Za-z0-9-]+)/)
  return m ? m[1] : ''
})

interface StackItem { name: string; icon?: string; href?: string }
const stackGroups = computed<Array<{ label: string; items: StackItem[] }>>(() => [
  {
    label: '前端',
    items: [
      { name: 'Vue 3', icon: 'vue' },
      { name: 'Vue Router', },
      { name: 'Pinia', },
      { name: 'TypeScript', icon: 'ts' },
      { name: 'Vite', icon: 'vite' },
      { name: 'Axios', icon: 'axios' },
      { name: 'Element Plus' },
      { name: 'Three.js', icon: 'three' },
    ],
  },
  {
    label: '后端',
    items: [
      { name: 'Java', icon: 'java' },
      { name: 'Spring Boot', icon: 'spring' },
      { name: 'Spring Cloud' },
      { name: 'MyBatis-Plus' },
      { name: 'MySQL', icon: 'mysql' },
      { name: 'Redis', icon: 'redis' },
      { name: 'RabbitMQ', icon: 'rabbitmq' },
      { name: 'Elasticsearch', icon: 'es' },
      { name: 'Docker', icon: 'docker' },
      { name: 'Jenkins', icon: 'jenkins' },
    ],
  },
  {
    label: 'AI 工具',
    items: [
      { name: 'Claude Code', icon: 'claude' },
      { name: 'Codex', icon: 'openai' },
      { name: 'Skills 开发' },
      { name: 'Python', icon: 'python' },
      { name: 'PyTorch', icon: 'pytorch' },
    ],
  },
  {
    label: '数据采集',
    items: [
      { name: 'Scrapy', icon: 'scrapy' },
      { name: 'Playwright' },
      { name: 'XPath / lxml' },
    ],
  },
  {
    label: '找到我',
    items: [
      ...(github.value ? [{ name: 'GitHub', icon: 'github', href: github.value }] : []),
      ...(email.value ? [{ name: '邮箱', href: `mailto:${email.value}` }] : []),
    ],
  },
])

onMounted(async () => {
  try {
    const [aboutRes, configRes] = await Promise.all([siteApi.getAbout(), siteApi.getConfig()])
    if (aboutRes.data.code === 200) {
      content.value = aboutRes.data.data.content || ''
    }
    if (configRes.data.code === 200) {
      const config = configRes.data.data
      description.value = config.site_description || config.site_subtitle || ''
      github.value = config.social_github || ''
      email.value = config.social_email || ''
    }
  } catch (_) {
    content.value = '<p>介绍加载失败。</p>'
  }
  loading.value = false
})
</script>

<style scoped>
.container-wide {
  max-width: 920px;
  margin: 0 auto;
  padding: 0 20px;
}

.about-hero {
  position: relative;
  padding: 72px 0 44px;
  overflow: hidden;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.45;
  pointer-events: none;
}

.orb-lavender {
  width: 240px;
  height: 240px;
  background: #e4deff;
  top: -150px;
  left: 4%;
}

.orb-peach {
  width: 200px;
  height: 200px;
  background: #ffdfc8;
  top: -140px;
  right: 8%;
}

.about-title,
.about-profile {
  position: relative;
  z-index: 1;
}

[data-theme='dark'] .orb {
  opacity: 0.22;
}

.about-title {
  font-family: var(--font-display);
  font-size: clamp(36px, 5vw, 48px);
  font-weight: 800;
  letter-spacing: -0.04em;
  margin-bottom: 24px;
}

.about-profile {
  display: flex;
  gap: 24px;
  align-items: center;
  position: relative;
}

.avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  overflow: hidden;
  flex: none;
  box-shadow: var(--shadow-soft);
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.about-lead {
  font-size: 16.5px;
  line-height: 1.85;
  color: var(--text-secondary);
  max-width: 620px;
}

/* ---------- NOW ---------- */
.now-card {
  border-radius: var(--radius-lg);
  padding: 26px 28px;
  margin-bottom: 24px;
}

.now-card h2 {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 800;
  margin-bottom: 14px;
}

.now-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.now-list li {
  font-size: 14.5px;
  line-height: 1.7;
  color: var(--text-secondary);
}

.now-list strong {
  color: var(--text-primary);
}

/* ---------- 技术栈（胶囊式） ---------- */
.stack-section {
  margin-bottom: 48px;
}

.stack-section h2 {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 800;
  margin-bottom: 18px;
}

.stack-group + .stack-group {
  margin-top: 22px;
}

.stack-label {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 10px;
}

.stack-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.stack-pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 7px 15px;
  border-radius: 999px;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12.5px;
  color: var(--text-primary);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

a.stack-pill {
  text-decoration: none;
}

.stack-pill:hover {
  transform: translateY(-1px);
  border-color: #d4d4d8;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.07);
}

[data-theme='dark'] .stack-pill:hover {
  border-color: rgba(255, 255, 255, 0.28);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.4);
}

.stack-pill svg {
  width: 15px;
  height: 15px;
  fill: currentColor;
  opacity: 0.72;
  flex-shrink: 0;
}

.about-content-wrap {
  padding-bottom: 40px;
}

.loading {
  text-align: center;
  padding: 80px;
  color: var(--text-muted);
}

.tint-lavender { background: var(--tint-lavender); }
.tint-peach { background: var(--tint-peach); }
.tint-mint { background: var(--tint-mint); }
.tint-sky { background: var(--tint-sky); }
.tint-butter { background: var(--tint-butter); }
/* 纸感浅色（NOW 卡）：近白渐变，暗色模式换深灰 */
.tint-paper { background: linear-gradient(150deg, #fefefe, #f6f7f4); }
[data-theme='dark'] .tint-paper { background: linear-gradient(150deg, #232326, #26262a); }

@media (max-width: 720px) {
  .about-profile {
    flex-direction: column;
    text-align: center;
  }

  .stack-pills {
    gap: 8px;
  }

  .stack-pill {
    padding: 6px 12px;
    font-size: 12px;
  }
}
</style>
