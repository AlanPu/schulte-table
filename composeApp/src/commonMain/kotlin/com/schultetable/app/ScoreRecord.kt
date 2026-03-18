package com.schultetable.app

import kotlinx.serialization.Serializable

@Serializable
data class ScoreRecord(
    val elapsedTime: Float,
    val timestamp: Long,
    val gridSize: Int,
    val gameMode: GameMode,
    val useColors: Boolean
)

@Serializable
data class ScoreList(
    val records: List<ScoreRecord> = emptyList()
)