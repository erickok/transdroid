package org.transdroid.connect.util;

import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.DispatchingAuthenticator;
import com.burgstaller.okhttp.basic.BasicAuthenticator;
import com.burgstaller.okhttp.digest.CachingAuthenticator;
import com.burgstaller.okhttp.digest.DigestAuthenticator;

import org.transdroid.connect.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public final class OkHttpBuilder {

	private final Configuration configuration;

	public OkHttpBuilder(Configuration configuration) {
		this.configuration = configuration;
	}

	public OkHttpClient build() {
		OkHttpClient.Builder okhttp = new OkHttpClient.Builder();

		if (configuration.loggingEnabled()) {
			HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
			loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
			okhttp.addInterceptor(loggingInterceptor);
		}
		if (configuration.credentials() != null) {
			BasicAuthenticator basicAuthenticator = new BasicAuthenticator(configuration.credentials());
			DigestAuthenticator digestAuthenticator = new DigestAuthenticator(configuration.credentials());
			DispatchingAuthenticator authenticator = new DispatchingAuthenticator.Builder()
					.with("digest", digestAuthenticator)
					.with("basic", basicAuthenticator)
					.build();

			Map<String, CachingAuthenticator> authCache = new ConcurrentHashMap<>();
			okhttp.authenticator(new CachingAuthenticatorDecorator(authenticator, authCache));
			okhttp.addInterceptor(new AuthenticationCacheInterceptor(authCache));
		}

		return okhttp.build();
	}

}
