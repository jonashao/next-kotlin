package com.junnanhao.next.data.model

import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import android.text.TextUtils
import com.github.ajalt.timberkt.Timber
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
//import timber.log.Timber
import java.io.File

/**
 * Created by Jonas on 2017/5/26.
 * song
 */
@Entity
data class Song(
        @Id var id: Long = 0,
        var resId: Long = 0,
        var path: String = "",
        var duration: Int = 0,
        var title: String = "",
        var artist: String = "",
        var displayName: String = "",
        var size: Int = 0,
        var album: String = "",
        var albumId: Long = 0,
        var mbid: String? = null) {

    var art: String? = null

    fun isSongValid(): Boolean {
        return duration > MIN_DURATION && artist != UNKNOWN
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
        var result = duration.toInt()
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + album.hashCode()
        return result
    }

    override fun toString(): String {
        return "Song(id=$id, path='$path', title='$title', artist='$artist', album='$album', albumId=$albumId, art=$art)"
    }

    companion object {
        val MIN_DURATION: Int = 40000
        val UNKNOWN: String = "<unknown>"

        fun fromCursor(cursor: Cursor): Song? {
            var song: Song? = null
            try {
                val realPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                val songFile = File(realPath)
                if (songFile.exists()) {
                    // Using song parsed from file to avoid encoding problems
                    song = fromFile(songFile)
                    if (song != null) {
                        song.albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                        song.resId = cursor.getLong(cursor.getColumnIndexOrThrow
                        (MediaStore.Audio.Media._ID))
                        return song
                    }
                }
                var displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                if (displayName.endsWith(".mp3")) {
                    displayName = displayName.substring(0, displayName.length - 4)
                }
                song = Song(displayName = displayName,
                        title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)),
                        artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)),
                        album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)),
                        albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)),
                        path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)),
                        duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media
                                .DURATION)),
                        size = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)),
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)))
            } catch (e: IllegalArgumentException) {
                Timber.wtf(e)
            }
            return song
        }

        private fun fromFile(file: File): Song? {
            if (file.length() == 0L) return null
            val metadataRetriever = MediaMetadataRetriever()
            metadataRetriever.setDataSource(file.absolutePath)

            val keyDuration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            // ensure the duration is a digit, otherwise return null song
            if (keyDuration == null || !keyDuration.matches("\\d+".toRegex())) return null
            val duration = Integer.parseInt(keyDuration)

            val title = extractMetadata(metadataRetriever, MediaMetadataRetriever.METADATA_KEY_TITLE, file.name)
            val displayName = extractMetadata(metadataRetriever, MediaMetadataRetriever.METADATA_KEY_TITLE, file.name)
            val artist = extractMetadata(metadataRetriever, MediaMetadataRetriever.METADATA_KEY_ARTIST, UNKNOWN)
            val album = extractMetadata(metadataRetriever, MediaMetadataRetriever.METADATA_KEY_ALBUM, UNKNOWN)

            return Song(title = title, displayName = displayName, artist = artist,
                    path = file.absolutePath, album = album, duration = duration,
                    size = file.length().toInt())
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