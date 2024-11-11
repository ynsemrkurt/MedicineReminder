package com.example.medicinereminder.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicine_table")
data class Medicine(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    val name: String,
    val dosage: String,
    val timeToTake: String
)
