// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.haptic

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Лёгкая обёртка над тактильной обратной связью для Compose.
 *
 * Почему мы используем прямой [Vibrator] вместо [View.performHapticFeedback]:
 *  - На многих устройствах (Xiaomi, Samsung, OnePlus и др.) системный вызов `performHapticFeedback`
 *    блокируется операционной системой, если пользователь отключил глобальный пункт "Виброотклик" в настройках звуков.
 *  - Однако, поскольку Sanctum является локальным бережным пространством рефлексии, пользователь
 *    должен иметь возможность включить тактильность внутри приложения независимо от глобального виброотклика ОС.
 *  - Используя прямой [Vibrator] с аудио-атрибутами [USAGE_ASSISTANCE_SONIFICATION] (так же, как в Дыхательном мосту),
 *    мы обходим ограничения на обычные касания, гарантируя стабильную и деликатную обратную связь на любом устройстве.
 */
class HapticFeedback internal constructor(
    private val context: Context,
    private val isEnabled: Boolean = true
) {

    private val vibrator: Vibrator? = resolveVibrator(context)

    /**
     * Тактильный отклик для вращения колец в «Песочном потоке» (Sand Flow).
     * Частота и амплитуда зависят от угловой скорости вращения.
     */
    fun sandFlowRotation(intensity: Float) {
        if (!isEnabled || intensity <= 0.05f) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)
        ) {
            val composition = VibrationEffect.startComposition()
            val scale = (0.08f + intensity * 0.42f).coerceIn(0.08f, 0.5f)
            composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, scale)
            vibrateEffect(v, composition.compose())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitude = (40f + intensity * 110f).toInt().coerceIn(30, 150)
            vibrateEffect(v, VibrationEffect.createOneShot(10, amplitude))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(10)
        }
    }

    /**
     * Тактильный отклик при прохождении песчинки сквозь каналы колец.
     * Деликатный, ультра-короткий импульс.
     */
    fun sandFlowPass() {
        if (!isEnabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            v.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)
        ) {
            val composition = VibrationEffect.startComposition()
            composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.12f)
            vibrateEffect(v, composition.compose())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrateEffect(v, VibrationEffect.createOneShot(8, 35))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(8)
        }
    }

    /**
     * Тактильный отклик при поглощении песчинки ядром.
     * Мягкий волновой эффект, создающий ощущение всасывания.
     */
    fun sandFlowCoreCapture() {
        if (!isEnabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrateEffect(
                v,
                VibrationEffect.createWaveform(
                    longArrayOf(0, 15, 30, 15),
                    intArrayOf(0, 45, 0, 25),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(30)
        }
    }

    /** Лёгкая «галочка» — выбор тега, переключатель (очень короткий деликатный импульс). */
    fun tick() {
        if (!isEnabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrateEffect(v, VibrationEffect.createOneShot(28, 195))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(28)
        }
    }

    /** Подтверждение действия — сохранение дня, успешная операция (двойной мягкий клик). */
    fun confirm() {
        if (!isEnabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrateEffect(
                v,
                VibrationEffect.createWaveform(
                    longArrayOf(0, 35, 100, 35),
                    intArrayOf(0, 220, 0, 220),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(longArrayOf(0, 35, 100, 35), -1)
        }
    }

    /** Лонг-пресс — для удаления и опасных действий (средний уверенный импульс). */
    fun longPress() {
        if (!isEnabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrateEffect(v, VibrationEffect.createOneShot(100, 225))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(100)
        }
    }

    private fun vibrateEffect(vibrator: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VIBRATION_ATTRIBUTES)
        } else {
            // На версиях от Oreo до Tiramisu также поддерживается вибрация с аудио-атрибутами
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    vibrator.vibrate(effect, VIBRATION_ATTRIBUTES)
                } catch (e: Exception) {
                    vibrator.vibrate(effect)
                }
            } else {
                vibrator.vibrate(effect)
            }
        }
    }

    private companion object {
        val VIBRATION_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        fun resolveVibrator(context: Context): Vibrator? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Используем безопасное получение системной службы без рефлексии
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
    }
}

/**
 * Получить инстанс [HapticFeedback], привязанный к контексту.
 * `remember` нужен, чтобы не пересоздавать обёртку на каждой рекомпозиции.
 */
@Composable
fun rememberHapticFeedback(isEnabled: Boolean = true): HapticFeedback {
    val context = LocalContext.current.applicationContext
    return remember(context, isEnabled) { HapticFeedback(context, isEnabled) }
}
