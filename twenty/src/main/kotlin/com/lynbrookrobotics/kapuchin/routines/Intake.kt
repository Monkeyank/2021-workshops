package com.lynbrookrobotics.kapuchin.routines

import com.lynbrookrobotics.kapuchin.control.electrical.*
import com.lynbrookrobotics.kapuchin.hardware.offloaded.*
import com.lynbrookrobotics.kapuchin.subsystems.*
import com.lynbrookrobotics.kapuchin.subsystems.drivetrain.*
import com.lynbrookrobotics.kapuchin.subsystems.intake.*
import info.kunalsheth.units.generated.*

suspend fun IntakeSliderComponent.set(target: IntakeSliderState) = startRoutine("Set") {
    controller { target }
}

suspend fun IntakeRollersComponent.set(target: DutyCycle) = startRoutine("Set") {
    controller { PercentOutput(hardware.escConfig, target) }
}

suspend fun IntakeRollersComponent.optimalEat(drivetrain: DrivetrainComponent, electrical: ElectricalSystemHardware) = startRoutine("Optimal Eat") {

    val vBat by electrical.batteryVoltage.readEagerly.withoutStamps

    val leftSpeed by drivetrain.hardware.leftSpeed.readEagerly.withoutStamps
    val rightSpeed by drivetrain.hardware.rightSpeed.readEagerly.withoutStamps

    controller {
        val voltage = eatSpeed - hardware.escConfig.voltageCompSaturation * ((leftSpeed + rightSpeed) / (drivetrain.maxSpeed * 2))
        PercentOutput(hardware.escConfig, voltageToDutyCycle(voltage, vBat))
    }
}
