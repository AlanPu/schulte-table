package com.schultetable.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.Star

@Composable
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var gridSize by remember { mutableStateOf(5) }
    var gameMode by remember { mutableStateOf(GameMode.Numbers) }
    var useColors by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showScoreHistory by remember { mutableStateOf(false) }

    val scoreManager = remember { ScoreManagerHolder.get() }
    var scores by remember { mutableStateOf<List<ScoreRecord>>(emptyList()) }

    fun loadScores() {
        scores = scoreManager.getScores(gridSize, gameMode, useColors)
    }

    fun saveScore(elapsedTime: Float) {
        scoreManager.saveScore(
            ScoreRecord(
                elapsedTime = elapsedTime,
                timestamp = System.currentTimeMillis(),
                gridSize = gridSize,
                gameMode = gameMode,
                useColors = useColors
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.APP_TITLE) },
                actions = {
                    IconButton(onClick = {
                        loadScores()
                        showScoreHistory = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = Strings.SCORE_RECORD
                        )
                    }
                    IconButton(onClick = { showAbout = true }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Info,
                            contentDescription = Strings.ABOUT
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                            contentDescription = Strings.SETTINGS
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            GameView(
                gridSize = gridSize,
                gameMode = gameMode,
                useColors = useColors,
                onGameOver = { elapsedTime -> saveScore(elapsedTime) }
            )
        }
        
        if (showSettings) {
            SettingsDialog(
                gridSize = gridSize,
                gameMode = gameMode,
                useColors = useColors,
                onDismiss = { showSettings = false },
                onConfirm = { newGridSize, newGameMode, newUseColors ->
                    gridSize = newGridSize
                    gameMode = newGameMode
                    useColors = newUseColors
                    showSettings = false
                }
            )
        }
        
        if (showAbout) {
            AboutDialog(
                onDismiss = { showAbout = false }
            )
        }

        if (showScoreHistory) {
            ScoreHistoryDialog(
                scores = scores,
                gridSize = gridSize,
                gameMode = gameMode,
                useColors = useColors,
                onDismiss = { showScoreHistory = false }
            )
        }
    }
}

