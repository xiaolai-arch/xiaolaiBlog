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
      <section class="now-card tint-mint">
        <h2>现在在做</h2>
        <ul class="now-list">
          <li><strong>AI Agent 18 周冲刺</strong> — 阶段四补强进行中：评测体系、上下文工程、面试冲刺（<router-link to="/agent">打卡进度 →</router-link>）</li>
          <li><strong>2026 秋招</strong> — 目标 Agent / LLM 应用开发岗</li>
          <li><strong>这个博客</strong> — 持续迭代中，改版记录见 <router-link to="/changelog">更新日志</router-link></li>
        </ul>
      </section>

      <!-- 事实卡 -->
      <section class="facts-grid">
        <div class="fact-card tint-lavender">
          <h3>方向</h3>
          <p>AI Agent · RAG · 多模态<br />CV 底子转化中</p>
        </div>
        <div class="fact-card tint-sky">
          <h3>技术栈</h3>
          <p>Java / Spring Boot 3<br />Vue 3 · Python · PyTorch</p>
        </div>
        <div class="fact-card tint-peach">
          <h3>爱好</h3>
          <p>折腾小工具 · 拍照<br />刷题攒成就</p>
        </div>
        <div class="fact-card tint-butter">
          <h3>找到我</h3>
          <p>
            <a v-if="github" :href="github" target="_blank" rel="noopener">GitHub</a>
            <template v-if="github && email"> · </template>
            <a v-if="email" :href="'mailto:' + email">邮箱</a>
          </p>
        </div>
      </section>

      <!-- 自我介绍（后台可编辑） -->
      <section class="about-content-wrap">
        <div v-if="loading" class="loading">加载中...</div>
        <div v-else class="about-content markdown-body" v-html="content"></div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { siteApi } from '@/api'
import avatarUrl from '@/assets/avatar.png'

const content = ref('')
const loading = ref(true)
const description = ref('')
const github = ref('')
const email = ref('')

onMounted(async () => {
  try {
    const [aboutRes, configRes] = await Promise.all([siteApi.getAbout(), siteApi.getConfig()])
    if (aboutRes.data.code === 200) {
      content.value = aboutRes.data.data.content || '<p>这里还没有介绍，可以在后台的站点设置里补充。</p>'
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

/* ---------- 事实卡 ---------- */
.facts-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 48px;
}

.fact-card {
  border-radius: var(--radius-md);
  padding: 20px;
}

.fact-card h3 {
  font-family: var(--font-display);
  font-size: 14.5px;
  font-weight: 700;
  margin-bottom: 8px;
}

.fact-card p {
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--text-secondary);
}

.fact-card a {
  color: var(--accent-color);
  font-weight: 600;
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

@media (max-width: 720px) {
  .about-profile {
    flex-direction: column;
    text-align: center;
  }

  .facts-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
