// 从「AI Agent 秋招冲刺」桌面 HTML 计划表提取数据 → blog-frontend/public/data/agent/*.json
// 用法: node tools/extract_agent_plan.js [源目录]
// 数据来源（本地文件，仅提取文本与结构，不做去重/重排）：
//   阶段一/二/三完整计划表.html、B站博主视频推荐完整表.html
const fs = require('fs');
const path = require('path');

const SRC = process.argv[2] || 'C:/Users/34191/DeskBox/我的桌面';
const OUT = path.join(__dirname, '..', 'blog-frontend', 'public', 'data', 'agent');

function readSrc(file) {
  return fs.readFileSync(path.join(SRC, file), 'utf8');
}

// 抽取 `const name = [ ... ]` 数组字面量（括号配对扫描，忽略字符串内括号）
function grabArray(src, name) {
  const start = src.indexOf(`const ${name} = `);
  if (start < 0) throw new Error(`const ${name} not found`);
  const i = src.indexOf('[', start);
  let depth = 0;
  let quote = null;
  for (let j = i; j < src.length; j++) {
    const c = src[j];
    if (quote) {
      if (c === quote && src[j - 1] !== '\\') quote = null;
    } else if (c === '"' || c === "'" || c === '`') {
      quote = c;
    } else if (c === '[') depth++;
    else if (c === ']') {
      depth--;
      if (depth === 0) return eval('(' + src.slice(i, j + 1) + ')'); // 本地可信数据文件
    }
  }
  throw new Error(`unbalanced array: ${name}`);
}

const stripTags = (s) => String(s).replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim();
const normTasks = (arr) => arr.map((t) => stripTags(Array.isArray(t) ? t.filter(Boolean).join(' · ') : t));

// ---------------- 阶段一 / 阶段二 ----------------
const PHASE1_META = [
  { num: 'W1', title: 'Transformer · 注意力机制 · 主流模型 · MoE · 多模态', dates: '7/7 — 7/13', tag: 'LLM 原理速通', tagClass: 'blue', sub: '7/7 (周二) → 7/13 (周一) · 目标：用白板画出 Transformer 架构，能解释"为什么 LLM 能理解语言"', checkTitle: 'W1 验收自检' },
  { num: 'W2', title: 'LangGPT · Few-shot/CoT · 系统提示词 · 结构化输出', dates: '7/14 — 7/20', tag: 'Prompt Engineering', tagClass: 'orange', sub: '7/14 (周二) → 7/20 (周一) · 目标：写出 3 套可复用的结构化 Prompt 模板', checkTitle: 'W2 验收自检' },
  { num: 'W3', title: 'Embedding · 向量数据库 · Chunking · 混合检索 · Rerank · RAGAS', dates: '7/21 — 7/27', tag: 'RAG 检索增强生成', tagClass: 'green', sub: '7/21 (周二) → 7/27 (周一) · 目标：用 ChromaDB 跑通一个最小 RAG demo', checkTitle: 'W3 验收自检' },
  { num: 'W4', title: 'ReAct · FC · MCP 协议 · MCP Server · Agent 四大模式', dates: '7/28 — 8/2', tag: 'Agent 原理 + Function Calling + MCP', tagClass: 'red', sub: '7/28 (周二) → 8/2 (周日) · 目标：能口述 ReAct 流程，写出一个 MCP Server', checkTitle: 'W4 验收自检' },
  { num: '复习', title: '阶段一总复习 · 知识点串联 · 代码复现 · 口述演练', dates: '8/3 — 8/7', tag: '查漏补缺', tagClass: 'purple', sub: '8/3 (周一) → 8/7 (周五) · 目标：整理笔记 + 代码归档 + 口述练习', checkTitle: '🏁 阶段一 最终验收清单' },
];

