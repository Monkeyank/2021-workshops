package com.lynbrookrobotics.kapuchin.tests.control

import com.lynbrookrobotics.kapuchin.control.electrical.MotorCurrentLimiter
import com.lynbrookrobotics.kapuchin.control.electrical.OutsideThresholdChecker
import com.lynbrookrobotics.kapuchin.control.electrical.RampRateLimiter
import com.lynbrookrobotics.kapuchin.control.electrical.voltageToDutyCycle
import com.lynbrookrobotics.kapuchin.control.stampWith
import com.lynbrookrobotics.kapuchin.control.withToleranceOf
import com.lynbrookrobotics.kapuchin.logging.Named
import com.lynbrookrobotics.kapuchin.tests.`is equal to?`
import com.lynbrookrobotics.kapuchin.tests.`is greater than or equal to?`
import com.lynbrookrobotics.kapuchin.tests.`is greater than?`
import info.kunalsheth.units.generated.*
import kotlin.math.sin
import kotlin.test.Test

class ElectricalTests {

    @Test
    fun `ramp rate limiting ramps up and down output`() {
        val startRampUpTime = 846.Minute

        val limiter = RampRateLimiter(0.Volt stampWith startRampUpTime) { 12.VoltPerSecond }

        val incr = 3.milli(::Second)

        generateSequence(startRampUpTime) { it + incr }
                .takeWhile { it - startRampUpTime < 1.Second }
                .forEach { 12.Volt `is greater than?` limiter(it, 12.Volt) }
        repeat(50) {
            limiter(startRampUpTime + 1.Second + incr * it, 12.Volt) `is equal to?` 12.Volt
        }

        val startRampDownTime = startRampUpTime + 1.Second + incr * 49

        generateSequence(startRampDownTime) { it + incr }
                .takeWhile { it - startRampDownTime < 2.Second }
                .forEach { limiter(it, -12.Volt) `is greater than?` -12.Volt }
        repeat(50) {
            limiter(startRampDownTime + 2.Second + incr * it, -12.Volt) `is equal to?` -12.Volt
        }
    }

    @Test
    fun `motor current limiting does not stall motors during startup`() {
        val maxVoltage = 12.Volt
        val stallCurrent = 130.Ampere
        val currentLimit = 25.Ampere
        val r = maxVoltage / stallCurrent

        val limiter = MotorCurrentLimiter(
                maxVoltage, 5300.Rpm,
                stallCurrent, currentLimit
        )

        val incr = 0.2.Volt
        val maxStartupVoltage = currentLimit * r
        repeat(100) {
            val target = incr * it
            limiter(0.Rpm, target) `is equal to?` if (target > maxStartupVoltage) maxStartupVoltage else target
            limiter(0.Volt, 0.Ampere, target) `is equal to?` if (target > maxStartupVoltage) maxStartupVoltage else target
        }
        repeat(100) {
            val target = -incr * it
            limiter(0.Rpm, target) `is equal to?` if (target < -maxStartupVoltage) -maxStartupVoltage else target
            limiter(0.Volt, 0.Ampere, target) `is equal to?` if (target < -maxStartupVoltage) -maxStartupVoltage else target
        }
    }

    @Test
    fun `threshold checker triggers after duration outside safe range`() {
        val duration = 3.Second
        val tolerance = 25.Ampere
        val safeRange = 0.Ampere withToleranceOf tolerance
        val checker = OutsideThresholdChecker(safeRange, duration)

        val insideStartTime = 846.Minute
        val incr = 3.milli(::Second)
        repeat(100) {
            checker(insideStartTime + incr * it, tolerance * sin(it.toFloat())) `is equal to?` false
        }

        val outsideStartTime = insideStartTime + incr * 99
        generateSequence(outsideStartTime) { it + incr }
                .takeWhile { it - outsideStartTime < duration }
                .forEach { checker(it, 25.1.Ampere) `is equal to?` false }
        checker(outsideStartTime + duration + incr * 1, 25.1.Ampere) `is equal to?` true
        checker(outsideStartTime + duration + incr * 2, 24.9.Ampere) `is equal to?` false
        checker(outsideStartTime + duration + incr * 3, 25.1.Ampere) `is equal to?` false
        checker(outsideStartTime + duration + incr * 4, -24.9.Ampere) `is equal to?` false

        val negativeOutsideStartTime = outsideStartTime + 3.Second + incr * 5
        generateSequence(negativeOutsideStartTime) { it + incr }
                .takeWhile { it - negativeOutsideStartTime < duration }
                .forEach { checker(it, -25.1.Ampere) `is equal to?` false }
        checker(negativeOutsideStartTime + duration + incr * 1, -25.1.Ampere) `is equal to?` true
    }

    @Test
    fun `battery compensator calculates correct duty cycle`() {
        object : Named("Battery Compensator Test") {}.run {
            voltageToDutyCycle(11.Volt, 11.Volt) `is equal to?` 100.Percent
            voltageToDutyCycle(9.Volt, 10.Volt) `is equal to?` 90.Percent
            voltageToDutyCycle(-7.5.Volt, 10.Volt) `is equal to?` -75.Percent
            voltageToDutyCycle(12.Volt, 10.Volt) `is greater than or equal to?` 100.Percent
        }
    }
}