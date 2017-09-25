package com.junnanhao.next.data.model

import android.support.v4.media.MediaMetadataCompat
import com.junnanhao.next.data.MusicProviderSource
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Transient
import java.util.*

/**
 * Created by jonashao on 2017/9/25.
 * record play history
 */
@Entity
data class Record(
        @Id var id: Long = 0,
        var mbid: String? = null,
        var title: String = "",
        var artist: String = "",
        var startTime: Date = Date(),
        var endTime: Date? = null,
        var pauseCount: Int = 0,
        var endWaySkip: Boolean = false,
        @Transient var mediaId: String? = ""
) {
    companion object {
        fun from(metaData: MediaMetadataCompat): Record {
            return Record(
                    title = metaData.description.title.toString(),
                    artist = metaData.description.subtitle.toString(),
                    mbid = metaData.getString(MusicProviderSource.CUSTOM_METADATA_MBID),
                    mediaId = metaData.description.mediaId
            )
        }
    }
}