const PHASE2_META = [
  { num: 'W5', title: 'Model I/O · LCEL · Memory · Document Loader · RAG Chain · Tool', dates: '8/8 — 8/14', tag: 'LangChain 基础', tagClass: 'blue', sub: '8/8 (周六) → 8/14 (周五) · 目标：理解 LCEL 链式调用，跑通 Retrieval QA 链', checkTitle: 'W5 验收自检' },
  { num: 'W6', title: 'State/Node/Edge · 条件分支 · Tool 集成 · Checkpoint · 流式输出', dates: '8/15 — 8/21', tag: 'LangGraph 核心', tagClass: 'purple', sub: '8/15 (周六) → 8/21 (周五) · 目标：理解 State/Node/Edge，搭建带分支的 LangGraph 图', checkTitle: 'W6 验收自检' },
  { num: 'W7', title: '子图 · Plan-and-Execute · Multi-Agent · Reflection · A2A', dates: '8/22 — 8/28', tag: 'LangGraph 进阶 + Multi-Agent', tagClass: 'orange', sub: '8/22 (周六) → 8/28 (周五) · 目标：实现子图/Multi-Agent/Reflection 模式', checkTitle: 'W7 验收自检' },
  { num: 'W8', title: 'Dify 部署 · 工作流 · 知识库 · Agent 节点 · API 发布 · Coze 入门', dates: '8/29 — 9/4', tag: 'Dify + Coze 低代码平台', tagClass: 'green', sub: '8/29 (周六) → 9/4 (周五) · 目标：Dify 本地部署 + 搭建完整 Agent 应用 + Coze 上手', checkTitle: 'W8 验收自检' },
  { num: 'W9', title: 'FastAPI · SSE · WebSocket · Agent API · Docker · Compose', dates: '9/5 — 9/11', tag: 'FastAPI 后端 + Docker 部署', tagClass: 'red', sub: '9/5 (周六) → 9/11 (周五) · 目标：把 Agent 封装成 API 服务并 Docker 部署', checkTitle: 'W9 验收自检' },
  { num: 'W10', title: 'LangGraph 精修 · API 封装 · 代码归档 · 面试笔记整理', dates: '9/12 — 9/19', tag: '阶段二综合冲刺', tagClass: 'purple', sub: '9/12 (周六) → 9/19 (周五) · 目标：代码精修 + 完整归档 + 评估能否独立搭建 Agent 应用', checkTitle: '🏁 阶段二 最终验收清单' },
];

function buildPhase(prefix, file, meta, name, sub) {
  const schedule = grabArray(readSrc(file), 'schedule');
  const weeks = schedule.map((w, wi) => {
    const m = meta[wi];
    const key = `${prefix}-w${w.week}`;
    return {
      key,
      num: m.num,
      title: m.title,
      dates: m.dates,
      tag: m.tag,
      tagClass: m.tagClass,
      sub: m.sub,
      checkTitle: m.checkTitle,
      checkItems: w.checkItems.map((t, i) => ({ id: `${key}-chk${i}`, text: t })),
      days: w.days.map((d, i) => ({
        id: `${key}-d${i}`,
        dow: d.dow,
        date: d.date,
        tasks: normTasks(d.tasks),
        output: d.output,
      })),
    };
  });
  return { name, sub, weeks };
}

// ---------------- 阶段三 ----------------
function buildPhase3() {
  const src = readSrc('阶段三完整计划表.html');
  const arr = (n) => grabArray(src, n);
  const arch = [...src.matchAll(/<div class="arch">([\s\S]*?)<\/div>/g)].map((m) => m[1].trim());
  const mkDays = (data, key) => data.map((d, i) => ({
    id: `${key}-d${i}`,
    dow: d.dow,
    date: d.date,
    tasks: normTasks(d.tasks),
    output: d.output,
  }));

  const projects = [
    {
      title: '项目① 多模态 RAG 问答系统',
      sub: 'W11-W12 · 9/20 → 10/3 · 把 CV 底子转化为 Agent 方向的独特优势',
      tech: '🛠 LangChain + Chroma/Qdrant + BGE-Rerank + FastAPI + SSE + DeepSeek/GPT-4o API',
      arch: arch[0] || '',
      weeks: [
        { key: 'p3-p1w11', header: '📅 W11：项目① 核心链路开发（9/20 — 9/27）', days: mkDays(arr('proj1W11'), 'p3-p1w11') },
        { key: 'p3-p1w12', header: '📅 W12：项目① 集成 + 优化 + 部署（9/28 — 10/3）', days: mkDays(arr('proj1W12'), 'p3-p1w12') },
      ],
      resume: [
        '【多模态RAG系统】针对纯文本RAG无法理解视觉信息的问题，搭建图文混合检索链路，融合向量检索+BM25稀疏检索+BGE-Rerank重排序，实现图片+文字双模态输入的精准问答，top-5召回准确率达XX%。',
        '【Agent编排】针对多步骤推理容易丢失上下文的问题，用LangGraph设计3节点工作流（意图解析→检索→生成），支持流式SSE输出和来源引用，用户等待时间降低XX%。',
      ],
      checkTitle: '项目① 验收清单',
      checklist: arr('checkProj1').map((t, i) => ({ id: `p3-p1-chk${i}`, text: t })),
    },
    {
      title: '项目② AI 研究助手 Agent',
      sub: 'W13 · 10/4 → 10/10 · 展示 Multi-Agent 编排能力',
      tech: '🛠 LangGraph + Multi-Agent + WebSocket + FastAPI + Docker',
      arch: arch[1] || '',
      weeks: [
        { key: 'p3-p2w13', header: '📅 W13：项目② 完整开发（10/4 — 10/10）', days: mkDays(arr('proj2W13'), 'p3-p2w13') },
      ],
      resume: [
        '【Multi-Agent编排】针对复杂研究任务难以一步完成的问题，用LangGraph子图机制设计多Agent协作架构（规划→执行→汇总→反思），支持并行子任务和自动重试，任务完成率达XX%。',
        '【实时交互】针对长时间任务用户无法感知进度的问题，通过WebSocket实现Agent执行状态实时推送，支持用户中途干预和任务终止。',
      ],
      checkTitle: '项目② 验收清单',
      checklist: arr('checkProj2').map((t, i) => ({ id: `p3-p2-chk${i}`, text: t })),
    },
  ];

  return {
    name: '阶段三 · 项目冲刺',
    sub: '9/20 — 10/18 · 2 个完整 Agent 项目 → 简历',
    projects,
    final: { key: 'p3-p2w14', header: '📅 W14：最终冲刺（10/11 — 10/18）', days: mkDays(arr('proj2W14'), 'p3-p2w14') },
    finalCheckTitle: '🏁 最终验收清单',
    finalChecklist: arr('checkFinal').map((t, i) => ({ id: `p3-final-chk${i}`, text: t })),
  };
}

