package org.transdroid.connect.mock

import org.transdroid.connect.model.Torrent
import org.transdroid.connect.model.TorrentStatus
import java.io.File
import java.util.*

object MockTorrent {

    val torrentUrl = "http://releases.ubuntu.com/17.04/ubuntu-17.04-desktop-amd64.iso.torrent"
    val magnetUrl = "http://torrent.ubuntu.com:6969/file?info_hash=%04%03%FBG%28%BDx%8F%BC%B6%7E%87%D6%FE%B2A%EF8%C7Z"
    val torrentFile = File("connect/src/test/resources/test/ubuntu.torrent").inputStream()

    val downloading = Torrent(0, "59066769B9AD42DA2E508611C33D7C4480B3857B", "ubuntu-17.04-desktop-amd64.iso", TorrentStatus.DOWNLOADING,
            "/downloads/", 1000000, 200000, 20, 20, 2, 50, null, 804519936, 160903987, 1609039872, 0.5F, 0.8F, "distros", Date(1492681983),
            null, null)
    val seeding = Torrent(0, "59066769B9AD42DA2E508611C33D7C4480B3857B", "ubuntu-17.04-desktop-amd64.iso", TorrentStatus.SEEDING,
            "/downloads/", 0, 1000000, 0, 24, 10, 50, null, 1609039872, 2609039872, 1609039872, 1F, 1F, "distros", Date(1492681983),
            Date(1492781983), null)
    val error = Torrent(0, "59066769B9AD42DA2E508611C33D7C4480B3857B", "ubuntu-17.04-desktop-amd64.iso", TorrentStatus.ERROR,
            "/downloads/", 0, 1000000, 0, 0, 0, 0, null, 1609039872, 2609039872, 1609039872, 1F, 1F, "distros", Date(1492681983),
            Date(1492781983), "tracker error")

}