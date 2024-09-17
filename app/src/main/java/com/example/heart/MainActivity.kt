package com.example.heart

import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), View.OnClickListener {

    // Define all the buttons and text views
    lateinit var btnSymptoms: Button
    lateinit var btnSigns: Button
    lateinit var btnHeartRate: Button
    lateinit var btnResRate: Button
    lateinit var resultHeartRateTv: TextView
    lateinit var resultResRateTv: TextView
    private var heartRate: Float = 0.0f
    private var resRate: Float = 0.0f
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)
        // Find the VideoView by its ID and set the video URI
        val videoView = findViewById<VideoView>(R.id.tv_camera)
        val videoUri = Uri.parse("android.resource://${packageName}/${R.raw.beats}")
        videoView.setVideoURI(videoUri)

        // Add media controls to the VideoView
        val mediaController = android.widget.MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        // Start the video
        videoView.start()

        // Initialize buttons and text views
        btnSymptoms = findViewById(R.id.btn_symptoms)
        btnSigns = findViewById(R.id.btn_upload_signs)
        btnHeartRate = findViewById(R.id.btn_measure_heart_rate)
        btnResRate = findViewById(R.id.btn_measure_respiratory_rate)
        resultHeartRateTv = findViewById(R.id.tv_heart_rate)
        resultResRateTv = findViewById(R.id.tv_respiratory_rate)

        // Set onClick listeners for buttons
        btnSymptoms.setOnClickListener(this)
        btnSigns.setOnClickListener(this)
        btnHeartRate.setOnClickListener(this)
        btnResRate.setOnClickListener(this)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_symptoms -> {
                val intent = Intent(this, SymptomsActivity::class.java)
                intent.putExtra("heartRate", resultHeartRateTv.text.toString().toFloatOrNull() ?: 0f)
                intent.putExtra("resRate", resultResRateTv.text.toString().toFloatOrNull() ?: 0f)
                startActivity(intent)
            }
            R.id.btn_upload_signs -> {
                saveSignsToDatabase()
            }
            R.id.btn_measure_heart_rate -> {
                calculateHeartRate()
            }
            R.id.btn_measure_respiratory_rate -> {
                calculateRespiratoryRate()
            }
        }
        // Update text views with current heart rate and respiratory rate values
        resultHeartRateTv.text = "Heart Rate: $heartRate bpm"
        resultResRateTv.text = "Respiratory Rate: $resRate breaths/min"
    }

    // heart rate calculation and helper functions

    @RequiresApi(Build.VERSION_CODES.P)
    private fun calculateHeartRate() {
        val videoUri = Uri.parse("android.resource://${packageName}/${R.raw.beats}")

        // Launch coroutine to calculate heart rate
        GlobalScope.launch(Dispatchers.Main) {
            val heartRateResult = heartRateCalculator(videoUri, contentResolver)
            heartRate = heartRateResult.toFloat()  // Store heart rate
            resultHeartRateTv.text = "Heart Rate: $heartRate bpm"
        }
    }


    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun heartRateCalculator(uri: Uri, contentResolver: ContentResolver): Int {
        return withContext(Dispatchers.IO) {
            var result: Int
            val retriever = MediaMetadataRetriever()
            val frameList = ArrayList<Bitmap>()

            try {
                // Set the data source using the URI directly
                retriever.setDataSource(this@MainActivity, uri)

                // Get frame count and set the frame duration
                val duration = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toInt() ?: 0
                val frameDuration = min(duration, 425)

                // Extract frames at regular intervals
                var i = 10
                while (i < frameDuration) {
                    val bitmap = retriever.getFrameAtTime(i * 1_000_000L) // Time in microseconds
                    bitmap?.let { frameList.add(it) }
                    i += 15
                }

                // Process frames to calculate heart rate
                var redBucket: Long
                var pixelCount: Long = 0
                val redValues = mutableListOf<Long>()

                for (frame in frameList) {
                    redBucket = 0
                    for (y in 350 until 450) {
                        for (x in 350 until 450) {
                            val pixel = frame.getPixel(x, y)
                            redBucket += Color.red(pixel)
                            pixelCount++
                        }
                    }
                    redValues.add(redBucket)
                }

                // Calculate rate based on red values
                val smoothedValues = redValues.windowed(5, 1) { it.average().toLong() }
                var previousValue = smoothedValues.first()
                var count = 0

                for (currentValue in smoothedValues.drop(1)) {
                    if ((currentValue - previousValue) > 3500) {
                        count++
                    }
                    previousValue = currentValue
                }

                val rate = (count.toFloat() * 60).toInt()
                result = rate / 4 // Adjust this if needed based on sampling

            } catch (e: Exception) {
                Log.e("HeartRateCalculator", "Error retrieving video frames", e)
                result = 0
            } finally {
                retriever.release()
            }

            result
        }
    }


    // respiratory rate calculations and helper functions
    // Function to calculate respiratory rate
    private fun calculateRespiratoryRate() {
        GlobalScope.launch(Dispatchers.Main) {
            val accelValuesX = loadCSVData(R.raw.x)
            val accelValuesY = loadCSVData(R.raw.y)
            val accelValuesZ = loadCSVData(R.raw.z)

            // Call respiratoryRateCalculator with the lists
            val respiratoryRateResult = respiratoryRateCalculator(accelValuesX, accelValuesY, accelValuesZ)
            resRate = respiratoryRateResult.toFloat()  // Store respiratory rate
            resultResRateTv.text = "Respiratory Rate: $resRate breaths/min"
        }
    }


    // Helper function to read CSV file and convert to MutableList<Float>
    private suspend fun loadCSVData(resourceId: Int): MutableList<Float> {
        return withContext(Dispatchers.IO) {
            val inputStream = resources.openRawResource(resourceId)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val dataList = mutableListOf<Float>()

            reader.useLines { lines ->
                lines.forEach { line ->
                    // Assuming each line contains a single float value
                    dataList.add(line.toFloatOrNull() ?: 0f)
                }
            }
            dataList
        }
    }

    // Helper function for respiratory rate calculation
    private fun respiratoryRateCalculator(
        accelValuesX: MutableList<Float>,
        accelValuesY: MutableList<Float>,
        accelValuesZ: MutableList<Float>
    ): Int {
        var previousValue: Float = 10f
        var k = 0

        for (i in 11 until accelValuesY.size) {
            val currentValue = sqrt(
                accelValuesZ[i].toDouble().pow(2.0) +
                        accelValuesX[i].toDouble().pow(2.0) +
                        accelValuesY[i].toDouble().pow(2.0)
            ).toFloat()

            if (abs(previousValue - currentValue) > 0.15) {
                k++
            }
            previousValue = currentValue
        }

        val ret = (k.toDouble() / 45.0)
        return (ret * 30).toInt()
    }

    private fun saveSignsToDatabase() {
        // Save heart rate and respiratory rate to database
        dbHelper.addHeartRateAndResRate(heartRate, resRate)

        // Optionally, you could also update the UI or show a confirmation message
        Toast.makeText(this, "Heart rate and respiratory rate saved successfully!", Toast.LENGTH_SHORT).show()
    }

}