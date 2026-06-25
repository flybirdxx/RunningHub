# 权限管理系统设计方案

> 文档版本：v1.0
> 创建日期：2026-04-28
> 状态：已确认，待实现

---

## 一、问题背景

### 1.1 当前问题

运行时出现 NPE（空指针异常）：

```
deliverResultsIfNeeded java.lang.NullPointerException:
  Attempt to invoke virtual method 'java.lang.String android.os.Bundle.getString(java.lang.String)'
  on a null object reference
```

**根因分析**：
1. `permissionLauncher` 和 `mediaPickerLauncher` 同时 launch，两个 Activity Result API 回调并行触发
2. Composable 重建（recomposition）导致 launcher 引用不稳定
3. 系统框架在分发结果时，callback 对象已为 null
4. 混合式权限管理（DataStore + Activity lifecycle awareness 缺失），状态判断逻辑分散

**影响范围**：AppDetailScreen 的媒体上传功能（图片/视频选择）、QuickCreateScreen 的素材选择

### 1.2 设计目标

1. **修复 NPE**：消除 launcher 并行 launch 导致的 null callback 问题
2. **统一管理**：所有运行时权限集中管理，避免散落各处
3. **优雅降级**：支持权限被拒绝后的引导流程（再次请求 → 永久拒绝 → 跳转设置）
4. **跨平台抽象**：KMP 架构下，shared 层定义接口，平台层各自实现
5. **数据持久化**：权限状态跨进程持久化，避免每次冷启动重新请求

---

## 二、架构设计

### 2.1 分层架构

```
┌─────────────────────────────────────────────┐
│          composeApp (UI Layer)              │
│  PermissionBottomSheet / Feature Screens   │
├─────────────────────────────────────────────┤
│     composeApp (Platform — Android)        │
│  PermissionControllerImpl (Activity 级别)   │
├─────────────────────────────────────────────┤
│              shared (Domain + Data)         │
│  Permission Model / DataStore Interface      │
├─────────────────────────────────────────────┤
│            Android Framework                │
│  Activity Result API / Activity Lifecycle   │
└─────────────────────────────────────────────┘
```

### 2.2 核心组件

| 组件 | 层级 | 职责 |
|------|------|------|
| `Permission` | shared/domain | 权限定义（sealed class） |
| `PermissionStatus` | shared/domain | 权限状态枚举 |
| `PermissionDataStore` | shared/data | 权限状态持久化接口 |
| `PermissionController` | composeApp/platform | 平台实现：权限请求 + 媒体选择 |
| `PermissionBottomSheet` | composeApp/ui | 引导弹窗 UI |

### 2.3 数据流

```
用户触发操作
     │
     ▼
Feature Screen ──发出 intent 事件──▶ ViewModel
                                            │
                              查询 DataStore 权限状态
                                            │
                 ┌─────────────────────────┼─────────────────────────┐
                 │                         │                         │
         GRANTED                  DENIED / UNKNOWN         PERMANENTLY_DENIED
                 │                         │                         │
         直接执行操作            弹出引导弹窗                弹出引导弹窗
                                  │                              │
                            用户授权 ──▶ Request Permission ──▶ 跳转设置
                                  │
                            拒绝 ──▶ 记录 DENIED 状态
```

---

## 三、核心模型

### 3.1 Permission sealed class

```kotlin
// shared/src/commonMain/kotlin/.../domain/model/Permission.kt
sealed class Permission(
    val androidManifest: String,
    val description: String,         // 用户可见的说明文案
    val requiredFor: String,          // "上传图片" / "扫码" 等
    val icon: String                  // Material icon name
) {
    // 媒体类
    data object MediaImages : Permission(
        androidManifest = "android.permission.READ_MEDIA_IMAGES",
        description = "需要读取您的图片，以便上传应用封面和素材",
        requiredFor = "上传图片",
        icon = "image"
    )
    data object MediaVideo : Permission(
        androidManifest = "android.permission.READ_MEDIA_VIDEO",
        description = "需要读取您的视频，以便上传演示内容",
        requiredFor = "上传视频",
        icon = "videocam"
    )
    data object MediaAudio : Permission(
        androidManifest = "android.permission.READ_MEDIA_AUDIO",
        description = "需要读取您的音频文件，以便添加背景音乐",
        requiredFor = "添加音频",
        icon = "music_note"
    )
    // 媒体降级（< Android 13）
    data object StorageRead : Permission(
        androidManifest = "android.permission.READ_EXTERNAL_STORAGE",
        description = "需要访问存储，以便读取媒体文件",
        requiredFor = "访问媒体",
        icon = "folder"
    )
    data object StorageWrite : Permission(
        androidManifest = "android.permission.WRITE_EXTERNAL_STORAGE",
        description = "需要写入存储，以便保存生成内容",
        requiredFor = "保存文件",
        icon = "save"
    )

    // 设备类
    data object Camera : Permission(
        androidManifest = "android.permission.CAMERA",
        description = "需要使用相机，以便拍照或扫描二维码",
        requiredFor = "拍照/扫码",
        icon = "camera_alt"
    )

    // 系统类
    data object Notifications : Permission(
        androidManifest = "android.permission.POST_NOTIFICATIONS",
        description = "需要发送通知，以便告知您任务完成状态",
        requiredFor = "任务通知",
        icon = "notifications"
    )

    companion object {
        fun fromManifest(manifest: String): Permission? =
            entries.find { it.androidManifest == manifest }
    }
}
```

