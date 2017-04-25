package org.transdroid.connect.model

data class TorrentDetails(
        val trackers: List<String>,
        var errors: List<String>)