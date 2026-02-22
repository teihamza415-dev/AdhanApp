package com.adhan.app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_CODE = 1001;
    private static final int NOTIFICATION_PERMISSION_CODE = 1002;
    private static final String CHANNEL_ID = "adhan_channel";
    private static final String TAG = "AdhanApp";

    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvLocation, tvDate, tvNextPrayer, tvNextPrayerTime;
    private TextView tvFajr, tvSunrise, tvDhuhr, tvAsr, tvMaghrib, tvIsha;
    private LinearLayout loadingLayout, mainContent;
    private TextView tvLoading;

    private double latitude = 0;
    private double longitude = 0;
    private OkHttpClient httpClient = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        createNotificationChannel();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkPermissions();
    }

    private void initViews() {
        tvLocation = findViewById(R.id.tvLocation);
        tvDate = findViewById(R.id.tvDate);
        tvNextPrayer = findViewById(R.id.tvNextPrayer);
        tvNextPrayerTime = findViewById(R.id.tvNextPrayerTime);
        tvFajr = findViewById(R.id.tvFajr);
        tvSunrise = findViewById(R.id.tvSunrise);
        tvDhuhr = findViewById(R.id.tvDhuhr);
        tvAsr = findViewById(R.id.tvAsr);
        tvMaghrib = findViewById(R.id.tvMaghrib);
        tvIsha = findViewById(R.id.tvIsha);
        loadingLayout = findViewById(R.id.loadingLayout);
        mainContent = findViewById(R.id.mainContent);
        tvLoading = findViewById(R.id.tvLoading);

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE، d MMMM yyyy", new Locale("ar"));
        tvDate.setText(sdf.format(new Date()));
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        } else {
            checkNotificationPermission();
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
                return;
            }
        }
        getCurrentLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkNotificationPermission();
            } else {
                Toast.makeText(this, "تحتاج إذن الموقع لتحديد مواقيت الصلاة", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        tvLoading.setText("جاري تحديد موقعك...");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    SharedPreferences prefs = getSharedPreferences("adhan_prefs", MODE_PRIVATE);
                    prefs.edit().putFloat("lat", (float) latitude).putFloat("lng", (float) longitude).apply();
                    tvLocation.setText(String.format(Locale.US, "%.4f, %.4f", latitude, longitude));
                    tvLoading.setText("جاري جلب مواقيت الصلاة...");
                    fetchPrayerTimes();
                } else {
                    tvLoading.setText("فعّل GPS وأعد فتح التطبيق");
                }
            });
        }
    }

    private void fetchPrayerTimes() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
        String date = sdf.format(new Date());
        String url = "https://api.aladhan.com/v1/timings/" + date
                + "?latitude=" + latitude + "&longitude=" + longitude + "&method=4";

        Request request = new Request.Builder().url(url).build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvLoading.setText("خطأ في الاتصال بالإنترنت"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    JSONObject timings = json.getJSONObject("data").getJSONObject("timings");

                    Map<String, String> times = new HashMap<>();
                    String[] keys = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
                    for (String key : keys) {
                        String t = timings.getString(key);
                        times.put(key, t.contains(" ") ? t.split(" ")[0] : t);
                    }

                    runOnUiThread(() -> {
                        loadingLayout.setVisibility(View.GONE);
                        mainContent.setVisibility(View.VISIBLE);
                        tvFajr.setText(times.get("Fajr"));
                        tvSunrise.setText(times.get("Sunrise"));
                        tvDhuhr.setText(times.get("Dhuhr"));
                        tvAsr.setText(times.get("Asr"));
                        tvMaghrib.setText(times.get("Maghrib"));
                        tvIsha.setText(times.get("Isha"));
                        updateNextPrayer(times);
                        scheduleAlarms(times);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> tvLoading.setText("خطأ في معالجة البيانات"));
                }
            }
        });
    }

    private void updateNextPrayer(Map<String, String> times) {
        String[] keys = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
        String[] arabic = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};
        Calendar now = Calendar.getInstance();
        int currentMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        for (int i = 0; i < keys.length; i++) {
            String time = times.get(keys[i]);
            if (time != null) {
                String[] parts = time.split(":");
                int prayerMin = Integer.parseInt(parts[0].trim()) * 60 + Integer.parseInt(parts[1].trim());
                if (prayerMin > currentMin) {
                    tvNextPrayer.setText("الصلاة القادمة: " + arabic[i]);
                    tvNextPrayerTime.setText(time);
                    return;
                }
            }
        }
        tvNextPrayer.setText("الصلاة القادمة: الفجر غداً");
        tvNextPrayerTime.setText(times.get("Fajr"));
    }

    private void scheduleAlarms(Map<String, String> times) {
        String[] keys = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
        String[] arabic = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        for (int i = 0; i < keys.length; i++) {
            String time = times.get(keys[i]);
            if (time == null) continue;
            try {
                String[] parts = time.split(":");
                int hour = Integer.parseInt(parts[0].trim());
                int minute = Integer.parseInt(parts[1].trim());

                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                if (calendar.before(Calendar.getInstance())) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                }

                Intent intent = new Intent(this, AdhanReceiver.class);
                intent.putExtra("prayer_name", arabic[i]);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, i, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                if (alarmManager != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error scheduling " + keys[i], e);
            }
        }
        Toast.makeText(this, "تم ضبط تنبيهات الأذان", Toast.LENGTH_SHORT).show();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "تنبيهات الأذان", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("تنبيهات مواقيت الصلاة");
            channel.enableVibration(true);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}
