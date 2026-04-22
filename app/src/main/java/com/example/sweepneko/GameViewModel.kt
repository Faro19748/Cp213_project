package com.example.sweepneko

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.sin
import android.content.Context
import androidx.core.content.edit

class GameViewModel : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state = _state.asStateFlow()

    private var gameLoopJob: Job? = null
    private var projectileIdCounter = 0L
    private var spawnCount = 0L
    private var lastEnemyHitTime = 0L
    private var timeSinceLastSpawn = 0L
    private var nextSwarmTime = 0L

    // Constants or Screen dimensions set from Composable
    var screenWidthPx = 0f
    var screenHeightPx = 0f
    var pixelDensity = 1f
    var characterX = 0f
    var characterY = 0f
    var characterHitboxWidthPx = 0f
    var characterHitboxHeightPx = 0f

    fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch(Dispatchers.Default) {
            var lastFrameTime = System.nanoTime()
            nextSwarmTime = System.currentTimeMillis() + 10000L
            SoundManager.playMainMusic()

            while (true) {
                if (_state.value.isGameOver) break
                if (_state.value.isPaused) { 
                    delay(32)
                    lastFrameTime = System.nanoTime()
                    continue 
                }

                val currentTimeNano = System.nanoTime()
                val dtNano = currentTimeNano - lastFrameTime
                val dtMs = dtNano / 1_000_000L
                lastFrameTime = currentTimeNano

                if (dtMs > 0) {
                    updateGameState(System.currentTimeMillis(), dtMs)
                }
                
                // ใช้การคำนวณที่สัมพันธ์กับ VSync จะดีกว่า แต่ใน ViewModel 
                // เราจะพยายามรักษา Frame Rate ให้คงที่ประมาณ 60 FPS
                val processingTime = (System.nanoTime() - currentTimeNano) / 1_000_000L
                val waitTime = max(1L, 16L - processingTime)
                delay(waitTime)
            }
        }
    }

    private fun updateGameState(currentTime: Long, dt: Long) {
        val prevState = _state.value
        val newState = _state.updateAndGet { s ->
            if (s.isGameOver || s.isPaused) return@updateAndGet s

            var newHp = s.hp
            var newStamina = s.stamina
            val newUltimateGauge = s.ultimateGauge
            var newComboCount = s.comboCount
            var newIsNextSlashRed = s.isNextSlashRed
            var newPlayerImmuneUntil = s.playerImmuneUntil
            val newIsUltimateActive = s.isUltimateActive

            if (newComboCount > 0 && currentTime - lastEnemyHitTime > 3000L) {
                newComboCount = 0
                newIsNextSlashRed = false
            }

            if (s.slashStart == null && newStamina < 100f) {
                val staminaRegenRate = if (currentTime < s.infiniteStaminaUntil) 1000f else 10f
                newStamina = min(100f, newStamina + (dt * staminaRegenRate) / 1000f)
            }
            if (currentTime < s.infiniteStaminaUntil) {
                newStamina = 100f
            }

            val newFadingSlashes = s.fadingSlashes.filter { currentTime - it.startTime < 400 }
            val newFadingEnemies = s.fadingEnemies.filter { currentTime - it.deathTime < 1000 }

            // Wave Management
            var newWave = s.wave
            var newEnemiesKilledInWave = s.enemiesKilledInWave
            var newTargetKillsForWave = s.targetKillsForWave

            val newEnemies = s.enemies.toMutableList()
            timeSinceLastSpawn += dt
            
            // Check for wave transition
            if (newEnemiesKilledInWave >= newTargetKillsForWave) {
                newWave++
                newEnemiesKilledInWave = 0
                newTargetKillsForWave += 5
                
                // Spawn Power-up when wave increases
                // (Already handled below using newWave > s.wave)
                
                // Spawn Boss every 5 waves
                if (newWave % 5 == 0) {
                    spawnCount++
                    val eType = EnemyType.BOSS
                    val eWidthPx = eType.widthDp * pixelDensity
                    val eHeightPx = eType.heightDp * pixelDensity
                    val bSpawnX = (Math.random() * (screenWidthPx - eWidthPx) + eWidthPx / 2f).toFloat()
                    val bSpawnY = -eHeightPx * 1.5f
                    
                    newEnemies.add(Enemy(
                        id = System.currentTimeMillis() + spawnCount,
                        x = bSpawnX,
                        y = bSpawnY,
                        type = eType,
                        speed = eType.speed,
                        hp = eType.initialHp,
                        widthPx = eWidthPx,
                        heightPx = eHeightPx,
                        hitboxWidthPx = eWidthPx * eType.hitboxWidthRatio,
                        hitboxHeightPx = eHeightPx * eType.hitboxHeightRatio,
                        widthDp = eType.widthDp,
                        heightDp = eType.heightDp,
                        isFlipped = (bSpawnX < screenWidthPx / 2f)
                    ))
                    SoundManager.playBossMusic()
                }
            }

            val newPowerUps = s.powerUps.toMutableList()
            if (newWave > s.wave) {
                spawnCount++
                val puType = PowerUpType.entries.random()
                val puWidthPx = 60f * pixelDensity
                val spawnX = (Math.random() * (screenWidthPx - puWidthPx) + puWidthPx / 2f).toFloat()
                val spawnY = (Math.random() * (screenHeightPx * 0.5f) + screenHeightPx * 0.2f).toFloat()
                
                val angle = Math.random() * 2 * Math.PI
                val puSpeed = 8f // Increased from 2f
                val pdx = (cos(angle) * puSpeed).toFloat()
                val pdy = (sin(angle) * puSpeed).toFloat()
                
                newPowerUps.add(PowerUp(
                    id = spawnCount,
                    x = spawnX,
                    y = spawnY,
                    type = puType,
                    dx = pdx,
                    dy = pdy
                ))
            }

            // Update PowerUp positions
            val updatedPowerUps = newPowerUps.map { pu ->
                val nx = pu.x + pu.dx
                val ny = pu.y + pu.dy
                var ndx = pu.dx
                var ndy = pu.dy

                val puWidthPx = pu.widthDp * pixelDensity
                val puHeightPx = pu.heightDp * pixelDensity

                if (nx < puWidthPx / 2f || nx > screenWidthPx - puWidthPx / 2f) ndx = -ndx
                if (ny < puHeightPx / 2f || ny > screenHeightPx - puHeightPx / 2f) ndy = -ndy

                pu.copy(x = nx, y = ny, dx = ndx, dy = ndy)
            }

            val spawnInterval = max(500L, 2000L - (newWave - 1) * 100L)
            if (timeSinceLastSpawn >= spawnInterval && newEnemies.size < 15) {
                spawnCount++
                newEnemies.add(Enemy.createRandomSpawn(
                    id = System.currentTimeMillis() + spawnCount,
                    screenWidthPx = screenWidthPx,
                    screenHeightPx = screenHeightPx,
                    pixelDensity = pixelDensity,
                    currentEnemies = newEnemies
                ))
                timeSinceLastSpawn = 0L
            }

            if (currentTime >= nextSwarmTime) {
                nextSwarmTime = currentTime + 10000L
                spawnCount++ // Ensure epicenter has a unique ID
                val epicenter = Enemy.createRandomSpawn(
                    id = System.currentTimeMillis() + spawnCount, 
                    screenWidthPx = screenWidthPx, 
                    screenHeightPx = screenHeightPx, 
                    pixelDensity = pixelDensity,
                    currentEnemies = newEnemies
                )
                
                val swarmEnemies = (1..4).map { _ ->
                    spawnCount++
                    val swarmType = EnemyType.FAST
                    val swPx = swarmType.widthDp * pixelDensity
                    val shPx = swarmType.heightDp * pixelDensity
                    epicenter.copy(
                        id = System.currentTimeMillis() + spawnCount,
                        x = epicenter.x + (Math.random().toFloat() - 0.5f) * 50f,
                        y = epicenter.y + (Math.random().toFloat() - 0.5f) * 50f,
                        type = swarmType, 
                        hp = swarmType.initialHp, 
                        speed = swarmType.speed, 
                        widthPx = swPx, 
                        heightPx = shPx,
                        hitboxWidthPx = swPx * swarmType.hitboxWidthRatio, // Correct hitbox
                        hitboxHeightPx = shPx * swarmType.hitboxHeightRatio, // Correct hitbox
                        widthDp = swarmType.widthDp,
                        heightDp = swarmType.heightDp,
                        lastHitTime = 0L // Reset to prevent inherited hit flash
                    )
                }
                newEnemies.addAll(swarmEnemies)
            }

            val remainingProjectiles = mutableListOf<Projectile>()
            val charLeft = characterX - characterHitboxWidthPx / 2f
            val charRight = characterX + characterHitboxWidthPx / 2f
            val charTop = characterY - characterHitboxHeightPx / 2f
            val charBottom = characterY + characterHitboxHeightPx / 2f
            val shooterStopDist = 450f * pixelDensity

            s.projectiles.forEach { p ->
                val pnx = p.x + p.dx
                val pny = p.y + p.dy
                val pRadius = (p.widthDp / 2f) * pixelDensity

                if (pnx + pRadius > charLeft && pnx - pRadius < charRight &&
                    pny + pRadius > charTop && pny - pRadius < charBottom) {
                    if (currentTime > newPlayerImmuneUntil) {
                        newHp -= 15
                        newPlayerImmuneUntil = currentTime + 1000L
                        newComboCount = 0
                        newIsNextSlashRed = false
                        SoundManager.playSFX("takedamage")
                    }
                } else if (pnx > -200f && pnx < screenWidthPx + 200f && pny > -200f && pny < screenHeightPx + 200f) {
                    remainingProjectiles.add(p.copy(x = pnx, y = pny))
                }
            }

            val nextEnemiesList = mutableListOf<Enemy>()
            newEnemies.forEach { enemy ->
                val dx = characterX - enemy.x
                val dy = characterY - enemy.y
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                var nx = enemy.x
                var ny = enemy.y
                var updatedLastAttackTime = enemy.lastAttackTime

                val isInsideScreen = enemy.x >= enemy.widthPx / 2f &&
                        enemy.x <= screenWidthPx - enemy.widthPx / 2f &&
                        enemy.y >= enemy.heightPx / 2f &&
                        enemy.y <= screenHeightPx - enemy.heightPx / 2f

                if (enemy.type == EnemyType.SHOOTING && dist <= shooterStopDist && isInsideScreen) {
                    if (currentTime - enemy.lastAttackTime > 4000L) {
                        updatedLastAttackTime = currentTime
                        val projSpeed = 8f // Increased from 4f
                        projectileIdCounter++
                        remainingProjectiles.add(Projectile(
                            id = projectileIdCounter,
                            x = enemy.x, y = enemy.y,
                            dx = if (dist > 0) (dx/dist) * projSpeed else 0f, 
                            dy = if (dist > 0) (dy/dist) * projSpeed else 0f
                        ))
                    }
                } else if (dist > 0) {
                    val actualSpeed = if (currentTime < s.enemySlowUntil) 1f else enemy.speed
                    nx += (dx/dist) * actualSpeed
                    ny += (dy/dist) * actualSpeed
                }

                if (nx + enemy.widthPx / 2f > charLeft && nx - enemy.widthPx / 2f < charRight &&
                    ny + enemy.heightPx / 2f > charTop && ny - enemy.heightPx / 2f < charBottom) {
                    nx = enemy.x
                    ny = enemy.y
                    if (currentTime > newPlayerImmuneUntil) {
                        newHp -= 25
                        newPlayerImmuneUntil = currentTime + 1000L
                        newComboCount = 0
                        newIsNextSlashRed = false
                        SoundManager.playSFX("takedamage")
                    }
                }
                nextEnemiesList.add(enemy.copy(x = nx, y = ny, lastAttackTime = updatedLastAttackTime))
            }

            // Separated collision to avoid ConcurrentModification/Mutation issues with immutable objects
            val collidedEnemies = nextEnemiesList.toMutableList()
            for (i in collidedEnemies.indices) {
                var e1 = collidedEnemies[i]
                for (j in i + 1 until collidedEnemies.size) {
                    var e2 = collidedEnemies[j]
                    val minDist = (e1.hitboxWidthPx + e2.hitboxWidthPx) / 2f
                    val edx = e2.x - e1.x
                    val edy = e2.y - e1.y
                    val distSq = edx * edx + edy * edy
                    
                    if (distSq < minDist * minDist && distSq > 0.01f) {
                        val distance = sqrt(distSq.toDouble()).toFloat()
                        val overlap = (minDist - distance) / distance * 0.5f
                        val pushX = edx * overlap
                        val pushY = edy * overlap
                        
                        e1 = e1.copy(x = e1.x - pushX, y = e1.y - pushY)
                        e2 = e2.copy(x = e2.x + pushX, y = e2.y + pushY)
                        collidedEnemies[i] = e1
                        collidedEnemies[j] = e2
                    }
                }
            }

            s.copy(
                hp = max(0, newHp),
                stamina = newStamina,
                enemies = collidedEnemies,
                projectiles = remainingProjectiles,
                fadingSlashes = newFadingSlashes,
                fadingEnemies = newFadingEnemies,
                ultimateGauge = newUltimateGauge,
                comboCount = newComboCount,
                isNextSlashRed = newIsNextSlashRed,
                playerImmuneUntil = newPlayerImmuneUntil,
                isUltimateActive = newIsUltimateActive,
                isGameOver = newHp <= 0,
                wave = newWave,
                enemiesKilledInWave = newEnemiesKilledInWave,
                targetKillsForWave = newTargetKillsForWave,
                powerUps = updatedPowerUps,
                inventory = s.inventory,
                infiniteStaminaUntil = s.infiniteStaminaUntil,
                enemySlowUntil = s.enemySlowUntil
            )
        }
        
        if (newState.isGameOver && !prevState.isGameOver) {
            SoundManager.playSFX("bomb")
            saveHighScore(newState.wave)
        }
        
        // After state update, check for boss presence to manage music
        val s = _state.value
        val hasBoss = s.enemies.any { it.type == EnemyType.BOSS }
        if (!hasBoss && !s.isGameOver) {
            SoundManager.stopBossMusic()
        }
    }

    fun usePowerUp(type: PowerUpType) {
        val currentTime = System.currentTimeMillis()
        _state.update { s ->
            val index = s.inventory.indexOf(type)
            if (index == -1) return@update s
            
            val newInventory = s.inventory.toMutableList()
            newInventory.removeAt(index)
            SoundManager.playSFX("use")
            
            when (type) {
                PowerUpType.CAT_CAN -> {
                    s.copy(hp = min(100, s.hp + 40), inventory = newInventory)
                }
                PowerUpType.CAT_BAR -> {
                    s.copy(infiniteStaminaUntil = currentTime + 5000L, inventory = newInventory)
                }
                PowerUpType.TIME_STOP -> {
                    s.copy(enemySlowUntil = currentTime + 5000L, inventory = newInventory)
                }
            }
        }
    }

    fun onSlashStart(offset: Offset) {
        _state.update { 
            if (it.stamina >= 5f) it.copy(slashStart = offset, slashEnd = offset) else it
        }
    }

    fun onSlashDrag(position: Offset) {
        _state.update { s ->
            if (s.slashStart != null && s.slashEnd != null) {
                val dx = position.x - s.slashEnd.x
                val dy = position.y - s.slashEnd.y
                val distance = sqrt(dx * dx + dy * dy)
                val staminaCost = distance * 0.02f
                if (s.stamina >= staminaCost) {
                    s.copy(slashEnd = position, stamina = s.stamina - staminaCost)
                } else {
                    s.copy(stamina = 0f)
                }
            } else s
        }
    }

    fun onSlashEnd() {
        val s = _state.value
        val start = s.slashStart
        val end = s.slashEnd
        if (start != null && end != null) {
            val dx = end.x - start.x; val dy = end.y - start.y
            val distSq = dx*dx + dy*dy
            if (distSq > 2500f) {
                SoundManager.playSFX("slash")
                performSlash(start, end)
            }
        }
        _state.update { it.copy(slashStart = null, slashEnd = null) }
    }

    private fun performSlash(start: Offset, end: Offset) {
        val currentTime = System.currentTimeMillis()
        _state.update { s ->
            val wasRed = s.isNextSlashRed
            val isUltSlash = s.isUltimateActive
            
            val slashesToApply = mutableListOf<Pair<Offset, Offset>>()
            slashesToApply.add(start to end)
            
            if (isUltSlash) {
                val dx = end.x - start.x
                val dy = end.y - start.y
                val angle = 0.6f
                val cosA = cos(angle.toDouble()).toFloat()
                val sinA = sin(angle.toDouble()).toFloat()
                
                val dx1 = dx * cosA - dy * sinA
                val dy1 = dx * sinA + dy * cosA
                val dx3 = dx * cosA + dy * sinA
                val dy3 = -dx * sinA + dy * cosA
                
                slashesToApply.add(start to Offset(start.x + dx1, start.y + dy1))
                slashesToApply.add(start to Offset(start.x + dx3, start.y + dy3))
            }

            val newFadingSlashes = s.fadingSlashes + slashesToApply.map { (st, en) ->
                FadingSlash(st, en, currentTime, isRed = wasRed, isGold = isUltSlash)
            }
            
            var killedInThisSlash = 0
            val nextEnemies = mutableListOf<Enemy>()
            val newFadingEnemies = s.fadingEnemies.toMutableList()
            
            var newEnemiesKilledInWave = s.enemiesKilledInWave

            s.enemies.forEach { enemy ->
                val isHit = slashesToApply.any { (st, en) ->
                    isLineIntersectingRect(st, en, enemy.x - enemy.hitboxWidthPx / 2, enemy.y - enemy.hitboxHeightPx / 2, enemy.hitboxWidthPx, enemy.hitboxHeightPx)
                }
                
                if (isHit && currentTime - enemy.lastHitTime > 300) {
                    val dmg = if (isUltSlash) 10 else 1
                    val newEnemyHp = enemy.hp - dmg
                    if (newEnemyHp <= 0) {
                        killedInThisSlash++
                        newEnemiesKilledInWave++
                        newFadingEnemies.add(FadingEnemy(enemy, currentTime))
                        if (enemy.type == EnemyType.BOSS) {
                            SoundManager.playSFX("mama")
                        }
                    } else {
                        nextEnemies.add(enemy.copy(hp = newEnemyHp, lastHitTime = currentTime))
                    }
                } else {
                    nextEnemies.add(enemy)
                }
            }
            
            val nextProjectiles = s.projectiles.filterNot { p ->
                slashesToApply.any { (st, en) ->
                    val ddx = en.x - st.x; val ddy = en.y - st.y
                    val ll2 = ddx*ddx + ddy*ddy
                    var t = ((p.x - st.x) * ddx + (p.y - st.y) * ddy) / ll2
                    t = if (ll2 > 0) max(0f, min(1f, t)) else 0f
                    val projX = st.x + t * ddx; val projY = st.y + t * ddy
                    val distToProjSq = (p.x - projX) * (p.x - projX) + (p.y - projY) * (p.y - projY)
                    val pRadius = (p.widthDp / 2f) * pixelDensity
                    distToProjSq <= pRadius * pRadius * 4
                }
            }

            var newComboCount = s.comboCount
            var newUltimateGauge = s.ultimateGauge
            var newStamina = s.stamina
            
            if (killedInThisSlash > 0) {
                lastEnemyHitTime = currentTime
                newComboCount += killedInThisSlash
                newUltimateGauge = min(100f, newUltimateGauge + (2f * killedInThisSlash))
                
                if (wasRed) {
                    newStamina = min(100f, newStamina + (20f * killedInThisSlash))
                }
            }

            s.copy(
                enemies = nextEnemies,
                projectiles = nextProjectiles,
                fadingSlashes = newFadingSlashes,
                fadingEnemies = newFadingEnemies,
                comboCount = newComboCount,
                ultimateGauge = newUltimateGauge,
                stamina = newStamina,
                isNextSlashRed = (newComboCount > 0 && newComboCount % 10 == 0),
                isUltimateActive = false,
                enemiesKilledInWave = newEnemiesKilledInWave,
                powerUps = s.powerUps.filterNot { pu ->
                    slashesToApply.any { (st, en) ->
                        isLineIntersectingRect(st, en, pu.x - (pu.widthDp * pixelDensity) / 2, pu.y - (pu.heightDp * pixelDensity) / 2, pu.widthDp * pixelDensity, pu.heightDp * pixelDensity)
                    }
                },
                inventory = if (s.inventory.isEmpty()) {
                    val hitPowerUps = s.powerUps.filter { pu ->
                        slashesToApply.any { (st, en) ->
                            isLineIntersectingRect(st, en, pu.x - (pu.widthDp * pixelDensity) / 2, pu.y - (pu.heightDp * pixelDensity) / 2, pu.widthDp * pixelDensity, pu.heightDp * pixelDensity)
                        }
                    }
                    if (hitPowerUps.isNotEmpty()) listOf(hitPowerUps.last().type) else s.inventory
                } else s.inventory
            ).also {
                if (s.isUltimateActive) {
                    SoundManager.stopUltMusic()
                }
            }
        }
    }

    fun onSlashCancel() {
        _state.update { it.copy(slashStart = null, slashEnd = null) }
    }

    fun pauseGame() { _state.update { it.copy(isPaused = true) } }
    fun resumeGame() { _state.update { it.copy(isPaused = false) } }
    fun restartGame() { 
        SoundManager.stopAllMusic()
        _state.value = GameState()
        spawnCount = 0L
        timeSinceLastSpawn = 0L
        startGameLoop()
    }

    fun activateUltimate() {
        _state.update { s ->
            if (!s.isUltimateActive && s.ultimateGauge >= 100f) {
                SoundManager.playUltMusic()
                s.copy(isUltimateActive = true, ultimateGauge = 0f, stamina = min(100f, s.stamina + 50f))
            } else s
        }
    }

    private fun saveHighScore(wave: Int) {
        val context = SoundManager.getContext() ?: return
        val prefs = context.getSharedPreferences("SweepNekoPrefs", Context.MODE_PRIVATE)
        val currentHigh = prefs.getInt("high_score_wave", 1)
        if (wave > currentHigh) {
            prefs.edit {
                putInt("high_score_wave", wave)
            }
        }
    }

    private fun isLineIntersectingRect(l1: Offset, l2: Offset, rx: Float, ry: Float, rw: Float, rh: Float): Boolean {
        if (l1.x > rx && l1.x < rx + rw && l1.y > ry && l1.y < ry + rh) return true
        if (l2.x > rx && l2.x < rx + rw && l2.y > ry && l2.y < ry + rh) return true
        
        return lineLine(l1.x, l1.y, l2.x, l2.y, rx, ry, rx + rw, ry) ||
               lineLine(l1.x, l1.y, l2.x, l2.y, rx + rw, ry, rx + rw, ry + rh) ||
               lineLine(l1.x, l1.y, l2.x, l2.y, rx + rw, ry + rh, rx, ry + rh) ||
               lineLine(l1.x, l1.y, l2.x, l2.y, rx, ry + rh, rx, ry)
    }

    private fun lineLine(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float): Boolean {
        val denominator = ((y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1))
        if (denominator == 0f) return false
        val uA = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denominator
        val uB = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denominator
        return uA in 0f..1f && uB in 0f..1f
    }
}
