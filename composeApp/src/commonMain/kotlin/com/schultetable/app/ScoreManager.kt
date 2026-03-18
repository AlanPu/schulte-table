package com.schultetable.app

import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ScoreManager(appDir: File) {

    private val scoresDir = File(appDir, "scores").also { it.mkdirs() }
    private val json = Json { ignoreUnknownKeys = true }
    private val scoreCache = mutableMapOf<String, List<ScoreRecord>>()

    fun generateKey(gridSize: Int, gameMode: GameMode, useColors: Boolean): String {
        return "${gridSize}_${gameMode.name}_$useColors"
    }

    private fun getFile(key: String): File {
        return File(scoresDir, "$key.json")
    }

    fun getScores(gridSize: Int, gameMode: GameMode, useColors: Boolean): List<ScoreRecord> {
        val key = generateKey(gridSize, gameMode, useColors)
        return getScoreList(key)
    }

    private fun getScoreList(key: String): List<ScoreRecord> {
        if (scoreCache.containsKey(key)) {
            return scoreCache[key]!!
        }
        val file = getFile(key)
        val list = if (file.exists()) {
            try {
                json.decodeFromString<ScoreList>(file.readText()).records
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        scoreCache[key] = list
        return list
    }

    fun saveScore(record: ScoreRecord) {
        val key = generateKey(record.gridSize, record.gameMode, record.useColors)
        val file = getFile(key)

        val existingList = if (file.exists()) {
            try {
                json.decodeFromString<ScoreList>(file.readText())
            } catch (e: Exception) {
                ScoreList()
            }
        } else {
            ScoreList()
        }

        val updatedList = existingList.copy(
            records = (existingList.records + record)
                .sortedBy { it.elapsedTime }
                .take(10)
        )

        file.writeText(Json.encodeToString(updatedList))

        scoreCache[key] = updatedList.records
    }
}