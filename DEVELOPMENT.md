# 开发指南

## 目录

1. [环境配置](#环境配置)
2. [项目设置](#项目设置)
3. [运行应用](#运行应用)
4. [调试技巧](#调试技巧)
5. [代码规范](#代码规范)
6. [常见问题](#常见问题)

## 环境配置

### 必需工具

#### 1. JDK 17+
```bash
# macOS (使用 Homebrew)
brew install openjdk@17

# 或者使用 SDKMAN
sdk install java 17.0.9-tem
```

#### 2. Android Studio
- 下载地址：https://developer.android.com/studio
- 推荐版本：最新版本

#### 3. Xcode (仅 macOS，用于 iOS 开发)
```bash
# 从 Mac App Store 安装
# 安装完成后运行
xcode-select --install
```

### 可选工具

#### IntelliJ IDEA
- 推荐安装 Ultimate 版本
- 安装 Kotlin Multiplatform 插件

#### Android SDK
```bash
# 通过 Android Studio 安装
# 确保安装以下组件：
- Android SDK Platform 34
- Android SDK Build-Tools
- Android Emulator
```

## 项目设置

### 1. 克隆项目
```bash
git clone <repository-url>
cd SchulteTable
```

### 2. 初始化 Gradle Wrapper
```bash
chmod +x setup-gradle.sh
./setup-gradle.sh
```

### 3. 同步 Gradle
```bash
./gradlew --refresh-dependencies
```

### 4. 在 Android Studio 中打开
1. 打开 Android Studio
2. 选择 "Open an Existing Project"
3. 选择 `SchulteTable` 目录
4. 等待 Gradle 同步完成

## 运行应用

### 桌面版 (推荐用于快速开发)

```bash
# 运行
./gradlew :composeApp:run

# 或指定主类
./gradlew :composeApp:run -PmainClass=MainKt
```

### Android 版

#### 使用 Android Studio
1. 点击运行按钮
2. 选择模拟器或真机
3. 应用将自动安装并启动

#### 使用命令行
```bash
# 构建 Debug 版本
./gradlew :composeApp:assembleDebug

# 安装到连接的设备
./gradlew :composeApp:installDebug

# 构建并运行
./gradlew :composeApp:installDebug
adb shell am start -n com.schultetable.app/.MainActivity
```

### iOS 版

#### 使用 Xcode
1. 打开 `iosApp/iosApp.xcodeproj`
2. 选择目标设备或模拟器
3. 点击运行按钮

#### 命令行 (需要 fastlane)
```bash
cd iosApp
xcodebuild -project iosApp.xcodeproj -scheme iosApp
```

## 调试技巧

### 日志输出

```kotlin
// 在代码中添加日志
import androidx.compose.runtime.Composable
import kotlin.log.Log

@Composable
fun GameView() {
    Log.d("GameView", "游戏视图已创建")
    // ... 其他代码
}
```

### Android 日志
```bash
# 查看 Logcat
adb logcat

# 过滤特定标签
adb logcat -s GameView
```

### iOS 日志
在 Xcode 控制台中查看输出

### 桌面日志
在运行终端中查看标准输出

### Compose 调试

启用 Compose 编译器指标：
```kotlin
// 在 build.gradle.kts 中
android {
    buildTypes {
        debug {
            isDebuggable = true
        }
    }
}
```

## 代码规范

### Kotlin 代码风格

遵循 [Kotlin 编码规范](https://kotlinlang.org/docs/coding-conventions.html)

#### 命名约定
```kotlin
// 类和对象 - PascalCase
class GameView { }

// 函数和变量 - camelCase
fun startNewGame() { }
var currentTime = 0

// 常量 - SCREAMING_SNAKE_CASE
const val MAX_GRID_SIZE = 6

// 私有属性 - 驼峰命名，可加下划线前缀
private val _numbers = mutableListOf<Int>()
```

#### 代码组织
```kotlin
class GameView {
    // 1. 伴生对象
    companion object { }
    
    // 2. 属性
    private var state = 0
    
    // 3. 构造函数
    constructor() { }
    
    // 4. 函数
    fun publicFunction() { }
    private fun privateFunction() { }
    
    // 5. Composable 函数
    @Composable
    fun Content() { }
}
```

### Compose 最佳实践

#### 1. 状态提升
```kotlin
// ❌ 不好的做法
@Composable
fun GameView() {
    var score by remember { mutableStateOf(0) }
    // ...
}

// ✅ 好的做法
@Composable
fun GameView(score: Int, onScoreChange: (Int) -> Unit) {
    // ...
}
```

#### 2. 避免重组
```kotlin
// ❌ 可能导致不必要的重组
Text("${currentTime}秒")

// ✅ 使用 derivedStateOf
val timeText by remember {
    derivedStateOf { "${currentTime}秒" }
}
Text(timeText)
```

#### 3. 使用 rememberSaveable
```kotlin
// 配置变更时保持状态
var score by rememberSaveable { mutableStateOf(0) }
```

### Git 提交规范

```bash
# 格式
<type>(<scope>): <subject>

# 示例
feat(game): 添加颜色模式支持
fix(timer): 修复计时器精度问题
docs(readme): 更新安装说明
style(format): 代码格式化
refactor(core): 重构游戏逻辑
test(game): 添加游戏逻辑测试
```

## 常见问题

### 问题 1: Gradle 同步失败

**错误**: `Could not resolve all files`

**解决方案**:
```bash
# 清理并重新构建
./gradlew clean
./gradlew --stop
./gradlew build --refresh-dependencies
```

### 问题 2: Compose 预览不工作

**解决方案**:
1. 确保安装了最新版的 Android Studio
2. 启用实验性功能：
   - Settings → Editor → Live Edit
   - 启用 "Live Edit"
3. 重建项目：Build → Rebuild Project

### 问题 3: iOS 构建失败

**错误**: `No such module 'ComposeApp'`

**解决方案**:
```bash
# 在 iOS 项目中
cd iosApp
pod install
```

### 问题 4: 桌面应用窗口不显示

**解决方案**:
检查 main.kt 中的导入：
```kotlin
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
```

### 问题 5: Android 模拟器性能差

**解决方案**:
1. 使用 Cold Boot
2. 增加模拟器内存
3. 使用 ARM64 镜像（Apple Silicon）
4. 考虑使用真机调试

## 性能优化

### 1. 减少重组
```kotlin
// 使用 stable 类型
@Stable
data class GameConfig(
    val gridSize: Int,
    val gameMode: GameMode
)
```

### 2. 图片优化
```kotlin
// 使用矢量图
ImageVector.vectorResource(R.drawable.icon)
```

### 3. 列表优化
```kotlin
// 使用 LazyVGrid 而不是普通 Grid
LazyVerticalGrid(columns = GridCells.Fixed(5)) {
    items(numbers.size) { index ->
        // ...
    }
}
```

## 测试

### 单元测试
```kotlin
// src/commonTest/kotlin/...
@Test
fun testShuffle() {
    val numbers = (1..25).toList()
    val shuffled = numbers.shuffled()
    assert(shuffled.size == numbers.size)
}
```

### UI 测试
```kotlin
// src/androidAndroidTest/kotlin/...
@Test
fun testGameStart() {
    composeTestRule.setContent {
        GameView(gridSize = 5, gameMode = GameMode.Numbers)
    }
    
    composeTestRule
        .onNodeWithText("开始游戏")
        .performClick()
    
    composeTestRule
        .onNodeWithText("1")
        .assertExists()
}
```

## 发布流程

### Android

1. 生成签名密钥
```bash
keytool -genkey -v -keystore schulte-table.keystore -alias schulte-table -keyalg RSA -keysize 2048 -validity 10000
```

2. 配置签名
```kotlin
// build.gradle.kts
android {
    signingConfigs {
        create("release") {
            storeFile = file("schulte-table.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "schulte-table"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
}
```

3. 构建发布版本
```bash
./gradlew :composeApp:assembleRelease
```

### macOS

```bash
./gradlew :composeApp:packageDmg
```

### iOS

1. 在 Xcode 中配置签名
2. Product → Archive
3. 分发应用

## 持续集成

### GitHub Actions 示例

```yaml
name: Build and Test

on: [push, pull_request]

jobs:
  build:
    runs-on: macos-latest
    
    steps:
    - uses: actions/checkout@v2
    
    - name: Set up JDK 17
      uses: actions/setup-java@v2
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Build
      run: ./gradlew build
    
    - name: Run tests
      run: ./gradlew test
```

## 资源

- [Compose Multiplatform 官方文档](https://www.jetbrains.com/compose-multiplatform/)
- [Kotlin 文档](https://kotlinlang.org/docs/home.html)
- [Material Design 3](https://m3.material.io/)
- [Android 开发者文档](https://developer.android.com/)

---

祝你开发愉快！🚀
