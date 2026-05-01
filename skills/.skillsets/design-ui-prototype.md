# Skill Set
Slug: design-ui-prototype
Name: UI 原型设计
Scene: design
SubScene: ui-prototype
Summary: 从产品需求零提问直出PRD到设计方向、设计稿像素级还原，到设计令牌体系与组件规范制定、UI设计质量审查与反模式识别，再到低保真线框图绘制和高保真生产级HTML/Tailwind原型输出的完整UI原型设计工作流。支持Figma/Sketch设计稿解析、响应式布局、移动优先设计、设计系统构建和交互原型生成。

## Workflow
---
scene: "design"
sub_scene: "ui-prototype"
skills:
  - "prd-to-prototype"
  - "design-to-code"
  - "ui-design-system"
  - "ui-design"
  - "wireframe"
  - "frontend-design-ultimate"
---

# UI 原型设计工作流

你现在要完成一项从产品需求到高保真原型的 UI 原型设计任务。你已安装以下 Skill，请按步骤串联使用：

## 步骤 1：需求分析与 PRD 输出（获取层）
使用 **PRD to Prototype** 完成：
- 接收用户的产品想法或需求描述
- 零提问模式直接输出结构化 PRD（产品需求文档）
- 明确目标用户、核心功能、页面列表和交互要求
- 确认目标平台（移动端/PC端/双端）
- 生成设计方向建议和功能优先级排序

输出 PRD 文档和平台选择确认。

## 步骤 2：设计稿解析与像素还原（获取层）
使用 **Design To Code** 完成：
- 如有现成设计稿（Figma、Sketch 或截图），进行像素级解析
- 提取设计稿中的布局结构、间距、字体、颜色等参数
- 识别响应式断点和自适应规则
- 将设计稿转为结构化的设计规格说明

输出设计规格参数和布局结构描述。若无现成设计稿则跳过此步，由后续步骤从零设计。

## 步骤 3：设计系统与规范制定（分析层）
使用 **Ui Design System** 完成：
- 基于 PRD 和设计规格，构建设计令牌体系（颜色、字体、间距、圆角等）
- 定义组件库规范（按钮、输入框、卡片、导航等基础组件）
- 设计响应式布局策略和断点规则
- 生成设计令牌文档（CSS 变量 / Tailwind 配置）
- 建立组件命名和状态管理规范

输出设计令牌文件和组件规范文档。

## 步骤 4：UI 设计质量审查（分析层）
使用 **UI Design** 完成：
- 审查设计方案的布局合理性（视觉层级、信息密度）
- 检查排版系统（字号层级、行高、对齐）
- 验证色彩搭配（对比度、可访问性 WCAG 标准）
- 评估间距一致性和组件复用性
- 识别常见 UI 反模式并给出修正建议

输出设计质量审查报告和优化建议。

## 步骤 5：低保真线框图绘制（输出层）
使用 **Wireframe** 完成：
- 绘制核心页面的低保真线框图（ASCII 或 SVG 格式）
- 标注页面间的跳转关系和用户流程
- 定义每个页面的功能区块和内容占位
- 导出线框图用于团队评审

输出线框图文件和用户流程图。

## 步骤 6：高保真原型生成（输出层）
使用 **Frontend Design Ultimate** 完成：
- 基于线框图和设计系统，生成高保真 HTML/Tailwind 原型
- 实现移动优先的响应式布局
- 添加微交互动效和过渡效果
- 确保视觉效果达到生产级水准（Awwwards 级别）
- 输出单文件可预览的 HTML 原型

## 最终输出
将以上步骤的结果整合为完整的 UI 原型设计包，交付以下文件：
1. **PRD 文档**：产品需求、功能列表、目标平台
2. **设计令牌与组件规范**：颜色/字体/间距令牌、组件库定义
3. **设计审查报告**：布局/排版/色彩/可访问性检查结果
4. **线框图**：核心页面低保真布局和用户流程
5. **高保真 HTML 原型**：可直接在浏览器中预览的交互原型

## Skills
- prd-to-prototype: PRD to Prototype - 从产品需求到可交互原型的完整工作流。当用户表达产品想法（如'我想做一个...'、'帮我设计...'）时触发。支持：1) 零提问直出PRD 2) 平台选择确认 3) 生成Awwwards级别的高保真HTML/Tailwind原型（移动端/PC端）。端到端产品设计流程。
- design-to-code: Design To Code - Implements UI from design mockups (Figma, Sketch, or image) with pixel-accurate layout, responsive behavior, and design tokens. Use when 还原设计图, implementing...
- ui-design-system: Ui Design System - UI design system toolkit for Senior UI Designer including design token generation, component documentation, responsive design calculations, and developer han...
- ui-design: UI Design - Comprehensive UI design skill covering fundamentals, patterns, and anti-patterns. Layout, typography, color, spacing, accessibility, motion, and component design. Use when building any web interface, reviewing design quality, or creating distinctive UIs.
- wireframe: Wireframe - Create wireframes and user flows. Use when sketching page layouts in ASCII/SVG, mapping flows, or exporting to HTML.
- frontend-design-ultimate: Frontend Design Ultimate - Create distinctive, production-grade static sites with React, Tailwind CSS, and shadcn/ui — no mockups needed. Generates bold, memorable designs from plain text requirements with anti-AI-slop aesthetics, mobile-first responsive patterns, and single-file bundling. Use when building landing pages, marketing sites, portfolios, dashboards, or any static web UI. Supports both Vite (pure static) and Next.js (Vercel deploy) workflows.
