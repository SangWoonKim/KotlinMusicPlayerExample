package com.study.musicplayerexamplekotlin.Service

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.provider.MediaStore
import android.util.Log
import com.study.musicplayerexamplekotlin.Service.IntentFilter.BroadCastActions
import com.study.musicplayerexamplekotlin.Service.IntentFilter.CommandActions
import com.study.musicplayerexamplekotlin.View.AudioAdapter
import com.study.musicplayerexamplekotlin.View.NotificationPlayer
import java.io.IOException
import java.util.*

class AudioService : Service(),
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener{
    private val mBinder: IBinder =AudioServiceBinder()
    private var mMediaPlayer: MediaPlayer? = null
    private var isPrepared = false
    private var mAudioIdArrayList = ArrayList<Long>()
    private var mCurrentPosition = 0
    private var mAudioItem : AudioAdapter.AudioItem? = null
    private var notificationPlayer: NotificationPlayer? = null
    companion object{
        //서비스 객체화 할시 (서비스가 인스턴스화 할 시 appwidgetprovider의 onReceive()실행) 즉 서비스 생성과 동시 appwidget을 위한 수신자 등록
        var appBroadCast: MusicPlayerWidgetProvider = MusicPlayerWidgetProvider()
    }

    //서비스의 노래목록 저장소인 mAudioArrayList에 노래 항목 추가
    fun setPlayList(audioIds: ArrayList<Long>) {
        if (mAudioIdArrayList.size != audioIds.size) {
            if (audioIds != mAudioIdArrayList) {
                mAudioIdArrayList.clear()
                mAudioIdArrayList.addAll(audioIds)
            }
        }
    }

    //재생할 음악들(mAudioIdArrayList)의 정보를 불러오는 메소드
    private fun queryAudioItem(position: Int) {
        mCurrentPosition = position
        var audioId = mAudioIdArrayList[position]
        var uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        var projection : Array<String> = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
        )
        var selection = MediaStore.Audio.Media._ID + " = ?"
        var selectionArgs = arrayOf(audioId.toString())
        var cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        if (cursor != null) {
            if (cursor.count > 0) {
                cursor.moveToFirst()
                //현재 시점의 커서의 데이터를 저장
                mAudioItem = AudioAdapter.AudioItem.bindCursor(cursor)
                Log.d(javaClass.simpleName,"서비스 커서 호출"+cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)))
            }
            cursor.close()
        }
    }

    //MediaPlayer를 재생가능한 상태로 만들어주는 메소드(MediaPlayer객체의 상태제어 주기에 따른 메소드)
    fun prepare(){
        try {
            mMediaPlayer?.setDataSource(mAudioItem?.mDataPath)
            mMediaPlayer?.setAudioAttributes(AudioAttributes
                    .Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
            mMediaPlayer?.prepareAsync()
        }catch (e: IOException){
            e.printStackTrace()
        }
    }

    //position 인자값을 갖는 play 함수는 최초 재생시에 호출하게 될 목적으로 구현
    fun play(position: Int) {
        queryAudioItem(position) //목록 불러오기
        stop() //정지
        prepare() //준비
        updateNotificationPlayer()
    }

    open fun play(){
        if (isPrepared){
            mMediaPlayer?.start()
            sendBroadcast(Intent(BroadCastActions.PLAY_STATE_CHANGED))
            updateNotificationPlayer()
        }
    }
    open fun pause() {
        if (isPrepared) {
            mMediaPlayer?.pause()
            sendBroadcast(Intent(BroadCastActions.PLAY_STATE_CHANGED)) //재생상태 변경알림
            updateNotificationPlayer()
        }
    }

    //음악을 종료하는 메소드
    private fun stop() {
        mMediaPlayer?.stop()
        mMediaPlayer?.reset()
    }


    //앞 시점의 음악 재생
    open fun forward() {
        if (mAudioIdArrayList.size - 1 > mCurrentPosition) {
            mCurrentPosition++
        } else {
            mCurrentPosition = 0
        }
        sendBroadcast(Intent(BroadCastActions.PLAY_STATE_CHANGED))
        play(mCurrentPosition)
    }

    //이전 시점의 음악 재생
    open fun rewind() {
        if (mCurrentPosition > 0) {
            mCurrentPosition--
        } else {
            mCurrentPosition = mAudioIdArrayList.size - 1
        }
        sendBroadcast(Intent(BroadCastActions.PLAY_STATE_CHANGED))
        play(mCurrentPosition)
    }

    //브로드캐스트를 이용하여 재생상태가 바뀌였음을 알리는 메소드
    private fun updateNotificationPlayer() {
        if (notificationPlayer != null) {
            notificationPlayer!!.updateNotificationPlayer()
            sendBroadcast(Intent(BroadCastActions.PLAY_STATE_CHANGED))
        }
    }

    private fun removeNotificationPlayer() {
        if (notificationPlayer != null) {
            notificationPlayer!!.removeNotificationPlayer()
            sendBroadcast(Intent(BroadCastActions.PLAY_STATE_CHANGED))
        }
    }

    //현재 실행하는 음악의 정보를 반환하는 메소드
    fun getAudioItem(): AudioAdapter.AudioItem? {
        return mAudioItem
    }

    inner class AudioServiceBinder : Binder() {
        fun getService(): AudioService? {
            //사용자가 이 클래스(서비스 클래스)에서 public으로 선언된 메소드를 호출할 수 있도록 인스턴스를 반환
            //즉 이 클래스의 binder(binder는 다른 앱 또는 컴포넌트에서 서비스의 메소드나 기능을 호출할 수 있는 객체)를 반환
            return this@AudioService
        }
    }

    //미디어 플레이어의 재생 상태를 반환하는 메소드, 재생일 경우 true
    open fun isPlaying(): Boolean? {
        return mMediaPlayer?.isPlaying
    }

    private fun registerBroadCast(){
        val filter = IntentFilter()
        filter.addAction(BroadCastActions.PLAY_STATE_CHANGED)
        filter.addAction(BroadCastActions.PREPARED)
        registerReceiver(appBroadCast, filter)
    }

    private fun unregisterBroadCast() {
        unregisterReceiver(appBroadCast)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("oncreate확인","호출됨")
        notificationPlayer = NotificationPlayer(this)
        mMediaPlayer = MediaPlayer()
        mMediaPlayer?.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)

        mMediaPlayer?.setOnPreparedListener(this@AudioService)
        mMediaPlayer?.setOnCompletionListener(this@AudioService)
        mMediaPlayer?.setOnErrorListener(this@AudioService)
        mMediaPlayer?.setOnSeekCompleteListener(this@AudioService)
        registerBroadCast()
    }

    override fun onBind(intent: Intent?): IBinder? {
       return mBinder
    }

    //notificationPlayer에서 setOnclickPendingIntent가 실행되지 않으니 당연히 액션을 못받음
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("onstartcommamd확인",intent?.action+"호출됨")
        if (intent != null) {
            val action = intent.action
            if (CommandActions.TOGGLE_PLAY.equals(action)) {
                if (isPlaying()!!) { //만약 재생중일 경우
                    pause()
                } else { //아닐경우
                    play()
                }
            } else if (CommandActions.REWIND.equals(action)) {
                rewind()
            } else if (CommandActions.FORWARD.equals(action)) {
                forward()
            } else if (CommandActions.CLOSE.equals(action)) {
                pause()
                removeNotificationPlayer()
            }
        }
        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        if (mMediaPlayer != null) {
            mMediaPlayer?.stop()
            mMediaPlayer?.release()
            mMediaPlayer = null
        }
        unregisterBroadCast()
    }

    override fun onPrepared(mp: MediaPlayer?) {
        isPrepared = true
        mp!!.start() //mediaPlayer재생
        sendBroadcast(Intent(BroadCastActions.PREPARED)) //prepared 전송(즉 준비 완료시 broadcast를 통해 상태 알림)
        updateNotificationPlayer()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        isPrepared = false
        sendBroadcast(Intent(BroadCastActions.PLAY_STATE_CHANGED)) // 재생상태 변경 전송
        updateNotificationPlayer()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        isPrepared = false
        sendBroadcast(Intent(BroadCastActions.PLAY_STATE_CHANGED)) // 재생상태 변경 전송
        updateNotificationPlayer()
        return false
    }

    override fun onSeekComplete(mp: MediaPlayer?) {

    }
}