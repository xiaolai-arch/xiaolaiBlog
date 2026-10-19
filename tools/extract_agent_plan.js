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
  { num: 'W2', title: 'LangGPT · Few-shot/CoT · 系统提示词 · 结构化输出', dates: '7/14 — 7/20', tag: 'Prompt → 上下文工程', tagClass: 'orange', sub: '7/14 (周二) → 7/20 (周一) · 目标：写出 3 套可复用的结构化 Prompt 模板；2026-10 提示：面试话术已从 Prompt Engineering 升级为 Context Engineering（管理整个上下文窗口），老知识保留、表述升级，详见阶段四 W16', checkTitle: 'W2 验收自检' },
  { num: 'W3', title: 'Embedding · 向量数据库 · Chunking · 混合检索 · Rerank · RAGAS', dates: '7/21 — 7/27', tag: 'RAG 检索增强生成', tagClass: 'green', sub: '7/21 (周二) → 7/27 (周一) · 目标：用 ChromaDB 跑通一个最小 RAG demo', checkTitle: 'W3 验收自检' },
  { num: 'W4', title: 'ReAct · FC · MCP 协议 · MCP Server · Agent 四大模式', dates: '7/28 — 8/2', tag: 'Agent 原理 + Function Calling + MCP', tagClass: 'red', sub: '7/28 (周二) → 8/2 (周日) · 目标：能口述 ReAct 流程，写出一个 MCP Server', checkTitle: 'W4 验收自检' },
  { num: '复习', title: '阶段一总复习 · 知识点串联 · 代码复现 · 口述演练', dates: '8/3 — 8/7', tag: '查漏补缺', tagClass: 'purple', sub: '8/3 (周一) → 8/7 (周五) · 目标：整理笔记 + 代码归档 + 口述练习', checkTitle: '🏁 阶段一 最终验收清单' },
];

const PHASE2_META = [
  { num: 'W5', title: 'LangChain 1.0 · create_agent · 标准化工具 · Middleware（旧 LCEL/Memory 仅需看懂）', dates: '8/8 — 8/14', tag: 'LangChain 1.0', tagClass: 'blue', sub: '8/8 (周六) → 8/14 (周五) · 2026-10 修订：LangChain 1.0（2025-10 发布）后底层直接跑在 LangGraph 上，create_agent 是新入口；旧 LCEL 长链与 Memory 模块已被 checkpoint/persistence 取代，会看懂遗留代码即可、别深钻；目标：用 create_agent 跑通标准 Agent', checkTitle: 'W5 验收自检' },
  { num: 'W6', title: 'State/Node/Edge · 条件分支 · Tool 集成 · Checkpoint · 流式输出', dates: '8/15 — 8/21', tag: 'LangGraph 核心', tagClass: 'purple', sub: '8/15 (周六) → 8/21 (周五) · 目标：理解 State/Node/Edge，搭建带分支的 LangGraph 图', checkTitle: 'W6 验收自检' },
  { num: 'W7', title: '子图 · Plan-and-Execute · Multi-Agent · Reflection · A2A', dates: '8/22 — 8/28', tag: 'LangGraph 进阶 + Multi-Agent', tagClass: 'orange', sub: '8/22 (周六) → 8/28 (周五) · 目标：实现子图/Multi-Agent/Reflection 模式', checkTitle: 'W7 验收自检' },
  { num: 'W8', title: 'Dify/Coze 选型对比 + MVP 演示（压缩至 2-3 天）· 省下时间预习 Agent 评测', dates: '8/29 — 9/4', tag: '低代码选型（降权）', tagClass: 'green', sub: '8/29 (周六) → 9/4 (周五) · 2026-10 修订：低代码平台不是招聘 JD 硬性要求（Coze 无私有化部署、企业场景受限），会演示 + 能口述选型即可；周中腾出的时间预习 Agent 评测（2026 JD 新增硬性要求，对应阶段四 W15）', checkTitle: 'W8 验收自检' },
  { num: 'W9', title: 'FastAPI · SSE · WebSocket · Agent API · Docker · Compose', dates: '9/5 — 9/11', tag: 'FastAPI 后端 + Docker 部署', tagClass: 'red', sub: '9/5 (周六) → 9/11 (周五) · 目标：把 Agent 封装成 API 服务并 Docker 部署', checkTitle: 'W9 验收自检' },
  { num: 'W10', title: 'LangGraph 精修 · API 封装 · 代码归档 · 面试笔记整理', dates: '9/12 — 9/19', tag: '阶段二综合冲刺', tagClass: 'purple', sub: '9/12 (周六) → 9/19 (周五) · 目标：代码精修 + 完整归档 + 评估能否独立搭建 Agent 应用', checkTitle: '🏁 阶段二 最终验收清单' },
];

