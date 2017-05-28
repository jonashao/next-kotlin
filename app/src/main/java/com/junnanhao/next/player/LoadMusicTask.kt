package com.junnanhao.next.player

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.support.v4.content.ContentResolverCompat
import android.support.v4.os.CancellationSignal
import com.junnanhao.next.model.Song
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import java.io.File
import kotlin.collections.ArrayList
import android.media.MediaMetadataRetriever
import com.junnanhao.next.model.Song.Companion.UNKNOWN
import android.text.TextUtils

/**
 * Created by Jonas on 2017/5/27.
 * Load music task
 */
class LoadMusicTask(private var context: Context) {
    private val MEDIA_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    private val WHERE = MediaStore.Audio.Media.IS_MUSIC + "=1 AND " + MediaStore.Audio.Media.SIZE + ">0"
    private val ORDER_BY = MediaStore.Audio.Media.DISPLAY_NAME + " ASC"
    private val PROJECTIONS = arrayOf(MediaStore.Audio.Media.DATA, // the real path
            MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.IS_RINGTONE, MediaStore.Audio.Media.IS_MUSIC, MediaStore.Audio.Media.IS_NOTIFICATION, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.SIZE)
    private var cancellationSignal: CancellationSignal? = null
    fun load() {
        cancellationSignal = CancellationSignal()
        val cursor: Cursor = ContentResolverCompat.query(
                context.contentResolver,
                MEDIA_URI,
                PROJECTIONS,
                WHERE,
                null,
                ORDER_BY, cancellationSignal)

        Observable.just(cursor)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .map {
                    cursor: Cursor? ->
                    val songs: ArrayList<Song> = ArrayList()
                    if (cursor != null && cursor.count > 0) {
                        cursor.moveToFirst()
                        do {
                            val song = cursorToMusic(cursor)
                            if (song.isSongValid())
                                songs.add(song)
                        } while (cursor.moveToNext())
                    }
                    songs
                }
                .subscribe({
                    songs: ArrayList<Song>? ->
                    val realm: Realm = Realm.getDefaultInstance()
                    realm.executeTransaction({ realm -> realm.copyToRealmOrUpdate(songs) })
                })
    }


    private fun cursorToMusic(cursor: Cursor): Song {
        val realPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
        val songFile = File(realPath)
        var song: Song?
        if (songFile.exists()) {
            // Using song parsed from file to avoid encoding problems
            song = fileToMusic(songFile)
            if (song != null) {
                return song
            }
        }

        var displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
        if (displayName.endsWith(".mp3")) {
            displayName = displayName.substring(0, displayName.length - 4)
        }
        song = Song(displayName = displayName)
        song.title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
        song.artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
        song.album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
        song.path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
        song.duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
        song.size = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))
        return song
    }


    fun fileToMusic(file: File): Song? {
        if (file.length() == 0L) return null

        val metadataRetriever = MediaMetadataRetriever()
        metadataRetriever.setDataSource(file.absolutePath)

        val duration: Int

        val keyDuration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        // ensure the duration is a digit, otherwise return null song
        if (keyDuration == null || !keyDuration.matches("\\d+".toRegex())) return null
        duration = Integer.parseInt(keyDuration)

        val title = extractMetadata(metadataRetriever, MediaMetadataRetriever.METADATA_KEY_TITLE, file.name)
        val displayName = extractMetadata(metadataRetriever, MediaMetadataRetriever.METADATA_KEY_TITLE, file.name)
        val artist = extractMetadata(metadataRetriever, MediaMetadataRetriever.METADATA_KEY_ARTIST, UNKNOWN)
        val album = extractMetadata(metadataRetriever, MediaMetadataRetriever.METADATA_KEY_ALBUM, UNKNOWN)

        val song = Song()
        song.title = title
        song.displayName = displayName
        song.artist = artist
        song.path = file.absolutePath
        song.album = album
        song.duration = duration
        song.size = file.length().toInt()
        return song
    }

    private fun extractMetadata(retriever: MediaMetadataRetriever, key: Int, defaultValue: String): String {
        var value = retriever.extractMetadata(key)
        if (TextUtils.isEmpty(value)) {
            value = defaultValue
        }
        return value
    }
}