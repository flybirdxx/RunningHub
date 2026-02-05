# 🤖 RunningHub Skills 作用归纳文档

本文档归纳了项目中所有可用 Skill 的作用和使用场景。

| 技能名称 | 描述与用途 |
| :--- | :--- |
| **2d-games** | 2D 游戏开发原则。包括精灵、瓦片地图、物理和摄像机。 |
| **3d-games** | 3D 游戏开发原则。包括渲染、着色器、物理和摄像机。 |
| **algorithmic-art** | 使用 p5.js 创建算法艺术，支持种子随机性和交互式参数探索。当用户请求使用代码创建艺术、生成艺术、算法艺术、流场或粒子系统时使用。创建原创算法艺术，避免复制现有艺术家的作品以防侵权。 |
| **api-patterns** | API 设计原则和决策。包括 REST、GraphQL、tRPC 的选择，以及响应格式、版本控制和分页。 |
| **app-builder** | 应用程序构建协调器。通过自然语言请求创建全栈应用程序。确定项目类型，选择技术栈，并协调各代理。 |
| **architecture** | 架构决策框架。包括需求分析、权衡评估和 ADR（架构决策记录）文档。在进行架构决策或分析系统设计时使用。 |
| **arxiv-to-md** | 将 arXiv 论文（TeX 源码）转换为干净的 Markdown 格式，供 LLM 阅读。当用户提供 arXiv ID 或 URL，或从 PDF 文件夹同步学术论文到 Markdown 目的地时调用。 |
| **bash-linux** | Bash/Linux 终端模式。包括关键命令、管道、错误处理和脚本编写。在 macOS 或 Linux 系统上工作时使用。 |
| **behavioral-modes** | AI 操作模式（头脑风暴、实现、调试、评估、教学、交付、协调）。根据任务类型调整行为。 |
| **brainstorming** | 苏格拉底式提问协议 + 用户沟通。对于复杂请求、新功能或不明确的需求是必须的。包括进度报告和错误处理。 |
| **brand-guidelines** | 将 Anthropic 官方品牌色彩和排版应用于任何可能受益于 Anthropic 外观的产出物。当应用品牌色彩、风格指南、视觉格式或公司设计标准时使用。 |
| **canvas-design** | 利用设计哲学在 .png 和 .pdf 文档中创建精美的视觉艺术。当用户要求创建海报、艺术品、设计或其他静态作品时使用。创建原创视觉设计，避免复制现有艺术家的作品。 |
| **clean-code** | 务实的编码标准——简洁、直接，不进行过度工程，不包含不必要的注释。 |
| **code-review-checklist** | 涵盖代码质量、安全性和最佳实践的代码评估指南。 |
| **codebase-analysis** | 当用户请求代码库分析、架构评估、安全评估或质量评估时，立即通过 Python 脚本调用。不要先自行探索，脚本会协调整个探索过程。 |
| **database-design** | 数据库设计原则和决策。包括架构设计、索引策略、ORM 选择和无服务器数据库。 |
| **decision-critic** | 立即通过 Python 脚本调用以压力测试决策和推理。不要先自行分析，脚本会协调批评工作流程。 |
| **deepthink** | 当用户对开放式分析问题请求结构化推理时，立即通过 Python 脚本调用。不要先自行探索，脚本会协调思考工作流程。 |
| **deployment-procedures** | 生产部署原则和决策。包括安全部署流程、回滚策略和验证。教授思考方式，而非单纯脚本。 |
| **dispatching-parallel-agents** | 当面对两个或更多可以独立完成且无共享状态或顺序依赖的任务时使用。 |
| **doc-coauthoring** | 引导用户完成共同撰写文档的结构化流程。当用户想要编写文档、提案、技术规范、决策文档或类似结构化内容时使用。帮助用户高效转移上下文，通过迭代完善内容。 |
| **doc-sync** | 跨存储库同步文档。当用户要求同步文档时使用。 |
| **docker-expert** | Docker 容器化专家，深谙多阶段构建、镜像优化、容器安全、Docker Compose 编排和生产部署模式。主动用于优化 Dockerfile、解决容器问题、镜像体积问题、安全加固、网络和编排挑战。 |
| **documentation-templates** | 文档模板和结构指南。包括 README、API 文档、代码注释和 AI 友好型文档。 |
| **docx** | 全面的文档创建、编辑和分析工具，支持修订追踪、评论、格式保留和文本提取。当需要处理专业文档（.docx 文件）以进行创建、修改、处理修订或添加评论时使用。 |
| **executing-plans** | 当你有书面的实施计划，并需要在带有评估检查点的单独会话中执行时使用。 |
| **finishing-a-development-branch** | 当实现完成且所有测试通过，需要决定如何整合工作时使用。通过提供合并、PR 或清理的结构化选项来引导开发工作的完成。 |
| **frontend-design** | Web UI 的设计思维和决策。在设计组件、布局、配色方案、排版或创建美观界面时使用。教授原则而非固定值。 |
| **game-art** | 游戏艺术原则。包括视觉风格选择、资产流水线和动画工作流。 |
| **game-audio** | 游戏音效原则。包括声音设计、音乐集成和自适应音效系统。 |
| **game-design** | 游戏设计原则。包括 GDD（游戏设计文档）结构、平衡性、玩家心理和进度设计。 |
| **game-development** | 游戏开发协调器。根据项目需求路由到特定平台的技能。 |
| **geo-fundamentals** | 针对 AI 搜索引擎（如 ChatGPT、Claude、Perplexity）的生成引擎优化。 |
| **i18n-localization** | 国际化和本地化模式。包括检测硬编码字符串、管理翻译、语言区域文件和 RTL（从右向左）支持。 |
| **incoherence** | 检测并解决文档、代码、规范与实现之间的不一致性。 |
| **internal-comms** | 一套帮助撰写各类内部沟通文件的资源，使用公司偏好的格式。当被要求编写状态报告、领导层更新、第三方更新、公司简报、FAQ、事故报告、项目更新等时使用。 |
| **lint-and-validate** | 自动质量评估、Lint 和静态分析程序。在每次代码修改后使用，以确保语法正确和符合项目标准。触发关键字：lint, format, check, validate, types, static analysis。 |
| **mcp-builder** | MCP（模型上下文协议）服务器构建原则。包括工具设计、资源模式和最佳实践。 |
| **mobile-design** | 针对 iOS 和 Android 应用的移动优先设计思维和决策。包括触摸交互、性能模式和平台惯例。教授原则而非固定值。在使用 React Native、Flutter 或原生开发构建移动应用时使用。 |
| **mobile-games** | 移动游戏开发原则。包括触摸输入、电池损耗、性能和应用商店。 |
| **multiplayer** | 多人游戏开发原则。包括架构、网络和同步。 |
| **nestjs-expert** | Nest.js 框架专家，专注于模块架构、依赖注入、中间件、守卫、拦截器，以及使用 Jest/Supertest 进行测试、TypeORM/Mongoose 集成和 Passport.js 身份验证。主动用于处理架构决策、测试策略、性能优化或调试复杂的依赖注入问题。 |
| **nextjs-best-practices** | Next.js App Router 原则。包括服务器组件（Server Components）、数据获取和路由模式。 |
| **nodejs-best-practices** | Node.js 开发原则和决策。包括框架选择、异步模式、安全性和架构。教授思考方式而非单纯复制代码。 |
| **parallel-agents** | 多智能体协作模式。当多个独立任务可以由具备不同领域专业知识的智能体完成，或全面分析需要多个视角时使用。 |
| **pc-games** | PC 和主机游戏开发原则。包括引擎选择、平台特性和优化策略。 |
| **pdf** | 全面的 PDF 操作工具包，用于提取文本和表格、创建新 PDF、合并/拆分文档以及处理表单。当需要大规模处理、生成或分析 PDF 文档，或者填写表单时使用。 |
| **performance-profiling** | 性能分析原则。包括测量、分析和优化技术。 |
| **plan-test-writer** | 将实施计划转换为全面的测试套件，用于测试驱动开发（TDD）。 |
| **plan-writing** | 结构化任务规划，具有清晰的任务分解、依赖关系和验证标准。在实现功能、重构或任何多步工作时使用。 |
| **planner** | 针对复杂任务的交互式规划和执行。当用户要求使用或调用 planner 技能时使用。 |
| **planning-with-files** | 实现 Manus 风格的基于文件的复杂任务规划。创建 `task_plan.md`、`findings.md` 和 `progress.md`。在开始复杂的多步任务、研究项目或任何需要 >5 个工具调用的任务时使用。 |
| **powershell-windows** | PowerShell Windows 模式。包括关键陷阱、运算符语法和错误处理。 |
| **pptx** | 演示文稿（PPT）创建、编辑和分析。当需要处理 .pptx 文件以进行创建、修改、处理布局或添加评论/演讲者备注时使用。 |
| **prisma-expert** | Prisma ORM 专家，负责架构设计、迁移、查询优化、关系建模和数据库操作。主动用于处理 Prisma 架构问题、迁移难题、查询性能、关系设计或数据库连接问题。 |
| **problem-analysis** | 立即调用以进行结构化问题分析和解决方案发现。 |
| **prompt-engineer** | 当用户请求 Prompt 优化时，立即通过 Python 脚本调用。不要先自行分析，脚本会协调优化流程。 |
| **python-patterns** | Python 开发原则和决策。包括框架选择、异步模式、类型提示和项目结构。教授原则而非单纯复制代码。 |
| **react-best-practices** | 维赛尔（Vercel）工程部的 React 和 Next.js 性能优化指南。编写、审查或重构 React/Next.js 代码时使用，涉及组件、页面、数据获取、包优化或性能改进。 |
| **react-patterns** | 现代 React 模式和原则。包括 Hooks、组件组合、性能和 TypeScript 最佳实践。 |
| **react:components** | 将 Stitch 设计转换为模块化的 Vite 和 React 组件，使用系统级网络和基于 AST 的验证。 |
| **receiving-code-review** | 在收到代码审查反馈时使用，特别是在反馈不明确或技术上有疑问时，要求技术严谨性和验证，而非盲目接受建议。 |
| **red-team-tactics** | 基于 MITRE ATT&CK 的红队战术原则。包括攻击阶段、检测规避和报告撰写。 |
| **refactor** | 当用户请求重构分析、技术债审查或代码质量改进时调用。探索代码中的“代码异味（code smells）”并生成可操作的任务项。 |
| **remotion-best-practices** | Remotion 的最佳实践——在 React 中创建视频。 |
| **requesting-code-review** | 在完成任务、实现重大功能或合并之前使用，以验证工作是否符合要求。 |
| **seo-fundamentals** | SEO 基础知识，包括 E-E-A-T 原则、核心 Web 指标（Core Web Vitals）和谷歌算法原则。 |
| **server-management** | 服务器管理原则和决策。包括进程管理、监控策略和扩展决策。教授思考方式而非单纯命令。 |
| **skill-creator** | 创建高效技能的指南。当用户想要创建新技能或更新现有技能以扩展 AI 的专业知识、工作流或工具集成时使用。 |
| **slack-gif-creator** | 为 Slack 优化动画 GIF 的知识和工具。提供限制、验证工具和动画概念。当用户请求为 Slack 制作动画 GIF 时使用。 |
| **solution-design** | 当用户有明确的问题或根本原因并需要解决方案选项时，从多个推理角度生成多样化的解决方案。 |
| **subagent-driven-development** | 在当前会话中执行具有独立任务的实施计划时使用。 |
| **systematic-debugging** | 包含根本原因分析和基于证据验证的四阶段系统化调试方法论。在调试复杂问题时使用。 |
| **tailwind-patterns** | Tailwind CSS v4 原则。包括 CSS 优先配置、容器查询、现代模式和设计令牌架构。 |
| **tdd-workflow** | 测试驱动开发（TDD）工作流原则。红-绿-重构循环。 |
| **template-skill** | 技能模板，使用具体描述替换此项以说明 Skill 的用途。 |
| **templates** | 用于新应用程序的项目脚手架模板。在从头开始创建新项目时使用，包含 12 个不同技术栈的模板。 |
| **test-driven-development** | 在实现任何功能或修复 Bug 时，在编写实现代码之前使用。 |
| **testing-patterns** | 测试模式和原则。包括单元测试、集成测试和模拟（Mocking）策略。 |
| **theme-factory** | 使用主题美化产出物的工具包。可以应用于幻灯片、文档、报告、HTML 落地页等。提供 10 个预设主题，也可动态生成新主题。 |
| **typescript-expert** | TypeScript 专家。 |
| **ui-ux-pro-max** | UI/UX 设计智能。包括 50 种风格、21 个调色板、50 种字体配对、20 种图表和 9 个技术栈（React, Next.js, Vue, Svelte, SwiftUI, React Native, Flutter, Tailwind, shadcn/ui）。用于设计、构建、审查和优化网站、仪表板、SaaS 应用等各类 UI 元素。 |
| **using-git-worktrees** | 在开始需要与当前工作区隔离的功能开发或执行实施计划之前使用，创建具有智能目录选择和安全验证的隔离 Git 工作树。 |
| **using-superpowers** | 在开始任何对话时使用，确立如何查找和使用技能，要求在做出任何响应之前调用技能工具。 |
| **verification-before-completion** | 在声称工作已完成、已修复或已通过测试之前使用。要求运行验证命令并确认输出，始终以证据为先。 |
| **vr-ar** | VR/AR 开发原则。包括舒适度、交互设计和性能要求。 |
| **vulnerability-scanner** | 高级漏洞分析原则。涵盖 OWASP 2025、供应链安全、攻击面映射和风险优化优先级。 |
| **web-artifacts-builder** | 用于创建复杂的、多组件的 HTML 产出物的工具套件，使用现代前端技术（React, Tailwind CSS, shadcn/ui）。用于需要状态管理、路由或复杂组件的场景，而非简单的单文件产出物。 |
| **web-games** | 浏览器游戏开发原则。包括框架选择、WebGPU、优化和 PWA（渐进式 Web 应用）。 |
| **webapp-testing** | Web 应用程序测试原则。包括 E2E 测试、Playwright 框架和深度审计策略。 |
| **writing-plans** | 当你有明确的规范或多步任务需求，但在动手编写代码之前使用。 |
| **writing-skills** | 在创建新技能、编辑现有技能或在部署前验证技能是否正常工作时使用。 |
| **xlsx** | 全面的电子表格创建、编辑和分析工具，支持公式、格式化、数据分析和可视化。在需要处理电子表格（.xlsx, .csv 等）以进行数据读取、修改或重新计算公式时使用。 |
