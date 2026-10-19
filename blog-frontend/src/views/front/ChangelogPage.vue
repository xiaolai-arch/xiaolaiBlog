<template>
  <div class="changelog-page">
    <div class="container-narrow">
      <header class="cl-head">
        <div class="orb orb-lavender"></div>
        <h1>更新日志</h1>
        <p>这个网站也是我的一个作品 —— 每次改版都记录在这里。</p>
      </header>

      <div class="timeline">
        <section v-for="entry in entries" :key="entry.date" class="entry">
          <div class="entry-head">
            <span class="entry-date">{{ entry.date }}</span>
            <span v-if="entry.tag" class="entry-tag" :class="entry.tagClass">{{ entry.tag }}</span>
          </div>
          <ul class="entry-list">
            <li v-for="item in entry.items" :key="item">{{ item }}</li>
          </ul>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 新的版本记录往 unshift 头部加即可
const entries = [
  {
    date: '2026.10.19',
    tag: '新功能',
    tagClass: 'tag-green',
    items: [
      '新增 3D 美术馆（three.js，对标 liumichelle.com/gallery）：11×12×4.2m 白盒展厅、眼高 2.1m、每墙 3 幅等距挂位、深色画框 + 白卡纸 + 铭牌 + 天花筒灯 + 暗角',
      '墙上是大都会艺术博物馆 Open Access（CC0 公有领域）名画 24 幅本地化打包：神奈川冲浪里、梵高鸢尾/麦田/丝柏、萨金特 Madame X、维米尔、伦勃朗、波提切利、勃鲁盖尔、丢勒、达·芬奇、塞尚……每次随机挂 12 幅，「换一批」即时换展',
      '交互：拖拽环视 / WASD 移动 / 滚轮变焦 / 点击画作平滑聚焦弹出铭牌（画家·年代·材质）/ Esc 返回；移动端虚拟方向键',
      '首页「好玩的东西」网格新增 3D 美术馆卡片（Try It Out! 直达）；房间与挂位参数参考 michelle-liu 开源仓库（MIT）',
    ],
  },
  {
    date: '2026.10.19',
    tag: '1:1 复刻',
    tagClass: 'tag-purple',
    items: [
      '首页按 liumichelle.com 1:1 复刻：190° 蓝→紫→粉 PageHeader 渐变 + 噪点纹理、左对齐 hero（36px 名字 + 18px 灰描述 + 行内链接）',
      'Work / Art / About 三胶囊 tab（毛玻璃浮岛激活态 + zinc 分隔线），内页新增同款精简头（Work/Art/About/Blog + More）',
      '项目网格对齐原站：2 列 24px 间距、26px 大圆角媒体卡、图上左下角白色标题胶囊（标题 • 年份）、hover 浮现「↗ Try It Out!」蓝链',
      '入场动画对齐：projectCardEnter 450ms 交错 60ms；页脚对齐：四栏 + 实时当地时间闪烁 + Let\'s chat! 邮箱 + Built with 行 + CHANGELOG 胶囊',
      '设计实现参考 michelle-liu 开源仓库（MIT License），内容与素材全部为本站自有',
    ],
  },
  {
    date: '2026.10.19',
    tag: '大改版',
    tagClass: 'tag-red',
    items: [
      '整体视觉改版：从蓝色博客风切换为暖白 + 柔彩的 portfolio 风格，全局换用 Manrope / DM Sans 字体',
      '布局重构：移除左侧边栏，改为居中单列大圆角卡片布局，导航改为胶囊样式',
      '全新首页：柔和渐变光晕 hero、「旅程」经历卡片、「好玩的东西」项目网格、最新写作',
      '新增玩具：拍立得工作室（滤镜 + 手写日期 + 导出）、数字速写本（画板 + 导出）',
      '「关于」页重写：现在在做 / 事实卡片 / 后台可编辑的自我介绍',
      '新增本更新日志页，页脚常驻 CHANGELOG 入口',
      '页脚重做：三栏品牌 / 导航 / 联系方式 + 圆角顶部造型',
    ],
  },
  {
    date: '2026.10.18',
    tag: '内容',
    tagClass: 'tag-blue',
    items: ['Agent 学习计划收官：阶段四「查缺补强」（W15-W18）上线，补齐评测体系 / 上下文工程 / 前沿补课 / 面试冲刺'],
  },
  {
    date: '2026.10.14',
    tag: '上线',
    tagClass: 'tag-green',
    items: ['博客 v1 上线：Spring Boot 3 + Vue 3 + MySQL 全栈自研，支持文章 / 分类 / 标签 / 图库 / 评论 / 后台管理'],
  },
]
</script>

<style scoped>
.container-narrow {
  max-width: 720px;
  margin: 0 auto;
  padding: 0 20px;
}

.cl-head {
  position: relative;
  padding: 72px 0 40px;
  overflow: hidden;
}

.cl-head h1 {
  font-family: var(--font-display);
  font-size: clamp(34px, 5vw, 44px);
  font-weight: 800;
  letter-spacing: -0.04em;
  margin-bottom: 10px;
}

.cl-head p {
  color: var(--text-secondary);
  font-size: 15px;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.5;
  pointer-events: none;
}

.orb-lavender {
  width: 240px;
  height: 240px;
  background: #e4deff;
  top: -160px;
  right: 0;
}

.cl-head h1,
.cl-head p {
  position: relative;
  z-index: 1;
}

[data-theme='dark'] .orb {
  opacity: 0.22;
}

.timeline {
  padding-bottom: 60px;
}

.entry {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  padding: 26px 28px;
  margin-bottom: 18px;
}

.entry-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.entry-date {
  font-family: 'DM Mono', 'JetBrains Mono', Consolas, monospace;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.entry-tag {
  font-size: 11.5px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 999px;
}

.tag-red { background: var(--tint-peach); color: #c05640; }
.tag-blue { background: var(--tint-sky); color: #3d6fb5; }
.tag-green { background: var(--tint-mint); color: #2f8a63; }
.tag-purple { background: var(--tint-lavender); color: #6c5ce7; }

[data-theme='dark'] .tag-red { color: #f0a48f; }
[data-theme='dark'] .tag-blue { color: #8db6e8; }
[data-theme='dark'] .tag-green { color: #7fc9a6; }
[data-theme='dark'] .tag-purple { color: #b3a7ff; }

.entry-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.entry-list li {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-secondary);
  padding-left: 18px;
  position: relative;
}

.entry-list li::before {
  content: '·';
  position: absolute;
  left: 4px;
  color: var(--accent-color);
  font-weight: 700;
}
</style>
