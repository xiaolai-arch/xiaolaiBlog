<template>
  <div class="tools-page">
    <div class="container">
      <header class="page-head">
        <h1>工具箱</h1>
        <p>一些常用的小工具，全部在浏览器本地运行，内容不会上传。</p>
      </header>

      <div class="tool-grid">
        <router-link v-for="tool in tools" :key="tool.to" :to="tool.to" class="tool-card">
          <div class="tool-info">
            <h3>{{ tool.name }}</h3>
            <p>{{ tool.desc }}</p>
          </div>
          <span class="tool-arrow">→</span>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const tools = [
  {
    name: 'Markdown 笔记本',
    desc: 'Typora 式即时渲染，多篇笔记管理；登录后云端同步，未登录本地保存',
    to: '/tools/markdown',
  },
  {
    name: '算法刷题',
    desc: '12 个专题的力扣题单（原帖章节顺序），支持打卡记录进度',
    to: '/leetcode',
  },
  {
    name: '四级备考',
    desc: '刘晓艳四级课程清单（2064 节），看课打卡、统计已学时长',
    to: '/cet4',
  },
  {
    name: 'Agent 开发',
    desc: 'Agent 开发 14 周学习计划（原理/框架/项目）+ 资源推荐',
    to: '/agent',
  },
]
</script>

<style scoped>
.page-head {
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

.tool-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
  padding-bottom: 40px;
}

.tool-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 20px 22px;
  box-shadow: var(--shadow-soft);
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}

.tool-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-hover);
  border-color: var(--accent-color);
}

.tool-info {
  flex: 1;
  min-width: 0;
}

.tool-info h3 {
  font-size: 16px;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.tool-info p {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.6;
}

.tool-arrow {
  color: var(--text-muted);
  flex: none;
  transition: transform 0.2s, color 0.2s;
}

.tool-card:hover .tool-arrow {
  transform: translateX(3px);
  color: var(--accent-color);
}
</style>
