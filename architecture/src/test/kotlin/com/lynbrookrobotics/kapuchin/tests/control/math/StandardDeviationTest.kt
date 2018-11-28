package com.lynbrookrobotics.kapuchin.tests.control.math

import com.lynbrookrobotics.kapuchin.control.math.FiniteStandardDeviation
import com.lynbrookrobotics.kapuchin.control.math.InfiniteStandardDeviation
import com.lynbrookrobotics.kapuchin.tests.`is greater than?`
import com.lynbrookrobotics.kapuchin.tests.`is within?`
import com.lynbrookrobotics.kapuchin.tests.anyDouble
import com.lynbrookrobotics.kapuchin.tests.anyInt
import info.kunalsheth.units.generated.Foot
import info.kunalsheth.units.generated.`±`
import info.kunalsheth.units.generated.times
import kotlin.test.Test

class StandardDeviationTest {

    @Test
    fun `Finite stdev of constant is zero`() {
        anyInt.filter { it > 2 }.forEach { falloff ->
            val stdev = FiniteStandardDeviation(::times, 0.Foot, falloff)
            anyDouble.forEach { const ->
                repeat(falloff) { stdev(const.Foot) }
                stdev(const.Foot) `is within?` (0.Foot `±` 0.01.Foot) // lots of floating point error
            }
        }
    }

    @Test
    fun `Infinite stdev of constant is zero`() {
        anyInt.filter { it > 0 }.forEach { falloff ->
            anyDouble.forEach { const ->
                val stdev = InfiniteStandardDeviation(::times, const.Foot)
                repeat(falloff) { stdev(const.Foot) `is within?` (0.Foot `±` 0.01.Foot) } // lots of floating point error
            }
        }
    }

    @Test
    fun `Finite stdev is finite`() {
        anyInt.filter { it > 2 }.forEach { falloff ->
            val stdev = FiniteStandardDeviation(::times, 10.Foot, falloff)

            repeat(falloff) {
                stdev(it.Foot) `is greater than?` 0.Foot
            }
            repeat(falloff) {
                stdev(8.46.Foot) `is greater than?` 0.Foot
            }
            stdev(8.46.Foot) `is within?` (0.Foot `±` 0.01.Foot) // lots of floating point error
        }
    }

    @Test
    fun `Infinite stdev is infinite`() {
        val stdev = InfiniteStandardDeviation(::times, 10.Foot)
        repeat(1000) {
            stdev(0.Foot) `is greater than?` 0.Foot
        }
    }

    @Test
    fun `Finite stdev of a increasing numbers is greater than zero`() {
        anyInt.filter { it > 0 }.forEach { falloff ->
            val stdev = FiniteStandardDeviation(::times, 10.Foot, falloff)
            anyDouble.sorted().forEach { value ->
                stdev(value.Foot) `is greater than?` 0.Foot
            }
        }
    }

    @Test
    fun `Infinite stdev of a positive number is greater than zero`() {
        val stdev = InfiniteStandardDeviation(::times, 10.Foot)
        anyDouble.sorted().forEach { value ->
            stdev(value.Foot) `is greater than?` 0.Foot
        }
    }
}