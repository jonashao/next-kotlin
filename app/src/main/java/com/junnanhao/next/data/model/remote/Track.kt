package com.junnanhao.next.data.model.remote

import com.google.gson.annotations.SerializedName



data class TrackResponse(val track: Track?)

/**
 * Track info from last.fm
 */
 data class Track(val mbid: String?, val artist: Artist?, val album: Album?)
/**
 * Artist info from last.fm
 */
data class Artist(val mbid: String?, val name: String?)

data class Album(val mid: String?, val title: String?, val image: List<Image>?)

data class Image(@SerializedName("#text") val url: String?, val size: String?)