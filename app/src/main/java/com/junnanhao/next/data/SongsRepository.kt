package com.junnanhao.next.data

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.MediaStore
import android.support.v4.content.ContentResolverCompat
import android.support.v4.content.ContextCompat
import android.support.v4.os.CancellationSignal
import com.junnanhao.next.App
import com.junnanhao.next.data.model.Song
import io.objectbox.Box
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Jonas on 2017/5/29.
 * songs repository
 */
class SongsRepository constructor(private var context: Application) : SongsDataSource {

    private val songBox: Box<Song> = (context as App).boxStore.boxFor(Song::class.java)

    override fun isInitialized(): Boolean {
        return songBox.count() > 0
    }

    companion object {
        private val MEDIA_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        private val WHERE = MediaStore.Audio.Media.IS_MUSIC + "=1 AND " + MediaStore.Audio.Media.SIZE + ">0"
        private val ORDER_BY = MediaStore.Audio.Media.DISPLAY_NAME + " ASC"
        private val PROJECTIONS = arrayOf(MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.IS_RINGTONE, MediaStore.Audio.Media.IS_MUSIC, MediaStore.Audio.Media.IS_NOTIFICATION, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ALBUM_ID)
        private val ALBUM_URI = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
        private val WHERE_ALBUM = MediaStore.Audio.Albums._ID + " = ?"
        private val PROJECTIONS_ALBUM = arrayOf(MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_KEY, MediaStore.Audio.Albums.ALBUM_ART)
    }

    private var cancellationSignal: CancellationSignal? = null

    override fun scanMusic(): Observable<List<Song>> {

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            return Observable.fromArray()
        }

        cancellationSignal = CancellationSignal()
        val cursor: Cursor = ContentResolverCompat.query(
                context.contentResolver,
                MEDIA_URI,
                PROJECTIONS,
                WHERE,
                null,
                ORDER_BY, cancellationSignal)

        return Observable.just(cursor)
                .subscribeOn(Schedulers.io())
                .map { c: Cursor? ->
                    val songs: MutableList<Song> = ArrayList()
                    if (c != null && c.count > 0) {
                        c.moveToFirst()
                        do {
                            val song = Song.fromCursor(c)
                            if (song != null && song.isSongValid())
                                songs.add(song)
                        } while (c.moveToNext())
                    }
                    songs.toList()
                }
                .doOnNext{songs: List<Song>? ->
                    songs?.forEach { song: Song? ->
                        val cursor1: Cursor = ContentResolverCompat.query(
                                context.contentResolver,
                                ALBUM_URI,
                                PROJECTIONS_ALBUM,
                                WHERE_ALBUM,
                                arrayOf(song?.albumId.toString()), null, null)
                        if (cursor1.count > 0) {
                            if (cursor1.moveToFirst()) {
                                song?.art = cursor1.getString(cursor1.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART))
                            }
                        }
                        cursor1.close()
                    }

                    songBox.put(songs)
                }

                .doOnComplete { cursor.close() }
                .doOnError { t: Throwable? ->
                    cursor.close()
                    t?.printStackTrace()
                }


    }

    override fun getSongs(): List<Song> {
        return songBox.all
    }


    override fun getSong(songId: Long): Observable<Song> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveSong(song: Song): Observable<Song> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}