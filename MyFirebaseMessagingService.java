package com.ece.aractakip;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.ece.aractakip.data.AnomalyNotificationRepository;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        if (remoteMessage.getNotification() != null) {
            String baslik = remoteMessage.getNotification().getTitle();
            String icerik = remoteMessage.getNotification().getBody();
            if (icerik != null && !icerik.trim().isEmpty()) {
                AnomalyNotificationRepository.add(this, icerik.trim());
            }
            bildirimFirlat(baslik, icerik);
        }
    }

    private void bildirimFirlat(String baslik, String icerik) {

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        String kanalId = "Arac_Anomali_Kanali";


        NotificationCompat.Builder bildirimMimar = new NotificationCompat.Builder(this, kanalId)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(baslik)
                .setContentText(icerik)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager bildirimYoneticisi = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel kanal = new NotificationChannel(
                    kanalId,
                    "Araç Anomali Uyarıları",
                    NotificationManager.IMPORTANCE_HIGH
            );
            bildirimYoneticisi.createNotificationChannel(kanal);
        }

        bildirimYoneticisi.notify(1, bildirimMimar.build());
    }
}