# 创意工坊：隐写术解码器 (Secret Signal Decoder) 开发计划

## 1. 项目概述
基于开源项目 [SS_tools](https://github.com/copyangle/SS_tools) (Super-Secure Media Protection)，在 RunningHub Android 端实现**图片隐写解码**功能。该功能允许用户上传一张看似普通的“鸭子图”（或其他载体图），在本地提取出隐藏在像素低位（LSB）中的原始媒体文件（视频、图片等）。

## 2. 核心价值
- **隐私保护**：在公开渠道传播“伪装图”，仅持有工具和密码的人可查阅真身。
- **趣味性**：作为“创意工坊”的招牌功能，增加 App 的极客属性。
- **离线安全**：所有解码过程均在 Android 本地完成，不上传服务器，保障隐私。

## 3. 技术方案

### 3.1 核心算法移植 (Python -> Kotlin)
原始算法基于 `LSB (Least Significant Bit)` 隐写术。我们需要在 Android 端使用 `Bitmap` 像素操作复刻以下逻辑：
- **像素遍历**：读取图片像素的 RGBA 通道。
- **位平面提取**：根据预设的位深（通常为 1-bit 或 2-bit LSB），提取隐藏的二进制流。
- **Header 解析**：识别隐写数据的头部信息（Magic Number、载荷长度、文件类型）。
- **解密 (可选)**：如果原图加密，需实现 `SHA-256` + `Salt` + `XOR` 流解密逻辑。

### 3.2 架构设计
- **输入层**：既支持从系统相册选图，也支持接收系统“分享”意图（直接从微信/QQ分享图片到 App 解码）。
- **处理层 (ViewModel + Repository)**：
    - `SteganographyRepository`: 封装 Bitmap LSB 操作，建议使用 `Coroutines` (`Dispatchers.Default`) 在后台线程处理，避免阻塞 UI。
    - **内存优化**：考虑到大图隐写可能导致 OOM，需采用流式处理或分块处理 Bitmap。
- **UI 层 (Jetpack Compose)**：
    - 全新的 `SecretDecodeScreen`。
    - **状态**：`Idle` (待上传) -> `Decoding` (解码中，显示进度) -> `Result` (预览/保存)。

## 4. 实施步骤

### Phase 1: 核心验证 (PoC)
1. 编写 Kotlin 版 `LSBDecoder` 工具类。
2. 验证：尝试解码官方提供的示例“鸭子图”，确保能提取出二进制数据。

### Phase 2: UI 开发
1. 在 `CommunityScreen` 增加“隐写解码”入口。
2. 开发解码详情页：大图预览、密码输入框（如有）、解码按钮、结果展示。
3. 实现视频/图片文件的本地带后缀保存。

### Phase 3: 优化
1. **性能**：优化大图解码速度（RenderScript 或 NDK 可选，首选 Kotlin Native 位运算）。
2. **体验**：添加震动反馈和酷炫的解码动画。

## 5. 决策结论 (Decisions)
- **兼容性**：**全量支持**。工具需支持自动尝试或让用户选择 2/6/8 bit 模式。
- **加密策略**：**用户输入**。界面提供密码输入框，用于解密。
- **输出处理**：**显式操作 (Option C)**。解码成功后跳转至结果页，提供播放/预览，并显示"保存到相册"按钮。
