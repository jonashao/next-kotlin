package com.junnanhao.next.data

import com.junnanhao.next.data.model.remote.TrackResponse
import io.reactivex.Observable
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query


/**
 * Created by jonashao on 2017/9/20.
 * Last.fm Service
 */
interface LastFmService {
    @GET("?method=track.getinfo&format=json")
    fun getTrack(@Query("api_key") apiKey: String,
                 @Query("artist") artist: String,
                 @Query("track") track: String): Observable<TrackResponse>

    @GET("?method=track.getinfo&format=json")
    fun getTrack2(@Query("api_key") apiKey: String,
                 @Query("artist") artist: String,
                 @Query("track") track: String): Call<TrackResponse>
}
