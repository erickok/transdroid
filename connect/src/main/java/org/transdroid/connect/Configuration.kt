package org.transdroid.connect

import com.burgstaller.okhttp.digest.Credentials
import org.transdroid.connect.clients.Client

/**
 * Configuration settings to connect to a torrent client.
 */
data class Configuration(
        val client: Client,
        val baseUrl: String,
        var endpoint: String? = null,
        var credentials: Credentials? = null,
        var loggingEnabled: Boolean = false) {

    fun createClient() = client.createClient(this)

}
