package com.example.sweepneko

enum class EnemyType(val initialHp: Int, val speed: Float, val widthDp: Float, val heightDp: Float) {
    NORMAL(initialHp = 1, speed = 5f, widthDp = 100f, heightDp = 100f),
    FAST(initialHp = 1, speed = 7f, widthDp = 60f, heightDp = 60f),
    BIG(initialHp = 3, speed = 3f, widthDp = 150f, heightDp = 150f),
    SHOOTING(initialHp = 1, speed = 5f, widthDp = 100f, heightDp = 100f),
    BOSS(initialHp = 10, speed = 1f, widthDp = 250f, heightDp = 250f)
}

data class Enemy(
    var id: Long,
    var x: Float,
    var y: Float,
    val type: EnemyType = EnemyType.NORMAL,
    val speed: Float = type.speed,
    var hp: Int = type.initialHp,
    var widthPx: Float = 0f,
    var heightPx: Float = 0f,
    var widthDp: Float = type.widthDp,
    var heightDp: Float = type.heightDp,
    var lastHitTime: Long = 0L,
    var lastAttackTime: Long = 0L,
    var isFlipped: Boolean = false
) {
    companion object {
        const val SPAWN_INTERVAL_MS = 2000L // ปรับอัตราการเกิด (มิลลิวินาที) เช่น 1000 = 1 วิ, 2000 = 2 วิ

        fun createRandomSpawn(id: Long, screenWidthPx: Float, screenHeightPx: Float, pixelDensity: Float, currentEnemies: List<Enemy> = emptyList()): Enemy {
            val randType = Math.random()
            var type = when {
                randType < 0.3 -> EnemyType.SHOOTING // โอกาสเกิด 30%
                randType < 0.6 -> EnemyType.NORMAL // โอกาสเกิด 30%
                randType < 0.85 -> EnemyType.FAST // โอกาสเกิด 25%
                else -> EnemyType.BIG // โอกาสเกิด 15%
            }

            // จำกัดการเกิดของ SHOOTING enemy สูงสุด 3 ตัว
            if (type == EnemyType.SHOOTING && currentEnemies.count { it.type == EnemyType.SHOOTING } >= 3) {
                type = EnemyType.NORMAL
            }

            val eWidthPx = type.widthDp * pixelDensity
            val eHeightPx = type.heightDp * pixelDensity

            val side = (0..2).random()
            val spawnX: Float
            val spawnY: Float

            // 0 = Top, 1 = Left, 2 = Right
            return when (side) {
                0 -> {
                    spawnX = (Math.random() * (screenWidthPx - eWidthPx) + eWidthPx / 2f).toFloat()
                    spawnY = -eHeightPx * 1.2f 
                    Enemy(
                        id = id,
                        x = spawnX,
                        y = spawnY,
                        type = type,
                        speed = type.speed,
                        hp = type.initialHp,
                        widthPx = eWidthPx,
                        heightPx = eHeightPx,
                        widthDp = type.widthDp,
                        heightDp = type.heightDp,
                        isFlipped = (type == EnemyType.NORMAL && spawnX < screenWidthPx / 2f)
                    )
                }
                1 -> {
                    spawnX = -eWidthPx * 1.2f 
                    spawnY = (Math.random() * (screenHeightPx * 0.15f) - eHeightPx).toFloat()
                    Enemy(
                        id = id,
                        x = spawnX,
                        y = spawnY,
                        type = type,
                        speed = type.speed,
                        hp = type.initialHp,
                        widthPx = eWidthPx,
                        heightPx = eHeightPx,
                        widthDp = type.widthDp,
                        heightDp = type.heightDp,
                        isFlipped = (type == EnemyType.NORMAL || type == EnemyType.BIG)
                    )
                }
                else -> {
                    spawnX = screenWidthPx + eWidthPx * 1.2f 
                    spawnY = (Math.random() * (screenHeightPx * 0.15f) - eHeightPx).toFloat()
                    Enemy(
                        id = id,
                        x = spawnX,
                        y = spawnY,
                        type = type,
                        speed = type.speed,
                        hp = type.initialHp,
                        widthPx = eWidthPx,
                        heightPx = eHeightPx,
                        widthDp = type.widthDp,
                        heightDp = type.heightDp,
                        isFlipped = false
                    )
                }
            }
        }
    }
}
