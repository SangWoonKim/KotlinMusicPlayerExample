package com.study.musicplayerexamplekotlin.View

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.squareup.picasso.Picasso
import com.study.musicplayerexamplekotlin.R
import com.study.musicplayerexamplekotlin.Service.AudioService
import com.study.musicplayerexamplekotlin.Service.IntentFilter.CommandActions

class NotificationPlayer {
    companion object{
        private const val NOTIFICATION_CHANNEL_ID = "channel_id"
        private const val NOTIFICATION_PLAYER_ID = 0X342
    }
    private var mService: AudioService
    private var notificationManager  : NotificationManager                                          //notification 생성 및 노출에 사용되는 객체 정의
    private lateinit var notificationManagerCompat: NotificationManagerCompat
    private var notificationManagerBuilder: NotificationManagerBuilder? = null
    private var isForeground : Boolean = false

    //생성자 서비스 실행중 또는 생성하여 AudioService를 받아와 NotificationManager에 할당
    constructor(service: AudioService){
        mService = service
        notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    //notification 갱신시 호출되는 메소드(음악 변경 또는 재생상태 변경시)
    fun updateNotificationPlayer(){
        cancel()
        notificationManagerBuilder = NotificationManagerBuilder()
        notificationManagerBuilder!!.execute()
    }

    fun removeNotificationPlayer(){
        cancel()
        mService.stopForeground(true)
        isForeground = false
    }

    //NotificationManagerBuilder(AsyncTask)를 취소하는 역할
    private fun cancel(){
        if (notificationManagerBuilder != null){
            notificationManagerBuilder!!.cancel(true)
            notificationManagerBuilder = null
        }
    }

    inner class NotificationManagerBuilder : AsyncTask<Void, Void, Notification>() {
       private lateinit var remoteViews : RemoteViews
       private lateinit var notificationBuilder : NotificationCompat.Builder
       private lateinit var mainPenddingIntent : PendingIntent

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onPreExecute() {
            super.onPreExecute()
            var mainActivityIntent : Intent = Intent(mService, MainActivity::class.java)
            mainPenddingIntent = PendingIntent.getActivity(mService, 0, mainActivityIntent, 0)
            remoteViews = createRemoteView(R.layout.notification_player)

            if (Build.VERSION.SDK_INT<Build.VERSION_CODES.O){
                notificationBuilder = NotificationCompat.Builder(mService)
                notificationBuilder.setSmallIcon(R.mipmap.ic_launcher)
                        .setOngoing(true)
                        .setContentIntent(mainPenddingIntent)
                        .setContent(remoteViews)

                var notification: Notification = notificationBuilder.build()
                notification.priority = NotificationCompat.PRIORITY_MAX
                notification.contentIntent = mainPenddingIntent
                if (!isForeground){
                    isForeground = true
                    mService.startForeground(NOTIFICATION_PLAYER_ID, notification)
                }
            }

            //Oreo 이후 버전
            if (Build.VERSION.SDK_INT>Build.VERSION_CODES.O){
                var channelMessage : NotificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "channel", NotificationManager.IMPORTANCE_HIGH)
                channelMessage.description="Custom음악플레이어"
                notificationManager.createNotificationChannel(channelMessage)

                notificationBuilder = NotificationCompat.Builder(mService, NOTIFICATION_CHANNEL_ID)
                notificationBuilder.setSmallIcon(R.mipmap.ic_launcher)
                        .setOngoing(true)
                        .setContentIntent(mainPenddingIntent)
                        .setCustomContentView(remoteViews)
                var notification : Notification = notificationBuilder.build()
                notification.contentIntent = mainPenddingIntent
                if (!isForeground){
                    isForeground = true
                    mService.startForeground(NOTIFICATION_PLAYER_ID, notification)
                }
            }
        }


        override fun doInBackground(vararg params: Void?): Notification {
            notificationBuilder.setCustomContentView(remoteViews)
            notificationBuilder.setContentIntent(mainPenddingIntent)
            notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH)

            var notification : Notification = notificationBuilder.build()
            updateRemoteView(remoteViews, notification)
            return notification
        }

        override fun onPostExecute(notification: Notification?) {
            super.onPostExecute(notification)
            try {
                notificationManager.notify(NOTIFICATION_PLAYER_ID, notification)
            }catch (e: Exception){
                e.printStackTrace()
            }
        }

        //여기서 문제 클릭시 응답없음
        @RequiresApi(Build.VERSION_CODES.O)
        private fun createRemoteView(layoutId: Int) : RemoteViews {
            val actionTogglePlay = Intent(CommandActions.TOGGLE_PLAY)
            val actionForward = Intent(CommandActions.FORWARD)
            val actionRewind = Intent(CommandActions.REWIND)
            val actionClose = Intent(CommandActions.CLOSE)

            //getSerivce()는 startService()대응
            //getForegroundService()는  startForegroundService()대응
            //왜 안될까? 이미 서비스 실행중이라 바로 onStartCommand로 갈텐데 왜 안갈까?
            val togglePlay = PendingIntent.getService(mService, 0, actionTogglePlay, 0)
            val forward = PendingIntent.getService(mService, 0, actionForward, 0)
            val rewind = PendingIntent.getService(mService, 0, actionRewind, 0)
            val close = PendingIntent.getService(mService, 0, actionClose, 0)

            val remoteView = RemoteViews(mService.packageName, layoutId).apply{
                //logcat에도 안나옴 notification이 갱신될때는 호출됨
                setOnClickPendingIntent(R.id.btn_noti_play2, togglePlay)
                setOnClickPendingIntent(R.id.btn_noti_forward3, forward)
                setOnClickPendingIntent(R.id.btn_noti_rewind1, rewind)
                setOnClickPendingIntent(R.id.btn_noti_close4, close)
            }



            return remoteView
        }

        //remoteView 갱신시 사용 되는 메소드
        private fun updateRemoteView(remoteViews: RemoteViews, notification: Notification?){

            if (mService.isPlaying()== true){
                remoteViews.setImageViewResource(R.id.btn_noti_play2, R.drawable.pause)
            }else{
                remoteViews.setImageViewResource(R.id.btn_noti_play2, R.drawable.play)
            }
            var title: String = mService.getAudioItem()?.mTitle!!
            remoteViews.setTextViewText(R.id.tv_noti_title, title)
            val albumArtUri: Uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mService.getAudioItem()!!.mAlbumId)
            Picasso.get().load(albumArtUri).error(R.drawable.ic_launcher_background).into(remoteViews, R.id.img_noti_album, NOTIFICATION_PLAYER_ID, notification!!)
        }
    }
}