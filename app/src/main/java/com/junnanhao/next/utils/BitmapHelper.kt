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
package com.junnanhao.next.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.github.ajalt.timberkt.wtf
import java.io.*

import java.net.HttpURLConnection
import java.net.URL

object BitmapHelper {

    // Max read limit that we allow our input stream to mark/reset.
    private val MAX_READ_LIMIT_PER_IMG = 1024 * 1024

    fun scaleBitmap(src: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val scaleFactor = Math.min(
                maxWidth.toDouble() / src.width, maxHeight.toDouble() / src.height)
        return Bitmap.createScaledBitmap(src,
                (src.width * scaleFactor).toInt(), (src.height * scaleFactor).toInt(), false)
    }

    fun scaleBitmap(scaleFactor: Int, input: InputStream?): Bitmap {
        // Get the dimensions of the bitmap
        val bmOptions = BitmapFactory.Options()

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scaleFactor

        return BitmapFactory.decodeStream(input, null, bmOptions)
    }

    fun findScaleFactor(targetW: Int, targetH: Int, input: InputStream?): Int {
        // Get the dimensions of the bitmap
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeStream(input, null, bmOptions)
        val actualW = bmOptions.outWidth
        val actualH = bmOptions.outHeight

        // Determine how much to scale down the image
        return Math.min(actualW / targetW, actualH / targetH)
    }

    @Throws(IOException::class)
    fun fetchAndRescaleBitmap(uri: String, width: Int, height: Int): Bitmap {
        var input: InputStream? = null
        try {
            if (uri.startsWith("http") || uri.startsWith("https")) {
                val url = URL(uri)
                val urlConnection = url.openConnection() as HttpURLConnection
                input = BufferedInputStream(urlConnection.inputStream)
                input.mark(MAX_READ_LIMIT_PER_IMG)
                val scaleFactor = findScaleFactor(width, height, input)
                wtf {
                    "Scaling bitmap $uri by factor  $scaleFactor, " +
                            "to support $width x $height requested dimension"
                }
                input.reset()
                return scaleBitmap(scaleFactor, input)
            } else {
                val file = File(uri)
                if (file.exists()) {
                    input = FileInputStream(file)
                }
                return BitmapFactory.decodeStream(input)
            }
        } finally {
            input?.close()
        }
    }


}
