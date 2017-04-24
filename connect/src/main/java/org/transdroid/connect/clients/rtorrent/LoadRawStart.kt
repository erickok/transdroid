package org.transdroid.connect.clients.rtorrent

data class LoadRawStart(
        val endpoint: String,
        val bytes: ByteArray
)