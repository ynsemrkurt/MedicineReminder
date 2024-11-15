package com.example.medicinereminder

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.medicinereminder.Const.MED_DOSAGE
import com.example.medicinereminder.Const.MED_ID
import com.example.medicinereminder.Const.MED_NAME
import com.example.medicinereminder.Const.MED_TIME
import java.util.Calendar

class MedicineReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val medicineName = intent.getStringExtra(MED_NAME)
        val medicineDosage = intent.getStringExtra(MED_DOSAGE)
        val medicineId = intent.getIntExtra(MED_ID, -1)
        val medicineTime = intent.getStringExtra(MED_TIME)

        val builder = NotificationCompat.Builder(context, "1")
            .setSmallIcon(R.drawable.logo_medicine)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(
                context.getString(
                    R.string.it_s_time_to_take_your_dosage,
                    medicineName,
                    medicineDosage
                ))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        with(NotificationManagerCompat.from(context)) {

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(medicineId, builder.build())
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MedicineReminderReceiver::class.java).apply {
            putExtra(MED_NAME, medicineName)
            putExtra(MED_DOSAGE, medicineDosage)
            putExtra(MED_ID, medicineId)
            putExtra(MED_TIME, medicineTime)
        }
        val pendingIntent = medicineId.let {
            PendingIntent.getBroadcast(
                context,
                it,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, medicineTime!!.split(":")[0].toInt())
            set(Calendar.MINUTE, medicineTime.split(":")[1].toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val alarmClockInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis+86400000, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }
}
