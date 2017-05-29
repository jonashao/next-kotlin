package com.junnanhao.next.data

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.support.v4.content.ContentResolverCompat
import android.support.v4.os.CancellationSignal
import com.junnanhao.next.data.model.Song
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import javax.inject.Inject

/**
 * Created by Jonas on 2017/5/29.
 * songs repository
 */
class SongsRepository @Inject constructor(var context: Context) : SongsDataSource {

    companion object {
        private val MEDIA_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        private val WHERE = MediaStore.Audio.Media.IS_MUSIC + "=1 AND " + MediaStore.Audio.Media.SIZE + ">0"
        private val ORDER_BY = MediaStore.Audio.Media.DISPLAY_NAME + " ASC"
        private val PROJECTIONS = arrayOf(MediaStore.Audio.Media.DATA, // the real path
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.IS_RINGTONE, MediaStore.Audio.Media.IS_MUSIC, MediaStore.Audio.Media.IS_NOTIFICATION, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.SIZE)
    }

    private var cancellationSignal: CancellationSignal? = null

    override fun scanMusic(): Observable<MutableList<Song>> {
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
                .map {
                    cursor: Cursor? ->
                    val songs: MutableList<Song> = ArrayList()
                    if (cursor != null && cursor.count > 0) {
                        cursor.moveToFirst()
                        do {
                            val song = Song.fromCursor(cursor)
                            if (song != null && song.isSongValid())
                                songs.add(song)
                        } while (cursor.moveToNext())
                    }
                    songs
                }
                .doOnNext {
                    songs: MutableList<Song>? ->
                    val realm: Realm = Realm.getDefaultInstance()
                    realm.executeTransaction({ realm -> realm.copyToRealmOrUpdate(songs) })
                }
                .doOnComplete { cursor.close() }
                .doOnError { t: Throwable? ->
                    cursor.close()
                    t?.printStackTrace()
                }
    }


    override fun getSong(songId: Long): Observable<Song> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveSong(song: Song): Observable<Song> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}