@Composable
fun GameView(
    gridSize: Int,
    gameMode: GameMode,
    useColors: Boolean,
    onGameOver: (Float) -> Unit = {}
) {
    var numbers by remember { mutableStateOf(emptyList<Int>()) }
    var currentNumber by remember { mutableStateOf(1) }
    var elapsedTime by remember { mutableStateOf(0f) }
    var isRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    
    fun startNewGame() {
        numbers = (1..gridSize * gridSize).shuffled()
        currentNumber = 1
        elapsedTime = 0f
        gameOver = false
        isRunning = true
        isPaused = false
    }
    
    fun togglePause() {
        if (isRunning && !gameOver) {
            isPaused = !isPaused
        }
    }
    
    fun handleTap(number: Int) {
        if (!isRunning || gameOver || isPaused) return
        
        if (number == currentNumber) {
            currentNumber++
            if (currentNumber > gridSize * gridSize) {
                gameOver = true
                isRunning = false
                isPaused = false
            }
        }
    }
    
    LaunchedEffect(gameOver) {
        if (gameOver) {
            onGameOver(elapsedTime)
        }
    }

    LaunchedEffect(gridSize, gameMode) {
        if (isRunning) {
            startNewGame()
        }
    }
    
    LaunchedEffect(isRunning, isPaused) {
        if (isRunning && !isPaused) {
            while (isRunning && !isPaused && !gameOver) {
                delay(10)
                elapsedTime += 0.01f
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // 计时器
        TimerDisplay(elapsedTime = elapsedTime, isRunning = isRunning)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 当前数字提示
        Text(
            text = Strings.CURRENT.format(getContent(currentNumber, gameMode)),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = Strings.TARGET.format(getContent(gridSize * gridSize, gameMode)),
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 游戏网格 - 使用固定大小
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth(0.9f),
            contentAlignment = Alignment.Center
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridSize),
                modifier = Modifier
                    .aspectRatio(1f)
                    .alpha(if (isPaused) 0.5f else 1.0f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(numbers.size) { index ->
                    val number = numbers[index]
                    CellView(
                        content = getContent(number, gameMode),
                        number = number,
                        expectedNumber = currentNumber,
                        gameMode = gameMode,
                        useColors = useColors,
                        enabled = !isPaused,
                        onClick = { handleTap(number) }
                    )
                }
            }
            
            // 暂停遮罩
            if (isPaused) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxWidth(0.9f),
                    contentAlignment = Alignment.Center
                ) {
                    // 透明遮罩，阻止点击
                }
            }
        }
        
        // 游戏控制按钮
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            // 暂停/继续按钮（游戏进行中显示）
            if (isRunning && !gameOver) {
                Button(
                        onClick = { togglePause() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isPaused) Strings.RESUME else Strings.PAUSE)
                    }

                    Button(
                        onClick = { startNewGame() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(Strings.RESTART)
                    }
            }
        }
        
        // 游戏状态显示
        if (gameOver) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = Strings.GAME_OVER,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = Strings.TIME_USED.format(elapsedTime),
                    fontSize = 18.sp,
                    color = Color.Green
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { startNewGame() },
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Text(Strings.PLAY_AGAIN)
                }
            }
        } else if (!isRunning) {
            Button(
                onClick = { startNewGame() },
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Text(Strings.START_GAME)
            }
        } else if (isPaused) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = Strings.GAME_PAUSED,
                fontSize = 20.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun CellView(
    content: String,
    number: Int,
    expectedNumber: Int,
    gameMode: GameMode,
    useColors: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    var isClicked by remember { mutableStateOf(false) }
    
    // 根据 useColors 决定是否使用彩色背景
    val backgroundColor = if (useColors) {
        colorForNumber(number)
    } else if (isClicked) {
        Color.Green.copy(alpha = 0.3f)
    } else {
        Color.Blue.copy(alpha = 0.1f)
    }
    
    // 根据背景颜色选择合适的文字颜色（确保对比度）
    val textColor = if (useColors) {
        textColorForBackground(backgroundColor)
    } else {
        Color.Black
    }
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(
                enabled = enabled,
                onClick = {
                    isClicked = true
                    onClick()
                }
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        // 显示内容（数字、字母、汉字）
        Text(
            text = content,
            fontSize = when (gameMode) {
                GameMode.Chinese -> 20.sp
                GameMode.Letters -> 24.sp
                else -> 22.sp
            },
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
    
    LaunchedEffect(isClicked) {
        if (isClicked) {
            delay(100)
            isClicked = false
        }
    }
}

@Composable
fun TimerDisplay(elapsedTime: Float, isRunning: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                Color.Gray.copy(alpha = 0.1f),
                RoundedCornerShape(10.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = if (isRunning) Strings.TIMING else Strings.READY,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Text(
            text = "%.2f".format(elapsedTime),
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = if (isRunning) Color.Black else Color.Gray
        )
    }
}

@Composable
fun SettingsDialog(
    gridSize: Int,
    gameMode: GameMode,
    useColors: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, GameMode, Boolean) -> Unit
) {
    var selectedGridSize by remember { mutableStateOf(gridSize) }
    var selectedGameMode by remember { mutableStateOf(gameMode) }
    var selectedUseColors by remember { mutableStateOf(useColors) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.SETTINGS) },
        text = {
            Column {
                Text(Strings.DIFFICULTY)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(4, 5, 6).forEach { size ->
                        FilterChip(
                            selected = selectedGridSize == size,
                            onClick = { selectedGridSize = size },
                            label = { Text("${size}×${size}") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(Strings.CONTENT_MODE)
                Row {
                    GameMode.values().forEach { mode ->
                        FilterChip(
                            selected = selectedGameMode == mode,
                            onClick = { selectedGameMode = mode },
                            label = { Text(mode.displayName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(Strings.CELL_STYLE)
                Row {
                    FilterChip(
                        selected = selectedUseColors,
                        onClick = { selectedUseColors = !selectedUseColors },
                        label = { Text(if (selectedUseColors) Strings.COLORFUL else Strings.MONOCHROME) },
                        leadingIcon = {
                            if (selectedUseColors) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = androidx.compose.ui.Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedGridSize, selectedGameMode, selectedUseColors) }
            ) {
                Text(Strings.CONFIRM)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.CANCEL)
            }
        }
    )
}

fun getContent(number: Int, gameMode: GameMode): String {
    return when (gameMode) {
        GameMode.Numbers -> number.toString()
        GameMode.Letters -> ((number - 1 + 'A'.code).toChar()).toString()
        GameMode.Chinese -> {
            val chineseNumbers = listOf(
                "零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十",
                "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
                "二十一", "二十二", "二十三", "二十四", "二十五", "二十六", "二十七", "二十八", "二十九", "三十",
                "三十一", "三十二", "三十三", "三十四", "三十五", "三十六"
            )
            chineseNumbers[number - 1]
        }
    }
}

fun colorForNumber(number: Int): Color {
    // 选择饱和度较高、亮度适中的颜色，确保与文字有良好对比
    val colors = listOf(
        Color(0xFFE53935), // 红色 - 深红
        Color(0xFFFB8C00), // 橙色 - 深橙
        Color(0xFFFDD835), // 黄色 - 金黄
        Color(0xFF43A047), // 绿色 - 深绿
        Color(0xFF1E88E5), // 蓝色 - 深蓝
        Color(0xFF8E24AA), // 紫色 - 深紫
        Color(0xFFD81B60), // 粉色 - 深粉
        Color(0xFF5E35B1)  // 靛色 - 深靛
    )
    return colors[number % colors.size]
}

/**
 * 根据背景颜色返回合适的文字颜色
 * 确保有足够的对比度（WCAG 标准）
 */
fun textColorForBackground(backgroundColor: Color): Color {
    // 计算背景颜色的亮度
    // 使用简单的亮度公式：0.299*R + 0.587*G + 0.114*B
    val red = (backgroundColor.red * 255).toInt()
    val green = (backgroundColor.green * 255).toInt()
    val blue = (backgroundColor.blue * 255).toInt()
    
    val brightness = (red * 299 + green * 587 + blue * 114) / 1000
    
    // 如果背景较亮（亮度 > 128），使用黑色文字；否则使用白色文字
    return if (brightness > 128) Color.Black else Color.White
}

enum class GameMode(val displayName: String) {
    Numbers("数字"),
    Letters("字母"),
    Chinese("汉字")
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = androidx.compose.ui.Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = Strings.ABOUT_SCHULTE_TABLE,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = Strings.INTRO,
                    fontSize = 14.sp
                )

                Text(
                    text = Strings.TRAINING_METHOD,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = Strings.TRAINING_TIPS,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Text(
                    text = Strings.SCORING_STANDARD,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                Text(
                    text = Strings.GRID_5X5,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Column(
                    modifier = androidx.compose.ui.Modifier.padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = Strings.EXCELLENT_5X5,
                        fontSize = 13.sp,
                        color = Color(0xFFFFD700)
                    )
                    Text(
                        text = Strings.GOOD_5X5,
                        fontSize = 13.sp,
                        color = Color(0xFFC0C0C0)
                    )
                    Text(
                        text = Strings.OK_5X5,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = Strings.GRID_4X4,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Column(
                    modifier = androidx.compose.ui.Modifier.padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = Strings.EXCELLENT_4X4,
                        fontSize = 13.sp,
                        color = Color(0xFFFFD700)
                    )
                    Text(
                        text = Strings.GOOD_4X4,
                        fontSize = 13.sp,
                        color = Color(0xFFC0C0C0)
                    )
                    Text(
                        text = Strings.OK_4X4,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = Strings.GRID_6X6,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Column(
                    modifier = androidx.compose.ui.Modifier.padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = Strings.EXCELLENT_6X6,
                        fontSize = 13.sp,
                        color = Color(0xFFFFD700)
                    )
                    Text(
                        text = Strings.GOOD_6X6,
                        fontSize = 13.sp,
                        color = Color(0xFFC0C0C0)
                    )
                    Text(
                        text = Strings.OK_6X6,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss
            ) {
                Text(Strings.GOT_IT)
            }
        }
    )
}
