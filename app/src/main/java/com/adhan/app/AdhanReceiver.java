package com.adhan.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class AdhanReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "adhan_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String prayerName = intent.getStringExtra("prayer_name");
        
        // مسار صوت الأذان الذي أضفته في مجلد raw
        Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.adhan);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, 
                "تنبيهات الأذان", NotificationManager.IMPORTANCE_HIGH);
            // ربط الصوت بالقناة (لأندرويد 8 فما فوق)
            channel.setSound(soundUri, null);
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }

        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("حان وقت صلاة " + prayerName)
                .setContentText("حيّ على الصلاة - حيّ على الفلاح")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSound(soundUri) // للصدارات الأقدم من أندرويد 8
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(1, builder.build());
    }
}
