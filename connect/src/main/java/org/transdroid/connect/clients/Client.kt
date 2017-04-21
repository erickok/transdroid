package org.transdroid.connect.clients

import org.transdroid.connect.Configuration
import org.transdroid.connect.clients.rtorrent.Rtorrent
import org.transdroid.connect.clients.transmission.Transmission
import kotlin.reflect.KClass

/**
 * Support clients enum, allowing you to create instances (given a configuration) and query for feature support.
 */
enum class Client constructor(internal val type: KClass<*>) {

    RTORRENT(Rtorrent::class) {
        override fun create(configuration: Configuration): Rtorrent {
            return Rtorrent(configuration)
        }
    },
    TRANSMISSION(Transmission::class) {
        override fun create(configuration: Configuration): Transmission {
            return Transmission()
        }
    };

    internal abstract fun create(configuration: Configuration): Any

    fun createClient(configuration: Configuration): ClientSpec {
        return ClientDelegate(configuration.client, create(configuration))
    }

    fun supports(feature: Feature): Boolean {
        return feature.type.java.isAssignableFrom(type.java)
    }

}
