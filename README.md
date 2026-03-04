# Schulte Table - 舒尔特表

一个使用 Jetpack Compose Multiplatform 开发的跨平台舒尔特表应用，支持 Android、iOS、iPad 和 macOS。

## 功能特点

### 难度等级
- **基础款**: 5×5 网格，随机排列 1-25
- **入门款**: 4×4 网格，随机排列 1-16
- **进阶段**: 6×6 网格，随机排列 1-36

### 游戏模式
- **数字模式**: 经典的数字舒尔特表
- **字母模式**: 使用英文字母 A-Z
- **汉字模式**: 使用中文数字（一、二、三...）
- **颜色模式**: 使用不同颜色方块

### 功能
- ⏱️ 精确计时器（毫秒级）
- 🎯 自动检测点击正确性
- 🎨 美观的 Material Design 3 界面
- 📱 响应式设计，适配各种屏幕尺寸
- 🔄 一键重新开始游戏

## 技术栈

- **框架**: Jetpack Compose Multiplatform
- **语言**: Kotlin
- **构建工具**: Gradle
- **目标平台**:
  - Android (API 24+)
  - iOS (iOS 15+)
  - iPadOS (iOS 15+)
  - macOS (macOS 10.15+)

## 项目结构

```
SchulteTable/
├── composeApp/                    # 共享模块
│   ├── src/
│   │   ├── commonMain/           # 共享代码（所有平台）
│   │   │   └── kotlin/
│   │   │       └── com/schultetable/app/
│   │   │           └── App.kt    # 主应用逻辑
│   │   ├── androidMain/          # Android 特定代码
│   │   ├── desktopMain/          # 桌面端特定代码
│   │   └── iosMain/              # iOS 特定代码
│   └── build.gradle.kts
├── iosApp/                        # iOS 应用包装器
│   └── iosApp/
└── build.gradle.kts               # 根构建配置
```

## 开发环境要求

- Android Studio Hedgehog 或更高版本
- JDK 17+
- Kotlin Multiplatform 插件
- 对于 iOS 开发：Xcode 14+ 和 macOS

## 构建和运行

### Android
```bash
./gradlew :composeApp:assembleDebug
```

### Desktop (macOS/Windows/Linux)
```bash
./gradlew :composeApp:run
```

### iOS
在 Xcode 中打开 `iosApp/iosApp.xcodeproj` 并运行

## 使用说明

1. 选择难度（4×4、5×5 或 6×6）
2. 选择游戏模式（数字、字母、汉字或颜色）
3. 点击"开始游戏"
4. 按照从 1 开始的顺序依次点击方格
5. 尽快完成，时间越短越好！

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！
