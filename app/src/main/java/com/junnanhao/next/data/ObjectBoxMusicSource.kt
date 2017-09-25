package com.junnanhao.next.data

import android.app.Application
import android.support.v4.media.MediaMetadataCompat
import com.github.ajalt.timberkt.wtf
import com.junnanhao.next.App
import com.junnanhao.next.BuildConfig
import com.junnanhao.next.data.model.Song
import com.junnanhao.next.data.model.remote.TrackResponse
import io.objectbox.Box
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory


/**
 * Created by jonashao on 2017/9/11.
 * Music Provider Source implemented by ObjectBox
 */
class ObjectBoxMusicSource(application: Application) : MusicProviderSource {

    private val source: SongsDataSource = SongsRepository(application)


    private val retrofit: Retrofit
    private val service: LastFmService
    private val songBox: Box<Song> = (application as App).boxStore.boxFor(Song::class.java)

    init {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        val client = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
                }).build()
        retrofit = Retrofit.Builder()
                .baseUrl("http://ws.audioscrobbler.com/2.0/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
                .build()
        service = retrofit.create<LastFmService>(LastFmService::class.java)

    }


    override fun iterator(): Iterator<MediaMetadataCompat> {
        if (!source.isInitialized()) {
            source.scanMusic().subscribe()
        }

        return source.getSongs()
                .map { song ->
                    if (song.art == null && song.mbid == null) {
                        service.getTrack2(BuildConfig.LAST_FM_API_KEY, song.artist, song.title)
                                .enqueue(object : Callback<TrackResponse> {
                                    override fun onFailure(call: Call<TrackResponse>?, t: Throwable?) {
                                        wtf { "t :${t?.message}" }
                                    }

                                    override fun onResponse(call: Call<TrackResponse>?, response: Response<TrackResponse>?) {
                                        val track = response?.body()?.track
                                        if (track != null) {
                                            song.mbid = track.mbid
                                            song.art = track.album?.image?.
                                                    getOrNull(track.album.image.size - 1)?.url
                                            songBox.put(song)
                                        }
                                    }
                                })
                    }

                    val uri = if (song.art?.startsWith("/storage") == true)
                        "file://${song.art}" else song.art

                    return@map MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, song.resId.toString())
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration.toLong())
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, uri)
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, song.path)
                            .putString(MusicProviderSource.CUSTOM_METADATA_MBID, song.mbid)
                            .build()
                }.iterator()
    }
}