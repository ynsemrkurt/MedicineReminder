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
import java.util.Calendar

class MedicineReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        //intentten gelen verileri çeker
        val medicineName = intent.getStringExtra("medicineName")
        val medicineDosage = intent.getStringExtra("medicineDosage")
        val medicineId = intent.getIntExtra("medicineId", -1)
        val medicineTime = intent.getStringExtra("medicineTime")
        //Bildirimi oluşturur
        val builder = NotificationCompat.Builder(context, "1")
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("Medicine Reminder")
            .setContentText("It's time to take your $medicineName, $medicineDosage dosage")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        with(NotificationManagerCompat.from(context)) {
            //Bildirim izni varsa bildirimi gösterir
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(medicineId, builder.build())
            }
        }

        //Aynı bildirimi tekrar sonraki güne ekler
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MedicineReminderReceiver::class.java).apply {
            putExtra("medicineName", medicineName)
            putExtra("medicineDosage", medicineDosage)
            putExtra("medicineId", medicineId)
            putExtra("medicineTime", medicineTime)
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
