package com.example.sweepneko

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

import android.os.Build.VERSION.SDK_INT
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest

data class FadingSlash(val start: Offset, val end: Offset, val startTime: Long, val isRed: Boolean = false, val isGold: Boolean = false)
data class Projectile(val id: Long, val x: Float, val y: Float, val dx: Float, val dy: Float, val widthDp: Float = 40f, val heightDp: Float = 40f)
data class FadingEnemy(val enemy: Enemy, val deathTime: Long)

@Composable
fun GameScreen(modifier: Modifier = Modifier) {
    var hp by remember { mutableIntStateOf(100) }
    var stamina by remember { mutableFloatStateOf(100f) }
    var enemies by remember { mutableStateOf(listOf<Enemy>()) }
    var projectiles by remember { mutableStateOf(listOf<Projectile>()) }
    var projectileIdCounter by remember { mutableLongStateOf(0L) }
    var playerImmuneUntil by remember { mutableLongStateOf(0L) }
    
    var ultimateGauge by remember { mutableFloatStateOf(0f) }
    var isUltimateActive by remember { mutableStateOf(false) }
    
    var comboCount by remember { mutableIntStateOf(0) }
    var lastEnemyHitTime by remember { mutableLongStateOf(0L) }
    var isNextSlashRed by remember { mutableStateOf(false) }
    
    var slashStart by remember { mutableStateOf<Offset?>(null) }
    var slashEnd by remember { mutableStateOf<Offset?>(null) }
    var fadingSlashes by remember { mutableStateOf(listOf<FadingSlash>()) }
    var fadingEnemies by remember { mutableStateOf(listOf<FadingEnemy>()) }
    
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val activity = LocalActivity.current
    val context = LocalContext.current
    
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val baseSizeDp = min(configuration.screenWidthDp, configuration.screenHeightDp).dp
    val sizechar = baseSizeDp * 0.6f // 60% ของด้านที่แคบกว่า ทำให้ไม่เพี้ยนใน Tablet ยามสัดส่วนและขนาดเปลี่ยน
    val characterWidthDp = sizechar
    val characterHeightDp = sizechar

    val hitboxchar = sizechar * 0.5f // Hitbox ลดลงตามสัดส่วนตัวละคร
    val characterHitboxWidthPx = with(density) { hitboxchar.toPx() }
    val characterHitboxHeightPx = with(density) { hitboxchar.toPx() }
    
    val characterWidthPx = with(density) { characterWidthDp.toPx() }
    val characterHeightPx = with(density) { characterHeightDp.toPx() }
    
    val characterX = screenWidthPx / 2f
    val characterY = screenHeightPx - with(density) { (sizechar * 0.4f).toPx() } // จัดตำแหน่งอิงตามขนาดตัวละคร ไม่ให้ล้นจอ
    
    var spawnCount by remember { mutableLongStateOf(0L) }
    var isPaused by remember { mutableStateOf(false) }

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    // Pre-load painters to prevent stuttering during state changes
    val bulletPainter = rememberAsyncImagePainter(model = ImageRequest.Builder(context).data(R.drawable.bullet).size(coil.size.Size.ORIGINAL).build(), imageLoader = imageLoader)
    val fastEnemyPainter = rememberAsyncImagePainter(model = ImageRequest.Builder(context).data(R.drawable.sm_enemy).size(coil.size.Size.ORIGINAL).build(), imageLoader = imageLoader)
    val bigEnemyPainter = rememberAsyncImagePainter(model = ImageRequest.Builder(context).data(R.drawable.b_enemy).size(coil.size.Size.ORIGINAL).build(), imageLoader = imageLoader)
    val shootEnemyMovePainter = rememberAsyncImagePainter(model = ImageRequest.Builder(context).data(R.drawable.s_enemy).size(coil.size.Size.ORIGINAL).build(), imageLoader = imageLoader)
    val shootEnemyIdlePainter = rememberAsyncImagePainter(model = ImageRequest.Builder(context).data(R.drawable.ss_enemy).size(coil.size.Size.ORIGINAL).build(), imageLoader = imageLoader)
    val normalEnemyPainter = rememberAsyncImagePainter(model = ImageRequest.Builder(context).data(R.drawable.n_enemy).size(coil.size.Size.ORIGINAL).build(), imageLoader = imageLoader)
    val charPainter = rememberAsyncImagePainter(model = ImageRequest.Builder(context).data(R.drawable.b_cat).size(coil.size.Size.ORIGINAL).build(), imageLoader = imageLoader)

    val deadFastPainter = painterResource(id = R.drawable.d_sm_enemy)
    val deadBigPainter = painterResource(id = R.drawable.d_b_enemy)
    val deadShootPainter = painterResource(id = R.drawable.d_s_enemy)
    val deadNormalPainter = painterResource(id = R.drawable.d_n_enemy)

    LaunchedEffect(isPaused, hp > 0) {
        if (isPaused || hp <= 0) return@LaunchedEffect
        var lastFrameTime = System.currentTimeMillis()
        var timeSinceLastSpawn = 0L
        var nextSwarmTime = System.currentTimeMillis() + 10000L

        while (hp > 0) {
            val currentTime = System.currentTimeMillis()
            val dt = currentTime - lastFrameTime
            lastFrameTime = currentTime

            if (!isPaused) {
                if (comboCount > 0 && currentTime - lastEnemyHitTime > 3000L) {
                    comboCount = 0
                    isNextSlashRed = false
                }

                if (stamina < 100f && slashStart == null) {
                    stamina += (dt * 10f) / 1000f
                    if (stamina > 100f) stamina = 100f
                }

                fadingSlashes = fadingSlashes.filter { currentTime - it.startTime < 400 }
                fadingEnemies = fadingEnemies.filter { currentTime - it.deathTime < 1000 }

                timeSinceLastSpawn += dt
                if (timeSinceLastSpawn >= Enemy.SPAWN_INTERVAL_MS) {
                    spawnCount++
                    enemies = enemies + Enemy.createRandomSpawn(
                        id = spawnCount, 
                        screenWidthPx = screenWidthPx, 
                        screenHeightPx = screenHeightPx, 
                        pixelDensity = density.density,
                        currentEnemies = enemies
                    )
                    timeSinceLastSpawn = 0L
                }
                
                // Swarm Logic
                if (currentTime >= nextSwarmTime) {
                    nextSwarmTime = currentTime + 10000L
                    val epicenter = Enemy.createRandomSpawn(
                        id = spawnCount, 
                        screenWidthPx = screenWidthPx, 
                        screenHeightPx = screenHeightPx, 
                        pixelDensity = density.density,
                        currentEnemies = enemies
                    )
                    
                    val swarmEnemies = (1..6).map { _ ->
                        spawnCount++
                        epicenter.copy(
                            id = spawnCount,
                            x = epicenter.x + (Math.random().toFloat() - 0.5f) * 10f,
                            y = epicenter.y + (Math.random().toFloat() - 0.5f) * 10f,
                            type = EnemyType.FAST, 
                            hp = EnemyType.FAST.initialHp, 
                            speed = EnemyType.FAST.speed, 
                            widthPx = EnemyType.FAST.widthDp * density.density, 
                            heightPx = EnemyType.FAST.heightDp * density.density,
                            widthDp = EnemyType.FAST.widthDp,
                            heightDp = EnemyType.FAST.heightDp
                        )
                    }
                    enemies = enemies + swarmEnemies
                }
            
                var newHp = hp
                var frameImmuneUntil = playerImmuneUntil
                val newProjectiles = projectiles.toMutableList()
                val shooterStopDist = with(density) { 450.dp.toPx() }
                
                val newEnemies = enemies.map { enemy ->
                    val dx = characterX - enemy.x
                    val dy = characterY - enemy.y
                    val dist = sqrt((dx*dx + dy*dy).toDouble()).toFloat()
                    
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
                            newProjectiles.add(Projectile(
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
                    
                    val enemyLeft = nx - enemy.widthPx / 2; val enemyRight = nx + enemy.widthPx / 2
                    val enemyTop = ny - enemy.heightPx / 2; val enemyBottom = ny + enemy.heightPx / 2
                    
                    val charLeft = characterX - characterHitboxWidthPx / 2; val charRight = characterX + characterHitboxWidthPx / 2
                    val charTop = characterY - characterHitboxHeightPx / 2; val charBottom = characterY + characterHitboxHeightPx / 2

                    if (enemyRight > charLeft && enemyLeft < charRight &&
                        enemyBottom > charTop && enemyTop < charBottom) {
                        nx = enemy.x; ny = enemy.y
                        if (currentTime > frameImmuneUntil) {
                            newHp -= 25
                            frameImmuneUntil = currentTime + 3000L
                            playerImmuneUntil = frameImmuneUntil
                            comboCount = 0
                            isNextSlashRed = false
                        }
                    }
                    enemy.copy(x = nx, y = ny, lastAttackTime = updatedLastAttackTime)
                }
                
                val remainingProjectiles = mutableListOf<Projectile>()
                for (p in newProjectiles) {
                    val pnx = p.x + p.dx
                    val pny = p.y + p.dy
                    
                    val pRadius = with(density) { (p.widthDp / 2f).dp.toPx() }
                    
                    val charLeft = characterX - characterHitboxWidthPx / 2; val charRight = characterX + characterHitboxWidthPx / 2
                    val charTop = characterY - characterHitboxHeightPx / 2; val charBottom = characterY + characterHitboxHeightPx / 2

                    if (pnx + pRadius > charLeft && pnx - pRadius < charRight &&
                        pny + pRadius > charTop && pny - pRadius < charBottom) {
                        if (currentTime > frameImmuneUntil) {
                            newHp -= 15
                            frameImmuneUntil = currentTime + 3000L
                            playerImmuneUntil = frameImmuneUntil
                            comboCount = 0
                            isNextSlashRed = false
                        }
                    } else if (pnx > -200f && pnx < screenWidthPx + 200f && pny > -200f && pny < screenHeightPx + 200f) {
                        remainingProjectiles.add(p.copy(x = pnx, y = pny))
                    }
                }
                
                for (i in newEnemies.indices) {
                    for (j in i + 1 until newEnemies.size) {
                        val e1 = newEnemies[i]; val e2 = newEnemies[j]
                        val minDist = (e1.widthPx + e2.widthPx) / 2f
                        val dx = e2.x - e1.x; val dy = e2.y - e1.y
                        val dist2 = dx*dx + dy*dy
                        if (dist2 < minDist * minDist && dist2 > 0) {
                            val distance = sqrt(dist2.toDouble()).toFloat()
                            val overlap = minDist - distance
                            val pushX = (dx/distance) * overlap * 0.5f; val pushY = (dy/distance) * overlap * 0.5f
                            e1.x -= pushX; e1.y -= pushY; e2.x += pushX; e2.y += pushY
                        }
                    }
                }
                hp = newHp
                enemies = newEnemies
                projectiles = remainingProjectiles
            }
            delay(16)
        }
    }

    Box(modifier = modifier
        .fillMaxSize()
        .systemGestureExclusion()
        .background(Color(0xFFDCF0C3)) // Set background color to #DCF0C3 
        .pointerInput(isPaused) {
            if (isPaused) return@pointerInput
            detectDragGestures(
                onDragStart = { offset ->
                    if (stamina >= 5f) { slashStart = offset; slashEnd = offset }
                },
                onDrag = { change, dragAmount ->
                    if (slashStart != null && slashEnd != null) {
                        val distance = sqrt(dragAmount.x * dragAmount.x + dragAmount.y * dragAmount.y)
                        val staminaCost = distance * 0.02f
                        if (stamina >= staminaCost) {
                            stamina -= staminaCost
                            slashEnd = change.position
                        } else { stamina = 0f }
                    }
                },
                onDragEnd = {
                    val start = slashStart; val end = slashEnd
                    if (start != null && end != null) {
                        val dx = end.x - start.x; val dy = end.y - start.y
                        val distSq = dx*dx + dy*dy
                        if (distSq > 2500f) {
                            val currentTime = System.currentTimeMillis()
                            val wasRed = isNextSlashRed
                            if (wasRed) {
                                isNextSlashRed = false
                            }
                            
                            val isUltSlash = isUltimateActive
                            if (isUltSlash) {
                                isUltimateActive = false // Consume ultimate buff
                            }
                            
                            val slashesToApply = mutableListOf<Pair<Offset, Offset>>()
                            slashesToApply.add(start to end)
                            
                            if (isUltSlash) {
                                val angle = 0.6f
                                val cosA = kotlin.math.cos(angle.toDouble()).toFloat()
                                val sinA = kotlin.math.sin(angle.toDouble()).toFloat()
                                
                                val dx1 = dx * cosA - dy * sinA
                                val dy1 = dx * sinA + dy * cosA
                                
                                val dx3 = dx * cosA + dy * sinA
                                val dy3 = -dx * sinA + dy * cosA
                                
                                slashesToApply.add(start to Offset(start.x + dx1, start.y + dy1))
                                slashesToApply.add(start to Offset(start.x + dx3, start.y + dy3))
                            }

                            fadingSlashes = fadingSlashes + slashesToApply.map { (s, e) ->
                                FadingSlash(s, e, currentTime, isRed = wasRed, isGold = isUltSlash)
                            }
                            
                            var enemiesHitInThisSlash = 0
                            
                            projectiles = projectiles.filterNot { p ->
                                slashesToApply.any { (s, e) ->
                                    val ddx = e.x - s.x; val ddy = e.y - s.y
                                    val ll2 = ddx*ddx + ddy*ddy
                                    var t = ((p.x - s.x) * ddx + (p.y - s.y) * ddy) / ll2
                                    t = if (ll2 > 0) max(0f, min(1f, t)) else 0f
                                    val projX = s.x + t * ddx; val projY = s.y + t * ddy
                                    val distToProjSq = (p.x - projX) * (p.x - projX) + (p.y - projY) * (p.y - projY)
                                    val pRadius = with(density) { (p.widthDp / 2f).dp.toPx() }
                                    distToProjSq <= pRadius * pRadius * 4
                                }
                            }
                            
                            enemies = enemies.mapNotNull { enemy ->
                                val isHit = slashesToApply.any { (s, e) ->
                                    val ddx = e.x - s.x; val ddy = e.y - s.y
                                    val ll2 = ddx*ddx + ddy*ddy
                                    var t = ((enemy.x - s.x) * ddx + (enemy.y - s.y) * ddy) / ll2
                                    t = if (ll2 > 0) max(0f, min(1f, t)) else 0f
                                    val projX = s.x + t * ddx; val projY = s.y + t * ddy
                                    val distToEnemySq = (enemy.x - projX) * (enemy.x - projX) + (enemy.y - projY) * (enemy.y - projY)
                                    val hitRadius = enemy.widthPx / 2f
                                    distToEnemySq <= hitRadius * hitRadius
                                }
                                
                                if (isHit) {
                                    if (currentTime - enemy.lastHitTime > 300) {
                                        enemiesHitInThisSlash++
                                        val dmg = if (isUltSlash) 10 else 1
                                        val newEnemyHp = enemy.hp - dmg
                                        if (newEnemyHp > 0) {
                                            enemy.copy(hp = newEnemyHp, lastHitTime = currentTime)
                                        } else {
                                            fadingEnemies = fadingEnemies + FadingEnemy(enemy, currentTime)
                                            null
                                        }
                                    } else enemy
                                } else enemy
                            }
                            
                            if (enemiesHitInThisSlash > 0) {
                                lastEnemyHitTime = currentTime
                                repeat(enemiesHitInThisSlash) {
                                    comboCount++
                                    if (comboCount > 0 && comboCount % 10 == 0) {
                                        isNextSlashRed = true
                                    }
                                }
                                ultimateGauge = min(100f, ultimateGauge + (5f * enemiesHitInThisSlash))
                                
                                if (wasRed) {
                                    stamina = min(100f, stamina + (20f * enemiesHitInThisSlash))
                                }
                            }
                        }
                    }
                    slashStart = null; slashEnd = null
                },
                onDragCancel = { slashStart = null; slashEnd = null }
            )
        }
    ) {
        // Projectiles
        projectiles.forEach { p ->
            key(p.id) {
                Image(
                    painter = bulletPainter,
                    contentDescription = null,
                    modifier = Modifier
                        .offset {
                            val pxW = density.density * p.widthDp
                            val pxH = density.density * p.heightDp
                            androidx.compose.ui.unit.IntOffset(
                                (p.x - pxW / 2).toInt(),
                                (p.y - pxH / 2).toInt()
                            )
                        }
                        .size(width = p.widthDp.dp, height = p.heightDp.dp)
                )
            }
        }

        // Enemies
        val shooterStopDistDraw = with(density) { 450.dp.toPx() }
        enemies.forEach { enemy ->
            key(enemy.id) {
                val dx = characterX - enemy.x
                val dy = characterY - enemy.y
                val dist = sqrt((dx*dx + dy*dy).toDouble()).toFloat()
                val isInsideScreen = enemy.x >= enemy.widthPx / 2f && 
                                     enemy.x <= screenWidthPx - enemy.widthPx / 2f && 
                                     enemy.y >= enemy.heightPx / 2f && 
                                     enemy.y <= screenHeightPx - enemy.heightPx / 2f
                val isMoving = !(enemy.type == EnemyType.SHOOTING && dist <= shooterStopDistDraw && isInsideScreen)
                                     
                val painter = when (enemy.type) {
                    EnemyType.FAST -> fastEnemyPainter
                    EnemyType.BIG -> bigEnemyPainter
                    EnemyType.SHOOTING -> if (isMoving) shootEnemyMovePainter else shootEnemyIdlePainter
                    else -> normalEnemyPainter
                }
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .offset {
                            androidx.compose.ui.unit.IntOffset(
                                (enemy.x - enemy.widthPx / 2).toInt(),
                                (enemy.y - enemy.heightPx / 2).toInt()
                            )
                        }
                        .size(width = enemy.widthDp.dp, height = enemy.heightDp.dp)
                )
            }
        }
        
        // Fading (Dead) Enemies
        val currentTimeRender = System.currentTimeMillis()
        fadingEnemies.forEach { fading ->
            key("dead_${fading.enemy.id}") {
                val alpha = max(0f, 1f - (currentTimeRender - fading.deathTime) / 1000f)
                val painter = when (fading.enemy.type) {
                    EnemyType.FAST -> deadFastPainter
                    EnemyType.BIG -> deadBigPainter
                    EnemyType.SHOOTING -> deadShootPainter
                    else -> deadNormalPainter
                }
                Image(
                    painter = painter,
                    contentDescription = null,
                    alpha = alpha,
                    modifier = Modifier
                        .offset {
                            androidx.compose.ui.unit.IntOffset(
                                (fading.enemy.x - fading.enemy.widthPx / 2).toInt(),
                                (fading.enemy.y - fading.enemy.heightPx / 2).toInt()
                            )
                        }
                        .size(width = fading.enemy.widthDp.dp, height = fading.enemy.heightDp.dp)
                )
            }
        }
        
        // Character
        val isImmune = System.currentTimeMillis() < playerImmuneUntil
        Box(
            modifier = Modifier
                .offset {
                    androidx.compose.ui.unit.IntOffset(
                        (characterX - characterWidthPx / 2).toInt(),
                        (characterY - characterHeightPx / 2).toInt()
                    )
                }
                .size(width = characterWidthDp, height = characterHeightDp),
            contentAlignment = Alignment.Center
        ) {
             Image(painter = charPainter, contentDescription = null, modifier = Modifier.fillMaxSize())
             Box(modifier = Modifier.size(hitboxchar).border(2.dp, if (isImmune) Color.Red else Color.Transparent))
        }
        
        if (slashStart != null && slashEnd != null || fadingSlashes.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val drawSlash = { start: Offset, end: Offset, alphaOuter: Float, alphaInner: Float, isRed: Boolean, isGold: Boolean ->
                    val dx = end.x - start.x
                    val dy = end.y - start.y
                    val dist = sqrt(dx*dx + dy*dy)
                    if (dist > 0) {
                        val nx = -dy / dist
                        val ny = dx / dist
                        val midX = (start.x + end.x) / 2f
                        val midY = (start.y + end.y) / 2f
                        
                        val multiplier = if (isGold) 2.2f else if (isRed) 1.8f else 1f
                        val outerDist = min(150f, dist * 0.12f * multiplier)
                        val innerDist = min(25f, dist * 0.02f * multiplier)
                        val coreDist = min(80f, dist * 0.06f * multiplier)
                        val strokeWidth = min(35f, max(5f, dist * 0.02f * multiplier))

                        val cpOuterX = midX + nx * outerDist
                        val cpOuterY = midY + ny * outerDist
                        val cpInnerX = midX + nx * innerDist
                        val cpInnerY = midY + ny * innerDist
                        
                        val pathOuter = Path().apply {
                            moveTo(start.x, start.y)
                            quadraticTo(cpOuterX, cpOuterY, end.x, end.y)
                            quadraticTo(cpInnerX, cpInnerY, start.x, start.y)
                            close()
                        }
                        val coreCpX = midX + nx * coreDist
                        val coreCpY = midY + ny * coreDist
                        val pathCore = Path().apply {
                            moveTo(start.x, start.y)
                            quadraticTo(coreCpX, coreCpY, end.x, end.y)
                        }

                        val outerColor = if (isGold) Color(0xFFFFD700) else if (isRed) Color(0xFFFF1111) else Color(0xFF7AAEE0)
                        val alphaMult = if (isGold) 1.5f else if (isRed) 1.2f else 1f
                        
                        drawPath(
                            path = pathOuter,
                            color = outerColor.copy(alpha = min(1f, alphaOuter * alphaMult)),
                        )
                        drawPath(
                            path = pathCore,
                            color = Color.White.copy(alpha = alphaInner),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        
                        if (isRed || isGold) {
                            val innerGlowPath = Path().apply {
                                moveTo(start.x, start.y)
                                quadraticTo(coreCpX, coreCpY, end.x, end.y)
                            }
                            val glowColor = if (isGold) Color.White else Color(0xFFFFD700)
                            drawPath(
                                path = innerGlowPath,
                                color = glowColor.copy(alpha = alphaInner * 0.8f),
                                style = Stroke(width = strokeWidth * 0.4f, cap = StrokeCap.Round)
                            )
                        }
                    }
                }

                if (slashStart != null && slashEnd != null) {
                    if (isUltimateActive) {
                        val dx = slashEnd!!.x - slashStart!!.x
                        val dy = slashEnd!!.y - slashStart!!.y
                        val angle = 0.6f
                        val cosA = kotlin.math.cos(angle.toDouble()).toFloat()
                        val sinA = kotlin.math.sin(angle.toDouble()).toFloat()
                        val p1x = slashStart!!.x + (dx * cosA - dy * sinA)
                        val p1y = slashStart!!.y + (dx * sinA + dy * cosA)
                        val p2x = slashStart!!.x + (dx * cosA + dy * sinA)
                        val p2y = slashStart!!.y + (-dx * sinA + dy * cosA)
                        
                        drawSlash(slashStart!!, slashEnd!!, 0.9f, 1f, false, true)
                        drawSlash(slashStart!!, Offset(p1x, p1y), 0.9f, 1f, false, true)
                        drawSlash(slashStart!!, Offset(p2x, p2y), 0.9f, 1f, false, true)
                    } else {
                        drawSlash(slashStart!!, slashEnd!!, 0.9f, 1f, isNextSlashRed, false)
                    }
                }
                
                val currentTime = System.currentTimeMillis()
                fadingSlashes.forEach { slash ->
                    val elapsed = currentTime - slash.startTime
                    val progress = elapsed / 400f
                    if (progress in 0f..1f) {
                        val alpha = max(0f, 1f - progress)
                        drawSlash(slash.start, slash.end, alpha * 0.9f, alpha, slash.isRed, slash.isGold)
                    }
                }
            }
        }

        // ใช้ Component จากไฟล์ GameComponents.kt
        HpStaminaBar(hp = hp, stamina = stamina, ultimateGauge = ultimateGauge, comboCount = comboCount, isNextSlashRed = isNextSlashRed)
        
        if (hp <= 0) {
            GameOverMenu(
                onRestart = {
                    hp = 100
                    stamina = 100f
                    enemies = emptyList()
                    playerImmuneUntil = 0L
                    spawnCount = 0L
                    slashStart = null
                    slashEnd = null
                    fadingSlashes = emptyList()
                    fadingEnemies = emptyList()
                    projectiles = emptyList()
                    comboCount = 0
                    lastEnemyHitTime = 0L
                    isNextSlashRed = false
                    ultimateGauge = 0f
                    isUltimateActive = false
                },
                onMenu = { activity?.finish() }
            )
        }

        if (hp > 0 && !isPaused) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Box(
                    modifier = Modifier.align(Alignment.BottomStart),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { ultimateGauge / 100f },
                        modifier = Modifier.size(64.dp),
                        color = Color(0xFFFFD700),
                        strokeWidth = 4.dp,
                        trackColor = Color.Gray.copy(alpha = 0.5f)
                    )
                    FloatingActionButton(
                        onClick = { 
                            if (!isUltimateActive && ultimateGauge >= 100f) {
                                isUltimateActive = true
                                ultimateGauge = 0f
                                
                                // Grant Ultimate 3-Slash Buff & Restore SP 50%
                                stamina = min(100f, stamina + 50f)
                            }
                        }, 
                        containerColor = if (ultimateGauge >= 100f) Color(0xFFFFD700).copy(alpha = 0.9f) else Color.DarkGray.copy(alpha = 0.7f),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Text(
                            "ULT", 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = if (ultimateGauge >= 100f) Color.Black else Color.LightGray
                        )
                    }
                }
                
                FloatingActionButton(
                    onClick = { isPaused = true }, 
                    containerColor = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text("II", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }

        if (isPaused && hp > 0) {
            PauseMenu(onResume = { isPaused = false }, onMenu = { activity?.finish() })
        }
    }
}
