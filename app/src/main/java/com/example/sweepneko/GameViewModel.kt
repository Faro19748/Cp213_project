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
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.sin

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
        _state.update { s ->
            if (s.isGameOver || s.isPaused) return@update s

            var newHp = s.hp
            var newStamina = s.stamina
            var newUltimateGauge = s.ultimateGauge
            var newComboCount = s.comboCount
            var newIsNextSlashRed = s.isNextSlashRed
            var newPlayerImmuneUntil = s.playerImmuneUntil
            var newIsUltimateActive = s.isUltimateActive

            if (newComboCount > 0 && currentTime - lastEnemyHitTime > 3000L) {
                newComboCount = 0
                newIsNextSlashRed = false
            }

            if (s.slashStart == null && newStamina < 100f) {
                newStamina = min(100f, newStamina + (dt * 10f) / 1000f)
            }

            val newFadingSlashes = s.fadingSlashes.filter { currentTime - it.startTime < 400 }
            val newFadingEnemies = s.fadingEnemies.filter { currentTime - it.deathTime < 1000 }

            val newEnemies = s.enemies.toMutableList()
            timeSinceLastSpawn += dt
            if (timeSinceLastSpawn >= Enemy.SPAWN_INTERVAL_MS && newEnemies.size < 15) {
                spawnCount++
                newEnemies.add(Enemy.createRandomSpawn(
                    id = spawnCount,
                    screenWidthPx = screenWidthPx,
                    screenHeightPx = screenHeightPx,
                    pixelDensity = pixelDensity,
                    currentEnemies = newEnemies
                ))
                timeSinceLastSpawn = 0L
            }

            if (currentTime >= nextSwarmTime) {
                nextSwarmTime = currentTime + 10000L
                val epicenter = Enemy.createRandomSpawn(
                    id = spawnCount, 
                    screenWidthPx = screenWidthPx, 
                    screenHeightPx = screenHeightPx, 
                    pixelDensity = pixelDensity,
                    currentEnemies = newEnemies
                )
                
                val swarmEnemies = (1..4).map { _ ->
                    spawnCount++
                    epicenter.copy(
                        id = spawnCount,
                        x = epicenter.x + (Math.random().toFloat() - 0.5f) * 50f,
                        y = epicenter.y + (Math.random().toFloat() - 0.5f) * 50f,
                        type = EnemyType.FAST, 
                        hp = EnemyType.FAST.initialHp, 
                        speed = EnemyType.FAST.speed, 
                        widthPx = EnemyType.FAST.widthDp * pixelDensity, 
                        heightPx = EnemyType.FAST.heightDp * pixelDensity,
                        widthDp = EnemyType.FAST.widthDp,
                        heightDp = EnemyType.FAST.heightDp
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
                        newPlayerImmuneUntil = currentTime + 3000L
                        newComboCount = 0
                        newIsNextSlashRed = false
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
                        val projSpeed = 4f
                        projectileIdCounter++
                        remainingProjectiles.add(Projectile(
                            id = projectileIdCounter,
                            x = enemy.x, y = enemy.y,
                            dx = if (dist > 0) (dx/dist) * projSpeed else 0f, 
                            dy = if (dist > 0) (dy/dist) * projSpeed else 0f
                        ))
                    }
                } else if (dist > 0) {
                    nx += (dx/dist) * enemy.speed
                    ny += (dy/dist) * enemy.speed
                }

                if (nx + enemy.widthPx / 2f > charLeft && nx - enemy.widthPx / 2f < charRight &&
                    ny + enemy.heightPx / 2f > charTop && ny - enemy.heightPx / 2f < charBottom) {
                    nx = enemy.x
                    ny = enemy.y
                    if (currentTime > newPlayerImmuneUntil) {
                        newHp -= 25
                        newPlayerImmuneUntil = currentTime + 3000L
                        newComboCount = 0
                        newIsNextSlashRed = false
                    }
                }
                nextEnemiesList.add(enemy.copy(x = nx, y = ny, lastAttackTime = updatedLastAttackTime))
            }

            for (i in nextEnemiesList.indices) {
                val e1 = nextEnemiesList[i]
                for (j in i + 1 until nextEnemiesList.size) {
                    val e2 = nextEnemiesList[j]
                    val minDist = (e1.widthPx + e2.widthPx) / 2f
                    val edx = e2.x - e1.x
                    val edy = e2.y - e1.y
                    val distSq = edx * edx + edy * edy
                    
                    if (distSq < minDist * minDist && distSq > 0.01f) {
                        val distance = sqrt(distSq.toDouble()).toFloat()
                        val overlap = (minDist - distance) / distance * 0.5f
                        val pushX = edx * overlap
                        val pushY = edy * overlap
                        
                        e1.x -= pushX
                        e1.y -= pushY
                        e2.x += pushX
                        e2.y += pushY
                    }
                }
            }

            s.copy(
                hp = max(0, newHp),
                stamina = newStamina,
                enemies = nextEnemiesList,
                projectiles = remainingProjectiles,
                fadingSlashes = newFadingSlashes,
                fadingEnemies = newFadingEnemies,
                ultimateGauge = newUltimateGauge,
                comboCount = newComboCount,
                isNextSlashRed = newIsNextSlashRed,
                playerImmuneUntil = newPlayerImmuneUntil,
                isUltimateActive = newIsUltimateActive,
                isGameOver = newHp <= 0
            )
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
            
            s.enemies.forEach { enemy ->
                val isHit = slashesToApply.any { (st, en) ->
                    isLineIntersectingRect(st, en, enemy.x - enemy.widthPx / 2, enemy.y - enemy.heightPx / 2, enemy.widthPx, enemy.heightPx)
                }
                
                if (isHit && currentTime - enemy.lastHitTime > 300) {
                    val dmg = if (isUltSlash) 10 else 1
                    val newEnemyHp = enemy.hp - dmg
                    if (newEnemyHp <= 0) {
                        killedInThisSlash++
                        newFadingEnemies.add(FadingEnemy(enemy, currentTime))
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
                isNextSlashRed = (newComboCount > 0 && newComboCount % 5 == 0),
                isUltimateActive = false
            )
        }
    }

    fun onSlashCancel() {
        _state.update { it.copy(slashStart = null, slashEnd = null) }
    }

    fun pauseGame() { _state.update { it.copy(isPaused = true) } }
    fun resumeGame() { _state.update { it.copy(isPaused = false) } }
    fun restartGame() { 
        _state.value = GameState()
        spawnCount = 0L
        timeSinceLastSpawn = 0L
        startGameLoop()
    }

    fun activateUltimate() {
        _state.update { s ->
            if (!s.isUltimateActive && s.ultimateGauge >= 100f) {
                s.copy(isUltimateActive = true, ultimateGauge = 0f, stamina = min(100f, s.stamina + 50f))
            } else s
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
