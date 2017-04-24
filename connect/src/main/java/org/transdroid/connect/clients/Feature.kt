package org.transdroid.connect.clients

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import org.transdroid.connect.model.Torrent
import java.io.InputStream
import kotlin.reflect.KClass

/**
 * Available feature enum which can be implemented by clients. Use [Client.supports] to see if a certain [Client] support a [Feature].
 */
enum class Feature constructor(val type: KClass<*>) {

    VERSION(Version::class),
    LISTING(Listing::class),
    STARTING_STOPPING(StartingStopping::class),
    RESUMING_PAUSING(ResumingPausing::class),
    FORCE_STARTING(ForceStarting::class),
    ADD_BY_FILE(AddByFile::class),
    ADD_BY_URL(AddByUrl::class),
    ADD_BY_MAGNET(AddByMagnet::class);

    interface Version {

        fun clientVersion(): Single<String>

    }

    interface Listing {

        fun torrents(): Flowable<Torrent>

    }

    interface StartingStopping {

        fun start(torrent: Torrent): Single<Torrent>

        fun stop(torrent: Torrent): Single<Torrent>

    }

    interface ResumingPausing {

        fun resume(torrent: Torrent): Single<Torrent>

        fun pause(torrent: Torrent): Single<Torrent>

    }

    interface ForceStarting {

        fun forceStart(torrent: Torrent): Single<Torrent>

    }

    interface AddByFile {

        fun addByFile(file: InputStream): Completable

    }

    interface AddByUrl {

        fun addByUrl(url: String): Completable

    }

    interface AddByMagnet {

        fun addByMagnet(magnet: String): Completable

    }

}
