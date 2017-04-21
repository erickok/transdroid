package org.transdroid.connect.clients.rtorrent

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import nl.nl2312.xmlrpc.Nothing
import nl.nl2312.xmlrpc.XmlRpcConverterFactory
import org.transdroid.connect.Configuration
import org.transdroid.connect.clients.Feature
import org.transdroid.connect.model.Torrent
import org.transdroid.connect.model.TorrentStatus
import org.transdroid.connect.util.OkHttpBuilder
import org.transdroid.connect.util.flatten
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.util.*

class Rtorrent(private val configuration: Configuration) :
        Feature.Version,
        Feature.Listing,
        Feature.StartingStopping,
        Feature.ResumingPausing,
        //Feature.AddByFile,
        Feature.AddByUrl,
        Feature.AddByMagnet {

    private val service: Service = Retrofit.Builder()
            .baseUrl(configuration.baseUrl)
            .client(OkHttpBuilder.build(configuration))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(XmlRpcConverterFactory.builder()
                    .addArrayDeserializer(TorrentSpec::class.java) { arrayValues ->
                        TorrentSpec(
                                arrayValues.asString(0),
                                arrayValues.asString(1),
                                arrayValues.asLong(2),
                                arrayValues.asLong(3),
                                arrayValues.asLong(4),
                                arrayValues.asLong(5),
                                arrayValues.asLong(6),
                                arrayValues.asLong(7),
                                arrayValues.asLong(8),
                                arrayValues.asLong(9),
                                arrayValues.asLong(10),
                                arrayValues.asLong(11),
                                arrayValues.asLong(12),
                                arrayValues.asLong(13),
                                arrayValues.asLong(14),
                                arrayValues.asString(15),
                                arrayValues.asString(16),
                                arrayValues.asString(17),
                                arrayValues.asString(18),
                                arrayValues.asString(19),
                                arrayValues.asString(20),
                                arrayValues.asLong(21),
                                arrayValues.asLong(22)

                        )
                    }
                    .create())
            .build().create(Service::class.java)

    override fun clientVersion(): Single<String> {
        return service.clientVersion(configuration.endpoint, Nothing.NOTHING)
                .cache() // Cached, as it is often used but 'never' changes
    }

    private fun Single<String>.asVersionInt(): Single<Int> {
        return this.map {
            if (it == null) 10000 else {
                val versionParts = it.split(".")
                versionParts[0].toInt() * 10000 + versionParts[1].toInt() * 100 + versionParts[2].toInt()
            }
        }
    }

    override fun torrents(): Flowable<Torrent> {
        return service.torrents(
                configuration.endpoint,
                "",
                "main",
                "d.hash=",
                "d.name=",
                "d.state=",
                "d.down.rate=",
                "d.up.rate=",
                "d.peers_connected=",
                "d.peers_not_connected=",
                "d.bytes_done=",
                "d.up.total=",
                "d.size_bytes=",
                "d.left_bytes=",
                "d.creation_date=",
                "d.complete=",
                "d.is_active=",
                "d.is_hash_checking=",
                "d.base_path=",
                "d.base_filename=",
                "d.message=",
                "d.custom=addtime",
                "d.custom=seedingtime",
                "d.custom1=",
                "d.peers_complete=",
                "d.peers_accounted=")
                .flatten()
                .map { (hash, name, state, downloadRate, uploadRate, peersConnected, peersNotConnected, bytesDone, bytesUploaded, bytesTotal, bytesleft, timeCreated, isComplete, isActive, isHashChecking, basePath, baseFilename, errorMessage, timeAdded, timeFinished, label, seedersConnected, leechersConnected) ->
                    Torrent(
                            hash.hashCode().toLong(), hash, name,
                            torrentStatus(state, isComplete, isActive, isHashChecking),
                            basePath?.substring(0, basePath.indexOf(baseFilename.orEmpty())),
                            downloadRate.toInt(),
                            uploadRate.toInt(),
                            seedersConnected.toInt(),
                            (peersConnected + peersNotConnected).toInt(),
                            leechersConnected.toInt(),
                            (peersConnected + peersNotConnected).toInt(),
                            if (downloadRate > 0) bytesleft / downloadRate else null,
                            bytesDone, bytesUploaded, bytesTotal,
                            (bytesDone / bytesTotal).toFloat(),
                            null,
                            label,
                            torrentTimeAdded(timeAdded, timeCreated),
                            torrentTimeFinished(timeFinished), errorMessage
                    )
                }
    }

    override fun start(torrent: Torrent): Single<Torrent> {
        return service.start(
                configuration.endpoint,
                torrent.uniqueId).toSingle { torrent.mimicStart() }
    }

    override fun stop(torrent: Torrent): Single<Torrent> {
        return service.stop(
                configuration.endpoint,
                torrent.uniqueId).toSingle { torrent.mimicStop() }
    }

    override fun addByUrl(url: String): Completable {
        return clientVersion().asVersionInt().flatMapCompletable { integer ->
            if (integer > 904) {
                service.loadStart(configuration.endpoint, "", url)
            } else {
                service.loadStart(configuration.endpoint, url)
            }
        }
    }

    override fun addByMagnet(magnet: String): Completable {
        return clientVersion().asVersionInt().flatMapCompletable { integer ->
            if (integer > 904) {
                service.loadStart(configuration.endpoint, "", magnet)
            } else {
                service.loadStart(configuration.endpoint, magnet)
            }
        }
    }

    private fun torrentStatus(state: Long, complete: Long, active: Long, checking: Long): TorrentStatus {
        if (state == 0L) {
            return TorrentStatus.QUEUED
        } else if (active == 1L) {
            if (complete == 1L) {
                return TorrentStatus.SEEDING
            } else {
                return TorrentStatus.DOWNLOADING
            }
        } else if (checking == 1L) {
            return TorrentStatus.CHECKING
        } else {
            return TorrentStatus.PAUSED
        }
    }

    private fun torrentTimeAdded(timeAdded: String?, timeCreated: Long): Date =
            if (timeAdded.isNullOrBlank()) Date(timeCreated * 1000L) else Date(timeAdded!!.trim().toLong() * 1000L)

    private fun torrentTimeFinished(timeFinished: String?): Date? =
            if (timeFinished.isNullOrBlank()) null else Date(timeFinished!!.trim().toLong() * 1000L)

}
