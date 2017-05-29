package com.junnanhao.next.data.model

import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import android.text.TextUtils
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.io.File

/**
 * Created by Jonas on 2017/5/26.
 * song
 */
open class Song(
        @PrimaryKey var id: Long = 0,
        var path: String = "",
        var duration: Int = 0,
        var title: String = "",
        var artist: String = "",
        var displayName: String = "",
        var size: Int = 0,
        var album: String = "") : RealmObject() {

    override fun toString(): String {
        return "Song(path='$path', duration=$duration, title='$title', artist='$artist', displayName='$displayName')"
    }

    fun isSongValid(): Boolean {
        return duration > MIN_DURATION && artist != UNKNOWN
    }

    fun autoId(): Song {
        if (id == 0L) {
            var result: Long = duration.toLong()
            result = 63 * result + title.hashCode()
            result = 63 * result + artist.hashCode()
            result = 63 * result + album.hashCode()
            id = result
        }
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Song

        if (duration != other.duration) return false
        if (title != other.title) return false
        if (artist != other.artist) return false
        if (album != other.album) return false

        return true
    }

    override fun hashCode(): Int {
        var result = duration
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + album.hashCode()
        return result
    }


    companion object {
        val MIN_DURATION: Int = 40000
        val UNKNOWN: String = "<unknown>"

        fun fromCursor(cursor: Cursor): Song? {
            val realPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
            val songFile = File(realPath)
            var song: Song?
            if (songFile.exists()) {
                // Using song parsed from file to avoid encoding problems
                song = fromFile(songFile)
                if (song != null) {
                    return song
                }
            }
            try {
                var displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                if (displayName.endsWith(".mp3")) {
                    displayName = displayName.substring(0, displayName.length - 4)
                }
                song = Song(displayName = displayName,
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)),
                        title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)),
                        artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)),
                        album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)),
                        path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)),
                        duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)),
                        size = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)))
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                song = null
            }
            return song
        }

        fun fromFile(file: File): Song? {
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

            val song = Song(title = title, displayName = displayName, artist = artist,
                    path = file.absolutePath, album = album, duration = duration,
                    size = file.length().toInt()).autoId()
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
}