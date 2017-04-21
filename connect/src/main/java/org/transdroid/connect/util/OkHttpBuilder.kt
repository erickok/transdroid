package org.transdroid.connect.util

import com.burgstaller.okhttp.AuthenticationCacheInterceptor
import com.burgstaller.okhttp.CachingAuthenticatorDecorator
import com.burgstaller.okhttp.DispatchingAuthenticator
import com.burgstaller.okhttp.basic.BasicAuthenticator
import com.burgstaller.okhttp.digest.CachingAuthenticator
import com.burgstaller.okhttp.digest.DigestAuthenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.transdroid.connect.Configuration
import java.util.concurrent.ConcurrentHashMap

object OkHttpBuilder {

    fun build(configuration: Configuration): OkHttpClient {
        val okhttp = OkHttpClient.Builder()

        if (configuration.loggingEnabled) {
            okhttp.addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
        }
        if (configuration.credentials != null) {
            val authenticator = DispatchingAuthenticator.Builder()
                    .with("digest", DigestAuthenticator(configuration.credentials))
                    .with("basic", BasicAuthenticator(configuration.credentials))
                    .build()
            val authCache = ConcurrentHashMap<String, CachingAuthenticator>()
            okhttp.authenticator(CachingAuthenticatorDecorator(authenticator, authCache))
            okhttp.addInterceptor(AuthenticationCacheInterceptor(authCache))
        }

        return okhttp.build()
    }

}
