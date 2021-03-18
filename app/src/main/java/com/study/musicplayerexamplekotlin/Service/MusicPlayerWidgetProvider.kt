package com.study.musicplayerexamplekotlin.Service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.widget.RemoteViews
import com.squareup.picasso.Picasso
import com.study.musicplayerexamplekotlin.R
import com.study.musicplayerexamplekotlin.Service.IntentFilter.BroadCastActions
import com.study.musicplayerexamplekotlin.Service.IntentFilter.CommandActions
import com.study.musicplayerexamplekotlin.View.MainActivity

class MusicPlayerWidgetProvider : AppWidgetProvider() {

    lateinit var action :String
    lateinit var remoteView: RemoteViews

    //수신자
    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        action= intent?.action!!
        remoteView = RemoteViews(context?.packageName, R.layout.appwidget)
        if (BroadCastActions.PREPARED.equals(action)){
            updateAlbumArt(context, remoteView)
        }
        updatePlayState(context, remoteView)
        updateWidget(context!!, remoteView)
    }

    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    //앨범 사진 갱신시 사용되는 메소드
    fun updateAlbumArt(context: Context?, remoteViews: RemoteViews) {
        var appWidgetManager : AppWidgetManager = AppWidgetManager.getInstance(context)

        val appWidgetIds: IntArray? = appWidgetManager.getAppWidgetIds(ComponentName(context!!, javaClass))
        var albumId : Long = AudioApplication.getInstance()?.getServiceInterface()?.getAudioItem()?.mAlbumId!!
        val albumArtUri : Uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
        try {
            val handler = Handler()
            handler.post {
                Picasso.get().load(albumArtUri).into(remoteViews, R.id.img_widget_albumart, appWidgetIds!!) //에러부분
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    //시작 일시정지 아이콘 상태 변경 및 textview의 내용변경 메소드이며 해당 아이콘 클릭시 상태 변경 메소드
    fun updatePlayState(context: Context?, remoteViews: RemoteViews){
        if (AudioApplication.getInstance()?.getServiceInterface()?.isPlaying()!!){
            remoteViews.setImageViewResource(R.id.btn_widget_play, R.drawable.pause)
        }else{
            remoteViews.setImageViewResource(R.id.btn_widget_play, R.drawable.play)
        }

        var title : String ="재생중인 음악이 없습니다."
        if (AudioApplication.getInstance()?.getServiceInterface()?.getAudioItem() != null){
            title = AudioApplication.getInstance()?.getServiceInterface()?.getAudioItem()?.mTitle!!
        }
        remoteViews.setTextViewText(R.id.tv_widget_title, title)

        var actionLaunch = Intent(context, MainActivity::class.java)
        var actionTogglePlay = Intent(CommandActions.TOGGLE_PLAY)
        var actionForward = Intent(CommandActions.FORWARD)
        var actionRewind = Intent(CommandActions.REWIND)

        var launch = PendingIntent.getActivity(context, 0, actionLaunch, 0)
        var togglePlay = PendingIntent.getService(context, 0, actionTogglePlay, 0)
        var forward = PendingIntent.getService(context, 0, actionForward, 0)
        var rewind = PendingIntent.getService(context, 0, actionRewind, 0)

        //여기도 문제
        remoteViews.setOnClickPendingIntent(R.id.img_widget_albumart, launch)
        remoteViews.setOnClickPendingIntent(R.id.btn_widget_play, togglePlay)
        remoteViews.setOnClickPendingIntent(R.id.btn_widget_forward, forward)
        remoteViews.setOnClickPendingIntent(R.id.btn_widget_rewind, rewind)
    }

    //앱 위젯의 인스턴스를 얻어와 위에서 변경한 내용을 반영하여 갱신 즉 위의 메소드 호출후 이 메소드를 호출해야함
    private fun updateWidget(context: Context, remoteViews: RemoteViews) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, javaClass))
        if (appWidgetIds != null && appWidgetIds.size > 0) {
            appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
        }
    }
}