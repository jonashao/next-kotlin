package com.junnanhao.next.player

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.support.v4.content.Loader
import android.util.Log
import com.junnanhao.next.model.Song
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


/**
 * Created by Jonas on 2017/5/27.
 * Load music task
 */
class LoadMusicTask(private var context: Context) {
    private val TAG = "LocalMusicPresenter"
    private val URL_LOAD_LOCAL_MUSIC = 0
    private val MEDIA_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    private val WHERE = MediaStore.Audio.Media.IS_MUSIC + "=1 AND " + MediaStore.Audio.Media.SIZE + ">0"
    private val ORDER_BY = MediaStore.Audio.Media.DISPLAY_NAME + " ASC"
    private val PROJECTIONS = arrayOf(MediaStore.Audio.Media.DATA, // the real path
            MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.IS_RINGTONE, MediaStore.Audio.Media.IS_MUSIC, MediaStore.Audio.Media.IS_NOTIFICATION, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.SIZE)

    fun onLoadFinished(cursor: Cursor) {
        Observable.just(cursor)
                .flatMap { cursor ->
                    val songs: ArrayList<Song> = ArrayList()
                    if (cursor != null && cursor.count > 0) {
                        cursor.moveToFirst()
                        do {
                            val song = cursorToMusic(cursor)
                            songs.add(song)
                        } while (cursor.moveToNext())
                    }
                    Observable.fromArray(songs)
                }
                .doOnNext {
                    songs: List<Song>? ->
                    Log.d(TAG, "onLoadFinished: " + songs?.size)
                    Collections.sort(songs) { left, right -> left.displayName.compareTo(right.displayName) }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //                    mView!!.showProgress()
                }, {
                    //                    mView!!.hideProgress()
                }, {
                    //                    mView!!.hideProgress()
//                    Log.e(TAG, "onError: ", throwable)
                }, {
                    //                    mView!!.onLocalMusicLoaded(songs)
//                    mView!!.emptyView(songs.isEmpty())
                })
//        mSubscriptions.add(subscription)
    }


    private fun cursorToMusic(cursor: Cursor): Song {
        val realPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
        val songFile = File(realPath)
        var song: Song?
//        if (songFile.exists()) {
//            // Using song parsed from file to avoid encoding problems
//            song = FileUtils.fileToMusic(songFile)
//            if (song != null) {
//                return song
//            }
//        }
        song = Song()
        song.title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))

//        var displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
//        if (displayName.endsWith(".mp3")) {
//            displayName = displayName.substring(0, displayName.length - 4)
//        }
//        song!!.setDisplayName(displayName)
//        song!!.artist(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)))
//        song!!.setAlbum(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)))
//        song!!.setPath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)))
//        song!!.setDuration(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)))
//        song!!.setSize(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)))
        return song
    }


}