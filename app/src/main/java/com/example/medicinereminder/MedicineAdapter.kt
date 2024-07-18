package com.example.medicinereminder

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.medicinereminder.Room.Medicine
import com.example.medicinereminder.Room.MedicineDatabase
import com.example.medicinereminder.databinding.ItemReminderBinding
import java.util.Calendar

class MedicineAdapter(private val context: Context) :
    ListAdapter<Medicine, MedicineAdapter.MedicineViewHolder>(MedicineDiffCallback()) {

    private var alarmClockInfo: AlarmManager.AlarmClockInfo? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val binding =
            ItemReminderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MedicineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    // Verilen yeni medicine listesini RecyclerView'a gönderir
    fun submitMedicineList(newList: List<Medicine>) {
        submitList(newList)
    }

    inner class MedicineViewHolder(private val binding: ItemReminderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(medicine: Medicine) {
            binding.textViewMedicine.text = medicine.name
            binding.textViewTime.text = medicine.timeToTake
            binding.textViewDosage.text = "${medicine.dosage} Dosage"

            // Medicine silinir Alarm silinir.
            binding.imageButtonDelete.setOnClickListener {
                showDeleteDialog(medicine)
            }

            // Dialog açar
            binding.imageButtonEdit.setOnClickListener {
                showEditDialog(medicine)
            }
        }
    }

    // Medicine silme işlemini gerçekleştirir
    private fun deleteMedicine(deletedItem: Medicine) {
        val dao = MedicineDatabase.getDatabase(context).medicineDao()
        dao.delete(deletedItem)
        submitMedicineList(dao.getAllMedicines())
        showToast("Medicine Reminder Deleted!")
    }

    // Alarmı iptal eder
    private fun cancelAlarm(medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MedicineReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicine.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    // Toast mesajını gösterir
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Düzenleme dialogunu gösterir
    private fun showEditDialog(medicine: Medicine) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_medicine, null)
        val alertDialogBuilder = AlertDialog.Builder(context, R.style.TransparentDialog)
            .setView(dialogView)
            .create()

        val editTextMedicine = dialogView.findViewById<EditText>(R.id.editTextMedicine)
        val editTextDosage = dialogView.findViewById<EditText>(R.id.editTextDosage)
        val editTextHour = dialogView.findViewById<EditText>(R.id.editTextHour)
        val editTextMinute = dialogView.findViewById<EditText>(R.id.editTextMinute)
        val buttonSave = dialogView.findViewById<Button>(R.id.buttonSave)

        editTextMedicine.setText(medicine.name)
        editTextDosage.setText(medicine.dosage)
        editTextHour.focusable

        buttonSave.setOnClickListener {
            val updatedName = editTextMedicine.text.toString()
            val updatedDosage = editTextDosage.text.toString()
            var updatedHour = editTextHour.text.toString()
            var updatedMinute = editTextMinute.text.toString()

            // Gerekli alanların doldurulup doldurulmadığını kontrol eder
            if (updatedName.isBlank() || updatedDosage.isBlank() || updatedHour.isBlank() || updatedMinute.isBlank()) {
                showToast("Please fill in all fields!")
                return@setOnClickListener
            } else if (updatedHour.toInt() !in 0..23) {
                showToast("Please enter a valid hour (0-23)")
                return@setOnClickListener
            } else if (updatedMinute.toInt() !in 0..59) {
                showToast("Please enter a valid minute (0-59)")
                return@setOnClickListener
            } else {

                // Saat ve dakika değerlerini kontrol eder ve 0 ekler
                if (updatedHour.length==1 && updatedHour.toInt() in 0..9) {
                    updatedHour = "0$updatedHour"
                }

                if (updatedMinute.length==1 && updatedMinute.toInt() in 0..9) {
                    updatedMinute = "0$updatedMinute"
                }

                val updatedTime = "$updatedHour:$updatedMinute"
                val updatedMedicine = Medicine(medicine.id, updatedName, updatedDosage, updatedTime)
                cancelAlarm(medicine)
                updateMedicine(updatedMedicine)
                scheduleMedicineReminder(context, updatedMedicine)

                showToast("Medicine Reminder Updated!")
                alertDialogBuilder.dismiss()
            }
        }

        alertDialogBuilder.show()
    }

    // Medicine silme dialogunu gösterir
    private fun showDeleteDialog(medicine: Medicine) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_delete_medicine, null)
        val alertDialogBuilder = AlertDialog.Builder(context, R.style.TransparentDialog)
            .setView(dialogView)
            .create()

        val buttonDelete = dialogView.findViewById<Button>(R.id.buttonDelete)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)

        buttonDelete.setOnClickListener {
            deleteMedicine(medicine)
            cancelAlarm(medicine)
            showToast("Medicine Reminder Updated!")
            alertDialogBuilder.dismiss()
        }

        buttonCancel.setOnClickListener {
            alertDialogBuilder.dismiss()
        }

        alertDialogBuilder.show()
    }

    // Medicine Reminder'i planlar
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

        // Alarm zamanı bugün için geçmişse bir sonraki gün için ayarlar
        alarmClockInfo = if (calendar.timeInMillis < System.currentTimeMillis()) {
            AlarmManager.AlarmClockInfo(calendar.timeInMillis + 86400000, pendingIntent)
        } else {
            AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
        }

        alarmManager.setAlarmClock(alarmClockInfo!!, pendingIntent)
    }

    // Medicine verisini günceller
    private fun updateMedicine(updatedMedicine: Medicine) {
        val dao = MedicineDatabase.getDatabase(context).medicineDao()
        dao.update(updatedMedicine)
        submitMedicineList(dao.getAllMedicines())
    }

    // Medicine listesi için fark algoritması
    private class MedicineDiffCallback : DiffUtil.ItemCallback<Medicine>() {
        override fun areItemsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem == newItem
        }
    }
}