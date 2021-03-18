package com.study.musicplayerexamplekotlin.Service.IntentFilter

//notification과 appwidget을 위한 액션 정의 클래스
class CommandActions {
    companion object {
        const val REWIND = "REWIND"
        const val TOGGLE_PLAY = "TOGGLE_PLAY"
        const val FORWARD = "FORWARD"
        const val CLOSE = "CLOSE"
    }
}