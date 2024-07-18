package com.example.medicinereminder.Room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Medicine::class], version = 1, exportSchema = false)
abstract class MedicineDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao

    companion object {
        private var instance: MedicineDatabase? = null

        fun getDatabase(context: Context): MedicineDatabase {
            // Eğer instance null ise, yeni bir veritabanı oluşturur
            if (instance == null) {
                instance = Room.databaseBuilder(
                    context,
                    MedicineDatabase::class.java,
                    "medicine_database"
                ).allowMainThreadQueries()
                    .build()
            }
            // Veritabanı nesnesini döndür
            return instance as MedicineDatabase
        }
    }
}