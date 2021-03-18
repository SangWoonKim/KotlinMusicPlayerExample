package com.study.musicplayerexamplekotlin.Service.IntentFilter

//재생상태(액션)를 명시한 클래스
class BroadCastActions {
    companion object {
        const val PREPARED = "PREPARED"
        const val PLAY_STATE_CHANGED = "PLAY_STATE_CHANGED"
    }
}