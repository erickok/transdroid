package org.transdroid.connect.clients;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Thrown when trying to call into a client method for a feature which the client does not support.
 */
public class UnsupportedFeatureException extends NotImplementedException {

	private final Client client;
	private final Feature feature;

	UnsupportedFeatureException(Client client, Feature feature) {
		this.client = client;
		this.feature = feature;
	}

	public String getMessage() {
		return client.name() + " does not support " + feature.name();
	}

}
