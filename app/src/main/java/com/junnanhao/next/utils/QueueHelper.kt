package com.junnanhao.next.utils

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import android.text.TextUtils
import com.github.ajalt.timberkt.wtf
import com.junnanhao.next.VoiceSearchParams
import com.junnanhao.next.data.MusicProvider
import com.junnanhao.next.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE
import com.junnanhao.next.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_SEARCH
import java.util.*

/**
 * Utility class to help on queue related tasks.
 */
object QueueHelper {


    private val RANDOM_QUEUE_SIZE = 10

    fun getPlayingQueue(
            mediaId: String, musicProvider: MusicProvider): List<QueueItem>? {

        // extract the browsing hierarchy from the media ID:
        val hierarchy = MediaIDHelper.getHierarchy(mediaId)

        if (hierarchy.size != 2) {
            wtf { "Could not build a playing queue for this mediaId: $mediaId" }
            return null
        }

        val categoryType = hierarchy[0]
        val categoryValue = hierarchy[1]
        wtf { "Creating playing queue for $categoryType $categoryValue" }

        var tracks: Iterable<MediaMetadataCompat>? = null
        // This sample only supports genre and by_search category types.
        if (categoryType == MEDIA_ID_MUSICS_BY_GENRE) {
//            tracks = musicProvider.getMusicsByGenre(categoryValue)
        } else if (categoryType == MEDIA_ID_MUSICS_BY_SEARCH) {
            tracks = musicProvider.searchMusicBySongTitle(categoryValue)
        }

        if (tracks == null) {
            wtf { "Unrecognized category type: $categoryType from media $mediaId " }
            return null
        }

        return convertToQueue(tracks, hierarchy[0], hierarchy[1])
    }

    fun getPlayingQueueFromSearch(query: String,
                                  queryParams: Bundle, musicProvider: MusicProvider): List<QueueItem> {

        wtf { "Creating playing queue for musics from search: $query  params = $queryParams" }

        val params = VoiceSearchParams(query, queryParams)

        wtf { "VoiceSearchParams:$params" }

        if (params.isAny) {
            // If isAny is true, we will play anything. This is app-dependent, and can be,
            // for example, favorite playlists, "I'm feeling lucky", most recent, etc.
            return getRandomQueue(musicProvider)
        }

        var result: Iterable<MediaMetadataCompat>? = null
        if (params.isAlbumFocus) {
            result = musicProvider.searchMusicByAlbum(params.album)
        }
//        } else if (params.isGenreFocus) {
//            result = musicProvider.getMusicsByGenre(params.genre)
//        } else if (params.isArtistFocus) {
//            result = musicProvider.searchMusicByArtist(params.artist)
//        } else if (params.isSongFocus) {
//            result = musicProvider.searchMusicBySongTitle(params.song)
//        }

        // If there was no results using media focus parameter, we do an unstructured query.
        // This is useful when the user is searching for something that looks like an artist
        // to Google, for example, but is not. For example, a user searching for Madonna on
        // a PodCast application wouldn't get results if we only looked at the
        // Artist (podcast author). Then, we can instead do an unstructured search.
        if (params.isUnstructured || result == null || !result.iterator().hasNext()) {
            // To keep it simple for this example, we do unstructured searches on the
            // song title only. A real world application could search on other fields as well.
            result = musicProvider.searchMusicBySongTitle(query)
        }

        return convertToQueue(result, MEDIA_ID_MUSICS_BY_SEARCH, query)
    }


    fun getMusicIndexOnQueue(queue: Iterable<QueueItem>,
                             mediaId: String): Int {
        var index = 0
        for (item in queue) {
            if (mediaId == item.description.mediaId) {
                return index
            }
            index++
        }
        return -1
    }

    fun getMusicIndexOnQueue(queue: Iterable<QueueItem>,
                             queueId: Long): Int {
        var index = 0
        for (item in queue) {
            if (queueId == item.queueId) {
                return index
            }
            index++
        }
        return -1
    }

    private fun convertToQueue(
            tracks: Iterable<MediaMetadataCompat>, vararg categories: String): List<QueueItem> {
        val queue = ArrayList<QueueItem>()
        var count = 0
        for (track in tracks) {

            // We create a hierarchy-aware mediaID, so we know what the queue is about by looking
            // at the QueueItem media IDs.
            val hierarchyAwareMediaID = MediaIDHelper.createMediaID(
                    track.description.mediaId, *categories)

            val trackCopy = MediaMetadataCompat.Builder(track)
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
                    .build()

            // We don't expect queues to change after created, so we use the item index as the
            // queueId. Any other number unique in the queue would work.
            val item = QueueItem(
                    trackCopy.description, count++.toLong())
            queue.add(item)
        }
        return queue

    }

    /**
     * Create a random queue with at most [.RANDOM_QUEUE_SIZE] elements.
     *
     * @param musicProvider the provider used for fetching music.
     * @return list containing [QueueItem]'s
     */
    fun getRandomQueue(musicProvider: MusicProvider): List<QueueItem> {
        val result = ArrayList<MediaMetadataCompat>(RANDOM_QUEUE_SIZE)
        val shuffled = musicProvider.getShuffledMusic()
        for (metadata in shuffled) {
            if (result.size == RANDOM_QUEUE_SIZE) {
                break
            }
            result.add(metadata)
        }
        wtf { "getRandomQueue: result.size=${result.size}" }
        return convertToQueue(result, MEDIA_ID_MUSICS_BY_SEARCH, "random")
    }

    fun isIndexPlayable(index: Int, queue: List<QueueItem>?): Boolean {
        return queue != null && index >= 0 && index < queue.size
    }

    /**
     * Determine if two queues contain identical media id's in order.
     *
     * @param list1 containing [QueueItem]'s
     * @param list2 containing [QueueItem]'s
     * @return boolean indicating whether the queue's match
     */
    fun equals(list1: List<QueueItem>?,
               list2: List<QueueItem>?): Boolean {
        if (list1 === list2) {
            return true
        }
        if (list1 == null || list2 == null) {
            return false
        }
        if (list1.size != list2.size) {
            return false
        }
        for (i in list1.indices) {
            if (list1[i].queueId != list2[i].queueId) {
                return false
            }
            if (!TextUtils.equals(list1[i].description.mediaId,
                    list2[i].description.mediaId)) {
                return false
            }
        }
        return true
    }

//    /**
//     * Determine if queue item matches the currently playing queue item
//     *
//     * @param context   for retrieving the [MediaControllerCompat]
//     * @param queueItem to compare to currently playing [QueueItem]
//     * @return boolean indicating whether queue item matches currently playing queue item
//     */
//    fun isQueueItemPlaying(context: Context,
//                           queueItem: QueueItem): Boolean {
//        // Queue item is considered to be playing or paused based on both the controller's
//        // current media id and the controller's active queue item id
//        val controller = (context as FragmentActivity).getSupportMediaController()
//        if (controller != null && controller!!.getPlaybackState() != null) {
//            val currentPlayingQueueId = controller!!.getPlaybackState().getActiveQueueItemId()
//            val currentPlayingMediaId = controller!!.getMetadata().getDescription()
//                    .getMediaId()
//            val itemMusicId = MediaIDHelper.extractMusicIDFromMediaID(
//                    queueItem.description.mediaId!!)
//            if (queueItem.queueId == currentPlayingQueueId
//                    && currentPlayingMediaId != null
//                    && TextUtils.equals(currentPlayingMediaId, itemMusicId)) {
//                return true
//            }
//        }
//        return false
//    }
}
