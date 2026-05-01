# Skill Set
Slug: tech-bug-troubleshooting
Name: Bug 排查
Scene: tech
SubScene: bug-troubleshooting
Summary: 从多格式日志解析与错误模式分析到七步调试法、运行时执行追踪、四阶段系统化根因分析，再到零回归修复工作流和快速错误解释的完整Bug排查工作流。支持Python、Node.js、Java等多语言运行时调试，覆盖堆栈跟踪解析、错误模式识别、假设检验和修复验证。

## Workflow
---
scene: "tech"
sub_scene: "bug-troubleshooting"
skills:
  - "log-analyzer"
  - "debug-pro"
  - "runtime-debugging-skill"
  - "superpowers-systematic-debugging"
  - "bug-fixing"
  - "nexus-error-explain"
---

# Bug 排查工作流

你现在要完成一项软件 Bug 的定位与修复任务。你已安装以下 Skill，请按步骤串联使用：

## 步骤 1：日志收集与错误模式分析（获取层）
使用 **Log Analyzer** 完成：
- 解析应用程序日志文件（支持多种格式）
- 搜索错误信息、异常堆栈和告警记录
- 分析错误发生的时间线和频率模式
- 跨服务关联事件，追踪请求链路
- 解析堆栈跟踪（Stack Trace），定位异常抛出位置

输出日志分析摘要和关键错误事件时间线。

## 步骤 2：系统化调试（分析层）
使用 **Debug Pro** 完成：
- 执行七步调试协议：环境确认 → 问题复现 → 最小化复现 → 假设生成 → 假设验证 → 修复 → 回归验证
- 使用语言特定调试命令（Python pdb、Node.js inspector、Java jdb 等）
- 跨多环境（开发/测试/生产）系统化排查
- 记录每步调试证据和中间结果

输出调试过程记录和初步定位结果。

## 步骤 3：运行时执行追踪（分析层）
使用 **Runtime Debugging Skill** 完成：
- 对 Python、Node.js 或 Java 应用进行运行时执行追踪
- 动态插入断点和日志输出，观察变量状态变化
- 追踪函数调用链，定位异常传播路径
- 分析运行时内存、线程和I/O状态

输出运行时诊断数据和异常传播路径。

## 步骤 4：四阶段根因分析（分析层）
使用 **Superpowers Systematic Debugging** 完成：
- **根因调查**：收集所有相关证据（日志、状态、配置）
- **模式分析**：识别错误模式，对比历史类似问题
- **假设检验**：生成并逐一验证可能的根因假设
- **修复验证**：基于证据确认根因，验证修复方案有效性

输出根因分析报告和验证结论。

## 步骤 5：零回归修复工作流（输出层）
使用 **bug-fixing** 完成：
- **分诊**：评估 Bug 严重等级和影响范围
- **复现**：确认稳定复现路径
- **影响分析**：评估修复的波及面
- **修复**：实施修复方案
- **验证**：执行回归测试确认无新问题引入
- **知识沉淀**：记录 Bug 根因和修复方案到知识库

输出修复方案和回归验证结果。

## 步骤 6：快速错误解释（输出层）
使用 **NEXUS Error Explain** 完成：
- 粘贴任意错误信息、堆栈跟踪或异常
- 即时获得简明的根因解释
- 输出可操作的修复建议和代码示例

## 最终输出
将以上步骤的结果整合为完整的 Bug 排查包，交付以下文件：
1. **Bug 排查报告**：问题描述、复现步骤、根因分析、修复方案
2. **调试过程记录**：七步调试协议执行记录和证据链
3. **修复验证报告**：修复前后对比、回归测试结果
4. **知识沉淀文档**：Bug 根因模式和防范建议

## Skills
- log-analyzer: Log Analyzer - Parse, search, and analyze application logs across formats. Use when debugging from log files, setting up structured logging, analyzing error patterns, correlating events across services, parsing stack traces, or monitoring log output in real time.
- debug-pro: Debug Pro - Provides a 7-step debugging protocol plus language-specific commands to systematically identify, verify, and fix software bugs across multiple environments.
- runtime-debugging-skill: Runtime Debugging Skill - Diagnose and fix bugs using runtime execution traces. Use when debugging errors, analyzing failures, or finding root causes in Python, Node.js, or Java appli...
- superpowers-systematic-debugging: Superpowers Systematic Debugging - Enforce a four-phase debugging process—root cause investigation, pattern analysis, hypothesis testing, and evidence-based fix verification—for all bugs and f...
- bug-fixing: bug-fixing - Zero-regression bug fix workflow: triage → reproduce → root cause → impact analysis → fix → verify → knowledge deposit → self-reflect. Use when: - Feature br...
- nexus-error-explain: NEXUS Error Explain - Paste any error message, stack trace, or exception and get a plain-English explanation with root cause analysis and fix suggestions.
