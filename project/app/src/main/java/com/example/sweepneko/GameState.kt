package com.example.sweepneko

import androidx.compose.ui.geometry.Offset

data class FadingSlash(val start: Offset, val end: Offset, val startTime: Long, val isRed: Boolean = false, val isGold: Boolean = false)
data class Projectile(val id: Long, val x: Float, val y: Float, val dx: Float, val dy: Float, val widthDp: Float = 40f, val heightDp: Float = 40f)
data class FadingEnemy(val enemy: Enemy, val deathTime: Long)

data class FadingBomb(val x: Float, val y: Float, val startTime: Long, val widthDp: Float, val heightDp: Float)

data class C4Hazard(
    val id: Long,
    val x: Float,
    val y: Float,
    val dx: Float,
    val dy: Float,
    val spawnTime: Long,
    val widthDp: Float = 80f,
    val heightDp: Float = 80f
)

enum class PowerUpType {
    CAT_CAN,
    CAT_BAR,
    TIME_STOP
}

data class PowerUp(
    val id: Long,
    val x: Float,
    val y: Float,
    val type: PowerUpType,
    val dx: Float,
    val dy: Float,
    val spawnTime: Long,
    val widthDp: Float = 60f,
    val heightDp: Float = 60f
)

data class GameState(
    val hp: Int = 100,
    val stamina: Float = 100f,
    val enemies: List<Enemy> = emptyList(),
    val projectiles: List<Projectile> = emptyList(),
    val powerUps: List<PowerUp> = emptyList(),
    val inventory: List<PowerUpType> = emptyList(),
    val fadingSlashes: List<FadingSlash> = emptyList(),
    val fadingEnemies: List<FadingEnemy> = emptyList(),
    val ultimateGauge: Float = 0f,
    val isUltimateActive: Boolean = false,
    val comboCount: Int = 0,
    val maxComboInRun: Int = 0,
    val isNextSlashRed: Boolean = false,
    val slashStart: Offset? = null,
    val slashEnd: Offset? = null,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val playerImmuneUntil: Long = 0L,
    val wave: Int = 1,
    val enemiesKilledInWave: Int = 0,
    val targetKillsForWave: Int = 15,
    val infiniteStaminaUntil: Long = 0L,
    val enemySlowUntil: Long = 0L,
    val c4s: List<C4Hazard> = emptyList(),
    val fadingBombs: List<FadingBomb> = emptyList(),
    val shakeTriggerTime: Long = 0L,
    val shakeIntensity: Float = 0f,
    val lastDamageTime: Long = 0L
)
