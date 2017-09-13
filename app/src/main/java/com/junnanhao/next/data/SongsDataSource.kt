package com.junnanhao.next.data

import android.support.annotation.NonNull
import com.junnanhao.next.data.model.Song
import io.reactivex.Observable

/**
 * Created by Jonas on 2017/5/29.
 * music data source
 */
interface SongsDataSource {

    fun isInitialized(): Boolean

    fun scanMusic(): Observable<List<Song>>

    fun getSongs():List<Song>

    fun getSong(@NonNull songId: Long): Observable<Song>

    fun saveSong(@NonNull song: Song): Observable<Song>

}