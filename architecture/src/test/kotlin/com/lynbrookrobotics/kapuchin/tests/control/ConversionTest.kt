package com.lynbrookrobotics.kapuchin.tests.control

import com.lynbrookrobotics.kapuchin.control.conversion.EncoderConversion
import com.lynbrookrobotics.kapuchin.control.conversion.GearTrain
import com.lynbrookrobotics.kapuchin.control.conversion.OffloadedNativeConversion
import com.lynbrookrobotics.kapuchin.control.conversion.WheelConversion
import com.lynbrookrobotics.kapuchin.control.loops.Gain
import com.lynbrookrobotics.kapuchin.control.loops.pid.PidGains
import com.lynbrookrobotics.kapuchin.tests.`is equal to?`
import com.lynbrookrobotics.kapuchin.tests.anyDouble
import com.lynbrookrobotics.kapuchin.tests.anyInt
import info.kunalsheth.units.generated.*
import kotlin.test.Test

class ConversionTest {
    private val t = 1.Second

    @Test
    fun `encoder ticks and angle methods are inverses`() {
        anyInt.filter { it != 0 }.map { resolution -> EncoderConversion(resolution.Tick, 360.Degree) }
                .forEach { conversion ->
                    anyDouble.map { it.Tick }.forEach { x ->
                        x `is equal to?` conversion.ticks(conversion.angle(x))

                        val ix = x * t
                        ix `is equal to?` conversion.ticks(conversion.angle(ix))

                        val dx = x / t
                        dx `is equal to?` conversion.ticks(conversion.angle(dx))

                        val ddx = dx / t
                        ddx `is equal to?` conversion.ticks(conversion.angle(ddx))
                    }
                }
    }

    @Test
    fun `wheel length and angle methods are inverses`() {
        anyInt.filter { it > 0 }
                .map { radius -> WheelConversion(radius.Inch) }
                .forEach { conversion ->
                    anyDouble.map { it.Foot }.forEach { x ->
                        x `is equal to?` conversion.length(conversion.angle(x))

                        val ix = x * t
                        ix `is equal to?` conversion.length(conversion.angle(ix))

                        val dx = x / t
                        dx `is equal to?` conversion.length(conversion.angle(dx))

                        val ddx = dx / t
                        ddx `is equal to?` conversion.length(conversion.angle(ddx))
                    }
                }
    }

    @Test
    fun `offloaded real and native methods are inverses`() {
        anyInt.filter { it != 0 }.map { resolution -> OffloadedNativeConversion(1023, 12.Volt, resolution, 8.46.Metre) }
                .forEach { conversion ->
                    anyDouble.map { it.Foot }.forEach { x ->
                        x `is equal to?` conversion.realPosition(conversion.native(x))

                        val dx = x / t
                        dx `is equal to?` conversion.realVelocity(conversion.native(dx))
                    }
                }
    }

    @Test
    fun `offloaded native methods are linear`() {
        anyInt.filter { it != 0 }.map { resolution -> OffloadedNativeConversion(1023, 12.Volt, resolution, 8.46.Metre) }
                .forEach { conversion ->
                    anyDouble.map { it.Foot }.forEach { x ->
                        conversion.native(-x) * 2 `is equal to?` -conversion.native(x * 2)
                        conversion.native(Gain(20.Volt, x)) `is equal to?` conversion.native(Gain(10.Volt, x)) * 2

                        val ix = x * 1.Second
                        conversion.native(-ix) * 2 `is equal to?` -conversion.native(ix * 2)
                        conversion.native(Gain(20.Volt, ix)) `is equal to?` conversion.native(Gain(10.Volt, ix)) * 2

                        val dx = x / 1.Second
                        conversion.native(-dx) * 2 `is equal to?` -conversion.native(dx * 2)
                        conversion.native(Gain(20.Volt, dx)) `is equal to?` conversion.native(Gain(10.Volt, dx)) * 2

                        val ddx = dx / t
                        conversion.native(-ddx) * 2 `is equal to?` -conversion.native(ddx * 2)
                        conversion.native(Gain(20.Volt, ddx)) `is equal to?` conversion.native(Gain(10.Volt, ddx)) * 2
                    }

                    anyDouble.map { it.Volt }.forEach { x ->
                        conversion.native(-x) * 2 `is equal to?` -conversion.native(x * 2)
                    }
                }
    }

    @Test
    fun `gears input and output methods are inverses`() {
        anyInt.filter { it > 0 }.forEach { input ->
            anyInt.filter { it > 0 }.forEach { idlers ->
                anyInt.filter { it > 0 }
                        .map { output -> GearTrain(input, idlers, output) }
                        .forEach { gearTrain ->
                            anyDouble.map { it.Degree }.forEach { x ->
                                x `is equal to?` gearTrain.outputToInput(gearTrain.inputToOutput(x))

                                val dx = x / t
                                dx `is equal to?` gearTrain.outputToInput(gearTrain.inputToOutput(dx))

                                val ddx = dx / t
                                ddx `is equal to?` gearTrain.outputToInput(gearTrain.inputToOutput(ddx))
                            }
                        }
            }
        }
    }
}