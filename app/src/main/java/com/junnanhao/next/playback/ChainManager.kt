package com.junnanhao.next.playback

import android.support.v4.media.MediaMetadataCompat
import java.util.*

/**
 * Created by jonashao on 2017/9/26.
 * manage play history, and to decide next music
 */

interface ChainManager {
    fun current(): MediaMetadataCompat?
    fun updateMetaData()
    fun next(isSkip:Boolean): Boolean
    fun guess(calendar: Calendar?, nothing: Nothing?)
}
