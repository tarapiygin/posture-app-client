package com.example.postureapp.core.orientation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OrientationAngles(
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val azimuthDeg: Float = 0f
)

class OrientationManager @Inject constructor(
    @ApplicationContext context: Context
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(9)
    private val adjustedMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val _angles = MutableStateFlow(OrientationAngles())
    val angles: StateFlow<OrientationAngles> = _angles.asStateFlow()

    private var isRegistered = false
    private val alpha = 0.1f

    fun start() {
        if (!isRegistered && rotationSensor != null) {
            sensorManager.registerListener(
                this,
                rotationSensor,
                SensorManager.SENSOR_DELAY_GAME
            )
            isRegistered = true
        }
    }

    fun stop() {
        if (isRegistered) {
            sensorManager.unregisterListener(this)
            isRegistered = false
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.remapCoordinateSystem(
            rotationMatrix,
            SensorManager.AXIS_X,
            SensorManager.AXIS_Z,
            adjustedMatrix
        )
        SensorManager.getOrientation(adjustedMatrix, orientationAngles)
        val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
        val previous = _angles.value
        _angles.value = OrientationAngles(
            pitchDeg = lowPass(pitch, previous.pitchDeg),
            rollDeg = lowPass(roll, previous.rollDeg),
            azimuthDeg = lowPass(azimuth, previous.azimuthDeg)
        )
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

    private fun lowPass(newValue: Float, currentValue: Float): Float {
        return currentValue + alpha * (newValue - currentValue)
    }
}

