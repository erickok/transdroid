package org.transdroid.connect;

import com.burgstaller.okhttp.digest.Credentials;

import org.transdroid.connect.clients.Client;
import org.transdroid.connect.clients.ClientSpec;

/**
 * Configuration settings to connect to a torrent client.
 */
public final class Configuration {

	private final Client client;
	private final String baseUrl;
	private String endpoint;
	private Credentials credentials;
	private boolean loggingEnabled;

	public static class Builder {

		private final Client client;
		private String baseUrl;
		private String endpoint;
		private Credentials credentials;
		private boolean loggingEnabled;

		public Builder(Client client) {
			this.client = client;
		}

		public Builder baseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
			return this;
		}

		public Builder endpoint(String endpoint) {
			this.endpoint = endpoint;
			return this;
		}

		public Builder credentials(String user, String password) {
			this.credentials = new Credentials(user, password);
			return this;
		}

		public Builder loggingEnabled(boolean loggingEnabled) {
			this.loggingEnabled = loggingEnabled;
			return this;
		}

		public Configuration build() {
			Configuration configuration = new Configuration(client, baseUrl);
			configuration.endpoint = this.endpoint;
			configuration.credentials = this.credentials;
			configuration.loggingEnabled = this.loggingEnabled;
			return configuration;
		}

	}

	private Configuration(Client client, String baseUrl) {
		this.client = client;
		this.baseUrl = baseUrl;
	}

	public Client client() {
		return client;
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

	public ClientSpec createClient() {
		return client.createClient(this);
	}

}
