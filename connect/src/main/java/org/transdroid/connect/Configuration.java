package org.transdroid.connect;

import com.burgstaller.okhttp.digest.Credentials;

import org.transdroid.connect.clients.Client;
import org.transdroid.connect.clients.ClientSpec;
import org.transdroid.connect.util.StringUtil;

public final class Configuration {

	private final Client client;
	private final String baseUrl;
	private final String endpoint;
	private final Credentials credentials;
	private final boolean loggingEnabled;

	public Configuration(Client client, String baseUrl, String endpoint, String user, String password, boolean loggingEnabled) {
		this.client = client;
		this.baseUrl = baseUrl;
		this.endpoint = endpoint;
		this.credentials = (!StringUtil.isEmpty(user) && password != null) ? new Credentials(user, password) : null;
		this.loggingEnabled = loggingEnabled;
	}

	public String baseUrl() {
		return baseUrl;
	}

	public String endpoint() {
		return endpoint;
	}

	public boolean loggingEnabled() {
		return loggingEnabled;
	}

	public Credentials credentials() {
		return credentials;
	}

	public ClientSpec create() {
		return client.create(this);
	}

}