// ---------------- 2026-10 修订：就地替换每日任务/验收文本 ----------------
// 只改文本、不增删天数与清单位序（末尾追加除外），保留已保存的打卡记录 id
const INPLACE_PATCHES = {
  'p1-w2': {
    dayAppend: {
      5: ['延伸：了解 Prompt Engineering → Context Engineering 的行业转变（压缩/预算/记忆分层），面试用新话术表述老知识（阶段四 W16 展开）'],
    },
    checkAppend: ['能说清 Prompt 工程与上下文工程的区别（2026 面试新话术）'],
  },
  'p2-w5': {
    days: {
      1: { tasks: ['LCEL 管道操作符与 Runnable 接口快速过一遍（会看懂即可，1.0 后不再是主入口）', '精读 LangChain 1.0 发布说明：create_agent 新入口、middleware、标准化工具', '用 create_agent 搭一个最小 Agent（模型 + 1 个工具）'], output: 'create_agent 最小 demo + LCEL 速览笔记' },
      2: { tasks: ['旧 Memory 模块（Buffer/Summary/VectorStore）快速了解——1.0 已弃用，面试点在新方案', '用 LangGraph checkpointer 实现多轮记忆对话（短期记忆的正解）', '对比新旧思路：Memory 对象 → 状态持久化（checkpoint/persistence）'], output: '基于 checkpointer 的多轮对话 demo' },
      5: { tasks: ['学习 @tool 装饰器与 1.0 标准化工具接口', '用 create_agent 搭多 Tool Agent：搜索 + 计算 + 文件读写', '理解 Agent 循环：模型决策 → 工具执行 → 结果回填'] },
      6: { tasks: ['整理 W5 所有代码到 GitHub 仓库', '画「LangChain 1.0 架构分界图」：create_agent 快速搭 / LangGraph 自定义控制流 / LangSmith 监控评测', '口述：旧 LCEL 链时代 → 1.0 agent 时代的演进与取舍'] },
    },
    checks: {
      0: '理解 1.0 新架构：create_agent 入口 + LangGraph 底座（旧 LCEL 只需看懂）',
      1: '能用 LangGraph checkpointer 实现多轮记忆（旧 Memory 已弃用）',
    },
  },
  'p2-w8': {
    days: {
      2: { tasks: ['Dify 知识库 + 检索策略速过（与 W3 的 RAG 原理对照着看）', '预习：下载 3 份 2026 Agent 岗 JD，标出「评测与质量体系」相关要求', '预习：为什么 Agent 评测从 vibe check 变成了正规工程'], output: '知识库问答应用 + JD 标注笔记' },
      3: { tasks: ['Dify Agent 节点快速过（会演示即可，不深钻）', '预习：评测分层框架——离线组件级 / 离线端到端 / 在线 CUJ'], output: 'Dify Agent 演示 + 评测分层笔记' },
      4: { tasks: ['Dify API 发布 + SSE 调用速过', '预习：RAGAS 四指标原理（faithfulness / answer relevancy / context precision / recall）'], output: 'API 外部调用成功 + RAGAS 预习笔记' },
      5: { tasks: ['Coze 上手半天：搭 1 个 Bot、会演示即可', '口述选型：Dify vs Coze vs n8n vs 代码框架（私有化 / 生态 / 适用场景）'], output: 'Coze Bot + 选型口述练习' },
      6: { tasks: ['整理「LangChain / LangGraph / Dify / Coze(+n8n) 框架对比表」', '理解各自边界：低代码 = MVP 验证器，不是就业核心技能', '口述：什么场景用什么框架？为什么？'] },
    },
    checks: {
      4: 'Coze 上手并能口述 Dify / Coze / n8n 选型逻辑（不深钻）',
      5: '能横向对比 LangChain / LangGraph / Dify / Coze(+n8n) 的优劣势',
    },
    checkAppend: ['预习了 Agent 评测分层并产出笔记（为 W15 铺路）'],
  },
};

