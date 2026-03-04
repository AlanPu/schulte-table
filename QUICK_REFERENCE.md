# 📋 项目快速参考

## 一行命令运行

```bash
# 桌面版（最快测试）
./gradlew :composeApp:run

# Android 构建
./gradlew :composeApp:assembleDebug

# 清理构建
./gradlew clean build
```

## 核心文件

| 文件 | 说明 | 行数 |
|------|------|------|
| [`App.kt`](composeApp/src/commonMain/kotlin/com/schultetable/app/App.kt) | 核心游戏逻辑和 UI | ~350 行 |
| [`MainActivity.kt`](composeApp/src/androidMain/kotlin/com/schultetable/app/MainActivity.kt) | Android 入口 | ~15 行 |
| [`main.kt`](composeApp/src/desktopMain/kotlin/main.kt) | 桌面入口 | ~15 行 |
| [`MainViewController.kt`](composeApp/src/iosMain/kotlin/com/schultetable/app/MainViewController.kt) | iOS 入口 | ~8 行 |
| [`build.gradle.kts`](composeApp/build.gradle.kts) | 构建配置 | ~90 行 |

## 功能矩阵

| 功能 | 状态 | 说明 |
|------|------|------|
| 4×4 网格 | ✅ | 16 个格子，适合入门 |
| 5×5 网格 | ✅ | 25 个格子，标准模式 |
| 6×6 网格 | ✅ | 36 个格子，高难度 |
| 数字模式 | ✅ | 1-36 随机排列 |
| 字母模式 | ✅ | A-Z 随机排列 |
| 汉字模式 | ✅ | 中文数字显示 |
| 颜色模式 | ✅ | 彩色方块识别 |
| 计时器 | ✅ | 毫秒级精度 |
| 设置界面 | ✅ | 难度和模式选择 |
| Android 支持 | ✅ | API 24+ |
| iOS 支持 | ✅ | iOS 15+ |
| macOS 支持 | ✅ | macOS 10.15+ |

## 代码结构

```
App.kt (核心)
├── App()                    # 应用入口
├── MainScreen()             # 主屏幕
│   ├── GameView()           # 游戏视图
│   │   ├── TimerDisplay()   # 计时器
│   │   ├── CellView()       # 格子组件
│   │   └── SettingsDialog() # 设置对话框
│   └── 状态管理
├── GameMode                 # 游戏模式枚举
├── getContent()             # 内容生成
└── colorForNumber()         - 颜色生成
```

## 关键代码片段

### 游戏开始
```kotlin
fun startNewGame() {
    numbers = (1..gridSize * gridSize).shuffled()
    currentNumber = 1
    elapsedTime = 0f
    gameOver = false
    isRunning = true
}
```

### 处理点击
```kotlin
fun handleTap(number: Int) {
    if (!isRunning || gameOver) return
    
    if (number == currentNumber) {
        currentNumber++
        if (currentNumber > gridSize * gridSize) {
            gameOver = true
            isRunning = false
        }
    }
}
```

### 生成内容
```kotlin
fun getContent(number: Int, gameMode: GameMode): String {
    return when (gameMode) {
        GameMode.Numbers -> number.toString()
        GameMode.Letters -> ((number - 1 + 'A'.code).toChar()).toString()
        GameMode.Chinese -> chineseNumbers[number - 1]
        GameMode.Colors -> ""
    }
}
```

## 依赖版本

| 依赖 | 版本 |
|------|------|
| Kotlin | 2.0.0 |
| Compose Multiplatform | 1.6.2 |
| Android Gradle Plugin | 8.2.2 |
| Compose Compiler | 1.5.8 |
| AndroidX Activity | 1.8.2 |

## 平台配置

### Android
- `minSdk`: 24
- `targetSdk`: 34
- `applicationId`: com.schultetable.app

### iOS
- 最低版本：iOS 15
- Bundle ID: com.schultetable.app

### macOS
- 最低版本：macOS 10.15
- Bundle ID: com.schultetable.app

## 文档导航

- 📖 [README.md](README.md) - 项目介绍
- 🚀 [QUICKSTART.md](QUICKSTART.md) - 快速开始
- 🏗️ [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) - 项目结构
- 💻 [DEVELOPMENT.md](DEVELOPMENT.md) - 开发指南
- ✅ [COMPLETION.md](COMPLETION.md) - 完成总结

## 常用 Gradle 命令

```bash
# 运行
./gradlew :composeApp:run

# 构建
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:assembleRelease

# 测试
./gradlew :composeApp:test

# 清理
./gradlew clean

# 依赖
./gradlew :composeApp:dependencies

# 桌面打包
./gradlew :composeApp:packageDmg
./gradlew :composeApp:packageMsi
./gradlew :composeApp:packageDeb
```

## 调试技巧

### 查看日志
```bash
# Android
adb logcat -s SchulteTable

# 桌面
# 直接在终端查看输出

# iOS
# Xcode 控制台
```

### 热重载
```bash
# 使用 Live Edit (Android Studio)
# 或重新运行任务
./gradlew :composeApp:run --continuous
```

## 性能指标

| 指标 | 目标值 |
|------|--------|
| 启动时间 | < 2 秒 |
| 帧率 | 60 FPS |
| 内存占用 | < 100 MB |
| APK 大小 | < 20 MB |

## 联系与支持

- 📧 问题反馈：创建 Issue
- 💡 功能建议：创建 Issue
- 🤝 贡献代码：提交 PR

---

**快速开始**: `./gradlew :composeApp:run` 🚀
