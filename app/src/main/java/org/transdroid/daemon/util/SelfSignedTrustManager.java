/*
 * Copyright 2010-2013 Eric Kok et al.
 *
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transdroid.daemon.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

public class SelfSignedTrustManager implements X509TrustManager {

	private static final X509Certificate[] acceptedIssuers = new X509Certificate[]{};

	private String certKey = null;

	public SelfSignedTrustManager(String certKey) {
		super();
		this.certKey = certKey;
	}

	// Thank you: http://stackoverflow.com/questions/1270703/how-to-retrieve-compute-an-x509-certificates-thumbprint-in-java
	private static String getThumbPrint(X509Certificate cert)
			throws NoSuchAlgorithmException, CertificateEncodingException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] der = cert.getEncoded();
		md.update(der);
		byte[] digest = md.digest();
		return hexify(digest);
	}

	private static String hexify(byte bytes[]) {

		char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
		StringBuffer buf = new StringBuffer(bytes.length * 2);
		for (byte aByte : bytes) {
			buf.append(hexDigits[(aByte & 0xf0) >> 4]);
			buf.append(hexDigits[aByte & 0x0f]);
		}
		return buf.toString();

	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		if (this.certKey == null) {
			throw new CertificateException("Requires a non-null certificate key in SHA-1 format to match.");
		}

		// Qe have a certKey defined. We should now examine the one we got from the server.
		// They match? All is good. They don't, throw an exception.
		String ourKey = this.certKey.replaceAll("[^a-fA-F0-9]+", "");
		try {
			// Assume self-signed root is okay?
			X509Certificate sslCert = chain[0];
			String thumbprint = SelfSignedTrustManager.getThumbPrint(sslCert);
			if (ourKey.equalsIgnoreCase(thumbprint)) {
				return;
			}

			CertificateException certificateException =
					new CertificateException("Certificate key [" + thumbprint + "] doesn't match expected value.");
			//Log.e(SelfSignedTrustManager.class.getSimpleName(), certificateException.toString());
			throw certificateException;

		} catch (NoSuchAlgorithmException e) {
			throw new CertificateException("Unable to check self-signed cert, unknown algorithm. " + e.toString());
		}

	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return acceptedIssuers;
	}

}
