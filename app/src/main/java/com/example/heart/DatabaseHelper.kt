package com.example.heart

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 2
        private const val DATABASE_NAME = "health_data.db"
        private const val TABLE_SIGNS = "signs"
        private const val TABLE_SYMPTOMS = "symptoms"
        private const val COLUMN_ID = "id"
        private const val COLUMN_HEART_RATE = "heart_rate"
        private const val COLUMN_RESP_RATE = "resp_rate"
        private const val COLUMN_SYMPTOM_NAME = "symptom_name"
        private const val COLUMN_SYMPTOM_RATING = "symptom_rating"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Create table for storing heart rate and respiratory rate data
        val createSignsTable = ("CREATE TABLE $TABLE_SIGNS ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COLUMN_HEART_RATE REAL, "
                + "$COLUMN_RESP_RATE REAL)")

        // Create table for storing symptoms and their ratings
        val createSymptomsTable = ("CREATE TABLE $TABLE_SYMPTOMS ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COLUMN_SYMPTOM_NAME TEXT, "
                + "$COLUMN_SYMPTOM_RATING REAL)")

        db?.execSQL(createSignsTable)
        db?.execSQL(createSymptomsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_SIGNS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_SYMPTOMS")
        onCreate(db)
    }

    // Method to add heart rate and respiratory rate
    fun addHeartRateAndResRate(heartRate: Float, respRate: Float) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_HEART_RATE, heartRate)
            put(COLUMN_RESP_RATE, respRate)
        }
        db.insert(TABLE_SIGNS, null, values)
        db.close()
    }

    // Method to add symptom and rating
    fun addSymptomRating(symptom: String, rating: Float) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SYMPTOM_NAME, symptom)
            put(COLUMN_SYMPTOM_RATING, rating)
        }
        db.insert(TABLE_SYMPTOMS, null, values)
        db.close()
    }
}