function applyWeekPatch(week) {
  const p = INPLACE_PATCHES[week.key];
  if (!p) return;
  if (p.days) {
    for (const [i, d] of Object.entries(p.days)) {
      const day = week.days[+i];
      if (!day) continue;
      if (d.tasks) day.tasks = d.tasks;
      if (d.output) day.output = d.output;
    }
  }
  if (p.dayAppend) {
    for (const [i, extra] of Object.entries(p.dayAppend)) {
      const day = week.days[+i];
      if (day) day.tasks = [...day.tasks, ...extra];
    }
  }
  if (p.checks) {
    for (const [i, text] of Object.entries(p.checks)) {
      if (week.checkItems[+i]) week.checkItems[+i].text = text;
    }
  }
  if (p.checkAppend) {
    for (const text of p.checkAppend) {
      week.checkItems.push({ id: `${week.key}-chk${week.checkItems.length}`, text });
    }
  }
}

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
  weeks.forEach(applyWeekPatch);
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

// ---------------- 阶段四（2026-10 新增：查缺补强） ----------------
// 背景：原 14 周计划于 10/18 收官。对照 2026-10 招聘 JD 与行业前沿，
// 补齐四大缺口：Agent 评测体系 / 上下文工程 + 1.0 重构 / 框架与协议前沿 / 面试冲刺
function buildPhase4() {
  const mkWeek = (num, title, tag, tagClass, sub, days, checkItems) => {
    const key = `p4-w${num}`;
    return {
      key,
      num: `W${num}`,
      title,
      dates: `${days[0].date} — ${days[days.length - 1].date}`,
      tag,
      tagClass,
      sub,
      checkTitle: `W${num} 验收自检`,
      checkItems: checkItems.map((t, i) => ({ id: `${key}-chk${i}`, text: t })),
      days: days.map((d, i) => ({ id: `${key}-d${i}`, dow: d.dow, date: d.date, tasks: d.tasks, output: d.output })),
    };
  };

  const w15 = mkWeek(15, '评测分层 · RAGAS · LLM-as-judge · 轨迹评测 · 合成用户模拟', 'Agent 评测体系（JD 新增硬性要求）', 'red',
    '10/19 (周一) → 10/25 (周日) · 2026 招聘 JD 已把「Agent 评测与质量体系建设」列为单独要求项，这是原计划最大缺口；目标：两个项目都有可复现的评测脚本与真实指标',
    [
      { dow: '周一', date: '10/19', tasks: ['读 Google 官方博客《Evaluating AI agents: moving past the vibe check》', '建立评测分层框架：离线组件级（检索/生成）→ 离线端到端（轨迹/任务完成）→ 在线（CUJ 关键用户旅程）', '下载腾讯/字节 Agent 岗 JD，逐条标出评测相关要求'], output: '评测分层笔记 + JD 标注' },
      { dow: '周二', date: '10/20', tasks: ['RAGAS 四指标深入：faithfulness / answer relevancy / context precision / recall', '给项目①构建 RAG 测试集（≥50 条问答对，覆盖单跳/多跳/拒答）', '跑通 RAGAS 对项目①的组件级评测'], output: '项目① RAGAS 评测报告 v1' },
      { dow: '周三', date: '10/21', tasks: ['学 LLM-as-judge：rubric 设计与打分 Prompt 写法', '了解 judge 的坑：位置偏差、自我偏好、长度偏差及缓解手段', '给项目①答案质量接入 judge，与 RAGAS 结果交叉验证'], output: 'judge 评测脚本' },
      { dow: '周四', date: '10/22', tasks: ['学 Agent 轨迹评测：工具选择正确率、步骤成功率、任务完成率', '上手 DeepEval 或 LangSmith Evaluations（选一个）', '定义项目②的 20 个标准任务 + 成功判定条件'], output: '项目②任务评测集' },
      { dow: '周五', date: '10/23', tasks: ['学合成用户模拟：用 LLM 模拟多轮用户，按 CUJ 压测 Agent', '给项目②跑 20 任务 × 模拟用户，记录轨迹与失败点', '失败归因：检索没召回 / 工具选错 / 上下文丢失'], output: '项目②首轮评测报告' },
      { dow: '周六', date: '10/24', tasks: ['根据归因修 1-2 个最痛的问题（调 chunking / 重排 / 工具描述）', '复测对比：修复前后指标变化', '把简历里项目①②的「XX%」全部替换为真实测量值'], output: '简历数字落地' },
      { dow: '周日', date: '10/25', tasks: ['整理 W15 笔记：评测方法论一页纸', '评测脚本归档 GitHub（可复现）', '预览 W16 上下文工程材料'], output: '评测脚本归档' },
    ],
    ['能口述评测三分层及各自适用场景', '项目①有 ≥50 条测试集与 RAGAS + judge 双评测', '项目②有 20 任务评测集与首轮完成率数据', '能说出 LLM-as-judge 三个典型偏差及缓解方法', '简历中的指标数字全部来自真实测量']);

  const w16 = mkWeek(16, '上下文压缩 · Token 预算 · 记忆分层 · 子 Agent 隔离 · 1.0 重构', 'Context Engineering + 1.0 重构', 'purple',
    '10/26 (周一) → 11/1 (周日) · 2026 面试热词已从 Prompt Engineering 转向 Context Engineering；目标：两个项目跑在 1.0 新架构上，且有量化的上下文管理策略',
    [
      { dow: '周一', date: '10/26', tasks: ['精读 Anthropic《Effective context engineering for AI agents》', '建立心智模型：上下文是稀缺资源，注意力就是分配', '盘点项目当前 token 构成：系统指令 / 历史 / 检索内容 / 工具结果各占多少'], output: 'token 构成账单' },
      { dow: '周二', date: '10/27', tasks: ['学上下文压缩：compaction（对话摘要回填）与裁剪策略', '学 token 预算分配：系统指令 / 检索内容 / 历史各占多少', '给项目①长对话场景加摘要压缩'], output: '项目①压缩 demo' },
      { dow: '周三', date: '10/28', tasks: ['学记忆分层：短期（会话内 checkpointer）vs 长期（跨会话 store）', '对比长期记忆方案：LangGraph Store / Mem0 / Letta', '选一个方案接入项目②（如记住用户研究偏好）'], output: '项目②长期记忆 demo' },
      { dow: '周四', date: '10/29', tasks: ['学子 Agent 上下文隔离：为什么多 Agent 能省上下文', '理解「上下文污染」与工具结果膨胀问题', '把项目②汇总节点改为独立 sub-agent，对比 token 消耗'], output: 'sub-agent 隔离对比数据' },
      { dow: '周五', date: '10/30', tasks: ['项目①重构：用 1.0 create_agent 替换旧链式写法', '接 1.0 middleware：请求日志 + 评测埋点', '回归测试：重构后 W15 评测指标不降'], output: '项目①重构完成' },
      { dow: '周六', date: '10/31', tasks: ['项目②重构到 1.0 + LangGraph 生产化（checkpoint 持久化 + 断点恢复）', '整理重构 diff：旧写法 vs 新写法对照表', '更新两个项目 README 架构图'], output: '项目②重构完成' },
      { dow: '周日', date: '11/1', tasks: ['整理 W16 笔记：上下文工程一页纸', '简历话术升级：Prompt 工程 → 上下文工程（压缩/预算/记忆分层）', '预览 W17 前沿材料'], output: '简历话术 v2' },
    ],
    ['能口述 Prompt Engineering → Context Engineering 的转变', '两个项目均跑在 1.0 API 上且评测指标不降', '有 token 预算/压缩策略并能量化收益', '项目②接入长期记忆', '简历使用 2026 话术（上下文工程/评测体系）']);

  const w17 = mkWeek(17, '框架格局(Agents SDK/ADK) · 协议生态(MCP/A2A) · Agent Skills · Coding Agent 实战', '前沿补课 + Coding Agent', 'orange',
    '11/2 (周一) → 11/8 (周日) · JD 高频新面孔与面试谈资集中补课；目标：一张框架对比矩阵 + 一张协议格局图 + 一次 coding agent 真实交付',
    [
      { dow: '周一', date: '11/2', tasks: ['框架格局：OpenAI Agents SDK vs LangGraph vs Google ADK vs CrewAI/PydanticAI', '重点学 OpenAI Agents SDK（JD 高频）：agent 循环 + handoff 机制', '产出框架对比矩阵（定位/编排模型/评测/生产化）'], output: '框架对比矩阵' },
      { dow: '周二', date: '11/3', tasks: ['协议格局：MCP（agent↔工具）vs A2A（agent↔agent）vs ACP/UCP', 'A2A 现状：入 Linux 基金会一年、150+ 组织、首批生产部署；读懂 Agent Card', '画一张协议生态格局图并能口述'], output: '协议格局图' },
      { dow: '周三', date: '11/4', tasks: ['学 Agent Skills（SKILL.md）：模块化能力封装的官方模式', '给自己的项目写 1-2 个 Skill（如「生成评测报告」）', '了解 harness engineering：agent 运行环境即产品'], output: '1 个自定义 Agent Skill' },
      { dow: '周四', date: '11/5', tasks: ['概览 Computer Use / Browser Agent：能力边界与典型场景', '了解 Claude computer use、browser-use 的定位', '明确边界：什么任务给 API 工具，什么才需要 GUI 操作'], output: '场景判断笔记' },
      { dow: '周五', date: '11/6', tasks: ['Coding Agent 实战：用 Claude Code / Cursor 完成一个真实小需求全流程', '体验 sub-agent、hooks、skills 等高级能力', '记录：哪些环节 Agent 快、哪些环节必须亲自审'], output: '一次真实交付记录' },
      { dow: '周六', date: '11/7', tasks: ['Code Review 训练：审查 Agent 生成代码的 diff', '建立「验证 Agent 产出」流程：测试先行 + 关键路径人工审', '读 Harvard 研究要点：Agent 提升编码量，交付瓶颈在人审'], output: '个人 review checklist' },
      { dow: '周日', date: '11/8', tasks: ['整理 W17 笔记', '整理「最近 AI 圈发生了什么」素材卡（A2A 入基金会 / 1.0 发布 / 评测正规化 / coding agent 榜单）', '预览 W18 面试冲刺材料'], output: '前沿素材卡' },
    ],
    ['有框架对比矩阵并能口述选型逻辑', '有协议格局图并能讲清 MCP vs A2A 分工', '写过至少 1 个 Agent Skill', '用 coding agent 完成过一次真实交付', '有自己的 Agent 代码 review checklist']);

  const w18 = mkWeek(18, '简历定稿 · 项目深挖 Q&A · 原理口述 · 开放题 · 模拟面试', '面试冲刺', 'blue',
    '11/9 (周一) → 11/15 (周日) · 全部产出物收口；目标：简历无空洞指标，两个项目各能扛 20 层追问',
    [
      { dow: '周一', date: '11/9', tasks: ['简历终稿：两条项目 bullet 全量化（评测指标/延迟/成本）', '按 JD 关键词改写：评测体系、上下文工程、LangGraph 1.0、MCP/A2A', '准备 1 分钟项目电梯陈述 ×2'], output: '简历终稿' },
      { dow: '周二', date: '11/10', tasks: ['项目①深挖预演：为什么混合检索？rerank 怎么选型？', '准备 badcase 挖掘与修复故事（用 W15 真实数据）', '模拟追问 20 层：从架构问到具体参数'], output: '项目①问答手册' },
      { dow: '周三', date: '11/11', tasks: ['项目②深挖预演：多 Agent 架构权衡、失败重试、状态管理', '准备「评测怎么建」完整叙事（分层/指标/工具/结果）', '模拟追问 20 层'], output: '项目②问答手册' },
      { dow: '周四', date: '11/12', tasks: ['原理口述演练：Transformer / 注意力 / MoE / 训练与后训练（对镜或录音）', 'RAG 高频题串讲：chunking / 嵌入 / 混合检索 / 重排', 'Agent 高频题串讲：ReAct / 规划 / 反思 / 多 Agent'], output: '原理口述录音' },
      { dow: '周五', date: '11/13', tasks: ['开放题演练：「最近 AI 圈发生了什么」「Agent 落地最难的是什么」', '用 W17 素材卡组织 5 分钟前沿陈述', '系统设计题：现场设计一个 RAG / Agent 系统'], output: '开放题答题框架' },
      { dow: '周六', date: '11/14', tasks: ['完整模拟面试 2 轮（技术面 + 项目面）', '收集反馈，记录被问倒的题目', '针对性补漏'], output: '2 轮模拟面试记录' },
      { dow: '周日', date: '11/15', tasks: ['复查全部验收清单，确认无空洞指标', '整理 15-18 周产出物清单（GitHub / 笔记 / 报告）', '收官：计划归档'], output: '冲刺收官' },
    ],
    ['简历无「XX%」空洞指标，全部可追溯', '两个项目各能扛 20 层深挖追问', '原理题可口述 5 分钟不打结', '能 5 分钟聊清 2026 前沿（协议/评测/coding agent）', '完成 2 轮完整模拟面试并复盘']);

  return { name: '阶段四 · 查缺补强', sub: '10/19 — 11/15 · Agent 评测 → 上下文工程与 1.0 重构 → 前沿补课 → 面试冲刺', weeks: [w15, w16, w17, w18] };
}

