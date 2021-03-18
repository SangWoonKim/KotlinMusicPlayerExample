package com.study.musicplayerexamplekotlin.View

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.study.musicplayerexamplekotlin.R
import com.study.musicplayerexamplekotlin.Service.AudioApplication
import java.text.SimpleDateFormat


class AudioAdapter : CursorRecyclerViewAdapter<RecyclerView.ViewHolder> {

//    constructor(context: Context, cursor: Cursor?) : this(){
//        if (cursor == null) {
//            super.fromContextCursor(context, cursor)
//        }
//    }

    constructor(context: Context, cursor: Cursor?) : super(context, cursor){

    }


    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, cursor: Cursor?) {
        var audioItem:AudioItem = AudioItem.bindCursor(cursor!!)
        (viewHolder as AudioItemViewHolder).setAudioItem(audioItem, cursor.position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        var v: View? = LayoutInflater.from(parent.context).inflate(
                R.layout.item_music,
                parent,
                false
        )
        return AudioItemViewHolder(v)
    }

//    fun getAudioIds():ArrayList<Long>{
//        var count:Int = itemCount
//        var audioIds : ArrayList<Long> = ArrayList()
//        for (i in 0..count){
//            audioIds.add(getItemId(i))
//        }
//        return audioIds
//    }

    //null이 반환됨 왜?
    //호출도 안됨
    fun getAudioIds(): java.util.ArrayList<Long>? {
        var count = itemCount
        var audioIds = java.util.ArrayList<Long>()
        for (i in 0 until count) {
            audioIds.add(getItemId(i))
        }
        return audioIds
    }

    class AudioItem {
        var mId //오디오 고유 ID
                : Long = 0
        var mAlbumId // 오디오 앨범아트 ID
                : Long = 0
        var mTitle // 타이틀 정보
                : String? = null
        var mArtist // 아티스트 정보
                : String? = null
        var mAlbum // 앨범 정보
                : String? = null
        var mDuration // 재생시간
                : Long = 0
        var mDataPath // 실제 데이터위치
                : String? = null


        //cursor에 있는 값을 각각의 맴버변수에 setting
        //생성자 사용
        companion object{
            fun bindCursor(cursor: Cursor): AudioItem {
                val audioItem = AudioItem()
                audioItem.mId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns._ID))
                audioItem.mAlbumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID))
                audioItem.mTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE))
                audioItem.mArtist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST))
                audioItem.mAlbum = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM))
                audioItem.mDuration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION))
                audioItem.mDataPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA))
                return audioItem
            }
        }
    }



    inner class AudioItemViewHolder : RecyclerView.ViewHolder {

        private val artworkUri: Uri = Uri.parse("content://media/external/audio/albumart")
        private var imgAlbum: ImageView
        private var tvMainTitle: TextView
        private var tvSubTitle: TextView
        private var tvDuration: TextView
        private var item: AudioItem?=null
        private var mPosition: Int = 0

        constructor(itemView: View?) : super(itemView!!) {
            imgAlbum = itemView?.findViewById<View>(R.id.img_album) as ImageView
            tvMainTitle = itemView?.findViewById<View>(R.id.tv_main_title) as TextView
            tvSubTitle = itemView?.findViewById<View>(R.id.tv_sub_title) as TextView
            tvDuration = itemView?.findViewById<View>(R.id.tv_duration) as TextView
            itemView.setOnClickListener(View.OnClickListener {
                AudioApplication.getInstance()?.getServiceInterface()?.setPlayList(getAudioIds()!!) // 포지션값 보내는거 확인
                AudioApplication.getInstance()?.getServiceInterface()?.play(mPosition) // 선택한 오디오 재생
            })

        }
        //각각의 item들에 대해 정의
        fun setAudioItem(item: AudioItem, position: Int) {
            this.item = item
            mPosition = position;
            tvMainTitle.setText(item.mTitle)
            tvSubTitle.text = item.mArtist + "(" + item.mAlbum + ")"
            tvDuration.text = SimpleDateFormat("mm:ss").format(item.mDuration)
            var albumArtUri: Uri = ContentUris.withAppendedId(artworkUri, item.mAlbumId)
            Picasso.get().load(albumArtUri).error(R.drawable.ic_launcher_background).into(imgAlbum)
        }
    }


}