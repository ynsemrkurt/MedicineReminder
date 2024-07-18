package com.example.medicinereminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.medicinereminder.Room.Medicine
import com.example.medicinereminder.Room.MedicineDatabase
import com.example.medicinereminder.databinding.FragmentMedicineBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

class MedicineFragment : Fragment() {

    private lateinit var binding: FragmentMedicineBinding
    private var alarmClockInfo: AlarmManager.AlarmClockInfo? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMedicineBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Databaseyi belirtir
        val medicineDatabase: MedicineDatabase = MedicineDatabase.getDatabase(requireContext())

        binding.imageButtonBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.buttonSave.setOnClickListener {
            //Verileri kontrol eder
            if (binding.editTextMedicine.text.toString().trim().isEmpty()) {
                showToast("Please enter medicine name")
                return@setOnClickListener
            }
            if (binding.editTextDosage.text.toString().trim().isEmpty()) {
                showToast("Please enter dosage")
                return@setOnClickListener
            }
            if (binding.editTextHour.text.toString().trim()
                    .isEmpty() || binding.editTextHour.text.toString().toInt() > 23
            ) {
                showToast("Please enter valid hour")
                return@setOnClickListener
            }
            if (binding.editTextMinute.text.toString().trim()
                    .isEmpty() || binding.editTextMinute.text.toString().toInt() > 59
            ) {
                showToast("Please enter valid minute")
                return@setOnClickListener
            }

            //Saat ve dakika değerlerini kontrol eder gerekirse 0 ekler
            if (binding.editTextHour.text.toString().length == 1 && binding.editTextHour.text.toString()
                    .toInt() in 0..9
            ) {
                binding.editTextHour.setText("0${binding.editTextHour.text}")
            }

            if (binding.editTextMinute.text.toString().length == 1 && binding.editTextMinute.text.toString()
                    .toInt() in 0..9
            ) {
                binding.editTextMinute.setText("0${binding.editTextMinute.text}")
            }

            val name = binding.editTextMedicine.text.toString()
            val dosage = binding.editTextDosage.text.toString()
            val time =
                binding.editTextHour.text.toString() + ":" + binding.editTextMinute.text.toString()
            val medicine = Medicine(name = name, dosage = dosage, timeToTake = time)
            //Veritabanına ekler
            val medicineId = medicineDatabase.medicineDao().insert(medicine)
            //Medicine nesnesinin id'sini alır
            medicine.id = medicineId.toInt()
            scheduleMedicineReminder(requireContext(), medicine)
            showMaterialDialog()
        }
    }

    //AlarmManager'a ekler
    private fun scheduleMedicineReminder(context: Context, medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MedicineReminderReceiver::class.java).apply {
            putExtra("medicineName", medicine.name)
            putExtra("medicineDosage", medicine.dosage)
            putExtra("medicineId", medicine.id)
            putExtra("medicineTime", medicine.timeToTake)
        }
        val pendingIntent = medicine.id.let {
            PendingIntent.getBroadcast(
                context,
                it,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, medicine.timeToTake.split(":")[0].toInt())
            set(Calendar.MINUTE, medicine.timeToTake.split(":")[1].toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        //Geçmiş bir saat ise alarmı sonraki güne kurar
        alarmClockInfo = if (calendar.timeInMillis < System.currentTimeMillis()) {
            AlarmManager.AlarmClockInfo(calendar.timeInMillis + 86400000, pendingIntent)
        } else {
            AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
        }

        alarmManager.setAlarmClock(alarmClockInfo!!, pendingIntent)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    //Aynı ilaça ait veri kaydedilecekse sadece saat ve dakika EditText'lerini boşaltır
    private fun showMaterialDialog() {
        MaterialAlertDialogBuilder(requireContext()).setTitle("Reminder Successfully Added")
            .setMessage("Will you set another reminder for this medication?")
            .setPositiveButton("Add") { _, _ ->
                clearTime()
            }.setNegativeButton("Cancel") { _, _ ->
                clearAll()
            }.show()
    }

    //Tüm EditText'leri temizler
    private fun clearAll() {
        binding.editTextMedicine.text.clear()
        binding.editTextDosage.text.clear()
        binding.editTextHour.text.clear()
        binding.editTextMinute.text.clear()
        binding.editTextMedicine.requestFocus()
    }

    //Saat ve Dakika EditText'lerini temizler
    private fun clearTime() {
        binding.editTextHour.text.clear()
        binding.editTextMinute.text.clear()
        binding.editTextHour.requestFocus()
    }
}