// ---------------- 资源推荐 ----------------
// 2026-10 前沿补充（阶段四配套）+ 对旧条目就地降级标注（不改 id，保留打卡记录）
const FRONTIER_ITEMS = [
  { name: 'Anthropic: Effective context engineering for AI agents', url: 'https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents', desc: '上下文工程开山文：上下文是稀缺资源、compaction、子 Agent 隔离（W16 配套）' },
  { name: 'Google: Evaluating AI agents — moving past the vibe check', url: 'https://developers.googleblog.com/evaluating-ai-agents-moving-past-the-vibe-check', desc: '评测方法论：CUJ、合成用户模拟——2026 JD「评测体系」对应必读（W15 配套）' },
  { name: 'Anthropic: 2026 Agentic Coding Trends Report', url: 'https://resources.anthropic.com/hubfs/2026%20Agentic%20Coding%20Trends%20Report.pdf', desc: '官方趋势报告：层级化多 Agent 编排、agentic engineering 实践' },
  { name: 'LangChain 1.0 发布公告', url: 'https://www.langchain.com/blog/langchain-langgraph-1dot0', desc: '新旧架构分水岭：create_agent / middleware / 标准化工具（W5/W16 配套）' },
  { name: 'OpenAI Agents SDK 文档', url: 'https://openai.github.io/openai-agents-python/', desc: 'JD 高频新面孔：agent 循环 + handoff 机制，与 LangGraph 对比必知（W17 配套）' },
  { name: 'A2A 协议官网（Linux Foundation）', url: 'https://a2a-protocol.org', desc: 'agent↔agent 标准协议：一年 150+ 组织、首批生产部署；读懂 Agent Card（W17 配套）' },
  { name: 'anthropics/skills（Agent Skills 官方仓库）', url: 'https://github.com/anthropics/skills', desc: 'SKILL.md 模块化能力封装模式的官方示例（W17 配套）' },
  { name: 'LangChain 官方文档（含 LangGraph memory/checkpointing）', url: 'https://docs.langchain.com', desc: '1.0 后的权威参考：记忆分层、长期记忆 store、middleware' },
  { name: 'LangSmith Evaluations 文档', url: 'https://docs.smith.langchain.com', desc: '生产级评测/追踪平台，轨迹评测工具选型之一（W15 配套）' },
  { name: 'vals.ai: Vibe Code Bench 榜单', url: 'https://www.vals.ai/benchmarks/vibe-code', desc: 'coding agent 端到端建站任务榜单——面试聊 coding agent 的数据来源' },
  { name: 'Artificial Analysis: Coding Agent Leaderboard', url: 'https://artificialanalysis.ai/agents/coding-agents', desc: '真实软件工程任务上的 coding agent 性价比对比' },
  { name: 'Linux Foundation: A2A 一周年新闻稿', url: 'https://www.linuxfoundation.org/press/a2a-protocol-surpasses-150-organizations-lands-in-major-cloud-platforms-and-sees-enterprise-production-use-in-first-year', desc: 'A2A 采用数据的权威来源，写进「最近 AI 圈发生了什么」素材卡' },
];