### 3.2 PermissionStatus 枚举

```kotlin
enum class PermissionStatus {
    GRANTED,              // 已授予
    DENIED,               // 被拒绝（可再次请求）
    PERMANENTLY_DENIED,   // 永久拒绝（需跳转设置）
    UNKNOWN               // 尚未询问
}
```

---

## 四、持久化层

### 4.1 DataStore 接口

```kotlin
// shared/src/commonMain/kotlin/.../data/local/PermissionDataStore.kt
interface PermissionDataStore {
    val grantedPermissions: Flow<Set<String>>
    val deniedPermissions: Flow<Set<String>>
    val permanentlyDeniedPermissions: Flow<Set<String>>

    suspend fun markGranted(manifest: String)
    suspend fun markDenied(manifest: String)
    suspend fun markPermanentlyDenied(manifest: String)
    suspend fun reset(manifest: String)
    suspend fun resetAll()

    // 便捷方法：查询单个权限状态
    suspend fun getCurrentStatus(permission: Permission): PermissionStatus
}
```

### 4.2 存储结构

| DataStore Key | 类型 | 含义 |
|--------------|------|------|
| `permission_granted` | `Set<String>` | 已授予的权限 manifest 名 |
| `permission_denied` | `Set<String>` | 普通拒绝的权限 |
| `permission_permanently_denied` | `Set<String>` | 永久拒绝的权限 |

每条权限只出现在一个集合中。状态变更时需要做迁移：例如从 `denied` 变为 `granted` 时，从 `denied` 集合移除并加入 `granted` 集合。

---

## 五、Android 平台实现

### 5.1 NPE 修复关键点

**问题根源**：两个 launcher 并行 launch + Composable 重建导致引用失效

**修复策略**：

1. **Activity 级别注册 launcher**：使用 `activity.activityResultRegistry.register()` 而非 Composable 内的 `rememberLauncherForActivityResult`。Activity 存活期内只注册一次，不受 recomposition 影响。

2. **单一 pending 状态**：用 `pendingPermission` + `pendingCallback` 管理当前等待中的请求，避免多个并行请求状态混乱。

3. **严格串行化媒体选择**：先等权限结果回调，再决定是否启动媒体选择器。`pickMedia()` 方法内部自动完成这个流程。

### 5.2 PermissionController 实现

```kotlin
// composeApp/src/androidMain/kotlin/.../platform/PermissionController.kt
@OptIn(ExperimentalLifecycleApi::class)
class PermissionController(
    private val lifecycleOwner: LifecycleOwner,
    private val activity: ComponentActivity,
    private val dataStore: PermissionDataStore,
) {
    private var pendingPermission: Permission? = null
    private var pendingCallback: ((Boolean) -> Unit)? = null
    private var pendingMediaCallback: ((Uri) -> Unit)? = null

    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.activityResultRegistry.register(
            "permission_request",
            RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionResult(permissions)
        }

    private val mediaPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest> =
        activity.activityResultRegistry.register(
            "media_picker",
            PickVisualMedia()
        ) { uri ->
            handleMediaResult(uri)
        }

    fun pickMedia(
        mediaPermission: Permission,
        mediaType: MediaType,
        onSuccess: (Uri) -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        lifecycleOwner.lifecycleScope.launch {
            when (dataStore.getCurrentStatus(mediaPermission)) {
                PermissionStatus.GRANTED -> {
                    // 权限已有，直接选文件
                    launchMediaPicker(mediaType, onSuccess)
                }
                PermissionStatus.PERMANENTLY_DENIED -> {
                    onPermissionDenied()
                }
                else -> {
                    // 请求权限，等待回调后再选文件
                    pendingMediaCallback = onSuccess
                    permissionLauncher.launch(arrayOf(mediaPermission.androidManifest))
                }
            }
        }
    }

    private var pendingMediaType: MediaType? = null

    private fun launchMediaPicker(type: MediaType, onSuccess: (Uri) -> Unit) {
        val request = when (type) {
            MediaType.IMAGE -> PickVisualMediaRequest(ImageOnly)
            MediaType.VIDEO -> PickVisualMediaRequest(VideoOnly)
            MediaType.AUDIO -> return  // 走 GetContent
        }
        pendingMediaCallback = onSuccess
        pendingMediaType = type
        mediaPickerLauncher.launch(request)
    }

    private fun handlePermissionResult(permissions: Map<String, Boolean>) {
        val manifest = permissions.keys.firstOrNull() ?: return
        val granted = permissions[manifest] == true

        lifecycleOwner.lifecycleScope.launch {
            if (granted) {
                dataStore.markGranted(manifest)
                // 权限授予后，如果之前有 pending 媒体选择，立即启动
                pendingMediaType?.let { type ->
                    launchMediaPicker(type) { uri ->
                        pendingMediaCallback?.invoke(uri)
                        pendingMediaCallback = null
                    }
                }
            } else {
                val shouldShowRationale = activity.shouldShowRequestPermissionRationale(manifest)
                if (!shouldShowRationale) {
                    dataStore.markPermanentlyDenied(manifest)
                } else {
                    dataStore.markDenied(manifest)
                }
            }
        }
    }

    private fun handleMediaResult(uri: Uri?) {
        uri?.let { pendingMediaCallback?.invoke(it) }
        pendingMediaCallback = null
        pendingMediaType = null
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }
}
```

