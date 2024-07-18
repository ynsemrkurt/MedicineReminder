package com.example.medicinereminder.Room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MedicineDao {
    @Insert
    fun insert(medicine: Medicine): Long

    @Delete
    fun delete(medicine: Medicine)

    @Update
    fun update(medicine: Medicine)

    @Query("SELECT * FROM medicine_table ORDER BY timeToTake ASC")
    fun getAllMedicines(): List<Medicine>
}