package com.study.musicplayerexamplekotlin.View

import android.content.Context
import android.database.Cursor
import android.database.DataSetObserver
import androidx.recyclerview.widget.RecyclerView


abstract class CursorRecyclerViewAdapter<VH : RecyclerView.ViewHolder?>
    (private var mContext: Context, var cursor: Cursor?) : RecyclerView.Adapter<VH>() {

    private var mDataValid: Boolean
    private var mRowIdColumn: Int
    private var mDataSetObserver: DataSetObserver?

    //커서의 item개수 반환
    override fun getItemCount(): Int {
        return if (mDataValid && cursor != null) {
            cursor!!.count
        } else 0
    }

    override fun getItemId(position: Int): Long {
        return if (mDataValid && cursor != null && cursor!!.moveToPosition(position)) {
            cursor!!.getLong(mRowIdColumn)
        } else 0
    }

    override fun setHasStableIds(hasStableIds: Boolean) {
        super.setHasStableIds(true)
    }

    abstract fun onBindViewHolder(viewHolder: VH, cursor: Cursor?)
    override fun onBindViewHolder(viewHolder: VH, position: Int) {
        check(mDataValid) { "커서가 존재 할 때만 호출되어야합니다." }
        check(cursor!!.moveToPosition(position)) { "커서를 위치로 이동할 수 없습니다.(static, final 확인) $position" }
        onBindViewHolder(viewHolder, cursor)
    }

    //커서를 바꿀때 사용하는 메소드
    fun changeCursor(cursor: Cursor) {
        val old = swapCursor(cursor)
        old?.close()
    }

    //커서를 바꿀때 사용하는 메소드
    fun swapCursor(newCursor: Cursor?): Cursor? {
        if (newCursor === cursor) {
            return null
        }
        val oldCursor = cursor
        if (oldCursor != null && mDataSetObserver != null) {
            oldCursor.unregisterDataSetObserver(mDataSetObserver)
        }
        cursor = newCursor
        if (cursor != null) {
            if (mDataSetObserver != null) {
                cursor!!.registerDataSetObserver(mDataSetObserver)
            }
            mRowIdColumn = newCursor!!.getColumnIndexOrThrow("_id")
            mDataValid = true
            notifyDataSetChanged()
        } else {
            mRowIdColumn = -1
            mDataValid = false
            notifyDataSetChanged()
            //There is no notifyDataSetInvalidated() method in RecyclerView.Adapter
        }
        return oldCursor
    }

    //옵저버를 이용한 cursor의 데이터 변경시 사용되는 inner클래스
    //자동으로 리사이클러뷰 갱신
    private inner class NotifyingDataSetObserver : DataSetObserver() {
        override fun onChanged() {
            super.onChanged()
            mDataValid = true
            notifyDataSetChanged()
        }

        override fun onInvalidated() {
            super.onInvalidated()
            mDataValid = false
            notifyDataSetChanged()
        }
    }

    //생성자
    init {
        cursor = cursor
        mDataValid = cursor != null
        mRowIdColumn = if (mDataValid) cursor!!.getColumnIndex("_id") else -1
        mDataSetObserver = NotifyingDataSetObserver()
        if (cursor != null) {
            cursor!!.registerDataSetObserver(mDataSetObserver)
        }
    }
}