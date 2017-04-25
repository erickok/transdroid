package org.transdroid.connect.model

import java.util.*

data class Torrent(
        val id: Long,
        val hash: String?,
        val name: String,
        val statusCode: TorrentStatus,
        val locationDir: String?,
        val rateDownload: Int,
        val rateUpload: Int,
        val seedersConnected: Int,
        val seedersKnown: Int,
        val leechersConnected: Int,
        val leechersKnown: Int,
        val eta: Long?,
        val downloadedEver: Long,
        val uploadedEver: Long,
        val totalSize: Long,
        val partDone: Float,
        val available: Float?,
        val label: String?,
        val dateAdded: Date,
        private val realDateDone: Date?,
        val error: String?) {

    val dateDone: Date = realDateDone ?:
            if (this.partDone == 1f) Calendar.getInstance().apply {
                clear()
                set(1900, Calendar.DECEMBER, 31)
            }.time
            else if (eta == null || eta == -1L || eta == -2L) Date(java.lang.Long.MAX_VALUE)
            else Calendar.getInstance().apply {
                add(Calendar.SECOND, eta.toInt())
            }.time

    /**
     * The unique torrent-specific id, which is the torrent's hash or (if not available) the local index number
     */
    val uniqueId = this.hash ?: this.id.toString()

    /**
     * The upload/download seed ratio in range [0,r]
     */
    val ratio = uploadedEver.toDouble() / downloadedEver.toDouble()

    /**
     * Whether this torrents is actively downloading or not.
     * @param dormantAsInactive If true, dormant (0KB/s, so no data transfer) torrents are not considered actively downloading
     * @return True if this torrent is to be treated as being in a downloading state, that is, it is trying to finish a download
     */
    fun isDownloading(dormantAsInactive: Boolean) = statusCode === TorrentStatus.DOWNLOADING && (!dormantAsInactive || rateDownload > 0)

    /**
     * Whether this torrents is actively seeding or not.
     * @param dormantAsInactive If true, dormant (0KB/s, so no data transfer) torrents are not considered actively seeding
     * @return True if this torrent is to be treated as being in a seeding state, that is, it is sending data to leechers
     */
    fun isSeeding(dormantAsInactive: Boolean) = statusCode === TorrentStatus.SEEDING && (!dormantAsInactive || rateUpload > 0)

    /**
     * If the torrent can be paused at this moment
     */
    val canPause = statusCode === TorrentStatus.DOWNLOADING || statusCode === TorrentStatus.SEEDING

    /**
     * If the torrent can be resumed at this moment
     */
    val canResume = statusCode === TorrentStatus.PAUSED

    /**
     * If the torrent can be started at this moment
     */
    val canStart = statusCode === TorrentStatus.QUEUED

    /**
     * If the torrent can be stopped at this moment
     */
    val canStop: Boolean = statusCode === TorrentStatus.DOWNLOADING || statusCode === TorrentStatus.SEEDING || statusCode === TorrentStatus.PAUSED

    fun mimicResume(): Torrent = mimicStatus(if (partDone >= 1) TorrentStatus.SEEDING else TorrentStatus.DOWNLOADING)

    fun mimicPause(): Torrent = mimicStatus(TorrentStatus.PAUSED)

    fun mimicStart(): Torrent = mimicStatus(if (partDone >= 1) TorrentStatus.SEEDING else TorrentStatus.DOWNLOADING)

    fun mimicStop(): Torrent = mimicStatus(TorrentStatus.QUEUED)

    fun mimicChecking(): Torrent = mimicStatus(TorrentStatus.CHECKING)

    fun mimicNewLabel(newLabel: String): Torrent = this.copy(label = newLabel)

    fun mimicNewLocation(newLocation: String): Torrent = this.copy(locationDir = newLocation)

    private fun mimicStatus(newStatus: TorrentStatus): Torrent = this.copy(statusCode = newStatus)

    override fun toString(): String = "($uniqueId) $name"

}
