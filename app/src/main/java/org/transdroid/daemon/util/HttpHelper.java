/*
 *	This file is part of Transdroid <http://www.transdroid.org>
 *	
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *	
 */
package org.transdroid.daemon.util;

import android.net.Uri;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;
import org.transdroid.daemon.DaemonException;
import org.transdroid.daemon.DaemonException.ExceptionType;
import org.transdroid.daemon.DaemonSettings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Provides a set of general helper methods that can be used in web-based communication.
 * @author erickok
 */
public class HttpHelper {

	public static final int DEFAULT_CONNECTION_TIMEOUT = 8000;

	/**
	 * The 'User-Agent' name to send to the server
	 */
	public static String userAgent = "Transdroid Torrent Connect";
	/**
	 * HTTP request interceptor to allow for GZip-encoded data transfer
	 */
	public static HttpRequestInterceptor gzipRequestInterceptor = new HttpRequestInterceptor() {
		public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
			if (!request.containsHeader("Accept-Encoding")) {
				request.addHeader("Accept-Encoding", "gzip");
			}
		}
	};
	/**
	 * HTTP response interceptor that decodes GZipped data
	 */
	public static HttpResponseInterceptor gzipResponseInterceptor = new HttpResponseInterceptor() {
		public void process(final HttpResponse response, final HttpContext context) throws HttpException, IOException {
			HttpEntity entity = response.getEntity();
			Header ceheader = entity.getContentEncoding();
			if (ceheader != null) {
				HeaderElement[] codecs = ceheader.getElements();
				for (HeaderElement codec : codecs) {

					if (codec.getName().equalsIgnoreCase("gzip")) {
						response.setEntity(new GzipDecompressingEntity(response.getEntity()));
						return;
					}
				}
			}
		}

	};

	/**
	 * Creates a standard Apache HttpClient that is thread safe, supports different SSL auth methods and basic
	 * authentication
	 * @param settings The server settings to adhere
	 * @return An HttpClient that should be stored locally and reused for every new request
	 * @throws DaemonException Thrown when information (such as username/password) is missing
	 */
	public static DefaultHttpClient createStandardHttpClient(DaemonSettings settings, boolean userBasicAuth)
			throws DaemonException {
		return createStandardHttpClient(userBasicAuth && settings.shouldUseAuthentication(), settings.getUsername(),
				settings.getPassword(), settings.getSslTrustAll(), settings.getSslTrustKey(),
				settings.getTimeoutInMilliseconds(), settings.getAddress(), settings.getPort());
	}

	/**
	 * Creates a standard Apache HttpClient that is thread safe, supports different SSL auth methods and basic
	 * authentication
	 * @param sslTrustAll Whether to trust all SSL certificates
	 * @param sslTrustKey A specific SSL key to accept exclusively
	 * @param timeout The connection timeout for all requests
	 * @param authAddress The authentication domain address
	 * @param authPort The authentication domain port number
	 * @return An HttpClient that should be stored locally and reused for every new request
	 * @throws DaemonException Thrown when information (such as username/password) is missing
	 */
	public static DefaultHttpClient createStandardHttpClient(boolean userBasicAuth, String username, String password,
															 boolean sslTrustAll, String sslTrustKey, int timeout,
															 String authAddress, int authPort) throws DaemonException {

		// Register http and https sockets
		SchemeRegistry registry = new SchemeRegistry();
		SocketFactory httpsSocketFactory;
		if (sslTrustKey != null && sslTrustKey.length() != 0) {
			httpsSocketFactory = new TlsSniSocketFactory(sslTrustKey);
		} else if (sslTrustAll) {
			httpsSocketFactory = new TlsSniSocketFactory(true);
		} else {
			httpsSocketFactory = new TlsSniSocketFactory();
		}
		registry.register(new Scheme("http", new PlainSocketFactory(), 80));
		registry.register(new Scheme("https", httpsSocketFactory, 443));

		// Standard parameters
		HttpParams httpparams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpparams, timeout);
		HttpConnectionParams.setSoTimeout(httpparams, timeout);
		if (userAgent != null) {
			HttpProtocolParams.setUserAgent(httpparams, userAgent);
		}

		DefaultHttpClient httpclient =
				new DefaultHttpClient(new ThreadSafeClientConnManager(httpparams, registry), httpparams);

		// Authentication credentials
		if (userBasicAuth) {
			if (username == null || password == null) {
				throw new DaemonException(ExceptionType.AuthenticationFailure,
						"No username or password was provided while we had authentication enabled");
			}
			httpclient.getCredentialsProvider()
					.setCredentials(new AuthScope(authAddress, authPort, AuthScope.ANY_REALM),
							new UsernamePasswordCredentials(username, password));
		}

		return httpclient;

	}

	/*
	 * To convert the InputStream to String we use the BufferedReader.readLine() method. We iterate until the
	 * BufferedReader return null which means there's no more data to read. Each line will appended to a StringBuilder
	 * and returned as String.
	 *
	 * Taken from http://senior.ceng.metu.edu.tr/2009/praeda/2009/01/11/a-simple-restful-client-at-android/
	 */
	public static String convertStreamToString(InputStream is, String encoding) throws UnsupportedEncodingException {
		InputStreamReader isr;
		if (encoding != null) {
			isr = new InputStreamReader(is, encoding);
		} else {
			isr = new InputStreamReader(is);
		}
		BufferedReader reader = new BufferedReader(isr);
		StringBuilder sb = new StringBuilder();

		String line;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line).append("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	public static String convertStreamToString(InputStream is) {
		try {
			return convertStreamToString(is, null);
		} catch (UnsupportedEncodingException e) {
			// Since this is going to use the default encoding, it is never going to crash on an
			// UnsupportedEncodingException
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Parses the individual parameters from a textual cookie representation-like string and returns them in an unsorted
	 * map. Inspired by Android's (API level 11+) getQueryParameterNames(Uri).
	 * @param raw A string of the form key1=value1;key2=value
	 * @return An unsorted, unmodifiable map of pairs of string keys and string values
	 */
	public static Map<String, String> parseCookiePairs(String raw) {

		Map<String, String> pairs = new HashMap<String, String>();
		int start = 0;
		do {
			int next = raw.indexOf(';', start);
			int end = (next == -1) ? raw.length() : next;
			int separator = raw.indexOf('=', start);
			if (separator > end || separator == -1) {
				separator = end;
			}

			String name = raw.substring(start, separator);
			String value = raw.substring(separator + 1, end);
			pairs.put(Uri.decode(name), Uri.decode(value));

			start = end + 1;
		} while (start < raw.length());

		return Collections.unmodifiableMap(pairs);

	}

	/**
	 * HTTP entity wrapper to decompress GZipped HTTP responses
	 */
	private static class GzipDecompressingEntity extends HttpEntityWrapper {

		public GzipDecompressingEntity(final HttpEntity entity) {
			super(entity);
		}

		@Override
		public InputStream getContent() throws IOException, IllegalStateException {

			// the wrapped entity's getContent() decides about repeatability
			InputStream wrappedin = wrappedEntity.getContent();

			return new GZIPInputStream(wrappedin);
		}

		@Override
		public long getContentLength() {
			// length of ungzipped content is not known
			return -1;
		}

	}

}