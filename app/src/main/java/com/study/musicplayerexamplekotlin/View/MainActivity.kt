package com.study.musicplayerexamplekotlin.View

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.study.musicplayerexamplekotlin.R
import com.study.musicplayerexamplekotlin.Service.AudioApplication
import com.study.musicplayerexamplekotlin.Service.IntentFilter.BroadCastActions



class MainActivity : AppCompatActivity(), View.OnClickListener,LoaderManager.LoaderCallbacks<Cursor> {

    private val TAG: String = javaClass.simpleName
    companion object {
        private const val  LOADERID: Int = 0x001;
    }
    private var mRecyclerView : RecyclerView?=null
    private var mAdapter: AudioAdapter?= null
    private var img_main_album:ImageView?=null
    private var tv_main_title:TextView?=null
    private var imgbtn_main_rewind: ImageButton?=null
    private var imgbtn_main_play:ImageButton?=null
    private var imgbtn_main_forword:ImageButton?=null
    private var con_miniplayer: LinearLayout?=null


    private var mBroadCastReceiver = object :BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            updateUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main);
        mRecyclerView = findViewById(R.id.recyclerView)
        img_main_album = findViewById(R.id.img_main_album)
        tv_main_title = findViewById(R.id.tv_main_title)
        imgbtn_main_rewind = findViewById(R.id.imgbtn_main_rewind)
        imgbtn_main_play = findViewById(R.id.imgbtn_main_play)
        imgbtn_main_forword = findViewById(R.id.imgbtn_main_forword)
        con_miniplayer = findViewById(R.id.con_miniplayer)
        mRecyclerView = findViewById(R.id.recyclerView)

        con_miniplayer?.setOnClickListener (this)
        imgbtn_main_rewind?.setOnClickListener(this)
        imgbtn_main_play?.setOnClickListener(this)
        imgbtn_main_forword?.setOnClickListener(this)

        checkPermission()
        setAudioAdapter()
        registerBroadCast()
        updateUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterBroadCast()

    }
    //권한
    fun checkPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            when{
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED
                ->{
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1000)
                }
                else ->{
                    getAudioListFromMediaDatabase()
                }
            }
        }else{
            getAudioListFromMediaDatabase()
        }
    }
    //Media의 파일들을 쿼리하는 메소드
    fun getAudioListFromMediaDatabase(){
        LoaderManager.getInstance(this).initLoader(LOADERID, null, this)
    }

    //어뎁터 연결 메소드
    fun setAudioAdapter(){
        mAdapter = AudioAdapter(applicationContext, null)
        mRecyclerView?.adapter = mAdapter
        var layoutManager:LinearLayoutManager = LinearLayoutManager(this)
        layoutManager.orientation=LinearLayoutManager.VERTICAL
        mRecyclerView?.layoutManager = layoutManager
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "권한 허용됨", Toast.LENGTH_LONG).show()
            getAudioListFromMediaDatabase()
        }
    }

    //재생바 ui변경 메소드
    fun updateUI(){
        mAdapter?.notifyDataSetChanged()
        if (AudioApplication.getInstance()?.getServiceInterface()?.isPlaying() == true){
            imgbtn_main_play?.setImageResource(R.drawable.pause)
        }else{
            imgbtn_main_play?.setImageResource(R.drawable.play)
        }
        var audioItem: AudioAdapter.AudioItem? = AudioApplication.getInstance()?.getServiceInterface()?.getAudioItem()

        if (audioItem != null){
            var albumArtUri: Uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), audioItem.mAlbumId)
            Picasso.get().load(albumArtUri).error(R.drawable.ic_launcher_background).into(img_main_album)
            tv_main_title?.text = audioItem.mTitle
        }else {
            img_main_album?.setImageResource(R.drawable.ic_launcher_foreground)
            tv_main_title?.text = "재생중인 음악이 없습니다"
        }

    }

    //암시적 브로드캐스트 리시버 등록
    fun registerBroadCast(){
        var filter: IntentFilter = IntentFilter()
        filter.addAction(BroadCastActions.PLAY_STATE_CHANGED);
        filter.addAction(BroadCastActions.PREPARED);
        registerReceiver(mBroadCastReceiver, filter);
    }

    private fun unregisterBroadCast() {
        unregisterReceiver(mBroadCastReceiver)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.con_miniplayer -> {
            }
            R.id.imgbtn_main_rewind ->                 // 이전곡으로 이동
                AudioApplication.getInstance()!!.getServiceInterface()?.rewind()
            R.id.imgbtn_main_play ->                 // 재생 또는 일시정지
                AudioApplication.getInstance()!!.getServiceInterface()?.togglePlay()
            R.id.imgbtn_main_forword ->                 // 다음곡으로 이동
                AudioApplication.getInstance()!!.getServiceInterface()?.forward()
        }
    }

    //LoaderCallback 인터페이스
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        var uri : Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        var musicArray : Array<String> = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
        )

        var selection : String = MediaStore.Audio.Media.IS_MUSIC +" = 1"
        var sortOrder : String = MediaStore.Audio.Media.TITLE+" COLLATE LOCALIZED ASC"

        return CursorLoader(applicationContext, uri, musicArray, selection, null, sortOrder)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        if (data != null && data.getCount() > 0) {
            while (data.moveToNext()) {
                Log.i(TAG, "Title:" + data.getString(data.getColumnIndex(MediaStore.Audio.Media.TITLE)));
            }
        }
        //커서 어뎁터의 데이터를 갱신하는 메소드
        mAdapter?.swapCursor(data!!)

    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        mAdapter?.swapCursor(null)
    }

}