// 旧条目降级标注（按名称匹配，改 desc 原文）
const RES_DOWNGRADE = [
  { match: '大模型-教程', note: ' ⚠️ 2026-10 降级：若内容仍是 1.0 之前的链式 API，旧写法部分跳过，以官方 1.0 文档为准' },
  { match: 'BV1U1TS6gE5P', note: ' ⚠️ 2026-10 降级：入门扫盲可看，注意甄别旧版 LangChain 写法，勿照抄' },
];

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
  const downgrade = (items) => items.map((it) => ({
    ...it,
    desc: RES_DOWNGRADE.some((d) => it.name.includes(d.match) || (it.note || '').includes(d.match))
      ? it.desc + RES_DOWNGRADE.find((d) => it.name.includes(d.match) || (it.note || '').includes(d.match)).note
      : it.desc,
  }));
  return {
    groups: [
      {
        key: 'res-frontier',
        title: '🆕 2026-10 前沿补充',
        stars: '⭐⭐⭐⭐⭐',
        sub: '评测 · 上下文工程 · 协议生态 · Coding Agent —— 秋招差异化弹药（阶段四配套）',
        items: FRONTIER_ITEMS.map((it, i) => ({ id: `res-frontier-${i}`, name: it.name, link: it.url, note: '', desc: it.desc, tags: ['2026-10', '官方一手'] })),
      },
      { key: 'res-core', title: '🔥 核心推荐', stars: '⭐⭐⭐⭐⭐', sub: '必看 · 面试答原理就靠这些', items: downgrade(map('res-core', arr('core'))) },
      { key: 'res-focus', title: '🌟 重点关注', stars: '⭐⭐⭐⭐', sub: '框架实操补充 · 建议重点跟练', items: downgrade(map('res-focus', arr('focus'))) },
      { key: 'res-aux', title: '📺 辅助推荐', stars: '⭐⭐⭐', sub: '扩展视野 · 行业趋势', items: downgrade(map('res-aux', arr('aux'))) },
      { key: 'res-online', title: '📖 非 B 站必看在线资源', stars: '', sub: '官方文档 + 开源课程 + 在线教程', items: downgrade(arr('online').map((it, i) => ({ id: `res-online-${i}`, name: it.name, link: it.url, note: '', desc: it.desc, tags: [] }))) },
    ],
  };
}

