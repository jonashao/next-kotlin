package com.junnanhao.next

/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.graphics.Bitmap
import android.util.LruCache
import com.github.ajalt.timberkt.wtf
import com.junnanhao.next.utils.BitmapHelper
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Implements a basic cache of album arts, with async loading support.
 */
class AlbumArtCache private constructor() {

    private val mCache: LruCache<String, Array<Bitmap>>

    init {
        // Holds no more than MAX_ALBUM_ART_CACHE_SIZE bytes, bounded by maxmemory/4 and
        // Integer.MAX_VALUE:
        val maxSize = Math.min(MAX_ALBUM_ART_CACHE_SIZE,
                Math.min(Integer.MAX_VALUE.toLong(), Runtime.getRuntime().maxMemory() / 4).toInt())
        mCache = object : LruCache<String, Array<Bitmap>>(maxSize) {
            override fun sizeOf(key: String, value: Array<Bitmap>): Int {
                return value[BIG_BITMAP_INDEX].byteCount + value[ICON_BITMAP_INDEX].byteCount
            }
        }
    }

    fun getBigImage(artUrl: String): Bitmap? {
        val result = mCache.get(artUrl)
        return if (result == null) null else result[BIG_BITMAP_INDEX]
    }

    fun getIconImage(artUrl: String): Bitmap? {
        val result = mCache.get(artUrl)
        return if (result == null) null else result[ICON_BITMAP_INDEX]
    }

    fun fetch(artUrl: String, listener: FetchListener) {
        // WARNING: for the sake of simplicity, simultaneous multi-thread fetch requests
        // are not handled properly: they may cause redundant costly operations, like HTTP
        // requests and bitmap rescales. For production-level apps, we recommend you use
        // a proper image loading library, like Glide.
        val bitmap = mCache.get(artUrl)
        if (bitmap != null) {
            wtf { "getOrFetch: album art is in cache, using it $artUrl" }
            listener.onFetched(artUrl, bitmap[BIG_BITMAP_INDEX], bitmap[ICON_BITMAP_INDEX])
            return
        }
        wtf { "getOrFetch: starting asynctask to fetch $artUrl" }

        Single.just(artUrl)
                .subscribeOn(Schedulers.io())
                .map { url: String ->
                    val bitmap = BitmapHelper.fetchAndRescaleBitmap(url,
                            MAX_ART_WIDTH, MAX_ART_HEIGHT)
                    val icon = BitmapHelper.scaleBitmap(bitmap,
                            MAX_ART_WIDTH_ICON, MAX_ART_HEIGHT_ICON)
                    val bitmaps = arrayOf(bitmap, icon)
                    wtf { "doInBackground: putting bitmap in cache. cache size= ${mCache.size()}" }

                    mCache.put(artUrl, bitmaps)
                    return@map bitmaps
                }.observeOn(AndroidSchedulers.mainThread())
                .subscribe { bitmaps: Array<Bitmap>?, e: Throwable? ->
                    if (bitmaps == null) {
                        listener.onError(artUrl, IllegalArgumentException("got null bitmaps"))
                    } else {
                        listener.onFetched(artUrl,
                                bitmaps[BIG_BITMAP_INDEX], bitmaps[ICON_BITMAP_INDEX])
                    }
                    e?.printStackTrace()
                }
    }


    abstract class FetchListener {
        abstract fun onFetched(artUrl: String, bigImage: Bitmap, iconImage: Bitmap)
        fun onError(artUrl: String, e: Exception) {
            wtf { "AlbumArtFetchListener: error while downloading $artUrl :  $e" }
        }
    }

    companion object {

        private val MAX_ALBUM_ART_CACHE_SIZE = 12 * 1024 * 1024  // 12 MB
        private val MAX_ART_WIDTH = 800  // pixels
        private val MAX_ART_HEIGHT = 480  // pixels

        // Resolution reasonable for carrying around as an icon (generally in
        // MediaDescription.getIconBitmap). This should not be bigger than necessary, because
        // the MediaDescription object should be lightweight. If you set it too high and try to
        // serialize the MediaDescription, you may get FAILED BINDER TRANSACTION errors.
        private val MAX_ART_WIDTH_ICON = 128  // pixels
        private val MAX_ART_HEIGHT_ICON = 128  // pixels

        private val BIG_BITMAP_INDEX = 0
        private val ICON_BITMAP_INDEX = 1

        val instance = AlbumArtCache()
    }
}
