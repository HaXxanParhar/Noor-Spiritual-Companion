package com.parhar.noor.ui.more

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityQiblaBinding
import com.parhar.noor.utils.BaseActivity
import kotlin.math.roundToInt

class QiblaActivity : BaseActivity<ActivityQiblaBinding>(), SensorEventListener {

    private val sensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private var hasAccelerometerReading = false
    private var hasMagnetometerReading = false

    override fun inflateBinding(): ActivityQiblaBinding =
        ActivityQiblaBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.toolbar.toolbarTitleTextView.setText(R.string.qibla_title)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        } ?: run {
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { sensor ->
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
            }
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { sensor ->
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        super.onPause()
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                updateCompass()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                hasAccelerometerReading = true
                updateCompassFromFallbackSensors()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
                hasMagnetometerReading = true
                updateCompassFromFallbackSensors()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun updateCompassFromFallbackSensors() {
        if (hasAccelerometerReading && hasMagnetometerReading) {
            SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                accelerometerReading,
                magnetometerReading,
            )
            updateCompass()
        }
    }

    private fun updateCompass() {
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        val azimuthDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        val normalizedAzimuth = (azimuthDegrees + 360f) % 360f
        val qiblaBearingFromIslamabad = 253f
        val needleRotation = qiblaBearingFromIslamabad - normalizedAzimuth

        binding.compassNeedleTextView.rotation = needleRotation
        binding.headingTextView.text =
            "${normalizedAzimuth.roundToInt()}° · Qibla ${qiblaBearingFromIslamabad.roundToInt()}°"
    }
}