// ---------------- 汇总输出 ----------------
const phase1 = buildPhase('p1', '阶段一完整计划表.html', PHASE1_META, '阶段一 · 速成基础', 'LLM 原理 → Prompt/上下文工程 → RAG → MCP');
const phase2 = buildPhase('p2', '阶段二完整计划表.html', PHASE2_META, '阶段二 · 框架实操', 'LangChain 1.0 → LangGraph → 低代码选型 → FastAPI/Docker');
const phase3 = buildPhase3();
const phase4 = buildPhase4();
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
    { key: 'p4', name: phase4.name, total: phaseTotal(phase4) },
  ],
  resources: { key: 'res', name: '资源推荐', total: resTotal(resources) },
  totalItems: phaseTotal(phase1) + phaseTotal(phase2) + phase3Total(phase3) + phaseTotal(phase4) + resTotal(resources),
};

fs.mkdirSync(OUT, { recursive: true });
const write = (f, d) => fs.writeFileSync(path.join(OUT, f), JSON.stringify(d), 'utf8');
write('phase1.json', phase1);
write('phase2.json', phase2);
write('phase3.json', phase3);
write('phase4.json', phase4);
write('resources.json', resources);
write('index.json', index);

console.log(`阶段一: ${phaseTotal(phase1)} 项`);
console.log(`阶段二: ${phaseTotal(phase2)} 项`);
console.log(`阶段三: ${phase3Total(phase3)} 项`);
console.log(`阶段四: ${phaseTotal(phase4)} 项`);
console.log(`资源: ${resTotal(resources)} 条`);
console.log(`合计: ${index.totalItems} 项 → ${OUT}`);
