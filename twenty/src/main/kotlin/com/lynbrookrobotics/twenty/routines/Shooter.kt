package com.lynbrookrobotics.twenty.routines

import com.lynbrookrobotics.kapuchin.control.math.*
import com.lynbrookrobotics.kapuchin.hardware.offloaded.*
import com.lynbrookrobotics.twenty.subsystems.driver.OperatorHardware
import com.lynbrookrobotics.twenty.subsystems.shooter.*
import com.lynbrookrobotics.twenty.subsystems.shooter.flywheel.FlywheelComponent
import com.lynbrookrobotics.twenty.subsystems.shooter.turret.TurretComponent
import info.kunalsheth.units.generated.*

suspend fun FlywheelComponent.set(target: AngularVelocity) = startRoutine("Set Omega") {
    controller {
        VelocityOutput(hardware.escConfig, velocityGains, hardware.conversions.encoder.native(target))
    }
}

suspend fun FlywheelComponent.set(target: DutyCycle) = startRoutine("Set Duty Cycle") {
    controller {
        PercentOutput(hardware.escConfig, target)
    }
}

suspend fun FeederRollerComponent.set(target: AngularVelocity) = startRoutine("Set Omega") {
    controller {
        VelocityOutput(hardware.escConfig, velocityGains, hardware.conversions.native(target))
    }
}

suspend fun FeederRollerComponent.set(target: DutyCycle) = startRoutine("Set Duty Cycle") {
    controller {
        PercentOutput(hardware.escConfig, target)
    }
}

suspend fun TurretComponent.set(target: Angle, tolerance: Angle = 0.2.Degree) = startRoutine("Set") {
    val current by hardware.position.readOnTick.withoutStamps

    controller {
        PositionOutput(hardware.escConfig, positionGains, hardware.conversions.encoder.native(target))
            .takeUnless { target - current in `±`(tolerance) }
    }
}

suspend fun TurretComponent.manualOverride(operator: OperatorHardware) = startRoutine("Manual Override") {
    val precision by operator.turretManual.readOnTick.withoutStamps
    controller { PercentOutput(hardware.escConfig, precision) }
}

suspend fun TurretComponent.manualPrecisionOverride(operator: OperatorHardware) =
    startRoutine("Manual Precision Override") {
        val precision by operator.turretPrecisionManual.readOnTick.withoutStamps
        controller { PercentOutput(hardware.escConfig, precision) }
    }

suspend fun ShooterHoodComponent.set(target: ShooterHoodState) = startRoutine("Set") {
    controller { target }
}
