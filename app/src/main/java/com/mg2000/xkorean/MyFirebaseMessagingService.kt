package com.mg2000.xkorean

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
	val mChannelID = "345"

	override fun onNewToken(token: String) {
		println("토큰 갱신: $token")

		val preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
		preference.edit().putString("fcmToken", token).apply()
	}

	override fun onMessageReceived(message: RemoteMessage) {
		val intent = Intent(this, MainActivity::class.java)
		val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

		val builder = NotificationCompat.Builder(this, "xKorean")
			.setSmallIcon(R.drawable.notification)
			.setContentTitle(message.notification?.title ?: "xKorean 알림")
			.setContentText(message.notification?.body ?: "알림 내용이 없습니다.")
			.setStyle(NotificationCompat.BigTextStyle().bigText(message.notification?.body ?: "알림 내용이 없습니다."))
			.setContentIntent(pendingIntent)

		with(NotificationManagerCompat.from(this)) {
			notify(1234, builder.build())
		}
	}
}