# 项目结构说明

## 目录结构

```
SchulteTable/
├── 📁 composeApp/                      # 主要应用模块
│   ├── 📁 src/
│   │   ├── 📁 commonMain/              # 共享代码（所有平台通用）
│   │   │   └── 📁 kotlin/com/schultetable/app/
│   │   │       └── App.kt              # 核心游戏逻辑和 UI
│   │   │
│   │   ├── 📁 androidMain/             # Android 平台特定代码
│   │   │   ├── 📁 kotlin/.../
│   │   │   │   └── MainActivity.kt     # Android 主 Activity
│   │   │   ├── 📁 res/
│   │   │   │   └── 📁 values/
│   │   │   │       ├── colors.xml      # 颜色资源
│   │   │   │       ├── strings.xml     # 字符串资源
│   │   │   │       └── themes.xml      # 主题样式
│   │   │   └── AndroidManifest.xml     # Android 清单文件
│   │   │
│   │   ├── 📁 desktopMain/             # 桌面端特定代码
│   │   │   └── 📁 kotlin/
│   │   │       └── main.kt             # 桌面应用入口
│   │   │
│   │   └── 📁 iosMain/                 # iOS 平台特定代码
│   │       └── 📁 kotlin/.../
│   │           └── MainViewController.kt  # iOS 视图控制器
│   │
│   └── build.gradle.kts                # 模块构建配置
│
├── 📁 iosApp/                          # iOS 应用包装器
│   └── 📁 iosApp/
│       ├── Info.plist                  # iOS 配置文件
│       └── iOSApp.swift                # iOS 应用入口
│
├── 📁 gradle/
│   ├── 📁 wrapper/
│   │   ├── gradle-wrapper.jar          # Gradle Wrapper JAR
│   │   └── gradle-wrapper.properties   # Gradle Wrapper 配置
│   └── libs.versions.toml              # 版本目录（依赖版本管理）
│
├── 📄 .gitignore                       # Git 忽略文件
├── 📄 build.gradle.kts                 # 根项目构建配置
├── 📄 gradle.properties                # Gradle 属性配置
├── 📄 gradlew                          # Gradle Wrapper 脚本 (Unix)
├── 📄 gradlew.bat                      # Gradle Wrapper 脚本 (Windows)
├── 📄 settings.gradle.kts              # 项目设置
├── 📄 README.md                        # 项目说明文档
├── 📄 QUICKSTART.md                    # 快速开始指南
└── 📄 setup-gradle.sh                  # Gradle 设置脚本
```

## 核心文件说明

### 1. 共享代码 (commonMain)

**App.kt** - 包含所有核心功能：
- `App()` - 应用根组件
- `MainScreen()` - 主界面
- `GameView()` - 游戏视图（网格、计时器、游戏逻辑）
- `CellView()` - 单个格子组件
- `TimerDisplay()` - 计时器显示
- `SettingsDialog()` - 设置对话框
- `GameMode` - 游戏模式枚举
- 辅助函数：`getContent()`, `colorForNumber()`

### 2. Android 平台 (androidMain)

**MainActivity.kt** - Android 入口点：
- 继承自 `ComponentActivity`
- 使用 `setContent { App() }` 加载 Compose UI

**AndroidManifest.xml** - Android 配置：
- 应用权限
- Activity 声明
- 启动器配置

### 3. 桌面平台 (desktopMain)

**main.kt** - 桌面应用入口：
- 创建窗口
- 设置窗口大小和标题
- 加载 Compose UI

### 4. iOS 平台 (iosMain)

**MainViewController.kt** - iOS 视图控制器：
- 使用 `ComposeUIViewController` 包装 Compose UI

## 构建系统

### Gradle 配置层次

1. **settings.gradle.kts** (根目录)
   - 项目命名
   - 包含模块
   - 依赖仓库配置

2. **build.gradle.kts** (根目录)
   - 插件声明
   - 全局配置

3. **composeApp/build.gradle.kts**
   - Kotlin Multiplatform 配置
   - 目标平台（Android、iOS、Desktop）
   - 依赖管理
   - Compose 配置

4. **gradle/libs.versions.toml**
   - 版本目录
   - 依赖定义
   - 插件版本

## 代码架构

### 游戏逻辑流程

```
用户启动应用
    ↓
MainScreen 显示
    ↓
用户点击"开始游戏"
    ↓
GameView.startNewGame()
    ├─ 生成随机数字数组
    ├─ 重置当前数字为 1
    ├─ 启动计时器
    └─ 开始游戏循环
    ↓
用户点击格子
    ↓
handleTap() 检查
    ├─ 正确 → currentNumber++
    │         └─ 检查是否完成
    └─ 错误 → 无操作
    ↓
完成所有数字
    ↓
显示完成时间和"重新开始"按钮
```

### 状态管理

使用 Jetpack Compose 的状态管理：
- `mutableStateOf()` - 可观察状态
- `LaunchedEffect()` - 副作用处理
- `remember()` - 状态保持

### UI 组件层次

```
App
└── MainScreen
    ├── Scaffold
    │   ├── TopAppBar
    │   └── Column
    │       ├── TimerDisplay
    │       ├── 当前数字提示
    │       ├── LazyVerticalGrid (游戏网格)
    │       │   └── CellView (多个)
    │       └── 游戏状态按钮
    └── SettingsDialog (弹出)
```

## 多平台支持

### Android
- 最低 API: 24 (Android 7.0)
- 目标 API: 34 (Android 14)
- 使用 ActivityCompose

### iOS
- 最低版本：iOS 15
- 支持设备：iPhone、iPad
- 使用 ComposeUIViewController

### macOS
- 最低版本：macOS 10.15
- 使用 Compose Desktop
- 支持 DMG、MSI、DEB 打包格式

## 扩展开发

### 添加新功能

1. 在 `commonMain/App.kt` 中添加功能
2. 如需平台特定功能，在各平台的 `main` 目录添加
3. 更新相关配置文件

### 添加新平台

1. 在 `composeApp/build.gradle.kts` 添加目标平台
2. 创建对应的 `src/{platform}Main` 目录
3. 实现平台入口代码

### 自定义主题

修改 `App.kt` 中的 `MaterialTheme`：
- 颜色方案
- 字体
- 形状

## 测试

建议添加测试目录：
```
src/
├── commonTest/     # 共享测试
├── androidTest/    # Android 测试
└── desktopTest/    # 桌面测试
```

## 发布

### Android
```bash
./gradlew :composeApp:assembleRelease
```

### iOS
使用 Xcode Archive

### macOS
```bash
./gradlew :composeApp:packageDmg
```
