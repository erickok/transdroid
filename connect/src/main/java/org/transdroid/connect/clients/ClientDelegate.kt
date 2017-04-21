package org.transdroid.connect.clients

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import org.transdroid.connect.model.Torrent
import java.io.InputStream

/**
 * Wraps an actual client implementation by calling through the appropriate method only if it is supported. This allows the final
 * [ClientSpec] API to expose all methods without forcing the individual implementations to implement unsupported featured with a no-op.
 */
internal class ClientDelegate(private val client: Client, private val actual: Any) : ClientSpec {

    override fun torrents(): Flowable<Torrent> {
        if (client.supports(Feature.LISTING))
            return (actual as Feature.Listing).torrents()
        throw UnsupportedFeatureException(client, Feature.LISTING)
    }

    override fun clientVersion(): Single<String> {
        if (client.supports(Feature.VERSION))
            return (actual as Feature.Version).clientVersion()
        throw UnsupportedFeatureException(client, Feature.VERSION)
    }

    override fun start(torrent: Torrent): Single<Torrent> {
        if (client.supports(Feature.STARTING_STOPPING))
            return (actual as Feature.StartingStopping).start(torrent)
        throw UnsupportedFeatureException(client, Feature.STARTING_STOPPING)
    }

    override fun stop(torrent: Torrent): Single<Torrent> {
        if (client.supports(Feature.STARTING_STOPPING))
            return (actual as Feature.StartingStopping).stop(torrent)
        throw UnsupportedFeatureException(client, Feature.STARTING_STOPPING)
    }

    override fun forceStart(torrent: Torrent): Single<Torrent> {
        if (client.supports(Feature.FORCE_STARTING))
            return (actual as Feature.ForceStarting).forceStart(torrent)
        throw UnsupportedFeatureException(client, Feature.FORCE_STARTING)
    }

    override fun addByFile(file: InputStream): Completable {
        if (client.supports(Feature.ADD_BY_FILE))
            return (actual as Feature.AddByFile).addByFile(file)
        throw UnsupportedFeatureException(client, Feature.ADD_BY_FILE)
    }

    override fun addByUrl(url: String): Completable {
        if (client.supports(Feature.ADD_BY_URL))
            return (actual as Feature.AddByUrl).addByUrl(url)
        throw UnsupportedFeatureException(client, Feature.ADD_BY_URL)
    }

    override fun addByMagnet(magnet: String): Completable {
        if (client.supports(Feature.ADD_BY_MAGNET))
            return (actual as Feature.AddByMagnet).addByMagnet(magnet)
        throw UnsupportedFeatureException(client, Feature.ADD_BY_MAGNET)
    }

}
