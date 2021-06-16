package com.lynbrookrobotics.twenty.routines

import com.lynbrookrobotics.kapuchin.hardware.offloaded.*
import com.lynbrookrobotics.twenty.subsystems.climber.ClimberPivotComponent
import com.lynbrookrobotics.twenty.subsystems.climber.ClimberPivotState
import com.lynbrookrobotics.twenty.subsystems.climber.ClimberWinchComponent
import com.lynbrookrobotics.twenty.subsystems.climber.ClimberWinchOutput.Running
import info.kunalsheth.units.generated.*

suspend fun ClimberWinchComponent.set(target: DutyCycle) = startRoutine("Set") {
    controller { Running(PercentOutput(hardware.escConfig, target)) }
}

suspend fun ClimberPivotComponent.set(target: ClimberPivotState) = startRoutine("Set") {
    controller { target }
}
