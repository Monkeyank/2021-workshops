package com.lynbrookrobotics.twenty.choreos

import com.lynbrookrobotics.kapuchin.control.math.*
import com.lynbrookrobotics.kapuchin.logging.*
import com.lynbrookrobotics.kapuchin.logging.Level.*
import com.lynbrookrobotics.kapuchin.routines.*
import com.lynbrookrobotics.twenty.Subsystems
import com.lynbrookrobotics.twenty.routines.*
import com.lynbrookrobotics.twenty.subsystems.carousel.CarouselSlot
import com.lynbrookrobotics.twenty.subsystems.intake.IntakeSliderState
import com.lynbrookrobotics.twenty.subsystems.shooter.ShooterHoodState
import info.kunalsheth.units.generated.*
import kotlinx.coroutines.*
import java.awt.Color

suspend fun Subsystems.digestionTeleop() = startChoreo("Digestion Teleop") {

    val shift by operator.shift.readEagerly().withoutStamps

    val eatBalls by driver.eatBalls.readEagerly().withoutStamps
    val pukeBallsIntakeIn by driver.pukeBallsIntakeIn.readEagerly().withoutStamps

    val pukeBallsIntakeOut by driver.pukeBallsIntakeOut.readEagerly().withoutStamps

    val centerTurret by operator.centerTurret.readEagerly().withoutStamps
    val shootFast by operator.shootFast.readEagerly().withoutStamps
    val shootSlow by operator.shootSlow.readEagerly().withoutStamps

    val presetAnitez by operator.presetAnitez.readEagerly().withoutStamps
    val presetClose by operator.presetClose.readEagerly().withoutStamps
    val presetMed by operator.presetMed.readEagerly().withoutStamps
    val presetFar by operator.presetFar.readEagerly().withoutStamps

    val turretManual by operator.turretManual.readEagerly().withoutStamps
    val turretPrecisionManual by operator.turretPrecisionManual.readEagerly().withoutStamps

    val carouselBall0 by driver.carouselBall0.readEagerly().withoutStamps
    val carouselLeft by driver.carouselLeft.readEagerly().withoutStamps
    val carouselRight by driver.carouselRight.readEagerly().withoutStamps

    choreography {
        if (!carousel.hardware.isZeroed) {
            withTimeout(2.Second) { carousel.rezero() }
        }

        runWhenever(
            { eatBalls } to { intakeBalls() },
            { pukeBallsIntakeIn } to { intakeRollers?.set(-100.Percent) ?: freeze() },
            { pukeBallsIntakeOut } to {
                launch { intakeSlider?.set(IntakeSliderState.Out) }
                intakeRollers?.set(-100.Percent) ?: freeze()
            },

            /*
                TODO: Set turret to the zero position
                Hint(s):
                  - Button name is: centerTurret
                  - Think: What position in degrees is the turret when Zeroed?
                  - If the routine is null then run freeze()

             */
            // TODO: REPLACE ME { buttonName } to { what To Do, but if null call freeze() }

            { shootFast } to { shootAll(carousel.shootFastSpeed) },
            { shootSlow } to { shootAll(carousel.shootSlowSpeed) },

            { presetAnitez } to { flywheel?.let { spinUpShooter(it.presetAnitez) } ?: freeze() },
            { presetClose } to { flywheel?.let { spinUpShooter(it.presetClose) } ?: freeze() },
            { presetMed } to { flywheel?.let { spinUpShooter(it.presetMed) } ?: freeze() },
            { presetFar } to { flywheel?.let { spinUpShooter(it.presetFar) } ?: freeze() },

            { !turretManual.isZero && turretPrecisionManual.isZero } to {
                turret?.manualOverride(operator) ?: freeze()
            },
            { turretManual.isZero && !turretPrecisionManual.isZero } to {
                turret?.manualPrecisionOverride(operator) ?: freeze()
            },

            { carouselBall0 } to { carousel.state.clear() },
            { carouselLeft && !eatBalls } to {
                carousel.set(carousel.hardware.nearestSlot() + 1.CarouselSlot,
                    0.Degree)
            },
            { carouselRight && !eatBalls } to {
                carousel.set(carousel.hardware.nearestSlot() - 1.CarouselSlot,
                    0.Degree)
            },
        )
    }
}

suspend fun Subsystems.intakeBalls() = startChoreo("Intake Balls") {
    val isBall by carousel.hardware.isBall.readEagerly().withoutStamps
    val carouselLeft by driver.carouselLeft.readEagerly().withoutStamps
    val carouselRight by driver.carouselRight.readEagerly().withoutStamps

    choreography {
        while (isActive) {
            val angle = carousel.state.intakeAngle()
            if (angle == null) {
                launch { leds?.blink(Color.RED) }
                log(Warning) { "I'm full. No open slots in carousel magazine." }

                launch { intakeSlider?.set(IntakeSliderState.In) }
                launch { intakeRollers?.set(0.Percent) }
                freeze()
            } else {
                launch { feederRoller?.set(0.Rpm) }
                launch { intakeRollers?.set(intakeRollers.pauseSpeed) }

                carousel.set(angle)
                launch { carousel.set(angle, 0.Degree) }
                launch { leds?.blink(Color.BLUE) }

                launch { intakeSlider?.set(IntakeSliderState.Out) }
                launch { intakeRollers?.set(intakeRollers.eatSpeed) }

                log(Debug) { "Waiting for a yummy mouthful of balls." }

                delayUntil { isBall || carouselLeft || carouselRight }
                launch { leds?.set(Color.RED) }
                carousel.state.push()
            }
        }
    }
}

suspend fun Subsystems.shootAll(speed: DutyCycle) = startChoreo("Shoot All") {
    choreography {
        val j = launch { shooterHood?.set(ShooterHoodState.Up) }
        launch { leds?.set(Color.GREEN) }
        try {
            delay(0.3.Second)
            carousel.set(speed)
        } finally {
            withContext(NonCancellable) {
                launch {
                    delay(0.3.Second)
                    j.cancel()
                }
                carousel.state.clear()
                carousel.rezero()
                carousel.hardware.encoder.position = 0.0
            }
        }
    }
}

suspend fun Subsystems.spinUpShooter(flywheelPreset: AngularVelocity) {
    if (flywheel == null || feederRoller == null) {
        log(Error) { "Need flywheel and feeder to spin up shooter" }
        freeze()
    } else startChoreo("Spin Up Shooter") {
        val flywheelSpeed by flywheel.hardware.speed.readEagerly().withoutStamps

        choreography {
            launch { feederRoller.set(0.Rpm) }

            carousel.rezero()
            carousel.set(carousel.state.shootInitialAngle() ?: carousel.hardware.nearestSlot())

            launch { feederRoller.set(feederRoller.feedSpeed) }
            launch { leds?.blink(Color.BLUE) }

            launch { flywheel.set(flywheelPreset) }

            runWhenever({ flywheelSpeed in flywheelPreset `±` flywheel.tolerance } to {
                launch { leds?.set(Color.GREEN) }
                rumble.set(100.Percent)
            })
        }
    }
}