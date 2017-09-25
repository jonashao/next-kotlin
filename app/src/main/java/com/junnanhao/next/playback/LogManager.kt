package com.junnanhao.next.playback

import android.app.Application
import android.support.v4.media.MediaMetadataCompat
import com.github.ajalt.timberkt.wtf
import com.junnanhao.next.App
import com.junnanhao.next.data.model.Record
import io.objectbox.Box
import java.util.*

/**
 * Created by jonashao on 2017/9/25.
 * Log for play record
 */

class LogManager(application: Application) {
    private val recordBox: Box<Record> = (application as App).boxStore.boxFor(Record::class.java)
    private var currentLog: Record? = null

    @Throws(IllegalStateException::class)
    fun createLog(mediaMetaData: MediaMetadataCompat) {
        if (currentLog != null) {
            if (currentLog!!.mediaId == mediaMetaData.description?.mediaId) {
                return
            } else {
                throw IllegalStateException("can not create new Log when current is not closed")
            }
        }

        currentLog = Record.from(mediaMetaData)
        recordBox.put(currentLog)
        wtf { "current log: $currentLog" }
    }

    @Throws(IllegalStateException::class)
    fun close(isSkip: Boolean) {
        if (currentLog == null) {
            throw IllegalStateException("cannot close log, cuz current log is null")
        }
        // end previous log
        currentLog!!.endWaySkip = isSkip
        currentLog!!.endTime = Date()
        recordBox.put(currentLog)
        wtf { "current log: $currentLog" }
        currentLog = null
    }

    @Throws(IllegalStateException::class)
    fun pause() {
        if (currentLog == null) {
            throw IllegalStateException("cannot log pause, cuz current log is null")
        }
        currentLog!!.pauseCount++
        recordBox.put(currentLog)
        wtf { "current log: $currentLog" }
    }


}
