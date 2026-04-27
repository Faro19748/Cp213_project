package com.example.sweepneko

enum class EnemyType(
    val initialHp: Int, 
    val speed: Float, 
    val widthDp: Float, 
    val heightDp: Float,
    val hitboxWidthRatio: Float = 0.8f,
    val hitboxHeightRatio: Float = 0.8f
) {
    NORMAL(initialHp = 1, speed = 5f, widthDp = 100f, heightDp = 100f),
    FAST(initialHp = 1, speed = 7f, widthDp = 60f, heightDp = 60f, hitboxWidthRatio = 0.9f, hitboxHeightRatio = 0.9f),
    BIG(initialHp = 3, speed = 3f, widthDp = 150f, heightDp = 150f, hitboxWidthRatio = 0.35f, hitboxHeightRatio = 0.35f),
    SHOOTING(initialHp = 1, speed = 5f, widthDp = 100f, heightDp = 100f),
    BOSS(initialHp = 10, speed = 1f, widthDp = 250f, heightDp = 250f, hitboxWidthRatio = 1.0f, hitboxHeightRatio = 1.0f)
}

data class Enemy(
    val id: Long,
    val x: Float,
    val y: Float,
    val type: EnemyType = EnemyType.NORMAL,
    val speed: Float = type.speed,
    val hp: Int = type.initialHp,
    val widthPx: Float = 0f,
    val heightPx: Float = 0f,
    val widthDp: Float = type.widthDp,
    val heightDp: Float = type.heightDp,
    val hitboxWidthPx: Float = 0f,
    val hitboxHeightPx: Float = 0f,
    val lastHitTime: Long = 0L,
    val lastAttackTime: Long = 0L,
    val isFlipped: Boolean = false
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
            val hitboxWPx = eWidthPx * type.hitboxWidthRatio
            val hitboxHPx = eHeightPx * type.hitboxHeightRatio

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
                        hitboxWidthPx = hitboxWPx,
                        hitboxHeightPx = hitboxHPx,
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
                        hitboxWidthPx = hitboxWPx,
                        hitboxHeightPx = hitboxHPx,
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
                        hitboxWidthPx = hitboxWPx,
                        hitboxHeightPx = hitboxHPx,
                        widthDp = type.widthDp,
                        heightDp = type.heightDp,
                        isFlipped = false
                    )
                }
            }
        }
    }
}
