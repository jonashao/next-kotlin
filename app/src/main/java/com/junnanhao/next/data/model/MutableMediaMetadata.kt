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

package com.junnanhao.next.data.model

import android.support.v4.media.MediaMetadataCompat
import android.text.TextUtils

/**
 * Holder class that encapsulates a MediaMetadata and allows the actual metadata to be modified
 * without requiring to rebuild the collections the metadata is in.
 */
class MutableMediaMetadata(private val trackId: String, var metadata: MediaMetadataCompat) {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || other.javaClass != MutableMediaMetadata::class.java) {
            return false
        }

        val that = other as MutableMediaMetadata?

        return TextUtils.equals(trackId, that!!.trackId)
    }

    override fun hashCode(): Int {
        return trackId.hashCode()
    }
}
