package org.transdroid.connect.model

data class TorrentFile(
        val key: String,
        val name: String,
        val relativePath: String,
        val fullPath: String,
        val totalSize: Long,
        val downloaded: Long,
        val priority: Priority) {

    fun mimicPriority(newPriority: Priority): TorrentFile = this.copy(priority = newPriority)

}