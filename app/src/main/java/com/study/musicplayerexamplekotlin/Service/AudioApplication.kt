package com.study.musicplayerexamplekotlin.Service

import android.app.Application

//서비스(AudioServiceInterface)에 대해 다른 컴포넌트들이 공동으로 AudioService 클래스에 접근할 수 있도록하는 클래스

class AudioApplication: Application() {

    companion object{
        private var mInstance: AudioApplication?= null
        @JvmStatic
        fun getInstance(): AudioApplication? { return mInstance }
    }


    private var serviceInterface:AudioServiceInterface?=null

    fun getServiceInterface(): AudioServiceInterface? { return serviceInterface }


    override fun onCreate() {
        super.onCreate()
        mInstance = this
        serviceInterface = AudioServiceInterface(applicationContext)
    }
}