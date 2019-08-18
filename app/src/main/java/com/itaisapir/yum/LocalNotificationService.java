package com.itaisapir.yum;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LocalNotificationService extends Service {
    private final int AMOUNT_OF_TIME = 7;
    private final TimeUnit UNIT = TimeUnit.DAYS;

    public static boolean isServiceRunning = false;
    public static boolean toRunService = true;

    @Override
    public void onCreate() {
        super.onCreate();
        if(isServiceRunning)
            stopMyService();
        startServiceWithNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            startServiceWithNotification();
        }
        else stopMyService();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        isServiceRunning = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case of bound services.
        return null;
    }


    private void startServiceWithNotification() {
        if(!toRunService) {
            if (isServiceRunning) {
                stopMyService();
            }
            return;
        }

        if (isServiceRunning) return;
        isServiceRunning = true;
        String channelId = "default_channel";

        Intent notificationIntent = new Intent(getApplicationContext(), LoginActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        final Notification notification = new NotificationCompat.Builder(this,channelId)
                .setContentTitle(getResources().getString(R.string.message_from_yum))
                .setTicker(getResources().getString(R.string.app_name))
                .setContentText(getResources().getString(R.string.body_from_yum))
                .setSmallIcon(R.mipmap.yum_launcher_round)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)
                .build();

        final NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if(toRunService)
                    notificationManager.notify(0, notification);
                else{
                    stopMyService();
                }
            }
        }, AMOUNT_OF_TIME, AMOUNT_OF_TIME, UNIT);
    }

    private void stopMyService() {
        stopSelf();
        isServiceRunning = false;
    }
}