// ---------------- 资源推荐 ----------------
function buildResources() {
  const src = readSrc('B站博主视频推荐完整表.html');
  const arr = (n) => grabArray(src, n);
  const map = (key, data) => data.map((it, i) => ({
    id: `${key}-${i}`,
    name: it.name,
    link: it.link || '',
    note: it.link ? '' : (it.id || ''),
    desc: it.desc,
    tags: it.tags || [],
  }));
  return {
    groups: [
      { key: 'res-core', title: '🔥 核心推荐', stars: '⭐⭐⭐⭐⭐', sub: '必看 · 面试答原理就靠这些', items: map('res-core', arr('core')) },
      { key: 'res-focus', title: '🌟 重点关注', stars: '⭐⭐⭐⭐', sub: '框架实操补充 · 建议重点跟练', items: map('res-focus', arr('focus')) },
      { key: 'res-aux', title: '📺 辅助推荐', stars: '⭐⭐⭐', sub: '扩展视野 · 行业趋势', items: map('res-aux', arr('aux')) },
      { key: 'res-online', title: '📖 非 B 站必看在线资源', stars: '', sub: '官方文档 + 开源课程 + 在线教程', items: arr('online').map((it, i) => ({ id: `res-online-${i}`, name: it.name, link: it.url, note: '', desc: it.desc, tags: [] })) },
    ],
  };
}

// ---------------- 汇总输出 ----------------
const phase1 = buildPhase('p1', '阶段一完整计划表.html', PHASE1_META, '阶段一 · 速成基础', 'LLM 原理 → Prompt → RAG → MCP');
const phase2 = buildPhase('p2', '阶段二完整计划表.html', PHASE2_META, '阶段二 · 框架实操', 'LangChain → LangGraph → Dify/Coze → FastAPI/Docker');
const phase3 = buildPhase3();
const resources = buildResources();

const phaseTotal = (p) => p.weeks.reduce((s, w) => s + w.days.length + w.checkItems.length, 0);
const phase3Total = (p) =>
  p.projects.reduce((s, pr) => s + pr.weeks.reduce((t, w) => t + w.days.length, 0) + pr.checklist.length, 0) +
  p.final.days.length + p.finalChecklist.length;
const resTotal = (r) => r.groups.reduce((s, g) => s + g.items.length, 0);

const index = {
  phases: [
    { key: 'p1', name: phase1.name, total: phaseTotal(phase1) },
    { key: 'p2', name: phase2.name, total: phaseTotal(phase2) },
    { key: 'p3', name: phase3.name, total: phase3Total(phase3) },
  ],
  resources: { key: 'res', name: '资源推荐', total: resTotal(resources) },
  totalItems: phaseTotal(phase1) + phaseTotal(phase2) + phase3Total(phase3) + resTotal(resources),
};

fs.mkdirSync(OUT, { recursive: true });
const write = (f, d) => fs.writeFileSync(path.join(OUT, f), JSON.stringify(d), 'utf8');
write('phase1.json', phase1);
write('phase2.json', phase2);
write('phase3.json', phase3);
write('resources.json', resources);
write('index.json', index);

console.log(`阶段一: ${phaseTotal(phase1)} 项`);
console.log(`阶段二: ${phaseTotal(phase2)} 项`);
console.log(`阶段三: ${phase3Total(phase3)} 项`);
console.log(`资源: ${resTotal(resources)} 条`);
console.log(`合计: ${index.totalItems} 项 → ${OUT}`);
