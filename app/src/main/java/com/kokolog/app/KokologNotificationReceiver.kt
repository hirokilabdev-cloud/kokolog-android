package com.kokolog.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlin.random.Random
import android.app.PendingIntent

class KokologNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        showKokologNotification(context)
        scheduleKokologNotification(context, 21, 0)
    }
}

fun showKokologNotification(context: Context) {
    val channelId = "kokolog_daily_channel"

    val messages = listOf(
        "今日のココロを、そっと置いてみますか？",
        "今の気持ちを、少しだけ残してみましょう。",
        "ふり返るためじゃなく、置いておくために。",
        "今日はどんな一日でしたか？",
        "どんなココロも、そのままで大丈夫。"
    )

    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "ココログ通知",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    val launchIntent = Intent(context, MainActivity::class.java)

    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("ココログ")
        .setContentText(messages[Random.nextInt(messages.size)])
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(1001, notification)
}