package com.lynbrookrobotics.kapuchin.subsystems.intake.collector.hookslider

import com.lynbrookrobotics.kapuchin.*
import com.lynbrookrobotics.kapuchin.logging.*
import com.lynbrookrobotics.kapuchin.logging.Level.*
import com.lynbrookrobotics.kapuchin.preferences.*
import com.lynbrookrobotics.kapuchin.subsystems.*
import com.lynbrookrobotics.kapuchin.subsystems.intake.collector.hookslider.HookSliderPosition.*
import com.lynbrookrobotics.kapuchin.timing.*
import edu.wpi.first.wpilibj.Solenoid
import info.kunalsheth.units.generated.*
import info.kunalsheth.units.math.*

sealed class HookSliderPosition(val output: Boolean) {
    object In : HookSliderPosition(false)
    object Out : HookSliderPosition(true)
    companion object {
      val hookSliderQueryCode = 0b00_00_000_0_1
    }
}

class HookSliderComponent(hardware: HookSliderHardware) : Component<HookSliderComponent, HookSliderHardware, HookSliderPosition>(hardware, Subsystems.pneumaticTicker) {

    override val fallbackController: HookSliderComponent.(Time) -> HookSliderPosition = { In }

    override fun HookSliderHardware.output(value: HookSliderPosition) {
        val legal = legalRanges()

        when {
            !legal.any() -> log(Warning) { "No legal states found" }
            value in legal -> solenoid.set(value.output)
            else -> solenoid.set(legal.first().output)
        }
    }
}

class HookSliderHardware : SubsystemHardware<HookSliderHardware, HookSliderComponent>() {
    override val priority: Priority = Priority.Low
    override val period: Time = 100.milli(Second)
    override val syncThreshold: Time = 20.milli(Second)
    override val name: String = "Hook Slider"

    val solenoidPort by pref(2)
    val solenoid = Solenoid(solenoidPort)
}