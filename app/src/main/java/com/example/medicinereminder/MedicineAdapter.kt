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
import com.example.medicinereminder.Const.MED_DOSAGE
import com.example.medicinereminder.Const.MED_ID
import com.example.medicinereminder.Const.MED_NAME
import com.example.medicinereminder.Const.MED_TIME
import com.example.medicinereminder.room.Medicine
import com.example.medicinereminder.room.MedicineDatabase
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

    fun submitMedicineList(newList: List<Medicine>) {
        submitList(newList)
    }

    inner class MedicineViewHolder(private val binding: ItemReminderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(medicine: Medicine) {
            binding.textViewMedicine.text = medicine.name
            binding.textViewTime.text = medicine.timeToTake
            binding.textViewDosage.text = context.getString(R.string.maybe_dosage, medicine.dosage)

            binding.imageButtonDelete.setOnClickListener {
                showDeleteDialog(medicine)
            }

            binding.imageButtonEdit.setOnClickListener {
                showEditDialog(medicine)
            }
        }
    }

    private fun deleteMedicine(deletedItem: Medicine) {
        val dao = MedicineDatabase.getDatabase(context).medicineDao()
        dao.delete(deletedItem)
        submitMedicineList(dao.getAllMedicines())
        showToast(context.getString(R.string.medicine_reminder_deleted))
    }

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

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

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

            if (updatedName.isBlank() || updatedDosage.isBlank() || updatedHour.isBlank() || updatedMinute.isBlank()) {
                showToast(context.getString(R.string.please_fill_in_all_fields))
                return@setOnClickListener
            } else if (updatedHour.toInt() !in 0..23) {
                showToast(context.getString(R.string.please_enter_a_valid_hour))
                return@setOnClickListener
            } else if (updatedMinute.toInt() !in 0..59) {
                showToast(context.getString(R.string.please_enter_a_valid_minute))
                return@setOnClickListener
            } else {

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

                showToast(context.getString(R.string.medicine_reminder_updated))
                alertDialogBuilder.dismiss()
            }
        }

        alertDialogBuilder.show()
    }

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
            showToast(context.getString(R.string.medicine_reminder_updated))
            alertDialogBuilder.dismiss()
        }

        buttonCancel.setOnClickListener {
            alertDialogBuilder.dismiss()
        }

        alertDialogBuilder.show()
    }

    private fun scheduleMedicineReminder(context: Context, medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MedicineReminderReceiver::class.java).apply {
            putExtra(MED_NAME, medicine.name)
            putExtra(MED_DOSAGE, medicine.dosage)
            putExtra(MED_ID, medicine.id)
            putExtra(MED_TIME, medicine.timeToTake)
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

        alarmClockInfo = if (calendar.timeInMillis < System.currentTimeMillis()) {
            AlarmManager.AlarmClockInfo(calendar.timeInMillis + 86400000, pendingIntent)
        } else {
            AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
        }

        alarmManager.setAlarmClock(alarmClockInfo!!, pendingIntent)
    }

    private fun updateMedicine(updatedMedicine: Medicine) {
        val dao = MedicineDatabase.getDatabase(context).medicineDao()
        dao.update(updatedMedicine)
        submitMedicineList(dao.getAllMedicines())
    }

    private class MedicineDiffCallback : DiffUtil.ItemCallback<Medicine>() {
        override fun areItemsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem == newItem
        }
    }
}