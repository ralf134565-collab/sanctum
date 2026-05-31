// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.mandala.sandbox.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs

class MandalaTuning(
    var gravity: Float = 400f,
    var friction: Float = 0.98f,
    var spawnIntervalMs: Long = 280L,
    var coreFillThreshold: Int = 80,
    var ringThickness: Float = Collision.RING_THICKNESS,
    var breathingSyncEnabled: Boolean = false,
)

class MandalaEngine(
    private val maxParticles: Int = 300,
) {
    val particlePool = ParticlePool(maxParticles)
    val metrics = MandalaMetrics()
    val tuning = MandalaTuning()

    val frameNonce = mutableLongStateOf(0L)

    var phase by mutableStateOf(MandalaPhase.IntroFocus)
        private set

    var introElapsedSec: Float = 0f
        private set

    var phaseElapsedSec: Float = 0f
        private set

    var coreGlowIntensity: Float = 0f
        private set

    val uiSecondTick = mutableLongStateOf(0L)

    var centerX: Float = 0f
        private set

    var centerY: Float = 0f
        private set

    var coreRadius: Float = 48f
        private set

    // Добавляем эффект пульсации при поглощении песчинок
    var corePulseScale: Float = 1.0f
        private set

    // Блуждающий Проводник (Orb)
    var orbX: Float = 0f
        private set

    var orbY: Float = 0f
        private set

    var orbVx: Float = 0f
        private set

    var orbVy: Float = 0f
        private set

    var orbRadius: Float = 24f
        private set

    var isDraggingOrb: Boolean = false
        private set

    val rings = arrayOf(
        RingModel(radius = 150f, gapStartAngle = 45f, gapWidth = 38f),
        RingModel(radius = 260f, gapStartAngle = 180f, gapWidth = 34f),
        RingModel(radius = 370f, gapStartAngle = 290f, gapWidth = 30f),
    )

    val ringPassFlags = BooleanArray(3)
    var rotationHapticIntensity: Float = 0f
        private set

    var coreCapturedFlag = false
        private set

    private var activeDragRingIndex: Int = -1

    fun consumeCoreCapturedFlag(): Boolean {
        val value = coreCapturedFlag
        coreCapturedFlag = false
        return value
    }

    fun startDrag(touchX: Float, touchY: Float) {
        if (phase != MandalaPhase.Playing) {
            activeDragRingIndex = -1
            isDraggingOrb = false
            return
        }
        
        // УВЕЛИЧЕННАЯ ЗОНА КАСАНИЯ (75px вместо 49px) для безупречного попадания пальцем
        val distToOrb = Collision.distanceFromCenter(touchX, touchY, orbX, orbY)
        if (distToOrb <= orbRadius + 50f) {
            activeDragRingIndex = -1
            isDraggingOrb = true
            orbVx = 0f
            orbVy = 0f
            return
        }

        isDraggingOrb = false
        val touchDist = abs(Collision.distanceFromCenter(touchX, touchY, centerX, centerY))
        var closestIndex = 0
        var closestDelta = abs(rings[0].radius - touchDist)
        var index = 1
        while (index < rings.size) {
            val delta = abs(rings[index].radius - touchDist)
            if (delta < closestDelta) {
                closestDelta = delta
                closestIndex = index
            }
            index++
        }
        // Захватываем кольцо только если касание произошло относительно близко к нему (в пределах 60px)
        if (closestDelta < 60f) {
            activeDragRingIndex = closestIndex
        } else {
            activeDragRingIndex = -1
        }
    }

    fun endDrag() {
        activeDragRingIndex = -1
        isDraggingOrb = false
    }

    private var spawnAccumulatorMs: Float = 0f
    private var layoutReady = false
    private var baseGapWidths = FloatArray(3)
    private var lastUiSecond = -1

    init {
        var index = 0
        while (index < rings.size) {
            baseGapWidths[index] = rings[index].gapWidth
            index++
        }
    }

    fun onLayout(width: Float, height: Float) {
        centerX = width * 0.5f
        centerY = height * 0.5f
        
        if (orbX == 0f && orbY == 0f) {
            // Проводник (Orb) спавнится чуть выше центра
            orbX = centerX
            orbY = centerY - 100f
            orbVx = 35f
            orbVy = -25f
        }

        // Outer ring must fit inside the canvas; minDim * 0.56 overflows on portrait (radius > width/2).
        val strokePadding = minOf(width, height) * 0.04f + 8f
        val maxRadius = minOf(
            centerX - strokePadding,
            width - centerX - strokePadding,
            centerY - strokePadding,
            height - centerY - strokePadding,
        ).coerceAtLeast(48f)

        val outerRadius = maxRadius * 0.92f
        rings[0].radius = outerRadius * 0.50f
        rings[1].radius = outerRadius * 0.75f
        rings[2].radius = outerRadius
        coreRadius = outerRadius * 0.16f
        layoutReady = true
    }

    fun skipIntro() {
        if (phase == MandalaPhase.IntroFocus) {
            transitionTo(MandalaPhase.Playing)
        }
    }

    fun restartSession() {
        particlePool.resetAll()
        metrics.reset()
        spawnAccumulatorMs = 0f
        phaseElapsedSec = 0f
        introElapsedSec = 0f
        coreGlowIntensity = 0f
        rotationHapticIntensity = 0f
        lastUiSecond = -1
        uiSecondTick.longValue = 0L
        clearRingPassFlags()
        
        orbX = centerX
        orbY = centerY - 100f
        orbVx = 35f
        orbVy = -25f
        
        var index = 0
        while (index < rings.size) {
            rings[index].currentAngle = 0f
            rings[index].velocity = 0f
            rings[index].alpha = 1f
            rings[index].gapWidth = baseGapWidths[index]
            index++
        }
        transitionTo(MandalaPhase.IntroFocus)
        invalidateDraw()
    }

    fun applyRotationImpulse(touchX: Float, touchY: Float, dragX: Float, dragY: Float) {
        if (phase != MandalaPhase.Playing) return
        
        // Если мы перетаскиваем Проводник (Orb), плавно интерполируем его положение к координатам пальца
        if (isDraggingOrb) {
            // Используем линейную интерполяцию (Lerp) для невероятной плавности движения (тягучести)
            orbX += (touchX - orbX) * 0.18f
            orbY += (touchY - orbY) * 0.18f
            
            // Ограничиваем Проводник внешним кольцом
            val distToCenter = Collision.distanceFromCenter(orbX, orbY, centerX, centerY)
            val maxAllowedRadius = rings[2].radius - orbRadius - 5f
            if (distToCenter > maxAllowedRadius) {
                val nx = (orbX - centerX) / distToCenter
                val ny = (orbY - centerY) / distToCenter
                orbX = centerX + nx * maxAllowedRadius
                orbY = centerY + ny * maxAllowedRadius
            }
            
            // Сохраняем скорость движения пальца для инерционного полета при отпускании
            orbVx = dragX * 50f
            orbVy = dragY * 50f
            return
        }

        if (activeDragRingIndex == -1) return
        val ring = rings[activeDragRingIndex]
        val dx = touchX - centerX
        val dy = touchY - centerY

        val tangentX = -dy
        val tangentY = dx
        val tangentLen = Collision.distanceFromCenter(tangentX, tangentY, 0f, 0f)
        if (tangentLen <= 0.001f) return

        val normalizedTangentX = tangentX / tangentLen
        val normalizedTangentY = tangentY / tangentLen
        val tangentialDrag = dragX * normalizedTangentX + dragY * normalizedTangentY
        ring.velocity += tangentialDrag * 0.18f
        rotationHapticIntensity = abs(tangentialDrag).coerceIn(0f, 1f)
    }

    fun consumeRingPassFlags(): Boolean {
        var any = false
        var index = 0
        while (index < ringPassFlags.size) {
            if (ringPassFlags[index]) {
                any = true
            }
            ringPassFlags[index] = false
            index++
        }
        return any
    }

    fun consumeRotationHapticIntensity(): Float {
        val value = rotationHapticIntensity
        rotationHapticIntensity = 0f
        return value
    }

    fun update(dtSec: Float) {
        if (!layoutReady || dtSec <= 0f) return

        clearRingPassFlags()
        updatePhaseTimers(dtSec)

        when (phase) {
            MandalaPhase.IntroFocus -> Unit
            MandalaPhase.Playing -> updatePlaying(dtSec)
            MandalaPhase.CoreGlow -> updateCoreGlow(dtSec)
            MandalaPhase.WindDestroy -> updateWind(dtSec)
            MandalaPhase.Complete -> Unit
        }

        invalidateDraw()
    }

    private fun updatePhaseTimers(dtSec: Float) {
        phaseElapsedSec += dtSec
        if (phase == MandalaPhase.IntroFocus) {
            introElapsedSec += dtSec
            val second = introElapsedSec.toInt()
            if (second != lastUiSecond) {
                lastUiSecond = second
                uiSecondTick.longValue = second.toLong()
            }
            if (introElapsedSec >= 10f) {
                transitionTo(MandalaPhase.Playing)
            }
        }
    }

    private fun updatePlaying(dtSec: Float) {
        updateRings(dtSec)
        updateBreathingSync()
        
        // Возвращаем пульсацию центрального ядра к исходному размеру
        if (corePulseScale > 1.0f) {
            corePulseScale = (corePulseScale - 1.5f * dtSec).coerceAtLeast(1.0f)
        }
        
        // Обновляем позицию блуждающего Проводника (Orb)
        orbX += orbVx * dtSec
        orbY += orbVy * dtSec
        
        // Применяем вязкое трение воздуха к Проводнику, чтобы он плавно замедлялся до ленивого дрейфа
        orbVx *= 0.975f
        orbVy *= 0.975f
        
        // Если Проводник слишком сильно замедлился, даем ему ленивый толчок в случайную сторону
        val speed = Collision.distanceFromCenter(orbVx, orbVy, 0f, 0f)
        if (speed < 15f) {
            orbVx = FastRandom.nextSignedFloat(35f)
            orbVy = FastRandom.nextSignedFloat(35f)
        }
        
        // Ограничиваем движение Проводника рамками внешнего кольца (rings[2].radius)
        // Чтобы он плавно отскакивал от внешнего контура изнутри
        val distToCenter = Collision.distanceFromCenter(orbX, orbY, centerX, centerY)
        val maxAllowedRadius = rings[2].radius - orbRadius - 10f
        if (distToCenter > maxAllowedRadius) {
            val nx = (orbX - centerX) / distToCenter
            val ny = (orbY - centerY) / distToCenter
            
            // Проецируем Проводник обратно на границу
            orbX = centerX + nx * maxAllowedRadius
            orbY = centerY + ny * maxAllowedRadius
            
            // Отражаем вектор скорости (отскок)
            val dot = orbVx * nx + orbVy * ny
            orbVx = (orbVx - 2f * dot * nx) * 0.85f
            orbVy = (orbVy - 2f * dot * ny) * 0.85f
        }

        // ВОЗВРАЩАЕМ АВТОМАТИЧЕСКИЙ СПАВН ПЕСЧИНОК СВЕРХУ!
        // Это гарантирует, что на экране всегда будет песок, даже если распад текста не сработал.
        spawnAccumulatorMs += dtSec * 1000f
        while (spawnAccumulatorMs >= tuning.spawnIntervalMs) {
            spawnAccumulatorMs -= tuning.spawnIntervalMs.toFloat()
            spawnParticle()
        }
        
        updateParticles(dtSec, applyGravity = true, applyWind = false)
        if (metrics.coreFill >= tuning.coreFillThreshold) {
            transitionTo(MandalaPhase.CoreGlow)
        }
    }

    private fun updateCoreGlow(dtSec: Float) {
        updateRings(dtSec)
        
        if (corePulseScale > 1.0f) {
            corePulseScale = (corePulseScale - 1.5f * dtSec).coerceAtLeast(1.0f)
        }
        
        coreGlowIntensity = (phaseElapsedSec / 2f).coerceIn(0f, 1f)
        updateParticles(dtSec, applyGravity = false, applyWind = false)
        if (phaseElapsedSec >= 2f) {
            transitionTo(MandalaPhase.WindDestroy)
        }
    }

    private fun updateWind(dtSec: Float) {
        updateRings(dtSec)
        var index = 0
        while (index < rings.size) {
            rings[index].alpha = (rings[index].alpha - WindEffect.RING_FADE_PER_SEC * dtSec).coerceAtLeast(0f)
            index++
        }
        updateParticles(dtSec, applyGravity = false, applyWind = true)

        val alive = particlePool.aliveCount()
        if (alive == 0 || phaseElapsedSec >= 8f) {
            transitionTo(MandalaPhase.Complete)
        }
    }

    private fun updateRings(dtSec: Float) {
        var index = 0
        while (index < rings.size) {
            val ring = rings[index]
            ring.currentAngle = Collision.normalizeAngle(ring.currentAngle + ring.velocity * dtSec)
            ring.velocity *= tuning.friction
            index++
        }
    }

    private fun updateBreathingSync() {
        if (!tuning.breathingSyncEnabled) {
            var index = 0
            while (index < rings.size) {
                rings[index].gapWidth = baseGapWidths[index]
                index++
            }
            return
        }

        val phaseRadians = (phaseElapsedSec % 10f) / 10f * (Math.PI * 2.0)
        val breath = ((kotlin.math.sin(phaseRadians) + 1.0) * 0.5).toFloat()
        var index = 0
        while (index < rings.size) {
            rings[index].gapWidth = baseGapWidths[index] + breath * 8f
            index++
        }
    }

    private fun spawnParticle() {
        val particle = particlePool.acquire() ?: return
        // Спавним песчинки вверху экрана, но с легким разбросом по ширине
        particle.x = centerX + FastRandom.nextSignedFloat(180f)
        particle.y = 32f
        particle.vx = FastRandom.nextSignedFloat(15f)
        particle.vy = 80f
        particle.radius = 5f + FastRandom.nextInt(4)
        particle.colorIndex = FastRandom.nextInt(8)
        particle.alpha = 0f
        particle.age = 0f
        particle.isAlive = true
        metrics.totalSpawned++
    }

    private fun updateParticles(dtSec: Float, applyGravity: Boolean, applyWind: Boolean) {
        val pool = particlePool.items
        var index = 0
        
        // Рассчитываем максимальную скорость вращения колец для эффекта суеты/турбулентности
        var maxRingVelocity = 0f
        var rIdx = 0
        while (rIdx < rings.size) {
            val vAbs = abs(rings[rIdx].velocity)
            if (vAbs > maxRingVelocity) {
                maxRingVelocity = vAbs
            }
            rIdx++
        }
        
        // Порог суеты: если скорость вращения любого кольца выше 180 градусов в секунду
        val isTurbulent = maxRingVelocity > 180f
        val turbulenceForce = ((maxRingVelocity - 180f) * 0.4f).coerceAtMost(150f)

        while (index < pool.size) {
            val particle = pool[index]
            if (!particle.isAlive) {
                index++
                continue
            }

            particle.age += dtSec

            if (applyGravity) {
                // ДВУХЭТАПНАЯ ФИЗИКА ПРИТЯЖЕНИЯ:
                // 1. Сначала частицы притягиваются к блуждающему Проводнику (orbX, orbY)
                val dxOrb = orbX - particle.x
                val dyOrb = orbY - particle.y
                val distOrb = Collision.distanceFromCenter(particle.x, particle.y, orbX, orbY).coerceAtLeast(10f)
                
                // 2. Но если частица оказывается близко к Центральному Ядру (centerX, centerY), 
                // Центральное Ядро перехватывает управление и мощно затягивает частицу в себя!
                val dxCore = centerX - particle.x
                val dyCore = centerY - particle.y
                val distCore = Collision.distanceFromCenter(particle.x, particle.y, centerX, centerY).coerceAtLeast(10f)
                
                if (distCore < coreRadius * 2.2f) {
                    // Сильное притяжение к центру (черная дыра)
                    val gravityPull = (tuning.gravity * 150f) / (distCore * dtSec * 0.016f + 50f)
                    particle.vx += (dxCore / distCore) * gravityPull * dtSec
                    particle.vy += (dyCore / distCore) * gravityPull * dtSec
                } else {
                    // Обычное притяжение к блуждающему Проводнику
                    val gravityPull = (tuning.gravity * 80f) / (distOrb * dtSec * 0.016f + 100f)
                    
                    // ОРБИТАЛЬНЫЙ ХАОС И ФИЗИЧЕСКИЙ БАРЬЕР:
                    // Чтобы частицы физически не могли слипнуться в центре Проводника:
                    // 1. Если они подлетают слишком близко (ближе 28px), мы включаем радиальное выталкивание (барьер)
                    // 2. Добавляем тангенциальное закручивание и случайный шум
                    val targetDistance = 32f // Идеальный радиус орбиты вокруг Проводника
                    val radialForce = if (distOrb < targetDistance) {
                        // Выталкиваем наружу, если подлетели слишком близко
                        -120f * (1f - distOrb / targetDistance)
                    } else {
                        // Притягиваем, если мы снаружи
                        gravityPull
                    }
                    
                    val tangentX = -dyOrb / distOrb
                    val tangentY = dxOrb / distOrb
                    val orbitalSpinForce = 55f // Сила закручивания по орбите
                    
                    particle.vx += ((dxOrb / distOrb) * radialForce + tangentX * orbitalSpinForce + FastRandom.nextSignedFloat(15f)) * dtSec
                    particle.vy += ((dyOrb / distOrb) * radialForce + tangentY * orbitalSpinForce + FastRandom.nextSignedFloat(15f)) * dtSec
                }
                
                // Добавляем легкое вязкое трение к частицам, чтобы они не вращались бесконечно, а плавно поглощались
                particle.vx *= 0.985f
                particle.vy *= 0.985f
            }
            if (applyWind) {
                particle.vx += WindEffect.FORCE_X * dtSec
                particle.vy += WindEffect.FORCE_Y * dtSec
                particle.alpha = (particle.alpha - WindEffect.ALPHA_DECAY_PER_SEC * dtSec).coerceAtLeast(0f)
                if (particle.alpha <= 0.01f) {
                    particle.isAlive = false
                    index++
                    continue
                }
            } else {
                // Плавный спавн (fade-in) за первые 200 мс
                particle.alpha = (particle.age / 0.20f).coerceIn(0f, 1f)
            }

            // Применяем турбулентность от суеты: хаотичное горизонтальное и вертикальное расталкивание частиц
            if (isTurbulent && !applyWind) {
                // Добавляем случайный шум к скоростям частиц, расталкивая их от центра вращения колец
                val dx = particle.x - centerX
                val dy = particle.y - centerY
                val dist = Collision.distanceFromCenter(particle.x, particle.y, centerX, centerY).coerceAtLeast(1f)
                
                // Радиальная сила (выталкивание наружу) + вихревая сила (закручивание)
                val pushX = (dx / dist) * turbulenceForce
                val pushY = (dy / dist) * turbulenceForce
                
                particle.vx += (pushX + FastRandom.nextSignedFloat(turbulenceForce)) * dtSec
                particle.vy += (pushY + FastRandom.nextSignedFloat(turbulenceForce)) * dtSec
            }

            particle.x += particle.vx * dtSec
            particle.y += particle.vy * dtSec

            if (phase == MandalaPhase.Playing || phase == MandalaPhase.CoreGlow) {
                handleCollisions(particle)
                handleCoreCapture(particle)
            }

            if (
                particle.y > centerY * 2f + 250f || // Увеличили порог удаления по вертикали
                particle.x < -200f || // Увеличили порог удаления по горизонтали
                particle.x > centerX * 2f + 200f
            ) {
                particle.isAlive = false
            }

            index++
        }
    }

    private fun handleCollisions(particle: Particle) {
        val distance = Collision.distanceFromCenter(particle.x, particle.y, centerX, centerY)
        val angle = Collision.particleAngleDegrees(particle.x, particle.y, centerX, centerY)

        var ringIndex = 0
        while (ringIndex < rings.size) {
            val ring = rings[ringIndex]
            if (abs(distance - ring.radius) < tuning.ringThickness) {
                if (Collision.isAngleInGap(angle, ring)) {
                    ringPassFlags[ringIndex] = true
                    metrics.channelPasses++
                } else {
                    particle.vy *= -0.3f
                    particle.vx *= 0.7f
                    val push = if (distance > ring.radius) 5f else -5f
                    val radialX = (particle.x - centerX) / distance
                    val radialY = (particle.y - centerY) / distance
                    particle.x += radialX * push
                    particle.y += radialY * push
                }
            }
            ringIndex++
        }
    }

    private fun handleCoreCapture(particle: Particle) {
        val distance = Collision.distanceFromCenter(particle.x, particle.y, centerX, centerY)
        if (distance <= coreRadius) {
            particle.isAlive = false
            metrics.coreFill++
            coreCapturedFlag = true
            // СМЯГЧЕННЫЙ ИМПУЛЬС ПУЛЬСАЦИИ: ядро расширяется аккуратно и деликатно (+3% вместо +12%)
            corePulseScale = (corePulseScale + 0.03f).coerceAtMost(1.25f)
        }
    }

    private fun transitionTo(next: MandalaPhase) {
        phase = next
        phaseElapsedSec = 0f
        if (next == MandalaPhase.WindDestroy) {
            var index = 0
            while (index < rings.size) {
                rings[index].velocity *= 0.2f
                index++
            }
        }
    }

    private fun clearRingPassFlags() {
        var index = 0
        while (index < ringPassFlags.size) {
            ringPassFlags[index] = false
            index++
        }
    }

    private fun invalidateDraw() {
        frameNonce.longValue++
    }
}
