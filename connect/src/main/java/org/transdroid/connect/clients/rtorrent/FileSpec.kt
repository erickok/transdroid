package org.transdroid.connect.clients.rtorrent

data class FileSpec(
        val pathName: String,
        var size: Long,
        var chunksDone: Long,
        var chunksTotal: Long,
        var priority: Long,
        var pathFull: String)