package com.adhan.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.core.app.NotificationCompat;

public class AdhanReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "adhan_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String prayerName = intent.getStringExtra("prayer_name");
        if (prayerName == null) prayerName = "الصلاة";

        // إنشاء قناة الإشعارات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "تنبيهات الأذان", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }

        // فتح التطبيق عند الضغط على الإشعار
        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // صوت التنبيه
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        // بناء الإشعار
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("حان وقت صلاة " + prayerName)
                .setContentText("حيّ على الصلاة - حيّ على الفلاح")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("حان الآن موعد أذان " + prayerName + "\nحيّ على الصلاة.. حيّ على الفلاح"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(soundUri)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 200, 500, 200, 500});

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }

        // اهتزاز إضافي
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) vm.getDefaultVibrator().vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 200, 500}, -1));
            } else {
                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) v.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 200, 500}, -1));
            }
        } catch (Exception ignored) {}
    }
}