### 5.3 DI 集成

通过 Koin 将 `PermissionController` 注入为 `single` 或 `factory`。Factory 模式每次注入创建新实例绑定到当前 screen 的 lifecycle；single 模式则在 Activity 级别复用。

---

## 六、Compose UI 组件

### 6.1 PermissionBottomSheet

```kotlin
@Composable
fun PermissionBottomSheet(
    permission: Permission,
    onDismiss: () -> Unit,
    onAuthorize: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = getIconForPermission(permission.icon),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = permission.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAuthorize, modifier = Modifier.fillMaxWidth()) {
                Text("授权")
            }
            if (onOpenSettings != null) {
                TextButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("权限已被拒绝，前往设置开启")
                }
            } else {
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("暂不需要")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun getIconForPermission(icon: String): ImageVector = when (icon) {
    "camera_alt" -> Icons.Default.CameraAlt
    "image" -> Icons.Default.Image
    "videocam" -> Icons.Default.VideoCameraBack
    "music_note" -> Icons.Default.MusicNote
    "notifications" -> Icons.Default.Notifications
    else -> Icons.Default.Lock
}
```

### 6.2 Feature Screen 集成模式

Screen 层**不再直接持有** `ActivityResultLauncher`，而是：

1. 通过 ViewModel 发出权限事件（`PermissionRequired`、`MediaUploadRequested`）
2. Screen 层订阅这些事件，弹出 `PermissionBottomSheet` 引导用户
3. 授权结果通过 ViewModel 的回调处理
4. `PermissionController` 在 Screen 级别通过 Koin 注入

```kotlin
@Composable
fun AppDetailScreen(viewModel: AppDetailViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    var pendingPermission by remember { mutableStateOf<Permission?>(null) }

    LaunchedEffect(Unit) {
        viewModel.permissionEvents.collect { event ->
            when (event) {
                is PermissionRequired -> pendingPermission = event.permission
                is MediaUploadRequested -> {
                    // 通知 ViewModel 媒体已选择
                    viewModel.onMediaSelected(event.uri)
                }
            }
        }
    }

    if (pendingPermission != null) {
        PermissionBottomSheet(
            permission = pendingPermission!!,
            onDismiss = { pendingPermission = null },
            onAuthorize = {
                val perm = pendingPermission!!
                pendingPermission = null
                permissionController.checkAndRequest(/* ... */)
            },
        )
    }

    // UI 纯展示
    MediaUploadButton(onClick = { viewModel.requestMediaUpload() })
}
```

---

## 七、文件清单

### 新增文件

| 文件路径 | 说明 |
|---------|------|
| `shared/src/commonMain/.../domain/model/Permission.kt` | 权限 sealed class |
| `shared/src/commonMain/.../domain/model/PermissionStatus.kt` | 权限状态枚举 |
| `shared/src/commonMain/.../data/local/PermissionDataStore.kt` | DataStore 接口 |
| `shared/src/androidMain/.../data/local/PermissionDataStoreImpl.kt` | Android DataStore 实现 |
| `shared/src/iosMain/.../data/local/PermissionDataStoreImpl.kt` | iOS DataStore 实现 |
| `composeApp/src/androidMain/.../platform/PermissionController.kt` | Android 平台实现 |
| `composeApp/src/commonMain/.../ui/components/PermissionBottomSheet.kt` | 引导弹窗 UI |

### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `composeApp/src/commonMain/.../di/` | 添加 PermissionController Koin 模块 |
| `composeApp/src/.../AppDetailScreen.kt` | 接入新权限系统，移除旧 launcher |
| `composeApp/src/.../QuickCreateScreen.kt` | 同上 |

---

## 八、验收标准

1. [ ] NPE 不再出现，多次旋转屏幕、频繁 recomposition 均正常
2. [ ] 权限拒绝后重新打开 App，状态保留（不需要重新询问）
3. [ ] 永久拒绝时，用户点击"不再询问"，弹窗显示"前往设置"入口
4. [ ] 媒体选择前一定会先检查权限，不会并行 launch 两个 launcher
5. [ ] 权限系统可在其他 screen 复用（通知权限、相机权限等）
6. [ ] 单元测试覆盖 PermissionController 的各状态分支
