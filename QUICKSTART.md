# 快速开始指南

## 1. 环境准备

确保你已经安装了以下工具：

- **JDK 17 或更高版本**
  ```bash
  # 使用 SDKMAN 安装
  sdk install java 17.0.9-tem
  
  # 或使用 Homebrew 安装
  brew install openjdk@17
  ```

- **Android Studio** (用于 Android 开发)
  - 下载地址：https://developer.android.com/studio

- **Xcode** (用于 iOS/macOS 开发，仅 macOS)
  - 从 Mac App Store 安装

## 2. 运行应用

### 桌面版 (macOS/Windows/Linux)

```bash
# 运行桌面应用
./gradlew :composeApp:run
```

### Android 版

```bash
# 构建 Debug 版本
./gradlew :composeApp:assembleDebug

# 或在连接的设备上运行
./gradlew :composeApp:installDebug
```

### iOS 版

1. 在 Xcode 中打开 `iosApp/iosApp.xcodeproj`
2. 选择目标设备或模拟器
3. 点击运行按钮

## 3. 构建发布版本

### Android Release

```bash
./gradlew :composeApp:assembleRelease
```

### macOS DMG

```bash
./gradlew :composeApp:packageDmg
```

## 4. 常见问题

### 问题：找不到 Java 命令

**解决方案**: 设置 JAVA_HOME 环境变量
```bash
export JAVA_HOME=/path/to/your/jdk
```

### 问题：Gradle 构建缓慢

**解决方案**: 启用 Gradle 缓存和并行构建
```bash
# 在 gradle.properties 中添加
org.gradle.parallel=true
org.gradle.caching=true
```

### 问题：iOS 构建失败

**解决方案**: 
1. 确保 Xcode 已正确安装
2. 运行 `xcode-select --install`
3. 在 Xcode 中接受许可协议

## 5. 开发建议

- 使用 Android Studio 或 IntelliJ IDEA 进行开发
- 安装 Kotlin Multiplatform 插件
- 启用 Kotlin 代码格式化

## 6. 下一步

- 自定义主题颜色
- 添加音效
- 实现排行榜功能
- 添加统计信息
- 支持自定义网格大小

祝你开发愉快！🎉
