package org.transdroid.connect.clients

/**
 * Thrown when trying to call into a client method for a feature which the client does not support.
 */
class UnsupportedFeatureException internal constructor(
        private val client: Client,
        private val feature: Feature) : RuntimeException() {

    override val message: String?
        get() = client.name + " does not support " + feature.name

}
