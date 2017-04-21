package org.transdroid.connect.model

enum class TorrentStatus {
    WAITING,
    CHECKING,
    DOWNLOADING,
    SEEDING,
    PAUSED,
    QUEUED,
    ERROR,
    UNKNOWN
}
