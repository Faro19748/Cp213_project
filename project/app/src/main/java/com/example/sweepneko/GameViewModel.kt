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
    private var lastC4SpawnTime = 0L

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
        val startTime = System.currentTimeMillis()
        lastC4SpawnTime = startTime
        gameLoopJob = viewModelScope.launch(Dispatchers.Default) {
            var lastFrameTime = System.nanoTime()
            nextSwarmTime = startTime + 10000L
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
            var newShakeTriggerTime = s.shakeTriggerTime
            var newShakeIntensity = s.shakeIntensity

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
            val newFadingBombs = s.fadingBombs.filter { currentTime - it.startTime < 1000 }

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
            
            // C4 Logic
            val filteredC4s = s.c4s.filter { currentTime - it.spawnTime < 10000L }
            if (s.c4s.isNotEmpty() && filteredC4s.isEmpty()) {
                lastC4SpawnTime = currentTime
            }

            val updatedC4s = filteredC4s.map { c4 ->
                val w = c4.widthDp * pixelDensity
                val h = c4.heightDp * pixelDensity
                var nx = c4.x + c4.dx
                var ny = c4.y + c4.dy
                var ndx = c4.dx
                var ndy = c4.dy
                
                if ((nx < w / 2f && ndx < 0) || (nx > screenWidthPx - w / 2f && ndx > 0)) ndx = -ndx
                if ((ny < h / 2f && ndy < 0) || (ny > screenHeightPx - h / 2f && ndy > 0)) ndy = -ndy
                
                c4.copy(x = nx, y = ny, dx = ndx, dy = ndy)
            }
            
            val finalC4s = if (updatedC4s.isEmpty() && currentTime - lastC4SpawnTime > 20000L) {
                lastC4SpawnTime = currentTime
                val w = 80f * pixelDensity
                val h = 80f * pixelDensity
                val spawn = getRandomEdgeSpawn(screenWidthPx, screenHeightPx, w, h, 10f)
                val newList = updatedC4s.toMutableList()
                newList.add(C4Hazard(
                    id = System.currentTimeMillis(),
                    x = spawn.x,
                    y = spawn.y,
                    dx = spawn.dx,
                    dy = spawn.dy,
                    spawnTime = currentTime
                ))
                newList
            } else updatedC4s

            // PowerUp spawning with spawnTime
            val basePowerUps = if (newWave > s.wave) {
                spawnCount++
                val puType = PowerUpType.entries.random()
                val puWidthPx = 60f * pixelDensity
                val puHeightPx = 60f * pixelDensity
                val spawn = getRandomEdgeSpawn(screenWidthPx, screenHeightPx, puWidthPx, puHeightPx, 8f)
                val newList = newPowerUps.toMutableList()
                newList.add(PowerUp(id = spawnCount, x = spawn.x, y = spawn.y, type = puType, dx = spawn.dx, dy = spawn.dy, spawnTime = currentTime))
                newList
            } else newPowerUps

            // Filter PowerUps by lifetime (10 seconds)
            val alivePowerUps = basePowerUps.filter { currentTime - it.spawnTime < 10000L }

            val updatedPowerUps = alivePowerUps.map { pu ->
                val puWidthPx = pu.widthDp * pixelDensity
                val puHeightPx = pu.heightDp * pixelDensity
                var nx = pu.x + pu.dx
                var ny = pu.y + pu.dy
                var ndx = pu.dx
                var ndy = pu.dy
                if ((nx < puWidthPx / 2f && ndx < 0) || (nx > screenWidthPx - puWidthPx / 2f && ndx > 0)) ndx = -ndx
                if ((ny < puHeightPx / 2f && ndy < 0) || (ny > screenHeightPx - puHeightPx / 2f && ndy > 0)) ndy = -ndy
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
            val projectileSpeedMult = if (currentTime < s.enemySlowUntil) 0.125f else 1f

            s.projectiles.forEach { p ->
                val pnx = p.x + p.dx * projectileSpeedMult
                val pny = p.y + p.dy * projectileSpeedMult
                val pRadius = (p.widthDp / 2f) * pixelDensity

                if (pnx + pRadius > charLeft && pnx - pRadius < charRight &&
                    pny + pRadius > charTop && pny - pRadius < charBottom) {
                    if (currentTime > newPlayerImmuneUntil) {
                        newHp -= 15
                        newPlayerImmuneUntil = currentTime + 1000L
                        newComboCount = 0
                        newIsNextSlashRed = false
                        newShakeTriggerTime = currentTime
                        newShakeIntensity = 15f
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
                        newShakeTriggerTime = currentTime
                        newShakeIntensity = 25f
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
                fadingBombs = newFadingBombs,
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
                enemySlowUntil = s.enemySlowUntil,
                c4s = finalC4s,
                shakeTriggerTime = if (newHp <= 0 && s.hp > 0) currentTime else newShakeTriggerTime,
                shakeIntensity = if (newHp <= 0 && s.hp > 0) 30f else newShakeIntensity,
                lastDamageTime = if (newHp < s.hp) currentTime else s.lastDamageTime
            )
        }
        
        if (newState.isGameOver && !prevState.isGameOver) {
            SoundManager.playSFX("bomb")
            saveHighScore(newState.wave, newState.maxComboInRun)
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
            // Minimum slash length: 200 pixels (200^2 = 40000)
            if (distSq > 80000f) {
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
            var hitsInThisSlash = 0
            val nextEnemies = mutableListOf<Enemy>()
            val newFadingEnemies = s.fadingEnemies.toMutableList()
            val newFadingBombs = s.fadingBombs.toMutableList()
            
            var newEnemiesKilledInWave = s.enemiesKilledInWave
            var c4Hit = false

            s.c4s.forEach { c4 ->
                val cw = c4.widthDp * pixelDensity
                val ch = c4.heightDp * pixelDensity
                val cx = c4.x - cw / 2
                val cy = c4.y - ch / 2
                
                val isHit = slashesToApply.any { (st, en) ->
                    // High-performance AABB check
                    val minLX = if (st.x < en.x) st.x else en.x
                    val maxLX = if (st.x > en.x) st.x else en.x
                    val minLY = if (st.y < en.y) st.y else en.y
                    val maxLY = if (st.y > en.y) st.y else en.y
                    
                    if (maxLX < cx || minLX > cx + cw || maxLY < cy || minLY > cy + ch) false
                    else isLineIntersectingRect(st, en, cx, cy, cw, ch)
                }
                if (isHit) {
                    c4Hit = true
                    newFadingBombs.add(FadingBomb(c4.x, c4.y, currentTime, c4.widthDp * 2.5f, c4.heightDp * 2.5f))
                }
            }

            s.enemies.forEach { enemy ->
                val ex = enemy.x - enemy.hitboxWidthPx / 2
                val ey = enemy.y - enemy.hitboxHeightPx / 2
                val ew = enemy.hitboxWidthPx
                val eh = enemy.hitboxHeightPx

                val isHit = slashesToApply.any { (st, en) ->
                    // High-performance AABB check
                    val minLX = if (st.x < en.x) st.x else en.x
                    val maxLX = if (st.x > en.x) st.x else en.x
                    val minLY = if (st.y < en.y) st.y else en.y
                    val maxLY = if (st.y > en.y) st.y else en.y
                    
                    if (maxLX < ex || minLX > ex + ew || maxLY < ey || minLY > ey + eh) false
                    else isLineIntersectingRect(st, en, ex, ey, ew, eh)
                }
                
                if (isHit && currentTime - enemy.lastHitTime > 300) {
                    hitsInThisSlash++
                    // Boss takes only 1 damage even from Ultimate
                    val dmg = if (isUltSlash && enemy.type != EnemyType.BOSS) 10 else 1
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
            var newMaxCombo = s.maxComboInRun
            var newUltimateGauge = s.ultimateGauge
            var newStamina = s.stamina
            var newHpValue = s.hp
            var newShakeTriggerTime = s.shakeTriggerTime
            var newShakeIntensity = s.shakeIntensity

            if (c4Hit) {
                SoundManager.playSFX("bomb")
                lastC4SpawnTime = currentTime
                newHpValue = max(0, newHpValue - 30)
                newShakeTriggerTime = currentTime
                newShakeIntensity = 30f
                newComboCount = 0
            }
            
            if (hitsInThisSlash > 0) {
                lastEnemyHitTime = currentTime
                newComboCount += hitsInThisSlash
                if (newComboCount > newMaxCombo) {
                    newMaxCombo = newComboCount
                }
                newUltimateGauge = min(100f, newUltimateGauge + (2f * hitsInThisSlash))
                
                if (wasRed) {
                    newStamina = min(100f, newStamina + (20f * hitsInThisSlash))
                }
            }

            val finalLastDamageTime = if (newHpValue < s.hp) currentTime else s.lastDamageTime

            s.copy(
                hp = newHpValue,
                enemies = nextEnemies,
                projectiles = nextProjectiles,
                fadingSlashes = newFadingSlashes,
                fadingEnemies = newFadingEnemies,
                fadingBombs = newFadingBombs,
                comboCount = newComboCount,
                maxComboInRun = newMaxCombo,
                ultimateGauge = newUltimateGauge,
                stamina = newStamina,
                isNextSlashRed = (newComboCount > 0 && newComboCount % 10 == 0),
                isUltimateActive = false,
                enemiesKilledInWave = newEnemiesKilledInWave,
                shakeTriggerTime = when {
                    isUltSlash -> currentTime
                    wasRed -> currentTime
                    else -> newShakeTriggerTime
                },
                shakeIntensity = when {
                    isUltSlash -> 20f
                    wasRed -> 15f
                    else -> newShakeIntensity
                },
                lastDamageTime = finalLastDamageTime,
                c4s = s.c4s.filter { c4 ->
                    !slashesToApply.any { (st, en) ->
                        isLineIntersectingRect(st, en, c4.x - (c4.widthDp * pixelDensity) / 2, c4.y - (c4.heightDp * pixelDensity) / 2, c4.widthDp * pixelDensity, c4.heightDp * pixelDensity)
                    }
                },
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
                s.copy(
                    isUltimateActive = true, 
                    ultimateGauge = 0f, 
                    stamina = min(100f, s.stamina + 50f)
                )
            } else s
        }
    }

    fun saveHighScore(wave: Int, combo: Int) {
        val context = SoundManager.getContext() ?: return
        val prefs = context.getSharedPreferences("SweepNekoPrefs", Context.MODE_PRIVATE)
        val currentHighWave = prefs.getInt("high_score_wave", 1)
        val currentHighCombo = prefs.getInt("high_score_combo", 0)
        
        prefs.edit {
            if (wave > currentHighWave) {
                putInt("high_score_wave", wave)
            }
            if (combo > currentHighCombo) {
                putInt("high_score_combo", combo)
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

    private fun getRandomEdgeSpawn(
        screenWidth: Float,
        screenHeight: Float,
        objWidth: Float,
        objHeight: Float,
        speed: Float
    ): SpawnResult {
        val edge = (0..3).random() // 0: Top, 1: Bottom, 2: Left, 3: Right
        var x = 0f
        var y = 0f
        var dx = 0f
        var dy = 0f

        when (edge) {
            0 -> { // Top
                x = (Math.random() * (screenWidth - objWidth) + objWidth / 2).toFloat()
                y = -objHeight / 2
                dx = (Math.random().toFloat() - 0.5f) * speed
                dy = speed
            }
            1 -> { // Bottom
                x = (Math.random() * (screenWidth - objWidth) + objWidth / 2).toFloat()
                y = screenHeight + objHeight / 2
                dx = (Math.random().toFloat() - 0.5f) * speed
                dy = -speed
            }
            2 -> { // Left
                x = -objWidth / 2
                y = (Math.random() * (screenHeight - objHeight) + objHeight / 2).toFloat()
                dx = speed
                dy = (Math.random().toFloat() - 0.5f) * speed
            }
            3 -> { // Right
                x = screenWidth + objWidth / 2
                y = (Math.random() * (screenHeight - objHeight) + objHeight / 2).toFloat()
                dx = -speed
                dy = (Math.random().toFloat() - 0.5f) * speed
            }
        }
        return SpawnResult(x, y, dx, dy)
    }

    private data class SpawnResult(val x: Float, val y: Float, val dx: Float, val dy: Float)
}
