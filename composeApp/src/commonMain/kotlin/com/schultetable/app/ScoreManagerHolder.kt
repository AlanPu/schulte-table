package com.schultetable.app

import java.io.File

object ScoreManagerHolder {
    private var _manager: ScoreManager? = null

    fun initialize(appDir: File) {
        if (_manager == null) {
            _manager = ScoreManager(File(appDir, "scores"))
        }
    }

    fun get(): ScoreManager {
        return _manager ?: throw IllegalStateException("ScoreManager not initialized")
    }
}