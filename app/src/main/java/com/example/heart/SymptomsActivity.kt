package com.example.heart

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SymptomsActivity : AppCompatActivity() {

    private val symptomsList = listOf(
        "Nausea", "Headache", "Diarrhea", "Sore Throat", "Fever",
        "Muscle Ache", "Loss of Smell or Taste", "Cough", "Shortness of Breath", "Feeling Tired"
    )
    private lateinit var dbHelper: DatabaseHelper
    private var heartRate: Float = 0.0f
    private var respRate: Float = 0.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symptoms)

        val symptomsSpinner = findViewById<Spinner>(R.id.symptoms_spinner)
        val ratingBar = findViewById<RatingBar>(R.id.symptom_rating_bar)
        val saveButton = findViewById<Button>(R.id.save_rating_button)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, symptomsList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        symptomsSpinner.adapter = adapter

        dbHelper = DatabaseHelper(this)

        // Retrieve heart rate and respiratory rate from the intent
        heartRate = intent.getFloatExtra("heartRate", 0.0f)
        respRate = intent.getFloatExtra("resRate", 0.0f)

        saveButton.setOnClickListener {
            val selectedSymptom = symptomsSpinner.selectedItem.toString()
            val rating = ratingBar.rating

            // Save the heart rate, respiratory rate, and symptom rating to the database
            dbHelper.addHeartRateAndResRate(heartRate, respRate)
            dbHelper.addSymptomRating(selectedSymptom, rating)

            Toast.makeText(this, "Saved $selectedSymptom: $rating stars", Toast.LENGTH_SHORT).show()
        }
    }
}
