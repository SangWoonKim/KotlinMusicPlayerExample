package com.study.musicplayerexamplekotlin.Service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.study.musicplayerexamplekotlin.View.AudioAdapter

//서비스 바인드를 위한 클래스
//bindService()를 이용하여 AudioService의 메서드들과 상호작용이 가능한 클래스
class AudioServiceInterface() {
    private var mServiceConnection: ServiceConnection? = null
    private var mService:AudioService? = null

    constructor(context: Context) : this() {
        mServiceConnection = object: ServiceConnection {

            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                mService = (service as AudioService.AudioServiceBinder).getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                mServiceConnection = null
                mService = null
            }
        }
        context.bindService(Intent(context, AudioService::class.java).setPackage(context.packageName),
                mServiceConnection as ServiceConnection, Context.BIND_AUTO_CREATE)
    }

    fun setPlayList(audioIds: ArrayList<Long>){
        if (mService != null){
            mService?.setPlayList(audioIds)
        }
    }

    fun play(position: Int) {
        if (mService != null) {
            mService?.play(position)
            isPlaying()
        }
    }

    fun play() {
        if (mService != null) {
            mService?.play()
        }
    }

    fun pause() {
        if (mService != null) {
            mService?.pause()
        }
    }

    fun forward() {
        if (mService != null) {
            mService?.forward()
        }
    }

    fun rewind() {
        if (mService != null) {
            mService?.rewind()
        }
    }

    //재생중일 경우 일시정지, 일시정지일 경우 재생
    fun togglePlay() {
        if (isPlaying()!!) {
            mService?.pause()
        } else {
            mService?.play()
        }
    }

    //서비스에서 음악의 재생되는지 확인하는 메소드
    fun isPlaying(): Boolean? {
        return if (mService != null) {
            mService?.isPlaying()
        } else false
    }

    //서비스에서 재생하는 음악의 정보를 알아내어 호출시 반환하는 메소드
    fun getAudioItem(): AudioAdapter.AudioItem? {
        return if (mService != null) {
            mService?.getAudioItem()
        } else null
    }


}