package com.fittrack.app.services

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.util.concurrent.atomic.AtomicInteger

class PedometerService : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var stepCounterSensor: Sensor? = null
    private val initialSteps = AtomicInteger(-1)
    private val currentSteps = AtomicInteger(0)

    var onStepUpdate: ((Int) -> Unit)? = null

    fun isAvailable(context: Context): Boolean {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return false
        return sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
    }

    fun start(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepCounterSensor?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        stepCounterSensor = null
    }

    fun getSteps(): Int = currentSteps.get()

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSinceReboot = event.values[0].toInt()
            if (initialSteps.get() < 0) {
                initialSteps.set(totalSinceReboot)
            }
            val stepsSinceStart = (totalSinceReboot - initialSteps.get()).coerceAtLeast(0)
            currentSteps.set(stepsSinceStart)
            onStepUpdate?.invoke(stepsSinceStart)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}
