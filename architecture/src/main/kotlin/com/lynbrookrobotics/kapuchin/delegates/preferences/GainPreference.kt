package com.lynbrookrobotics.kapuchin.delegates.preferences

import com.lynbrookrobotics.kapuchin.Quan
import com.lynbrookrobotics.kapuchin.control.loops.Gain
import com.lynbrookrobotics.kapuchin.delegates.DelegateProvider
import com.lynbrookrobotics.kapuchin.delegates.WithEventLoop
import com.lynbrookrobotics.kapuchin.subsystems.Named
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

class GainPreference<Error, Compensation>(
        private val fallbackError: Double,
        private val errorConversion: KProperty1<Double, Error>,
        private val fallbackComp: Double,
        private val compConversion: KProperty1<Double, Compensation>,
        private val get: (String, Double) -> Double
) : WithEventLoop, DelegateProvider<Named, Gain<Error, Compensation>>
        where Error : Quan<Error>,
              Compensation : Quan<Compensation> {

    private lateinit var errorName: String
    private lateinit var compName: String

    private var value: Gain<Error, Compensation>? = null

    override fun update() {
        if (this::errorName.isInitialized && this::compName.isInitialized) {
            value = Gain(
                    compConversion(get(compName, fallbackComp)),
                    errorConversion(get(errorName, fallbackError))
            )
        }
    }

    override fun provideDelegate(thisRef: Named, prop: KProperty<*>): ReadOnlyProperty<Named, Gain<Error, Compensation>> {
        val baseName = namePreference(thisRef, prop)
        errorName = "$baseName (error, ${errorConversion.name})"
        compName = "$baseName (compensation, ${compConversion.name})"

        return object : ReadOnlyProperty<Named, Gain<Error, Compensation>> {
            override fun getValue(thisRef: Named, property: KProperty<*>) = value ?: update().let { value!! }
        }
    }
}